package ro.gilest.flyprint.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import ro.gilest.flyprint.config.AppConfig
import ro.gilest.flyprint.service.FlyPrintService

/**
 * Root composable — routes between setup wizard and status dashboard.
 *
 * Shows setup wizard if not configured, otherwise the status screen.
 * Uses simple state-based navigation (no NavHost — only 2 destinations).
 */
@Composable
fun FlyPrintApp() {
    val context = LocalContext.current
    val config = remember { AppConfig(context) }
    var showSetup by remember { mutableStateOf(!config.isConfigured()) }

    if (showSetup) {
        SetupWizardScreen(
            config = config,
            onSetupComplete = {
                FlyPrintService.start(context)
                showSetup = false
            }
        )
    } else {
        StatusScreen(
            config = config,
            onReconfigure = { showSetup = true }
        )
    }
}
