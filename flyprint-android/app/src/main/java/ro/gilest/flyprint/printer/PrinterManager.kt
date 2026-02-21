package ro.gilest.flyprint.printer

/**
 * Abstract interface for thermal printer operations.
 *
 * Implementations wrap vendor-specific SDKs (Brother, Zebra, etc.).
 */
interface PrinterManager {
    /** Discover available printers via Bluetooth/WiFi. */
    fun discoverPrinters(): List<PrinterInfo>

    /** Connect to a specific printer. Returns true on success. */
    fun connect(printer: PrinterInfo): Boolean

    /** Get current printer status string. */
    fun getStatus(): String

    /** Print image data. Returns true on success. */
    fun print(imageData: ByteArray, copies: Int): Boolean

    /** Disconnect from current printer. */
    fun disconnect()
}

/**
 * Exception thrown by printer operations.
 */
class PrinterException(message: String, cause: Throwable? = null) : Exception(message, cause)
