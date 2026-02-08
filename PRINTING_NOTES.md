# CUPS Printing on Dymo LabelWriter - Hard-Won Knowledge

## The Fundamental Rule

**Use PDF, not PNG, for CUPS printing on Dymo thermal printers.**

PDF embeds 300 DPI content at correct physical dimensions. CUPS rasterizes
the PDF at the printer's native DPI (300 for Dymo), giving full quality.

## Why NOT PNG

CUPS image filter treats 1 pixel = 1 point (1/72 inch) with `scaling=100`.
This means:
- 300 DPI image → 4x too large → prints over multiple labels
- 72 DPI image → correct size BUT too low resolution for barcodes
- Supersampled 72 DPI → correct size, better text, BUT barcodes still unreadable
  (LANCZOS antialiasing introduces gray pixels that confuse barcode readers)

### Failed PNG Approaches

| Approach | Result |
|----------|--------|
| `scaling=100` + 300 DPI image | Image 4x too large |
| `scaling=100` + 72 DPI image | Correct size, blurry barcodes |
| `scaling=100` + supersampled 72 DPI | Correct size, barcodes still unreadable |
| `ppi=300` | Ignored by CUPS image filter |
| `natural-scaling=100` + 300 DPI | Still prints over multiple labels |
| `fit-to-page` | Doesn't respect PageSize on Dymo |
| `scaling=24` (72/300*100) | Prints minuscule (scaling applied after page fitting) |

## What WORKS: PDF with 300 DPI Content

```
Server (pdf_generator.py):
  create_batch_label_pdf(for_print=True):
    1. Render label as PNG at 300 DPI (sharp text, QR, barcodes)
    2. Embed PNG in a portrait PDF at correct physical dimensions
       - Dymo 11352: 72pt x 153pt (= 25.4mm x 54mm)
    3. Return PDF bytes

Agent (agent.py):
  process_job():
    1. Download PDF from /agent/jobs/{id}/pdf
    2. Print via print_pdf() → lp -o PageSize=w72h154 -o scaling=100

CUPS:
  1. Reads page dimensions from PDF (72pt x 153pt)
  2. Rasterizes at printer's native 300 DPI
  3. Result: 300 DPI quality at correct physical size
```

## Do NOT Change

- Agent MUST use PDF endpoint (not PNG image endpoint)
- PDF page dimensions MUST match the physical label (portrait)
- Do NOT try PNG approaches — they fundamentally can't do 300 DPI on Dymo via CUPS
- The `for_print=True` flag on `create_batch_label_pdf` is essential
- The `for_print=False` path is for web preview (landscape PDF)
