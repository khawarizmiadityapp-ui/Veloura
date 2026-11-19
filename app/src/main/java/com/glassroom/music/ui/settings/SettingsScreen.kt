package com.glassroom.music.ui.settings

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassroom.music.data.local.OfflineAudioManager
import com.glassroom.music.data.local.PreferencesManager
import com.glassroom.music.data.remote.NetworkClient
import com.glassroom.music.ui.components.*
import com.glassroom.music.ui.home.SectionHeader
import com.glassroom.music.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    offlineAudioManager: OfflineAudioManager? = null,
    onOpenOfflineLibrary: () -> Unit = {},
    onShowMessage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val themeMode by preferencesManager.themeModeFlow.collectAsState(initial = "light_glass")
    val backendUrl by preferencesManager.backendUrlFlow.collectAsState(initial = NetworkClient.DEFAULT_BASE_URL)
    val isGapless by preferencesManager.gaplessFlow.collectAsState(initial = true)
    val isAutoplay by preferencesManager.autoplayFlow.collectAsState(initial = true)

    val glassIntensity by preferencesManager.glassIntensityFlow.collectAsState(initial = 0.75f)
    val downloadedIds by (offlineAudioManager?.downloadedTrackIds ?: flowOf(emptySet<String>()))
        .collectAsState(initial = emptySet())
    val offlineStorageBytes by (offlineAudioManager?.offlineStorageBytes ?: flowOf(0L))
        .collectAsState(initial = 0L)

    var editingUrl by remember(backendUrl) { mutableStateOf(backendUrl) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    var isCheckingConnection by remember { mutableStateOf(false) }
    var showAdvancedServer by remember { mutableStateOf(false) }
    var showClearOfflineDialog by remember { mutableStateOf(false) }

    val glassConfig = LocalGlassConfig.current

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        GlassTopBar(
            title = "Setelan",
            subtitle = "Preferensi & Sistem"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 150.dp)
        ) {
            // Appearance Section
            item {
                SectionHeader(title = "Tampilan", modifier = Modifier.padding(horizontal = 0.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Palet Tema",
                            style = GlassTypography.titleMedium,
                            color = glassConfig.textPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassChip(
                                    text = "Light Glass",
                                    isSelected = themeMode == "light_glass",
                                    onClick = {
                                        coroutineScope.launch {
                                            preferencesManager.setThemeMode("light_glass")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                GlassChip(
                                    text = "Dark Glass",
                                    isSelected = themeMode == "dark_glass",
                                    onClick = {
                                        coroutineScope.launch {
                                            preferencesManager.setThemeMode("dark_glass")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassChip(
                                    text = "Paper (Kertas)",
                                    isSelected = themeMode == "paper",
                                    onClick = {
                                        coroutineScope.launch {
                                            preferencesManager.setThemeMode("paper")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                GlassChip(
                                    text = "Otomatis",
                                    isSelected = themeMode == "auto",
                                    onClick = {
                                        coroutineScope.launch {
                                            preferencesManager.setThemeMode("auto")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Adaptive Surface Tuning: Glass Translucency or Paper Texture & Warmth
                        val intensityPercent = (glassIntensity * 100).toInt()
                        val intensityLabel = if (glassConfig.isPaper) {
                            when {
                                glassIntensity < 0.45f -> "Putih Gading"
                                glassIntensity < 0.82f -> "Serat Katun"
                                else -> "Perkamen Hangat"
                            }
                        } else {
                            when {
                                glassIntensity < 0.45f -> "Bening Kristal"
                                glassIntensity < 0.82f -> "Kaca Buram"
                                else -> "Kaca Pekat"
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (glassConfig.isPaper) "Kehangatan & Serat Kertas" else "Transparansi Kaca Veloura",
                                style = GlassTypography.titleMedium,
                                color = glassConfig.textPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$intensityPercent% • $intensityLabel",
                                style = GlassTypography.labelSmall,
                                color = glassConfig.accentPrimary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        androidx.compose.material3.Slider(
                            value = glassIntensity,
                            onValueChange = { newVal ->
                                coroutineScope.launch {
                                    preferencesManager.setGlassIntensity(newVal)
                                }
                            },
                            valueRange = 0.25f..0.95f,
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                thumbColor = if (glassConfig.isDark) Color.White else glassConfig.accentPrimary,
                                activeTrackColor = glassConfig.accentPrimary,
                                inactiveTrackColor = glassConfig.inactiveTrackColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Presets
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassChip(
                                text = if (glassConfig.isPaper) "Gading (35%)" else "💎 Bening (35%)",
                                isSelected = (glassIntensity - 0.35f).let { kotlin.math.abs(it) < 0.05f },
                                onClick = {
                                    coroutineScope.launch {
                                        preferencesManager.setGlassIntensity(0.35f)
                                    }
                                }
                            )
                            GlassChip(
                                text = if (glassConfig.isPaper) "Katun (75%)" else "❄️ Buram (75%)",
                                isSelected = (glassIntensity - 0.75f).let { kotlin.math.abs(it) < 0.05f },
                                onClick = {
                                    coroutineScope.launch {
                                        preferencesManager.setGlassIntensity(0.75f)
                                    }
                                }
                            )
                            GlassChip(
                                text = if (glassConfig.isPaper) "Klasik (95%)" else "🥛 Pekat (95%)",
                                isSelected = (glassIntensity - 0.95f).let { kotlin.math.abs(it) < 0.05f },
                                onClick = {
                                    coroutineScope.launch {
                                        preferencesManager.setGlassIntensity(0.95f)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Live Mini Preview inside card
                        Text(
                            text = if (glassConfig.isPaper) "Pratinjau Kertas:" else "Pratinjau Kaca Veloura:",
                            style = GlassTypography.labelSmall,
                            color = glassConfig.textTertiary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(brush = glassConfig.cardBrush)
                                .border(1.dp, glassConfig.borderHighlight, RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (glassConfig.isPaper) {
                                    "Kertas Veloura: Hangat, matte, bertekstur serat & tinta tajam"
                                } else {
                                    "Kaca Veloura: Transparan, halus, & berkedalaman optik"
                                },
                                style = GlassTypography.bodyMedium,
                                color = glassConfig.textPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Playback Settings Section
            item {
                SectionHeader(title = "Pemutaran", modifier = Modifier.padding(horizontal = 0.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingToggleRow(
                            icon = Icons.Filled.GraphicEq,
                            title = "Pemutaran Tanpa Jeda",
                            subtitle = "Transisi mulus antar lagu tanpa jeda",
                            checked = isGapless,
                            onCheckedChange = {
                                coroutineScope.launch { preferencesManager.setGapless(it) }
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        SettingToggleRow(
                            icon = Icons.Filled.PlayCircle,
                            title = "Putar Otomatis Lagu Serupa",
                            subtitle = "Lanjutkan lagu serupa saat antrean berakhir",
                            checked = isAutoplay,
                            onCheckedChange = {
                                coroutineScope.launch { preferencesManager.setAutoplay(it) }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Offline Storage Management Section
            if (offlineAudioManager != null) {
                item {
                    SectionHeader(title = "Penyimpanan Offline", modifier = Modifier.padding(horizontal = 0.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Storage,
                                        contentDescription = null,
                                        tint = glassConfig.accentPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "${downloadedIds.size} lagu tersimpan offline",
                                            style = GlassTypography.titleMedium,
                                            color = glassConfig.textPrimary
                                        )
                                        Text(
                                            text = "Ruang terpakai: ${formatStorageSize(offlineStorageBytes)} (penyimpanan internal HP)",
                                            style = GlassTypography.labelSmall,
                                            color = glassConfig.textSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassButton(
                                    text = "Lihat Lagu Offline",
                                    onClick = onOpenOfflineLibrary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (downloadedIds.isNotEmpty()) {
                                    GlassButton(
                                        text = "Hapus Semua",
                                        onClick = { showClearOfflineDialog = true },
                                        modifier = Modifier.weight(0.85f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Backend Server Configuration (Clean summary + collapsible developer options)
            item {
                SectionHeader(title = "Layanan Server", modifier = Modifier.padding(horizontal = 0.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "API Audio Veloura",
                                style = GlassTypography.titleMedium,
                                color = glassConfig.textPrimary
                            )
                            val badgeText = when (backendUrl) {
                                NetworkClient.SENTINEL_SERVER_URL -> "Veloura Utama"
                                NetworkClient.VERCEL_SERVER_URL -> "Cloud (Vercel)"
                                else -> "Server Kustom"
                            }
                            val badgeColor = if (backendUrl == NetworkClient.SENTINEL_SERVER_URL) {
                                glassConfig.accentSecondary
                            } else {
                                glassConfig.accentPrimary
                            }
                            Text(
                                text = badgeText,
                                style = GlassTypography.labelSmall,
                                color = badgeColor,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Terhubung ke: $backendUrl",
                            style = GlassTypography.labelSmall,
                            color = glassConfig.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassButton(
                                text = if (isCheckingConnection) "Memeriksa..." else "Test Koneksi",
                                isPrimary = !showAdvancedServer,
                                onClick = {
                                    if (isCheckingConnection) return@GlassButton
                                    isCheckingConnection = true
                                    connectionStatus = null
                                    coroutineScope.launch {
                                        try {
                                            val testService = NetworkClient.createServiceForUrl(editingUrl)
                                            val healthResp = testService.healthCheck()
                                            if (healthResp.isSuccessful && healthResp.body() != null) {
                                                val body = healthResp.body()!!
                                                val serviceName = body["service"] ?: "Veloura Audio Backend"
                                                val ver = body["version"] ?: "1.0.0"
                                                val ytdlp = body["yt_dlp_version"] ?: ""
                                                val ytdlpInfo = if (ytdlp.isNotBlank()) " (yt-dlp $ytdlp)" else ""
                                                connectionStatus = "✓ Online: $serviceName v$ver$ytdlpInfo"
                                            } else {
                                                connectionStatus = "✕ Server merespons HTTP ${healthResp.code()}"
                                            }
                                        } catch (e: Exception) {
                                            connectionStatus = "✕ Gagal terhubung: ${e.localizedMessage ?: "Timeout"}"
                                        }
                                        isCheckingConnection = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            if (showAdvancedServer) {
                                GlassButton(
                                    text = "Terapkan",
                                    isPrimary = true,
                                    onClick = {
                                        coroutineScope.launch {
                                            val normalized = NetworkClient.normalizeBaseUrl(editingUrl)
                                            val isCustom = normalized != NetworkClient.CLOUD_SERVER_URL
                                            preferencesManager.setBackendUrl(normalized, isCustom = isCustom)
                                            editingUrl = normalized
                                            connectionStatus = "✓ Server aktif diatur ke $normalized"
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (connectionStatus != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = connectionStatus!!,
                                style = GlassTypography.labelSmall,
                                color = if (connectionStatus!!.startsWith("✓")) GlassAccentMint else Color(0xFFEF4444),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(glassConfig.borderSubtle)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAdvancedServer = !showAdvancedServer }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pengaturan Server Lanjutan (Developer)",
                                style = GlassTypography.labelLarge,
                                color = glassConfig.accentPrimary
                            )
                            Icon(
                                imageVector = if (showAdvancedServer) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Toggle Pengaturan Lanjutan",
                                tint = glassConfig.accentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(visible = showAdvancedServer) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pilih Preset Server:",
                                    style = GlassTypography.labelSmall,
                                    color = glassConfig.textSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlassChip(
                                        text = "Server Utama (Sentinel)",
                                        isSelected = editingUrl == NetworkClient.SENTINEL_SERVER_URL,
                                        onClick = {
                                            editingUrl = NetworkClient.SENTINEL_SERVER_URL
                                            connectionStatus = null
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlassChip(
                                        text = "Cloud (Vercel)",
                                        isSelected = editingUrl == NetworkClient.VERCEL_SERVER_URL,
                                        onClick = {
                                            editingUrl = NetworkClient.VERCEL_SERVER_URL
                                            connectionStatus = null
                                        }
                                    )
                                    GlassChip(
                                        text = "Laptop Wi-Fi",
                                        isSelected = editingUrl == NetworkClient.WIFI_SERVER_URL,
                                        onClick = {
                                            editingUrl = NetworkClient.WIFI_SERVER_URL
                                            connectionStatus = null
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlassChip(
                                        text = "ADB (127.0.0.1)",
                                        isSelected = editingUrl == NetworkClient.ADB_REVERSE_URL,
                                        onClick = {
                                            editingUrl = NetworkClient.ADB_REVERSE_URL
                                            connectionStatus = null
                                        }
                                    )
                                    GlassChip(
                                        text = "Emulator (10.0.2.2)",
                                        isSelected = editingUrl == NetworkClient.EMULATOR_URL,
                                        onClick = {
                                            editingUrl = NetworkClient.EMULATOR_URL
                                            connectionStatus = null
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val inputBg = glassConfig.inputSurfaceColor
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(inputBg)
                                        .border(1.dp, glassConfig.borderBrush, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    BasicTextField(
                                        value = editingUrl,
                                        onValueChange = {
                                            editingUrl = it
                                            connectionStatus = null
                                        },
                                        singleLine = true,
                                        textStyle = GlassTypography.bodyLarge.copy(color = glassConfig.textPrimary),
                                        cursorBrush = SolidColor(glassConfig.textPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Reset ke Server Utama (veloura.sentinelofc.web.id)",
                                        style = GlassTypography.labelSmall,
                                        color = glassConfig.accentPrimary,
                                        modifier = Modifier
                                            .clickable {
                                                coroutineScope.launch {
                                                    preferencesManager.resetToDefaultBackendUrl()
                                                    editingUrl = NetworkClient.DEFAULT_BASE_URL
                                                    connectionStatus = "✓ Direset ke server utama"
                                                }
                                            }
                                            .padding(vertical = 4.dp, horizontal = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // About Veloura Section
            item {
                SectionHeader(title = "Tentang", modifier = Modifier.padding(horizontal = 0.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Glass Encapsulated Logo Showcase
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(brush = glassConfig.cardBrush)
                                .border(
                                    width = 1.dp,
                                    brush = glassConfig.borderBrush,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.glassroom.music.R.drawable.logo_veloura),
                                contentDescription = "Veloura Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "VELOURA",
                            style = GlassTypography.headlineMedium.copy(
                                letterSpacing = 3.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            color = glassConfig.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "FEEL EVERY FREQUENCY",
                            style = GlassTypography.labelSmall.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            ),
                            color = glassConfig.accentPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (glassConfig.isPaper) "Versi 1.0.0 • Edisi Kertas" else "Versi 1.0.0 • Edisi Kaca",
                            style = GlassTypography.labelSmall,
                            color = glassConfig.textTertiary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Dibangun dengan Kotlin, Jetpack Compose, Material 3, Android Media3 ExoPlayer, dan ekstraksi audio YouTube.",
                            style = GlassTypography.bodyMedium,
                            color = glassConfig.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showClearOfflineDialog && offlineAudioManager != null) {
        GlassDialog(onDismissRequest = { showClearOfflineDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Hapus Semua Unduhan Offline?",
                    style = GlassTypography.headlineMedium,
                    color = glassConfig.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sebanyak ${downloadedIds.size} file audio (${formatStorageSize(offlineStorageBytes)}) akan dihapus dari penyimpanan HP.",
                    style = GlassTypography.bodyMedium,
                    color = glassConfig.textSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = "Batal",
                        onClick = { showClearOfflineDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Hapus Semua",
                        isPrimary = true,
                        onClick = {
                            showClearOfflineDialog = false
                            coroutineScope.launch {
                                val deleted = offlineAudioManager.deleteAllOfflineTracks()
                                onShowMessage?.invoke("$deleted file offline dihapus")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val glassConfig = LocalGlassConfig.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = glassConfig.textPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GlassTypography.titleMedium,
                color = glassConfig.textPrimary
            )
            Text(
                text = subtitle,
                style = GlassTypography.labelSmall,
                color = glassConfig.textSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = if (glassConfig.isDark || glassConfig.isPaper) glassConfig.accentPrimary else Color(0xFF1E293B),
                uncheckedThumbColor = if (glassConfig.isDark) Color(0xFF64748B) else glassConfig.textTertiary,
                uncheckedTrackColor = glassConfig.inactiveTrackColor
            )
        )
    }
}
