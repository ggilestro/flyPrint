package ro.gilest.flyprint.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ro.gilest.flyprint.config.AppConfig
import ro.gilest.flyprint.service.FlyPrintService
import ro.gilest.flyprint.ui.theme.StatusOffline
import ro.gilest.flyprint.ui.theme.StatusOnline

/**
 * Status dashboard showing service state, printer, and job stats.
 */
@Composable
fun StatusScreen(
    config: AppConfig,
    onReconfigure: () -> Unit
) {
    val context = LocalContext.current

    var isServiceRunning by remember { mutableStateOf(config.serviceEnabled) }
    // Refresh heartbeat timestamp every second for relative time display
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
            isServiceRunning = config.serviceEnabled
        }
    }

    val lastHeartbeat = config.lastHeartbeat
    val isOnline = isServiceRunning && lastHeartbeat > 0 &&
            (System.currentTimeMillis() - lastHeartbeat) < 60_000

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with status indicator
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Print,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("FlyPrint", style = MaterialTheme.typography.headlineLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) StatusOnline else StatusOffline)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isOnline) StatusOnline else StatusOffline
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Service control
        if (isServiceRunning) {
            Button(
                onClick = {
                    FlyPrintService.stop(context)
                    isServiceRunning = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Service")
            }
        } else {
            Button(
                onClick = {
                    FlyPrintService.start(context)
                    isServiceRunning = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Service")
            }
        }

        Spacer(Modifier.height(24.dp))

        // Info cards
        StatusCard(
            icon = Icons.Default.Cloud,
            title = "Server",
            value = config.serverUrl.ifBlank { "Not configured" }
        )

        Spacer(Modifier.height(12.dp))

        StatusCard(
            icon = Icons.Default.Print,
            title = "Printer",
            value = config.printerName.ifBlank { "Not selected" }
        )

        Spacer(Modifier.height(12.dp))

        val heartbeatText = if (lastHeartbeat > 0L) {
            @Suppress("DEPRECATION")
            DateUtils.getRelativeTimeSpanString(
                lastHeartbeat,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS
            ).toString()
        } else {
            "Never"
        }
        StatusCard(
            icon = Icons.Default.Cloud,
            title = "Last Heartbeat",
            value = heartbeatText
        )

        Spacer(Modifier.height(12.dp))

        StatusCard(
            icon = Icons.Default.WorkHistory,
            title = "Jobs Processed",
            value = config.jobsProcessed.toString()
        )

        Spacer(Modifier.height(24.dp))

        // Reconfigure button
        OutlinedButton(
            onClick = onReconfigure,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Reconfigure")
        }
    }
}

@Composable
private fun StatusCard(
    icon: ImageVector,
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
