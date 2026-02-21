package ro.gilest.flyprint.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ro.gilest.flyprint.ui.theme.FlyPrintTheme

/**
 * Single-activity host for the Compose UI.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlyPrintTheme {
                FlyPrintApp()
            }
        }
    }
}
