package com.glassroom.music.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.glassroom.music.data.remote.NetworkClient
import com.glassroom.music.domain.model.Track
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class PlaybackSessionSnapshot(
    val queue: List<Track>,
    val currentIndex: Int,
    val positionMs: Long
)

private val Context.dataStore by preferencesDataStore(name = "glassroom_preferences")

class PreferencesManager(private val context: Context) {

    private val fastSyncPrefs = context.getSharedPreferences("veloura_fast_sync_prefs", Context.MODE_PRIVATE)

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_BACKEND_URL = stringPreferencesKey("backend_url")
        val KEY_CUSTOM_SERVER_SELECTED = booleanPreferencesKey("custom_server_selected")
        val KEY_GAPLESS = booleanPreferencesKey("gapless_playback")
        val KEY_AUTOPLAY = booleanPreferencesKey("autoplay")
        val KEY_SHUFFLE = booleanPreferencesKey("shuffle_state")
        val KEY_REPEAT = stringPreferencesKey("repeat_mode")
        val KEY_GLASS_INTENSITY = floatPreferencesKey("glass_intensity")
        val KEY_LAST_QUEUE_JSON = stringPreferencesKey("last_playback_queue_json")
        val KEY_LAST_INDEX = intPreferencesKey("last_playback_index")
        val KEY_LAST_POSITION_MS = longPreferencesKey("last_playback_position_ms")
        val KEY_SEARCH_HISTORY_JSON = stringPreferencesKey("search_history_json")

        private val LEGACY_DEV_HOSTS = listOf(
            "veloura-lilac-sigma.vercel.app",
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

    fun getCachedBackendUrlSync(): String {
        val raw = fastSyncPrefs.getString("cached_backend_url", null)?.trim()
        return if (raw.isNullOrBlank()) NetworkClient.DEFAULT_BASE_URL else NetworkClient.normalizeBaseUrl(raw)
    }

    private fun cacheBackendUrlSync(normalizedUrl: String) {
        fastSyncPrefs.edit().putString("cached_backend_url", normalizedUrl).apply()
    }

    val backendUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val saved = prefs[KEY_BACKEND_URL]?.trim()
        val isExplicitCustom = prefs[KEY_CUSTOM_SERVER_SELECTED] ?: false

        val resolved = if (saved.isNullOrBlank()) {
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
        cacheBackendUrlSync(resolved)
        resolved
    }

    val gaplessFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_GAPLESS] ?: true
    }

    val autoplayFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTOPLAY] ?: true
    }

    val shuffleFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHUFFLE] ?: false
    }

    val repeatModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_REPEAT] ?: "OFF"
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
        cacheBackendUrlSync(normalized)
        context.dataStore.edit { prefs ->
            prefs[KEY_BACKEND_URL] = normalized
            prefs[KEY_CUSTOM_SERVER_SELECTED] = isCustom
        }
        NetworkClient.updateBaseUrl(normalized)
    }

    suspend fun resetToDefaultBackendUrl() {
        cacheBackendUrlSync(NetworkClient.DEFAULT_BASE_URL)
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

    suspend fun setShuffle(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHUFFLE] = enabled
        }
    }

    suspend fun setRepeatMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REPEAT] = mode
        }
    }

    suspend fun savePlaybackSession(queue: List<Track>, index: Int, positionMs: Long) {
        val cappedQueue = queue.take(100)
        val json = try {
            Gson().toJson(cappedQueue)
        } catch (_: Exception) {
            return
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_QUEUE_JSON] = json
            prefs[KEY_LAST_INDEX] = index.coerceIn(0, (cappedQueue.size - 1).coerceAtLeast(0))
            prefs[KEY_LAST_POSITION_MS] = positionMs.coerceAtLeast(0L)
        }
    }

    suspend fun loadPlaybackSession(): PlaybackSessionSnapshot? {
        return try {
            val prefs = context.dataStore.data.first()
            val json = prefs[KEY_LAST_QUEUE_JSON] ?: return null
            if (json.isBlank()) return null
            val listType = object : TypeToken<List<Track>>() {}.type
            val queue: List<Track> = Gson().fromJson(json, listType) ?: return null
            if (queue.isEmpty()) return null
            val idx = (prefs[KEY_LAST_INDEX] ?: 0).coerceIn(0, queue.size - 1)
            val pos = (prefs[KEY_LAST_POSITION_MS] ?: 0L).coerceAtLeast(0L)
            PlaybackSessionSnapshot(queue = queue, currentIndex = idx, positionMs = pos)
        } catch (_: Exception) {
            null
        }
    }

    val searchHistoryFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        parseSearchHistoryJson(prefs[KEY_SEARCH_HISTORY_JSON])
    }

    suspend fun addSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        context.dataStore.edit { prefs ->
            val current = parseSearchHistoryJson(prefs[KEY_SEARCH_HISTORY_JSON])
            val updated = (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) })
                .take(10)
            prefs[KEY_SEARCH_HISTORY_JSON] = Gson().toJson(updated)
        }
    }

    suspend fun removeSearchHistory(query: String) {
        context.dataStore.edit { prefs ->
            val current = parseSearchHistoryJson(prefs[KEY_SEARCH_HISTORY_JSON])
            val updated = current.filterNot { it.equals(query, ignoreCase = true) }
            prefs[KEY_SEARCH_HISTORY_JSON] = Gson().toJson(updated)
        }
    }

    suspend fun clearSearchHistory() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SEARCH_HISTORY_JSON)
        }
    }

    private fun parseSearchHistoryJson(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            Gson().fromJson<List<String>>(json, listType)
                ?.filter { it.isNotBlank() }
                ?.take(10)
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
