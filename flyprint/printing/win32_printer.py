"""Windows printing backend using SumatraPDF (preferred) or win32print/ShellExecute."""

import logging
import os
import shutil
import subprocess
import tempfile
from pathlib import Path

from flyprint.printing.base import PrinterError

logger = logging.getLogger(__name__)

# Try to import win32 modules
try:
    import win32api
    import win32print

    WIN32_AVAILABLE = True
except ImportError:
    WIN32_AVAILABLE = False
    logger.debug("pywin32 not available - Windows printing disabled")


def _find_sumatra() -> str | None:
    """Find SumatraPDF executable.

    Checks common install locations and PATH.

    Returns:
        str | None: Path to SumatraPDF.exe or None if not found.
    """
    # Check PATH first
    sumatra = shutil.which("SumatraPDF") or shutil.which("SumatraPDF.exe")
    if sumatra:
        return sumatra

    # Check common install locations
    candidates = []
    for env_var in ("LOCALAPPDATA", "PROGRAMFILES", "PROGRAMFILES(X86)"):
        base = os.environ.get(env_var)
        if base:
            candidates.append(Path(base) / "SumatraPDF" / "SumatraPDF.exe")

    # Also check next to our own executable (bundled with installer)
    import sys

    if getattr(sys, "frozen", False):
        candidates.append(Path(sys.executable).parent / "SumatraPDF" / "SumatraPDF.exe")

    for path in candidates:
        if path.is_file():
            return str(path)

    return None


class Win32Printer:
    """Windows printing backend using SumatraPDF CLI or win32print API."""

    def __init__(self, printer_name: str | None = None):
        """Initialize Windows printer.

        Args:
            printer_name: Printer name (None = default printer).
        """
        self.printer_name = printer_name
        self._sumatra_path = _find_sumatra()
        if self._sumatra_path:
            logger.info(f"SumatraPDF found: {self._sumatra_path}")
        else:
            logger.debug("SumatraPDF not found, will use ShellExecute fallback")

    @property
    def is_available(self) -> bool:
        """Check if Windows printing is available.

        Returns:
            bool: True if win32print is importable.
        """
        return WIN32_AVAILABLE

    def get_printers(self) -> list[dict]:
        """Get list of available printers.

        Returns:
            list[dict]: List of printer info dicts.
        """
        if not WIN32_AVAILABLE:
            return []

        try:
            default = self.get_default_printer()
            # Reason: Flag 2 = PRINTER_ENUM_LOCAL, Flag 4 = PRINTER_ENUM_CONNECTIONS
            printers = win32print.EnumPrinters(
                win32print.PRINTER_ENUM_LOCAL | win32print.PRINTER_ENUM_CONNECTIONS
            )
            result = []
            for _flags, _description, name, comment in printers:
                result.append(
                    {
                        "name": name,
                        "state": 3,  # Map to CUPS-like idle state
                        "state_message": comment or "",
                        "is_default": name == default,
                    }
                )
            return result
        except Exception as e:
            logger.error(f"Error enumerating printers: {e}")
            return []

    def get_default_printer(self) -> str | None:
        """Get the default printer name.

        Returns:
            str | None: Default printer name or None.
        """
        if not WIN32_AVAILABLE:
            return None

        try:
            return win32print.GetDefaultPrinter()
        except Exception as e:
            logger.error(f"Error getting default printer: {e}")
            return None

    def get_printer_status(self, printer_name: str | None = None) -> str:
        """Get status of a specific printer.

        Args:
            printer_name: Printer name (None = configured or default).

        Returns:
            str: Status string ('ready', 'offline', 'busy', 'unknown').
        """
        if not WIN32_AVAILABLE:
            return "unknown"

        name = printer_name or self.printer_name or self.get_default_printer()
        if not name:
            return "unknown"

        try:
            handle = win32print.OpenPrinter(name)
            try:
                info = win32print.GetPrinter(handle, 2)
                status = info["Status"]
                if status == 0:
                    return "ready"
                # Reason: Common win32print status bits
                elif status & 0x00000400:  # PRINTER_STATUS_OFFLINE
                    return "offline"
                elif status & 0x00000004:  # PRINTER_STATUS_PRINTING
                    return "busy"
                return "unknown"
            finally:
                win32print.ClosePrinter(handle)
        except Exception as e:
            logger.error(f"Error getting printer status: {e}")
            return "unknown"

    def _print_pdf_sumatra(
        self,
        temp_path: str,
        printer_name: str | None,
        copies: int,
        page_size: str,
    ) -> bool:
        """Print a PDF using SumatraPDF CLI.

        Args:
            temp_path: Path to the temporary PDF file.
            printer_name: Target printer name (None = default).
            copies: Number of copies.
            page_size: CUPS-style page size string.

        Returns:
            bool: True if print succeeded.

        Raises:
            PrinterError: If SumatraPDF exits with an error.
        """
        target = printer_name or self.printer_name or self.get_default_printer() or "default"

        # Build print-settings string
        settings_parts = []
        if page_size:
            settings_parts.append(f"paper={page_size}")
        if copies > 1:
            settings_parts.append(f"copies={copies}")

        cmd = [
            self._sumatra_path,
            "-print-to",
            target,
        ]
        if settings_parts:
            cmd.extend(["-print-settings", ",".join(settings_parts)])
        cmd.append(temp_path)

        logger.debug(f"SumatraPDF command: {cmd}")

        # Reason: SumatraPDF -print-to waits until printing completes, no race condition
        result = subprocess.run(cmd, capture_output=True, timeout=60)

        if result.returncode != 0:
            stderr = result.stderr.decode("utf-8", errors="replace").strip()
            raise PrinterError(f"SumatraPDF print failed (exit {result.returncode}): {stderr}")

        logger.info(f"SumatraPDF printed to {target} ({copies} copies)")
        return True

    def _print_via_shellexecute(
        self,
        temp_path: str,
        printer_name: str | None,
        copies: int,
        verb: str = "printto",
    ) -> bool:
        """Print a file using win32api.ShellExecute.

        Args:
            temp_path: Path to the temporary file.
            printer_name: Target printer name (None = default).
            copies: Number of copies.
            verb: Shell verb ('print' or 'printto').

        Returns:
            bool: True if print job was submitted.

        Raises:
            PrinterError: If ShellExecute fails.
        """
        if not WIN32_AVAILABLE:
            raise PrinterError("pywin32 is not installed")

        name = printer_name or self.printer_name or self.get_default_printer()

        try:
            for _ in range(copies):
                if name:
                    win32api.ShellExecute(0, "printto", temp_path, f'"{name}"', ".", 0)
                else:
                    win32api.ShellExecute(0, "print", temp_path, None, ".", 0)
            logger.info(
                f"ShellExecute print job submitted to {name or 'default'} ({copies} copies)"
            )
            return True
        except Exception as e:
            raise PrinterError(f"Windows print failed: {e}") from e

    def print_pdf(
        self,
        pdf_data: bytes,
        title: str = "FlyPrint Label",
        copies: int = 1,
        printer_name: str | None = None,
        orientation: int = 0,
        page_size: str = "w72h154",
    ) -> bool:
        """Print a PDF document.

        Prefers SumatraPDF CLI when available (proper page size control,
        no race condition with temp files). Falls back to ShellExecute.

        Args:
            pdf_data: PDF file contents as bytes.
            title: Print job title.
            copies: Number of copies.
            printer_name: Override printer name.
            orientation: Rotation in degrees (ignored on Windows - handled by driver).
            page_size: CUPS page size (passed to SumatraPDF as paper size).

        Returns:
            bool: True if print job was submitted successfully.

        Raises:
            PrinterError: If printing fails.
        """
        if not WIN32_AVAILABLE and not self._sumatra_path:
            raise PrinterError("No printing backend available (need pywin32 or SumatraPDF)")

        with tempfile.NamedTemporaryFile(suffix=".pdf", delete=False) as f:
            f.write(pdf_data)
            temp_path = f.name

        try:
            if self._sumatra_path:
                return self._print_pdf_sumatra(temp_path, printer_name, copies, page_size)
            else:
                return self._print_via_shellexecute(temp_path, printer_name, copies)
        finally:
            if not self._sumatra_path:
                # Reason: ShellExecute is async; small delay to let the spooler read the file
                import time

                time.sleep(2)
            Path(temp_path).unlink(missing_ok=True)

    def print_png(
        self,
        png_data: bytes,
        title: str = "FlyPrint Label",
        copies: int = 1,
        printer_name: str | None = None,
        page_size: str = "w72h154",
        dpi: int = 300,
    ) -> bool:
        """Print a PNG image using ShellExecute.

        Args:
            png_data: PNG file contents as bytes.
            title: Print job title.
            copies: Number of copies.
            printer_name: Override printer name.
            page_size: Page size (informational on Windows).
            dpi: Image DPI (informational on Windows).

        Returns:
            bool: True if print job was submitted successfully.

        Raises:
            PrinterError: If printing fails.
        """
        if not WIN32_AVAILABLE:
            raise PrinterError("pywin32 is not installed")

        with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as f:
            f.write(png_data)
            temp_path = f.name

        try:
            return self._print_via_shellexecute(temp_path, printer_name, copies)
        finally:
            import time

            time.sleep(2)
            Path(temp_path).unlink(missing_ok=True)
