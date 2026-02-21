package ro.gilest.flyprint.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.gilest.flyprint.api.ApiClient
import ro.gilest.flyprint.api.HeartbeatRequest
import ro.gilest.flyprint.config.AppConfig
import ro.gilest.flyprint.printer.BrotherPrinterManager
import ro.gilest.flyprint.printer.PrinterInfo

/**
 * Three-step setup wizard: Server → Printer → Done.
 */
@Composable
fun SetupWizardScreen(
    config: AppConfig,
    onSetupComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var serverUrl by remember { mutableStateOf(config.serverUrl) }
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var connectionStatus by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }

    val printers = remember { mutableStateListOf<PrinterInfo>() }
    var selectedPrinter by remember { mutableStateOf<PrinterInfo?>(null) }
    var isDiscovering by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Icon(
            Icons.Default.Print,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text("FlyPrint Setup", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(4.dp))

        // Step indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val steps = listOf("Server", "Printer", "Done")
            steps.forEachIndexed { i, label ->
                val isActive = i == step
                val isDone = i < step
                Text(
                    text = if (isDone) "✓ $label" else "${i + 1}. $label",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (i < steps.size - 1) {
                    Text(" → ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        when (step) {
            // Step 0: Server configuration
            0 -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Server Connection", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("Server URL") },
                            placeholder = { Text("https://flypush.example.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key") },
                            placeholder = { Text("Paste your agent API key") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        if (connectionStatus.isNotBlank()) {
                            Text(
                                connectionStatus,
                                color = if (connectionStatus.startsWith("✓"))
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isTesting = true
                                    connectionStatus = ""
                                    config.serverUrl = serverUrl
                                    config.apiKey = apiKey
                                    scope.launch {
                                        connectionStatus = testConnection(config)
                                        isTesting = false
                                    }
                                },
                                enabled = serverUrl.isNotBlank() && apiKey.isNotBlank() && !isTesting
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text("Test Connection")
                            }

                            Button(
                                onClick = {
                                    config.serverUrl = serverUrl
                                    config.apiKey = apiKey
                                    step = 1
                                },
                                enabled = serverUrl.isNotBlank() && apiKey.isNotBlank()
                            ) {
                                Text("Next →")
                            }
                        }
                    }
                }
            }

            // Step 1: Printer selection
            1 -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Printer", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                isDiscovering = true
                                printers.clear()
                                scope.launch {
                                    val found = withContext(Dispatchers.IO) {
                                        BrotherPrinterManager(context).discoverPrinters()
                                    }
                                    printers.addAll(found)
                                    isDiscovering = false
                                }
                            },
                            enabled = !isDiscovering,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (isDiscovering) Icons.Default.Wifi else Icons.Default.Search,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isDiscovering) "Discovering…" else "Discover Printers")
                        }

                        Spacer(Modifier.height(12.dp))

                        if (printers.isEmpty() && !isDiscovering) {
                            Text(
                                "No printers found. Make sure your printer is on and Bluetooth is enabled.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        printers.forEach { printer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPrinter == printer,
                                    onClick = { selectedPrinter = printer }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(printer.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "${printer.connectionType} — ${printer.address}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(onClick = { step = 0 }) {
                                Text("← Back")
                            }
                            Button(
                                onClick = {
                                    selectedPrinter?.let {
                                        config.printerName = it.name
                                        config.printerAddress = it.address
                                    }
                                    step = 2
                                },
                                enabled = selectedPrinter != null
                            ) {
                                Text("Next →")
                            }
                        }
                    }
                }
            }

            // Step 2: Confirmation
            2 -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Ready!", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))

                        Text("Server: ${config.serverUrl}", style = MaterialTheme.typography.bodyLarge)
                        Text("Printer: ${config.printerName}", style = MaterialTheme.typography.bodyLarge)

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = onSetupComplete,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save & Start Service")
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(onClick = { step = 1 }) {
                            Text("← Back")
                        }
                    }
                }
            }
        }
    }
}

/** Test server connection — returns status string. */
private suspend fun testConnection(config: AppConfig): String {
    return withContext(Dispatchers.IO) {
        try {
            val api = ApiClient.create(config)
            val resp = api.sendHeartbeat(
                HeartbeatRequest(printerName = "android-setup", printerStatus = "testing")
            )
            when {
                resp.isSuccessful -> "✓ Connected to server"
                resp.code() == 401 -> "✗ Invalid API key"
                resp.code() == 404 -> "✗ Agent not found on server"
                else -> "✗ Server error: ${resp.code()}"
            }
        } catch (e: Exception) {
            "✗ Connection failed: ${e.message}"
        }
    }
}
