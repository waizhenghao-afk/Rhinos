package com.rhinos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhinos.app.ui.navigation.RhinosRoot
import com.rhinos.app.ui.theme.RhinosTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as RhinosApp).container
        setContent {
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
            RhinosTheme(themeMode = settings.themeMode) {
                RhinosRoot(container)
            }
        }
    }
}
