package io.github.project_kaat.gpsdrelay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.project_kaat.gpsdrelay.ui.MainScreen
import io.github.project_kaat.gpsdrelay.ui.theme.GpsdRelayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GpsdRelayTheme() {
                MainScreen((application as gpsdRelay).gpsdRelayDatabase.serverDao)
            }
        }
    }
}