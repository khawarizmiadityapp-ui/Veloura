package com.glassroom.music.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.glassroom.music.data.remote.NetworkClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "glassroom_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_BACKEND_URL = stringPreferencesKey("backend_url")
        val KEY_CUSTOM_SERVER_SELECTED = booleanPreferencesKey("custom_server_selected")
        val KEY_GAPLESS = booleanPreferencesKey("gapless_playback")
        val KEY_AUTOPLAY = booleanPreferencesKey("autoplay")
        val KEY_SHUFFLE = booleanPreferencesKey("shuffle_state")
        val KEY_REPEAT = stringPreferencesKey("repeat_mode")
        val KEY_GLASS_INTENSITY = floatPreferencesKey("glass_intensity")

        private val LEGACY_DEV_HOSTS = listOf(
            "172.30.189.56",
            "10.10.45.8",
            "10.117.54.56",
            "10.0.2.2",
            "127.0.0.1",
            "localhost"
        )
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "light_glass"
    }

    val glassIntensityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_GLASS_INTENSITY] ?: 0.75f
    }

    val isCustomServerFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_SERVER_SELECTED] ?: false
    }

    val backendUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val saved = prefs[KEY_BACKEND_URL]?.trim()
        val isExplicitCustom = prefs[KEY_CUSTOM_SERVER_SELECTED] ?: false

        if (saved.isNullOrBlank()) {
            NetworkClient.DEFAULT_BASE_URL
        } else if (!isExplicitCustom) {
            // Prevent old cached laptop/emulator IPs from overriding cloud production default
            val isLegacyLocal = LEGACY_DEV_HOSTS.any { saved.contains(it) }
            if (isLegacyLocal) {
                NetworkClient.DEFAULT_BASE_URL
            } else {
                NetworkClient.normalizeBaseUrl(saved)
            }
        } else {
            NetworkClient.normalizeBaseUrl(saved)
        }
    }

    val gaplessFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_GAPLESS] ?: true
    }

    val autoplayFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTOPLAY] ?: true
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setGlassIntensity(intensity: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GLASS_INTENSITY] = intensity.coerceIn(0.2f, 1.0f)
        }
    }

    suspend fun setBackendUrl(url: String, isCustom: Boolean = true) {
        val normalized = NetworkClient.normalizeBaseUrl(url)
        context.dataStore.edit { prefs ->
            prefs[KEY_BACKEND_URL] = normalized
            prefs[KEY_CUSTOM_SERVER_SELECTED] = isCustom
        }
        NetworkClient.updateBaseUrl(normalized)
    }

    suspend fun resetToDefaultBackendUrl() {
        context.dataStore.edit { prefs ->
            prefs[KEY_BACKEND_URL] = NetworkClient.DEFAULT_BASE_URL
            prefs[KEY_CUSTOM_SERVER_SELECTED] = false
        }
        NetworkClient.updateBaseUrl(NetworkClient.DEFAULT_BASE_URL)
    }

    suspend fun setGapless(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GAPLESS] = enabled
        }
    }

    suspend fun setAutoplay(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTOPLAY] = enabled
        }
    }
}
