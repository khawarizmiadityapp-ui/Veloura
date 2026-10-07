package com.glassroom.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.glassroom.music.ui.GlassroomApp
import com.glassroom.music.ui.theme.GlassroomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GlassroomApplication

        setContent {
            // Observe Theme and Glass Intensity dynamically from DataStore
            val themeMode by app.container.preferencesManager.themeModeFlow.collectAsState(initial = "light_glass")
            val glassIntensity by app.container.preferencesManager.glassIntensityFlow.collectAsState(initial = 0.75f)

            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "dark_glass" -> true
                "light_glass" -> false
                else -> isSystemDark
            }

            // Request Notification Permission for Android 13+ (Required for Dynamic Island / Media Notification)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { /* Permission handled */ }
                )
                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            GlassroomTheme(
                darkTheme = isDark,
                glassIntensity = glassIntensity
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    GlassroomApp(app = app)
                }
            }
        }
    }
}
