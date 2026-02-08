# CUPS Printing on Dymo LabelWriter - Hard-Won Knowledge

## The Fundamental Rule

**CUPS image filter treats 1 pixel = 1 point (1/72 inch) when using `scaling=100`.**

This is the ONLY reliable sizing method for Dymo thermal printers.

## What DOES NOT Work

| Approach | Result |
|----------|--------|
| `scaling=100` + 300 DPI image | Image 4x too large, prints over multiple labels |
| `ppi=300` | Ignored by CUPS image filter |
| `natural-scaling=100` + 300 DPI with DPI metadata | Still prints over multiple labels |
| `fit-to-page` | Prints over multiple labels (doesn't respect PageSize on Dymo) |
| `scaling=24` (72/300*100) | Prints miniscule output |

## What WORKS

`scaling=100` + image at **72 DPI** (pixels = points = correct physical size)

For a Dymo 11352 label (25.4mm x 54mm):
- PageSize: `w72h154` (72pt x 154pt)
- Image must be: 72px x 153px (at 72 DPI)
- With `scaling=100`: maps to 72pt x 153pt = 25.4mm x 54mm = correct!

## The Quality Problem

72 DPI is low resolution. Text looks blurry/blocky.

**Solution: Supersample.** Render at 300 DPI, then downsample to 72 DPI using
LANCZOS resampling. This gives much better quality than rendering directly at
72 DPI because:
- Text and QR codes are rendered with sharp edges at 300 DPI
- LANCZOS downsampling provides high-quality antialiasing
- Final image has correct pixel dimensions for CUPS

## Architecture

```
Server (pdf_generator.py):
  for_print=True → render at 300 DPI → LANCZOS downsample to 72 DPI → save PNG
  for_print=False → render at 300 DPI → save PNG (for web preview)

Agent (cups_printer.py):
  print_png() → lpr -o PageSize=w72h154 -o scaling=100
```

## Do NOT Change

- Do NOT remove `scaling=100` from print_png
- Do NOT add `fit-to-page`, `ppi`, or `natural-scaling` options
- Do NOT send 300 DPI images to CUPS with scaling=100
- The agent image endpoint MUST use `for_print=True`
