package com.glassroom.music.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassroom.music.data.local.PreferencesManager
import com.glassroom.music.data.remote.NetworkClient
import com.glassroom.music.ui.components.*
import com.glassroom.music.ui.home.SectionHeader
import com.glassroom.music.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val themeMode by preferencesManager.themeModeFlow.collectAsState(initial = "light_glass")
    val backendUrl by preferencesManager.backendUrlFlow.collectAsState(initial = NetworkClient.DEFAULT_BASE_URL)
    val isGapless by preferencesManager.gaplessFlow.collectAsState(initial = true)
    val isAutoplay by preferencesManager.autoplayFlow.collectAsState(initial = true)

    var editingUrl by remember(backendUrl) { mutableStateOf(backendUrl) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    var isCheckingConnection by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        GlassTopBar(
            title = "Settings",
            subtitle = "Preferences & System"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Appearance Section
            item {
                SectionHeader(title = "Appearance", modifier = Modifier.padding(horizontal = 0.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Theme Palette",
                            style = GlassTypography.titleMedium,
                            color = GlassTextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassChip(
                                text = "Light Glass (Default)",
                                isSelected = themeMode == "light_glass",
                                onClick = {
                                    coroutineScope.launch {
                                        preferencesManager.setThemeMode("light_glass")
                                    }
                                }
                            )
                            GlassChip(
                                text = "Auto",
                                isSelected = themeMode == "auto",
                                onClick = {
                                    coroutineScope.launch {
                                        preferencesManager.setThemeMode("auto")
                                    }
                                }
                            )
                            GlassChip(
                                text = "Dark Glass",
                                isSelected = themeMode == "dark_glass",
                                onClick = {
                                    coroutineScope.launch {
                                        preferencesManager.setThemeMode("dark_glass")
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Playback Settings Section
            item {
                SectionHeader(title = "Playback", modifier = Modifier.padding(horizontal = 0.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingToggleRow(
                            icon = Icons.Filled.GraphicEq,
                            title = "Gapless Playback",
                            subtitle = "Smooth transitions between songs",
                            checked = isGapless,
                            onCheckedChange = {
                                coroutineScope.launch { preferencesManager.setGapless(it) }
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        SettingToggleRow(
                            icon = Icons.Filled.PlayCircle,
                            title = "Autoplay Similar Songs",
                            subtitle = "Continue playing when queue ends",
                            checked = isAutoplay,
                            onCheckedChange = {
                                coroutineScope.launch { preferencesManager.setAutoplay(it) }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Backend Server Configuration
            item {
                SectionHeader(title = "Backend Service", modifier = Modifier.padding(horizontal = 0.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Audio Backend API URL",
                            style = GlassTypography.titleMedium,
                            color = GlassTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Runs yt-dlp on your machine/server and streams audio",
                            style = GlassTypography.labelSmall,
                            color = GlassTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x66FFFFFF))
                                .border(1.dp, GlassBorderWhite, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = editingUrl,
                                onValueChange = { editingUrl = it },
                                singleLine = true,
                                textStyle = GlassTypography.bodyLarge.copy(color = GlassTextPrimary),
                                cursorBrush = SolidColor(GlassTextPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassButton(
                                text = "Test Connection",
                                onClick = {
                                    isCheckingConnection = true
                                    coroutineScope.launch {
                                        NetworkClient.updateBaseUrl(editingUrl)
                                        preferencesManager.setBackendUrl(editingUrl)
                                        try {
                                            val health = NetworkClient.getApiService().healthCheck()
                                            if (health.isSuccessful) {
                                                connectionStatus = "Connected to Glassroom Backend"
                                            } else {
                                                connectionStatus = "Server responded with error"
                                            }
                                        } catch (e: Exception) {
                                            connectionStatus = "Offline (Curated Fallback Active)"
                                        }
                                        isCheckingConnection = false
                                    }
                                }
                            )

                            if (connectionStatus != null) {
                                Text(
                                    text = connectionStatus!!,
                                    style = GlassTypography.labelSmall,
                                    color = if (connectionStatus!!.startsWith("Connected")) GlassAccentMint else GlassTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // About Glassroom Section
            item {
                SectionHeader(title = "About", modifier = Modifier.padding(horizontal = 0.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Glassroom Music Player",
                            style = GlassTypography.headlineMedium,
                            color = GlassTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version 1.0.0 • White Transparent Glassroom",
                            style = GlassTypography.labelSmall,
                            color = GlassTextTertiary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Engineered with Kotlin, Jetpack Compose, Material 3, Android Media3 ExoPlayer, and YouTube audio resolution via backend yt-dlp.",
                            style = GlassTypography.bodyMedium,
                            color = GlassTextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GlassTextPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GlassTypography.titleMedium,
                color = GlassTextPrimary
            )
            Text(
                text = subtitle,
                style = GlassTypography.labelSmall,
                color = GlassTextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2C3E50),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0x330F172A)
            )
        )
    }
}
