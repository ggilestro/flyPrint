package ro.gilest.flyprint.printer

/**
 * Discovered printer descriptor.
 */
data class PrinterInfo(
    val name: String,
    val address: String,
    val connectionType: String  // "Bluetooth" or "WiFi"
)
