package ro.gilest.flyprint.printer

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Brother SDK stub implementation.
 *
 * Logs all operations until the real Brother Mobile SDK JAR is added.
 * To activate: add the SDK JAR to app/libs/, uncomment the dependency in
 * build.gradle.kts, and replace the stub bodies with real SDK calls.
 */
class BrotherPrinterManager(private val context: Context) : PrinterManager {

    companion object {
        private const val TAG = "BrotherPrinter"
    }

    private var connectedPrinter: PrinterInfo? = null

    override fun discoverPrinters(): List<PrinterInfo> {
        Log.i(TAG, "discoverPrinters() — stub: returning demo printer")
        // TODO: Replace with Brother SDK BluetoothDiscovery
        // val result = mutableListOf<PrinterInfo>()
        // val option = DiscoverOption().apply { ... }
        // DiscoverPrinter.startDiscovery(option) { printer ->
        //     result.add(PrinterInfo(printer.modelName, printer.macAddress, "Bluetooth"))
        // }
        return listOf(
            PrinterInfo("Brother QL-820NWB (stub)", "00:00:00:00:00:00", "Bluetooth")
        )
    }

    override fun connect(printer: PrinterInfo): Boolean {
        Log.i(TAG, "connect(${printer.name} @ ${printer.address}) — stub: simulating success")
        // TODO: Replace with Brother SDK connection
        // val channel = Channel.newBluetoothChannel(printer.address)
        // val printerDriver = PrinterDriver(channel)
        // printerDriver.openChannel()
        connectedPrinter = printer
        return true
    }

    override fun getStatus(): String {
        if (connectedPrinter == null) return "disconnected"
        Log.d(TAG, "getStatus() — stub: returning ready")
        // TODO: Replace with Brother SDK status query
        // val status = printerDriver.getPrinterStatus()
        // return status.toString()
        return "ready"
    }

    override fun print(imageData: ByteArray, copies: Int): Boolean {
        if (connectedPrinter == null) {
            throw PrinterException("Not connected to any printer")
        }

        Log.i(TAG, "print(${imageData.size} bytes, copies=$copies) — stub: simulating print")

        // Write image to temp file (real SDK needs file path)
        val tempFile = File(context.cacheDir, "flyprint_label.png")
        try {
            tempFile.writeBytes(imageData)
            Log.d(TAG, "Temp file written: ${tempFile.absolutePath} (${tempFile.length()} bytes)")

            // TODO: Replace with Brother SDK print call
            // val settings = PrinterSettings().apply {
            //     printerModel = PrinterModel.QL_820NWB
            //     port = Port.BLUETOOTH
            //     macAddress = connectedPrinter!!.address
            //     paperSize = PaperSize.CUSTOM
            //     customPaperWidth = 54
            //     isAutoCut = true
            //     isCutAtEnd = true
            // }
            // val printerDriver = PrinterDriver(settings)
            // repeat(copies) {
            //     printerDriver.printImage(tempFile.absolutePath)
            // }

            Log.i(TAG, "Print completed (stub) — $copies copies of ${imageData.size} bytes")
            return true
        } finally {
            tempFile.delete()
        }
    }

    override fun disconnect() {
        Log.i(TAG, "disconnect() — stub")
        // TODO: Replace with Brother SDK disconnect
        // printerDriver.closeChannel()
        connectedPrinter = null
    }
}
