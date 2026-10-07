package com.glassroom.music.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        val KEY_GAPLESS = booleanPreferencesKey("gapless_playback")
        val KEY_AUTOPLAY = booleanPreferencesKey("autoplay")
        val KEY_SHUFFLE = booleanPreferencesKey("shuffle_state")
        val KEY_REPEAT = stringPreferencesKey("repeat_mode")
        val KEY_GLASS_INTENSITY = androidx.datastore.preferences.core.floatPreferencesKey("glass_intensity")
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "light_glass"
    }

    val glassIntensityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_GLASS_INTENSITY] ?: 0.75f
    }

    val backendUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BACKEND_URL] ?: NetworkClient.DEFAULT_BASE_URL
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

    suspend fun setBackendUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BACKEND_URL] = url
        }
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
