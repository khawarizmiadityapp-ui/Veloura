package com.glassroom.music.data.repository

import com.glassroom.music.data.local.LocalDataSource
import com.glassroom.music.data.remote.NetworkClient
import com.glassroom.music.data.remote.dto.*
import com.glassroom.music.data.util.LyricsParser
import com.glassroom.music.domain.model.*
import com.glassroom.music.domain.repository.MusicRepository
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MusicRepositoryImpl(
    private val localDataSource: LocalDataSource
) : MusicRepository {

    // Curated fallback tracks with real YouTube IDs
    private val defaultCuratedTracks = listOf(
        Track(
            id = "kJQP7kiw5Fk",
            title = "Die With A Smile",
            artist = "Lady Gaga & Bruno Mars",
            album = "Die With A Smile",
            thumbnail = "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg",
            duration = 251,
            streamUrl = "/api/stream/kJQP7kiw5Fk",
            source = "youtube"
        ),
        Track(
            id = "eVli-tstM5E",
            title = "Espresso",
            artist = "Sabrina Carpenter",
            album = "Short n' Sweet",
            thumbnail = "https://i.ytimg.com/vi/eVli-tstM5E/hqdefault.jpg",
            duration = 175,
            streamUrl = "/api/stream/eVli-tstM5E",
            source = "youtube"
        ),
        Track(
            id = "d5gf9dXbPi0",
            title = "Birds of a Feather",
            artist = "Billie Eilish",
            album = "Hit Me Hard and Soft",
            thumbnail = "https://i.ytimg.com/vi/d5gf9dXbPi0/hqdefault.jpg",
            duration = 212,
            streamUrl = "/api/stream/d5gf9dXbPi0",
            source = "youtube"
        ),
        Track(
            id = "34Na4j8AVgA",
            title = "Starboy",
            artist = "The Weeknd ft. Daft Punk",
            album = "Starboy",
            thumbnail = "https://i.ytimg.com/vi/34Na4j8AVgA/hqdefault.jpg",
            duration = 230,
            streamUrl = "/api/stream/34Na4j8AVgA",
            source = "youtube"
        ),
        Track(
            id = "4NRXx6U8ABQ",
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            thumbnail = "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg",
            duration = 200,
            streamUrl = "/api/stream/4NRXx6U8ABQ",
            source = "youtube"
        ),
        Track(
            id = "dX3k_QDnzHE",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            thumbnail = "https://i.ytimg.com/vi/dX3k_QDnzHE/hqdefault.jpg",
            duration = 243,
            streamUrl = "/api/stream/dX3k_QDnzHE",
            source = "youtube"
        ),
        Track(
            id = "H5v3kku4y6Q",
            title = "As It Was",
            artist = "Harry Styles",
            album = "Harry's House",
            thumbnail = "https://i.ytimg.com/vi/H5v3kku4y6Q/hqdefault.jpg",
            duration = 167,
            streamUrl = "/api/stream/H5v3kku4y6Q",
            source = "youtube"
        ),
        Track(
            id = "5NV6Rdv1a3I",
            title = "Get Lucky",
            artist = "Daft Punk ft. Pharrell Williams",
            album = "Random Access Memories",
            thumbnail = "https://i.ytimg.com/vi/5NV6Rdv1a3I/hqdefault.jpg",
            duration = 248,
            streamUrl = "/api/stream/5NV6Rdv1a3I",
            source = "youtube"
        ),
        Track(
            id = "TUVcZfQe-Kw",
            title = "Levitating",
            artist = "Dua Lipa",
            album = "Future Nostalgia",
            thumbnail = "https://i.ytimg.com/vi/TUVcZfQe-Kw/hqdefault.jpg",
            duration = 203,
            streamUrl = "/api/stream/TUVcZfQe-Kw",
            source = "youtube"
        ),
        Track(
            id = "yKNxeF4KMsY",
            title = "Yellow",
            artist = "Coldplay",
            album = "Parachutes",
            thumbnail = "https://i.ytimg.com/vi/yKNxeF4KMsY/hqdefault.jpg",
            duration = 269,
            streamUrl = "/api/stream/yKNxeF4KMsY",
            source = "youtube"
        ),
        Track(
            id = "7Jm2Il2U-c0",
            title = "Satu Bulan",
            artist = "Bernadya",
            album = "Sialnya, Hidup Harus Tetap Berjalan",
            thumbnail = "https://i.ytimg.com/vi/7Jm2Il2U-c0/hqdefault.jpg",
            duration = 199,
            streamUrl = "/api/stream/7Jm2Il2U-c0",
            source = "youtube"
        ),
        Track(
            id = "ic8j13piAhQ",
            title = "Cruel Summer",
            artist = "Taylor Swift",
            album = "Lover",
            thumbnail = "https://i.ytimg.com/vi/ic8j13piAhQ/hqdefault.jpg",
            duration = 178,
            streamUrl = "/api/stream/ic8j13piAhQ",
            source = "youtube"
        )
    )

    override suspend fun search(query: String, category: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val api = NetworkClient.getApiService()
            val response = api.search(query, category)
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()?.tracks ?: emptyList()
                val tracks = dtos.map { mapDtoToTrack(it) }
                Result.success(tracks)
            } else {
                Result.failure(Exception("Pencarian gagal: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrack(trackId: String): Result<Track> = withContext(Dispatchers.IO) {
        try {
            val api = NetworkClient.getApiService()
            val response = api.getTrack(trackId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(mapDtoToTrack(response.body()!!))
            } else {
                val local = localDataSource.getTrackById(trackId)
                if (local != null) Result.success(local)
                else {
                    val curated = defaultCuratedTracks.firstOrNull { it.id == trackId }
                        ?: defaultCuratedTracks.first()
                    Result.success(curated)
                }
            }
        } catch (e: Exception) {
            val local = localDataSource.getTrackById(trackId)
            if (local != null) Result.success(local)
            else {
                val curated = defaultCuratedTracks.firstOrNull { it.id == trackId }
                    ?: defaultCuratedTracks.first()
                Result.success(curated)
            }
        }
    }

    private val onDeviceHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun getSafeString(jsonObj: JsonObject, key: String): String? {
        return try {
            if (jsonObj.has(key) && !jsonObj.get(key).isJsonNull) jsonObj.get(key).asString else null
        } catch (_: Exception) {
            null
        }
    }

    private fun getSafeInt(jsonObj: JsonObject, key: String): Int? {
        return try {
            if (jsonObj.has(key) && !jsonObj.get(key).isJsonNull) jsonObj.get(key).asInt else null
        } catch (_: Exception) {
            null
        }
    }

    private data class InnerTubeClientProfile(
        val clientName: String,
        val clientVersion: String,
        val clientNameId: String,
        val userAgent: String,
        val extraClientFields: Map<String, Any> = emptyMap()
    )

    @Volatile
    private var cachedVisitorData: String? = null

    @Volatile
    private var cachedVisitorDataExpiryMs: Long = 0L

    private val visitorDataLock = Any()

    private val youtubeClientProfiles = listOf(
        InnerTubeClientProfile(
            clientName = "VISIONOS",
            clientVersion = "1.02",
            clientNameId = "101",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15"
        ),
        InnerTubeClientProfile(
            clientName = "ANDROID",
            clientVersion = "21.26.364",
            clientNameId = "3",
            userAgent = "com.google.android.youtube/21.26.364 (Linux; U; Android 11) gzip",
            extraClientFields = mapOf(
                "androidSdkVersion" to 30,
                "osName" to "Android",
                "osVersion" to "11"
            )
        ),
        InnerTubeClientProfile(
            clientName = "ANDROID_VR",
            clientVersion = "1.65.10",
            clientNameId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            extraClientFields = mapOf(
                "deviceMake" to "Oculus",
                "deviceModel" to "Quest 3",
                "osName" to "Android",
                "osVersion" to "12L",
                "androidSdkVersion" to 32
            )
        )
    )

    private fun getYouTubeVisitorData(videoId: String? = null): String? {
        val now = System.currentTimeMillis()
        val existing = cachedVisitorData
        if (!existing.isNullOrBlank() && now < cachedVisitorDataExpiryMs) {
            return existing
        }

        synchronized(visitorDataLock) {
            val freshCheck = cachedVisitorData
            if (!freshCheck.isNullOrBlank() && System.currentTimeMillis() < cachedVisitorDataExpiryMs) {
                return freshCheck
            }

            val chromeUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36"
            val webClientVersion = "2.20260401.00.00"
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()

            // 1. Primary: InnerTube visitor_id endpoint
            try {
                val payload = mapOf(
                    "context" to mapOf(
                        "client" to mapOf(
                            "clientName" to "WEB",
                            "clientVersion" to webClientVersion,
                            "hl" to "en",
                            "gl" to "US"
                        )
                    )
                )
                val body = Gson().toJson(payload).toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/visitor_id?prettyPrint=false")
                    .header("User-Agent", chromeUa)
                    .header("Content-Type", "application/json")
                    .header("Origin", "https://www.youtube.com")
                    .header("X-YouTube-Client-Name", "1")
                    .header("X-YouTube-Client-Version", webClientVersion)
                    .post(body)
                    .build()

                onDeviceHttpClient.newCall(request).execute().use { response ->
                    val jsonStr = response.body?.string()
                    if (response.isSuccessful && !jsonStr.isNullOrBlank()) {
                        val jsonObj = try {
                            Gson().fromJson(jsonStr, JsonObject::class.java)
                        } catch (_: Exception) {
                            null
                        }
                        val vd = jsonObj?.getAsJsonObject("responseContext")?.let {
                            getSafeString(it, "visitorData")
                        }
                        if (!vd.isNullOrBlank()) {
                            cachedVisitorData = vd
                            cachedVisitorDataExpiryMs = System.currentTimeMillis() + 30L * 60L * 1000L
                            return vd
                        }
                    }
                }
            } catch (_: Exception) {
            }

            // 2. Fallback: scrape visitorData from embed HTML
            if (!videoId.isNullOrBlank()) {
                try {
                    val embedRequest = Request.Builder()
                        .url("https://www.youtube.com/embed/$videoId")
                        .header("User-Agent", chromeUa)
                        .get()
                        .build()

                    onDeviceHttpClient.newCall(embedRequest).execute().use { response ->
                        val html = response.body?.string()
                        if (response.isSuccessful && !html.isNullOrBlank()) {
                            val vd = Regex(""""visitorData"\s*:\s*"([^"]+)"""")
                                .find(html)
                                ?.groupValues
                                ?.getOrNull(1)
                            if (!vd.isNullOrBlank()) {
                                cachedVisitorData = vd
                                cachedVisitorDataExpiryMs = System.currentTimeMillis() + 30L * 60L * 1000L
                                return vd
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }

            return cachedVisitorData
        }
    }

    private fun selectBestYouTubeStreamUrl(jsonObj: JsonObject): String? {
        val playabilityStatus = jsonObj.getAsJsonObject("playabilityStatus")
        val status = playabilityStatus?.let { getSafeString(it, "status") }
        if (status != "OK") {
            return null
        }

        val streamingData = jsonObj.getAsJsonObject("streamingData") ?: return null
        val adaptiveList = streamingData.getAsJsonArray("adaptiveFormats")
            ?.mapNotNull { it.asJsonObject } ?: emptyList()

        // Prioritas 1: itag == 140 (AAC M4A)
        adaptiveList.firstOrNull {
            getSafeInt(it, "itag") == 140 && !getSafeString(it, "url").isNullOrBlank()
        }?.let { return getSafeString(it, "url") }

        // Prioritas 2: mimeType mengandung audio/mp4
        adaptiveList.firstOrNull {
            getSafeString(it, "mimeType")?.contains("audio/mp4", ignoreCase = true) == true &&
                    !getSafeString(it, "url").isNullOrBlank()
        }?.let { return getSafeString(it, "url") }

        // Prioritas 3: mimeType mengandung audio/webm
        adaptiveList.firstOrNull {
            getSafeString(it, "mimeType")?.contains("audio/webm", ignoreCase = true) == true &&
                    !getSafeString(it, "url").isNullOrBlank()
        }?.let { return getSafeString(it, "url") }

        // Prioritas 4: format audio adaptive lainnya
        adaptiveList.firstOrNull {
            getSafeString(it, "mimeType")?.startsWith("audio/", ignoreCase = true) == true &&
                    !getSafeString(it, "url").isNullOrBlank()
        }?.let { return getSafeString(it, "url") }

        // Fallback: streamingData.formats (itag == 18 / format progressive)
        val formats = streamingData.getAsJsonArray("formats")
            ?.mapNotNull { it.asJsonObject } ?: emptyList()

        formats.firstOrNull {
            getSafeInt(it, "itag") == 18 && !getSafeString(it, "url").isNullOrBlank()
        }?.let { return getSafeString(it, "url") }

        formats.firstOrNull {
            !getSafeString(it, "url").isNullOrBlank()
        }?.let { return getSafeString(it, "url") }

        return null
    }

    private fun fetchYouTubePlayerStreamUrl(videoId: String): String? {
        val playerUrl = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val visitorData = getYouTubeVisitorData(videoId)

        for (profile in youtubeClientProfiles) {
            try {
                val clientMap = mutableMapOf<String, Any>(
                    "clientName" to profile.clientName,
                    "clientVersion" to profile.clientVersion,
                    "hl" to "en",
                    "gl" to "US"
                )
                clientMap.putAll(profile.extraClientFields)
                if (!visitorData.isNullOrBlank()) {
                    clientMap["visitorData"] = visitorData
                }

                val payload = mapOf(
                    "context" to mapOf("client" to clientMap),
                    "videoId" to videoId,
                    "contentCheckOk" to true,
                    "racyCheckOk" to true,
                    "playbackContext" to mapOf(
                        "contentPlaybackContext" to mapOf(
                            "html5Preference" to "HTML5_PREF_WANTS"
                        )
                    )
                )

                val body = Gson().toJson(payload).toRequestBody(jsonMediaType)
                val requestBuilder = Request.Builder()
                    .url(playerUrl)
                    .header("User-Agent", profile.userAgent)
                    .header("Content-Type", "application/json")
                    .header("Origin", "https://www.youtube.com")
                    .header("X-YouTube-Client-Name", profile.clientNameId)
                    .header("X-YouTube-Client-Version", profile.clientVersion)
                    .post(body)

                if (!visitorData.isNullOrBlank()) {
                    requestBuilder.header("X-Goog-Visitor-Id", visitorData)
                }

                val selectedUrl = onDeviceHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    val jsonStr = response.body?.string()
                    if (!response.isSuccessful || jsonStr.isNullOrBlank()) {
                        null
                    } else {
                        val jsonObj = try {
                            Gson().fromJson(jsonStr, JsonObject::class.java)
                        } catch (_: Exception) {
                            null
                        }
                        jsonObj?.let { selectBestYouTubeStreamUrl(it) }
                    }
                }

                if (!selectedUrl.isNullOrBlank()) {
                    NetworkClient.registerStreamUserAgent(selectedUrl, profile.userAgent)
                    return selectedUrl
                }
            } catch (_: Exception) {
            }
        }

        return null
    }

    private fun extractFirstVideoIdFromSearchJson(responseText: String, excludedId: String?): String? {
        val rendererPattern = Regex(""""videoRenderer"\s*:\s*\{\s*"videoId"\s*:\s*"([a-zA-Z0-9_-]{11})"""")
        val rendererMatches = rendererPattern.findAll(responseText).map { it.groupValues[1] }.distinct().toList()
        if (rendererMatches.isNotEmpty()) {
            return if (!excludedId.isNullOrBlank()) {
                rendererMatches.firstOrNull { it != excludedId } ?: rendererMatches.firstOrNull()
            } else {
                rendererMatches.firstOrNull()
            }
        }

        val pattern = Regex(""""videoId"\s*:\s*"([a-zA-Z0-9_-]{11})"""")
        val matches = pattern.findAll(responseText).map { it.groupValues[1] }.distinct().toList()
        return if (!excludedId.isNullOrBlank()) {
            matches.firstOrNull { it != excludedId } ?: matches.firstOrNull()
        } else {
            matches.firstOrNull()
        }
    }

    private fun searchYouTubeVideoIdOnDevice(query: String, excludedId: String? = null): String? {
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val searchUrl = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false"
        val visitorData = getYouTubeVisitorData()

        // 1. Pencarian dengan context client WEB
        try {
            val webClient = mutableMapOf<String, Any>(
                "clientName" to "WEB",
                "clientVersion" to "2.20260401.00.00",
                "hl" to "en",
                "gl" to "US"
            )
            if (!visitorData.isNullOrBlank()) {
                webClient["visitorData"] = visitorData
            }
            val webPayload = mapOf(
                "context" to mapOf("client" to webClient),
                "query" to query
            )
            val webBody = Gson().toJson(webPayload).toRequestBody(jsonMediaType)
            val webRequestBuilder = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                .header("Content-Type", "application/json")
                .header("Origin", "https://www.youtube.com")
                .header("X-YouTube-Client-Name", "1")
                .header("X-YouTube-Client-Version", "2.20260401.00.00")
                .post(webBody)
            if (!visitorData.isNullOrBlank()) {
                webRequestBuilder.header("X-Goog-Visitor-Id", visitorData)
            }

            onDeviceHttpClient.newCall(webRequestBuilder.build()).execute().use { webResponse ->
                val responseText = webResponse.body?.string()
                if (webResponse.isSuccessful && !responseText.isNullOrBlank()) {
                    val candidate = extractFirstVideoIdFromSearchJson(responseText, excludedId)
                    if (!candidate.isNullOrBlank()) return candidate
                }
            }
        } catch (_: Exception) {
        }

        // 2. Fallback pencarian dengan context client ANDROID_VR
        try {
            val vrClient = mutableMapOf<String, Any>(
                "clientName" to "ANDROID_VR",
                "clientVersion" to "1.65.10",
                "androidSdkVersion" to 32,
                "hl" to "en",
                "gl" to "US"
            )
            if (!visitorData.isNullOrBlank()) {
                vrClient["visitorData"] = visitorData
            }
            val vrPayload = mapOf(
                "context" to mapOf("client" to vrClient),
                "query" to query
            )
            val vrBody = Gson().toJson(vrPayload).toRequestBody(jsonMediaType)
            val vrRequestBuilder = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip")
                .header("Content-Type", "application/json")
                .header("Origin", "https://www.youtube.com")
                .header("X-YouTube-Client-Name", "28")
                .header("X-YouTube-Client-Version", "1.65.10")
                .post(vrBody)
            if (!visitorData.isNullOrBlank()) {
                vrRequestBuilder.header("X-Goog-Visitor-Id", visitorData)
            }

            onDeviceHttpClient.newCall(vrRequestBuilder.build()).execute().use { vrResponse ->
                val responseText = vrResponse.body?.string()
                if (vrResponse.isSuccessful && !responseText.isNullOrBlank()) {
                    return extractFirstVideoIdFromSearchJson(responseText, excludedId)
                }
            }
        } catch (_: Exception) {
        }

        return null
    }

    private fun resolveYouTubeStreamOnDevice(
        trackId: String,
        title: String,
        artist: String,
        forceSearchAlternative: Boolean = false
    ): String? {
        val isYouTubeId = trackId.length == 11 && trackId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))

        // a) Jika bukan mode retry alternatif dan trackId adalah ID YouTube valid, ambil langsung
        if (!forceSearchAlternative && isYouTubeId) {
            val directUrl = fetchYouTubePlayerStreamUrl(trackId)
            if (!directUrl.isNullOrBlank()) {
                return directUrl
            }
        }

        // b) Cari videoId lewat search YouTube dengan query "$title $artist audio"
        val searchQuery = buildString {
            if (title.isNotBlank()) append(title.trim())
            if (artist.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(artist.trim())
            }
            if (isNotEmpty()) append(" ")
            append("audio")
        }.trim()

        val queryToUse = if (searchQuery.isNotBlank() && searchQuery != "audio") searchQuery else trackId
        val foundVideoId = searchYouTubeVideoIdOnDevice(
            query = queryToUse,
            excludedId = if (isYouTubeId) trackId else null
        )

        // c) Ambil stream URL dari hasil pencarian
        if (!foundVideoId.isNullOrBlank()) {
            val streamUrl = fetchYouTubePlayerStreamUrl(foundVideoId)
            if (!streamUrl.isNullOrBlank()) {
                return streamUrl
            }
        }

        // d) Jika mode retry alternatif belum menemukan ID lain, coba refresh trackId awal
        if (forceSearchAlternative && isYouTubeId) {
            val refreshedUrl = fetchYouTubePlayerStreamUrl(trackId)
            if (!refreshedUrl.isNullOrBlank()) {
                return refreshedUrl
            }
        }

        return null
    }

    private data class CachedStreamUrl(
        val url: String,
        val expiresAtMs: Long
    )

    private val resolvedStreamCache = ConcurrentHashMap<String, CachedStreamUrl>()

    private fun streamCacheKey(trackId: String, title: String, artist: String): String {
        return trackId.ifBlank { "${title.lowercase()}|${artist.lowercase()}" }
    }

    override suspend fun prewarmStreamUrl(track: Track) {
        val key = streamCacheKey(track.id, track.title, track.artist)
        val cached = resolvedStreamCache[key]
        if (cached != null && System.currentTimeMillis() < cached.expiresAtMs) {
            return
        }
        resolveStreamUrl(track.id, track.title, track.artist)
    }

    override suspend fun resolveStreamUrl(
        trackId: String,
        title: String,
        artist: String
    ): Result<String> = resolveStreamInternal(
        trackId = trackId,
        title = title,
        artist = artist,
        forceSearchAlternative = false
    )

    override suspend fun resolveAlternativeStreamUrl(
        trackId: String,
        title: String,
        artist: String
    ): Result<String> = resolveStreamInternal(
        trackId = trackId,
        title = title,
        artist = artist,
        forceSearchAlternative = true
    )

    private suspend fun resolveStreamInternal(
        trackId: String,
        title: String,
        artist: String,
        forceSearchAlternative: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        val cacheKey = streamCacheKey(trackId, title, artist)
        if (forceSearchAlternative) {
            resolvedStreamCache.remove(cacheKey)
        } else {
            val cached = resolvedStreamCache[cacheKey]
            if (cached != null && System.currentTimeMillis() < cached.expiresAtMs) {
                return@withContext Result.success(cached.url)
            }
        }

        // Prioritas Pertama: Ekstraksi stream YouTube langsung on-device menggunakan IP HP pengguna
        // agar bebas dari blokir IP server cloud ("Sign in to confirm you're not a bot").
        val onDeviceUrl = try {
            resolveYouTubeStreamOnDevice(trackId, title, artist, forceSearchAlternative)
        } catch (_: Exception) {
            null
        }

        if (!onDeviceUrl.isNullOrBlank() &&
            (onDeviceUrl.startsWith("http://") || onDeviceUrl.startsWith("https://")) &&
            !onDeviceUrl.contains("commondatastorage.googleapis.com") &&
            !onDeviceUrl.contains("codeskulptor")
        ) {
            if (resolvedStreamCache.size > 128) {
                resolvedStreamCache.clear()
            }
            resolvedStreamCache[cacheKey] = CachedStreamUrl(
                url = onDeviceUrl,
                expiresAtMs = System.currentTimeMillis() + 20L * 60L * 1000L
            )
            return@withContext Result.success(onDeviceUrl)
        }

        // Prioritas Kedua (Fallback): Gunakan backend audio jika on-device extraction gagal
        val activeBaseUrl = NetworkClient.getBaseUrl()
        val api = NetworkClient.getApiService()

        val response = try {
            api.getStream(
                trackId = trackId,
                title = title.ifBlank { null },
                artist = artist.ifBlank { null }
            )
        } catch (e: java.net.ConnectException) {
            return@withContext Result.failure(
                AudioStreamException.BackendUnreachable(
                    "Tidak dapat terhubung ke server backend audio ($activeBaseUrl). Periksa koneksi internet atau aktifkan server di Settings.",
                    e
                )
            )
        } catch (e: java.net.SocketTimeoutException) {
            return@withContext Result.failure(
                AudioStreamException.BackendUnreachable(
                    "Koneksi ke backend audio ($activeBaseUrl) melebihi batas waktu (timeout).",
                    e
                )
            )
        } catch (e: Exception) {
            return@withContext Result.failure(
                AudioStreamException.BackendUnreachable(
                    "Gagal menghubungi backend audio: ${e.localizedMessage ?: e.message}",
                    e
                )
            )
        }

        if (!response.isSuccessful) {
            val errorBodyStr = response.errorBody()?.string().orEmpty()
            val errorDto = try {
                Gson().fromJson(errorBodyStr, BackendErrorDto::class.java)
            } catch (_: Exception) {
                null
            }

            val detailMsg = errorDto?.detail.orEmpty()
            val isBotBlocked = errorDto?.code == "YOUTUBE_BOT_VERIFICATION" ||
                    detailMsg.contains("Sign in to confirm", ignoreCase = true) ||
                    detailMsg.contains("not a bot", ignoreCase = true) ||
                    errorBodyStr.contains("Sign in to confirm", ignoreCase = true) ||
                    errorBodyStr.contains("not a bot", ignoreCase = true)

            if (isBotBlocked) {
                return@withContext Result.failure(
                    AudioStreamException.YouTubeBotBlocked(
                        "YouTube menolak akses dari IP server cloud (${activeBaseUrl.trimEnd('/')}) dengan verifikasi: 'Sign in to confirm you\\'re not a bot'. Silakan beralih ke backend lokal atau hosting VPS."
                    )
                )
            }

            if (response.code() == 404) {
                val notFoundMsg = errorDto?.message ?: errorDto?.error ?: "Audio stream tidak ditemukan untuk track $trackId"
                return@withContext Result.failure(AudioStreamException.TrackNotFound(notFoundMsg))
            }

            return@withContext Result.failure(
                AudioStreamException.ServerError(
                    response.code(),
                    "Server backend mengembalikan HTTP ${response.code()}: ${errorDto?.message ?: errorDto?.error ?: errorBodyStr.take(120)}"
                )
            )
        }

        val body = response.body()
        if (body == null) {
            return@withContext Result.failure(
                AudioStreamException.InvalidStreamUrl("Respons backend kosong untuk track $trackId")
            )
        }

        // Determine final playable URL: proxy audio or direct googlevideo URL
        var finalUrl = body.streamUrl?.trim().orEmpty()
        if (finalUrl.isBlank() && !body.directUrl.isNullOrBlank()) {
            finalUrl = body.directUrl.trim()
        }

        if (finalUrl.isBlank()) {
            return@withContext Result.failure(
                AudioStreamException.InvalidStreamUrl("Backend tidak menyediakan streamUrl untuk track $trackId")
            )
        }

        // Resolve relative URL against active base URL and preserve HTTPS scheme behind reverse proxies
        if (finalUrl.startsWith("/")) {
            finalUrl = "${activeBaseUrl.trimEnd('/')}$finalUrl"
        } else if (activeBaseUrl.startsWith("https://", ignoreCase = true) && finalUrl.startsWith("http://", ignoreCase = true)) {
            val activeHost = activeBaseUrl.substringAfter("://").substringBefore("/")
            if (activeHost.isNotBlank() && finalUrl.startsWith("http://$activeHost", ignoreCase = true)) {
                finalUrl = "https://" + finalUrl.substring("http://".length)
            }
        }

        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
            return@withContext Result.failure(
                AudioStreamException.InvalidStreamUrl("Format URL stream tidak valid: $finalUrl")
            )
        }

        if (finalUrl.contains("commondatastorage.googleapis.com") || finalUrl.contains("codeskulptor")) {
            return@withContext Result.failure(
                AudioStreamException.InvalidStreamUrl("URL stream adalah sample dummy yang dilarang")
            )
        }

        Result.success(finalUrl)
    }

    override suspend fun getRecommendations(): Result<Map<String, List<Track>>> = withContext(Dispatchers.IO) {
        try {
            val api = NetworkClient.getApiService()
            val response = api.getRecommendations()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val result = mapOf(
                    "popular" to (body.popular?.map { mapDtoToTrack(it) } ?: defaultCuratedTracks),
                    "madeForYou" to (body.madeForYou?.map { mapDtoToTrack(it) } ?: defaultCuratedTracks.reversed()),
                    "recentlyPlayed" to (body.recentlyPlayed?.map { mapDtoToTrack(it) } ?: defaultCuratedTracks.take(3)),
                    "trending" to (body.trending?.map { mapDtoToTrack(it) } ?: defaultCuratedTracks)
                )
                Result.success(result)
            } else {
                Result.failure(Exception("Gagal memuat rekomendasi: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class SpotifyTrackMetadata(
        val title: String,
        val artist: String,
        val durationSec: Int,
        val spotifyTrackId: String
    )

    private data class SpotifyEmbedPayload(
        val id: String,
        val title: String,
        val description: String,
        val artwork: String,
        val tracks: List<SpotifyTrackMetadata>
    )

    private fun getSafeLong(jsonObj: JsonObject, key: String): Long? {
        return try {
            if (jsonObj.has(key) && !jsonObj.get(key).isJsonNull) jsonObj.get(key).asLong else null
        } catch (_: Exception) {
            null
        }
    }

    private fun getSafeObj(jsonObj: JsonObject, key: String): JsonObject? {
        return try {
            val el = jsonObj.get(key)
            if (el != null && el.isJsonObject) el.asJsonObject else null
        } catch (_: Exception) {
            null
        }
    }

    private fun getSafeArray(jsonObj: JsonObject, key: String): JsonArray? {
        return try {
            val el = jsonObj.get(key)
            if (el != null && el.isJsonArray) el.asJsonArray else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseSpotifyEntityTarget(rawInput: String): Pair<String, String>? {
        var resolved = rawInput.trim()
        if (resolved.isBlank()) return null

        // Expand short links such as spotify.link / spotify.app.link
        if (resolved.contains("spotify.link", ignoreCase = true) ||
            resolved.contains("spotify.app.link", ignoreCase = true)
        ) {
            try {
                val targetUrl = if (resolved.startsWith("http", ignoreCase = true)) resolved else "https://$resolved"
                val req = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                    .get()
                    .build()
                onDeviceHttpClient.newCall(req).execute().use { resp ->
                    resolved = resp.request.url.toString()
                }
            } catch (_: Exception) {
            }
        }

        Regex("""spotify:(playlist|album|track):([a-zA-Z0-9]{10,})""", RegexOption.IGNORE_CASE)
            .find(resolved)
            ?.let { match ->
                return match.groupValues[1].lowercase() to match.groupValues[2]
            }

        Regex("""open\.spotify\.com/(?:intl-[a-zA-Z-]+/)?(playlist|album|track)/([a-zA-Z0-9]{10,})""", RegexOption.IGNORE_CASE)
            .find(resolved)
            ?.let { match ->
                return match.groupValues[1].lowercase() to match.groupValues[2]
            }

        if (Regex("""^[a-zA-Z0-9]{22}$""").matches(resolved)) {
            return "playlist" to resolved
        }

        return null
    }

    private fun fetchSpotifyEmbedOnDevice(entityType: String, entityId: String): SpotifyEmbedPayload? {
        val embedUrl = "https://open.spotify.com/embed/$entityType/$entityId"
        val request = Request.Builder()
            .url(embedUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
            .get()
            .build()

        val html = try {
            onDeviceHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        } catch (_: Exception) {
            return null
        } ?: return null

        val nextDataJson = Regex(
            """<script id="__NEXT_DATA__"[^>]*>(.*?)</script>""",
            RegexOption.DOT_MATCHES_ALL
        ).find(html)?.groupValues?.getOrNull(1) ?: return null

        val root = try {
            Gson().fromJson(nextDataJson, JsonObject::class.java)
        } catch (_: Exception) {
            null
        } ?: return null

        val entity = getSafeObj(root, "props")
            ?.let { getSafeObj(it, "pageProps") }
            ?.let { getSafeObj(it, "state") }
            ?.let { getSafeObj(it, "data") }
            ?.let { getSafeObj(it, "entity") }
            ?: return null

        val title = (getSafeString(entity, "name") ?: getSafeString(entity, "title") ?: "Imported Spotify Playlist")
            .replace('\u00A0', ' ')
            .trim()
        val subtitle = getSafeString(entity, "subtitle")
            ?.replace('\u00A0', ' ')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val description = getSafeString(entity, "description")
            ?.takeIf { it.isNotBlank() }
            ?: if (subtitle != null) "By $subtitle • Imported from Spotify" else "Imported from Spotify"

        val coverArtUrl = getSafeObj(entity, "coverArt")
            ?.let { getSafeArray(it, "sources") }
            ?.firstOrNull()
            ?.takeIf { it.isJsonObject }
            ?.let { getSafeString(it.asJsonObject, "url") }
            ?: getSafeObj(entity, "visualIdentity")
                ?.let { getSafeArray(it, "image") }
                ?.firstOrNull()
                ?.takeIf { it.isJsonObject }
                ?.let { getSafeString(it.asJsonObject, "url") }
            ?: getSafeArray(entity, "images")
                ?.firstOrNull()
                ?.takeIf { it.isJsonObject }
                ?.let { getSafeString(it.asJsonObject, "url") }
            ?: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"

        val parsedTracks = mutableListOf<SpotifyTrackMetadata>()
        val trackList = getSafeArray(entity, "trackList")
        if (trackList != null && trackList.size() > 0) {
            for (element in trackList) {
                if (!element.isJsonObject) continue
                val item = element.asJsonObject
                val tTitle = getSafeString(item, "title")?.replace('\u00A0', ' ')?.trim().orEmpty()
                if (tTitle.isBlank()) continue
                val tArtist = getSafeString(item, "subtitle")?.replace('\u00A0', ' ')?.trim().orEmpty()
                val durationMs = getSafeLong(item, "duration") ?: 0L
                val durationSec = (durationMs / 1000L).toInt().coerceAtLeast(0)
                val uri = getSafeString(item, "uri").orEmpty()
                val spotifyId = uri.substringAfterLast(":").trim()
                parsedTracks.add(
                    SpotifyTrackMetadata(
                        title = tTitle,
                        artist = tArtist,
                        durationSec = durationSec,
                        spotifyTrackId = spotifyId
                    )
                )
            }
        } else if (entityType == "track" && title.isNotBlank()) {
            val durationMs = getSafeLong(entity, "duration") ?: 0L
            parsedTracks.add(
                SpotifyTrackMetadata(
                    title = title,
                    artist = subtitle.orEmpty(),
                    durationSec = (durationMs / 1000L).toInt().coerceAtLeast(0),
                    spotifyTrackId = entityId
                )
            )
        }

        if (parsedTracks.isEmpty()) return null

        // Spotify Embed caps trackList at 100 items; paginate remaining items via Pathfinder using embed's session token.
        val sessionToken = getSafeObj(root, "props")
            ?.let { getSafeObj(it, "pageProps") }
            ?.let { getSafeObj(it, "state") }
            ?.let { getSafeObj(it, "settings") }
            ?.let { getSafeObj(it, "session") }
            ?.let { getSafeString(it, "accessToken") }

        if (entityType == "playlist" && !sessionToken.isNullOrBlank() && parsedTracks.size >= 50) {
            fetchAdditionalSpotifyPlaylistTracks(
                playlistId = entityId,
                accessToken = sessionToken,
                startOffset = parsedTracks.size,
                outTracks = parsedTracks
            )
        }

        return SpotifyEmbedPayload(
            id = entityId,
            title = title,
            description = description,
            artwork = coverArtUrl,
            tracks = parsedTracks
        )
    }

    private fun fetchAdditionalSpotifyPlaylistTracks(
        playlistId: String,
        accessToken: String,
        startOffset: Int,
        outTracks: MutableList<SpotifyTrackMetadata>
    ) {
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val pathfinderUrl = "https://api-partner.spotify.com/pathfinder/v2/query"
        val persistedHash = "9c53fb83f35c6a177be88bf1b67cb080b853e86b576ed174216faa8f9164fc8f"
        val pageSize = 100

        var offset = startOffset
        var totalCount = Int.MAX_VALUE
        var pageGuard = 0

        while (offset < totalCount && pageGuard++ < 100) {
            val payload = mapOf(
                "variables" to mapOf(
                    "uri" to "spotify:playlist:$playlistId",
                    "offset" to offset,
                    "limit" to pageSize,
                    "enableWatchFeedEntrypoint" to false
                ),
                "operationName" to "fetchPlaylistContents",
                "extensions" to mapOf(
                    "persistedQuery" to mapOf(
                        "version" to 1,
                        "sha256Hash" to persistedHash
                    )
                )
            )

            val body = Gson().toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(pathfinderUrl)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                .post(body)
                .build()

            val responseJson = try {
                onDeviceHttpClient.newCall(request).execute().use { resp ->
                    val raw = resp.body?.string()
                    if (!resp.isSuccessful || raw.isNullOrBlank()) null
                    else Gson().fromJson(raw, JsonObject::class.java)
                }
            } catch (_: Exception) {
                null
            } ?: break

            val content = getSafeObj(responseJson, "data")
                ?.let { getSafeObj(it, "playlistV2") }
                ?.let { getSafeObj(it, "content") }
                ?: break

            val reportedTotal = getSafeInt(content, "totalCount")
            if (reportedTotal != null && reportedTotal > 0) {
                totalCount = reportedTotal
            }

            val items = getSafeArray(content, "items") ?: break
            if (items.size() == 0) break

            for (element in items) {
                if (!element.isJsonObject) continue
                val elemObj = element.asJsonObject
                val itemData = getSafeObj(elemObj, "itemV2")?.let { getSafeObj(it, "data") } ?: continue
                val tTitle = getSafeString(itemData, "name")?.replace('\u00A0', ' ')?.trim().orEmpty()
                if (tTitle.isBlank()) continue

                val artistsArray = getSafeObj(itemData, "artists")?.let { getSafeArray(it, "items") }
                val artistNames = mutableListOf<String>()
                if (artistsArray != null) {
                    for (artistEl in artistsArray) {
                        if (!artistEl.isJsonObject) continue
                        val artistName = getSafeObj(artistEl.asJsonObject, "profile")
                            ?.let { getSafeString(it, "name") }
                            ?.replace('\u00A0', ' ')
                            ?.trim()
                        if (!artistName.isNullOrBlank()) {
                            artistNames.add(artistName)
                        }
                    }
                }
                val tArtist = if (artistNames.isNotEmpty()) {
                    artistNames.joinToString(", ")
                } else {
                    (getSafeString(itemData, "artistName")
                        ?: getSafeObj(itemData, "podcastV2")
                            ?.let { getSafeObj(it, "data") }
                            ?.let { getSafeString(it, "name") }
                        ?: "")
                        .replace('\u00A0', ' ')
                        .trim()
                }

                val durationMs = getSafeObj(itemData, "trackDuration")?.let { getSafeLong(it, "totalMilliseconds") }
                    ?: getSafeObj(itemData, "duration")?.let { getSafeLong(it, "totalMilliseconds") }
                    ?: 0L
                val durationSec = (durationMs / 1000L).toInt().coerceAtLeast(0)
                val uri = getSafeString(itemData, "uri").orEmpty()
                val spotifyId = uri.substringAfterLast(":").trim()

                outTracks.add(
                    SpotifyTrackMetadata(
                        title = tTitle,
                        artist = tArtist,
                        durationSec = durationSec,
                        spotifyTrackId = spotifyId
                    )
                )
            }

            offset += items.size()
            if (items.size() < pageSize && reportedTotal == null) break
        }
    }

    override fun importSpotifyPlaylist(urlOrId: String): Flow<SpotifyImportState> = flow {
        emit(
            SpotifyImportState(
                status = ImportStatus.READING_PLAYLIST,
                progressPercent = 0.1f
            )
        )

        val target = parseSpotifyEntityTarget(urlOrId)
        if (target == null) {
            emit(
                SpotifyImportState(
                    status = ImportStatus.ERROR,
                    errorMessage = "Link Spotify tidak valid. Gunakan link playlist/album/lagu open.spotify.com."
                )
            )
            return@flow
        }

        // Prioritas 1: Ekstrak metadata playlist langsung dari Spotify Embed on-device (cepat, akurat, bebas timeout)
        val onDeviceEmbed = withContext(Dispatchers.IO) {
            fetchSpotifyEmbedOnDevice(target.first, target.second)
        }

        if (onDeviceEmbed != null && onDeviceEmbed.tracks.isNotEmpty()) {
            val totalCount = onDeviceEmbed.tracks.size
            emit(
                SpotifyImportState(
                    status = ImportStatus.SEARCHING_MATCHES,
                    totalTracks = totalCount,
                    progressPercent = 0.2f
                )
            )

            val orderedTracks = arrayOfNulls<Track>(totalCount)
            val completedItems = mutableListOf<TrackImportItem>()
            var matchedCount = 0
            val semaphore = Semaphore(8)
            val progressChannel = Channel<Triple<Int, TrackImportItem, Track>>(Channel.UNLIMITED)

            coroutineScope {
                val producerJob = launch(Dispatchers.IO) {
                    val jobs = onDeviceEmbed.tracks.mapIndexed { idx, spTrack ->
                        launch {
                            semaphore.withPermit {
                                val query = if (spTrack.artist.isNotBlank()) {
                                    "${spTrack.artist} - ${spTrack.title}"
                                } else {
                                    spTrack.title
                                }
                                val ytId = searchYouTubeVideoIdOnDevice(query)
                                    ?: if (spTrack.artist.isNotBlank()) searchYouTubeVideoIdOnDevice(spTrack.title) else null

                                val isMatched = !ytId.isNullOrBlank()
                                val track = Track(
                                    id = ytId ?: "sp_${spTrack.spotifyTrackId.ifBlank { "${idx}_${spTrack.title.hashCode()}" }}",
                                    title = spTrack.title,
                                    artist = spTrack.artist.ifBlank { "Unknown Artist" },
                                    album = onDeviceEmbed.title,
                                    thumbnail = if (isMatched) {
                                        "https://i.ytimg.com/vi/$ytId/hqdefault.jpg"
                                    } else {
                                        onDeviceEmbed.artwork
                                    },
                                    duration = spTrack.durationSec,
                                    streamUrl = if (isMatched) "/api/stream/$ytId" else "",
                                    source = if (isMatched) "youtube" else "spotify_imported"
                                )
                                val importItem = TrackImportItem(
                                    title = spTrack.title,
                                    artist = spTrack.artist,
                                    status = if (isMatched) ItemMatchStatus.MATCHED else ItemMatchStatus.NOT_FOUND
                                )
                                progressChannel.send(Triple(idx, importItem, track))
                            }
                        }
                    }
                    jobs.joinAll()
                    progressChannel.close()
                }

                for ((idx, item, track) in progressChannel) {
                    orderedTracks[idx] = track
                    if (item.status == ItemMatchStatus.MATCHED) matchedCount++
                    completedItems.add(item)
                    val completedCount = completedItems.size
                    val progress = 0.2f + (0.75f * (completedCount.toFloat() / totalCount.coerceAtLeast(1)))
                    emit(
                        SpotifyImportState(
                            status = ImportStatus.SEARCHING_MATCHES,
                            totalTracks = totalCount,
                            matchedTracks = matchedCount,
                            unavailableTracks = completedCount - matchedCount,
                            progressPercent = progress,
                            currentSearchingTitle = item.title,
                            items = completedItems.toList()
                        )
                    )
                }
                producerJob.join()
            }

            val rawOrdered = orderedTracks.filterNotNull()
            val seenTrackIds = HashSet<String>(rawOrdered.size)
            val finalTracks = rawOrdered.mapIndexed { idx, track ->
                if (seenTrackIds.add(track.id)) {
                    track
                } else {
                    track.copy(id = "sp_dup_${idx}_${track.id}")
                }
            }
            val playlist = Playlist(
                id = onDeviceEmbed.id,
                name = onDeviceEmbed.title,
                description = onDeviceEmbed.description,
                artwork = onDeviceEmbed.artwork,
                source = "spotify",
                createdAt = System.currentTimeMillis(),
                trackCount = finalTracks.size
            )

            localDataSource.saveImportedPlaylist(playlist, finalTracks)

            emit(
                SpotifyImportState(
                    status = ImportStatus.SUCCESS,
                    totalTracks = totalCount,
                    matchedTracks = matchedCount,
                    unavailableTracks = totalCount - matchedCount,
                    progressPercent = 1.0f,
                    items = completedItems.toList(),
                    importedPlaylist = playlist
                )
            )
            return@flow
        }

        // Prioritas 2: Fallback ke backend API bila Spotify Embed tidak bisa diakses langsung dari HP
        try {
            val api = NetworkClient.getApiService()
            val response = api.importSpotifyPlaylist(SpotifyImportRequest(url = urlOrId))
            val body = if (response.isSuccessful) response.body() else null
            val rawTracks = body?.tracks ?: emptyList()
            val isLegacyFakeFallback = body?.playlist?.title == "Glassroom Spotify Selections"

            if (body != null && rawTracks.isNotEmpty() && !isLegacyFakeFallback) {
                val totalCount = body.totalCount ?: rawTracks.size

                emit(
                    SpotifyImportState(
                        status = ImportStatus.SEARCHING_MATCHES,
                        totalTracks = totalCount,
                        progressPercent = 0.45f
                    )
                )

                val itemsList = mutableListOf<TrackImportItem>()
                val domainTracks = mutableListOf<Track>()

                rawTracks.forEachIndexed { index, tDto ->
                    val track = mapDtoToTrack(tDto)
                    domainTracks.add(track)
                    itemsList.add(
                        TrackImportItem(
                            title = track.title,
                            artist = track.artist,
                            status = ItemMatchStatus.MATCHED
                        )
                    )
                    if (index == rawTracks.lastIndex || index % 5 == 0) {
                        val progress = 0.45f + (0.50f * ((index + 1).toFloat() / totalCount.coerceAtLeast(1)))
                        emit(
                            SpotifyImportState(
                                status = ImportStatus.SEARCHING_MATCHES,
                                totalTracks = totalCount,
                                matchedTracks = itemsList.size,
                                progressPercent = progress,
                                currentSearchingTitle = track.title,
                                items = itemsList.toList()
                            )
                        )
                    }
                }

                val pDto = body.playlist
                val playlistId = pDto?.id ?: target.second
                val playlist = Playlist(
                    id = playlistId,
                    name = pDto?.title ?: "Imported Spotify Playlist",
                    description = pDto?.description ?: "Imported from Spotify",
                    artwork = pDto?.artwork ?: (domainTracks.firstOrNull()?.thumbnail ?: ""),
                    source = "spotify",
                    createdAt = System.currentTimeMillis(),
                    trackCount = domainTracks.size
                )

                localDataSource.saveImportedPlaylist(playlist, domainTracks)

                emit(
                    SpotifyImportState(
                        status = ImportStatus.SUCCESS,
                        totalTracks = totalCount,
                        matchedTracks = domainTracks.size,
                        progressPercent = 1.0f,
                        items = itemsList.toList(),
                        importedPlaylist = playlist
                    )
                )
                return@flow
            }
        } catch (_: Exception) {
        }

        emit(
            SpotifyImportState(
                status = ImportStatus.ERROR,
                errorMessage = "Gagal membaca playlist dari Spotify. Pastikan playlist bersifat Public dan koneksi internet stabil."
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun getCuratedRecommendations(): Map<String, List<Track>> {
        return mapOf(
            "popular" to defaultCuratedTracks,
            "madeForYou" to defaultCuratedTracks.reversed(),
            "recentlyPlayed" to defaultCuratedTracks.take(3),
            "trending" to defaultCuratedTracks
        )
    }

    private fun mapDtoToTrack(dto: TrackDto): Track {
        return Track(
            id = dto.id ?: UUID.randomUUID().toString(),
            title = dto.title ?: "Untitled",
            artist = dto.artist ?: "Unknown Artist",
            album = dto.album ?: "",
            thumbnail = dto.thumbnail ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            duration = dto.duration ?: 0,
            streamUrl = dto.streamUrl ?: "",
            source = dto.source ?: "youtube"
        )
    }

    private val lrclibHttpClient by lazy {
        onDeviceHttpClient.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun getLyrics(title: String, artist: String): Result<Lyrics> = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext Result.failure(IllegalArgumentException("Title is empty"))

        // 1. Try active backend server first
        try {
            val api = NetworkClient.getApiService()
            val response = api.getLyrics(title = title, artist = artist)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val lines = LyricsParser.parse(body.syncedLyrics)
                return@withContext Result.success(
                    Lyrics(
                        trackTitle = body.trackName ?: title,
                        artistName = body.artistName ?: artist,
                        plainLyrics = body.plainLyrics,
                        syncedLyrics = body.syncedLyrics,
                        lines = lines
                    )
                )
            }
        } catch (_: Exception) {
            // Active backend failed or has no lyrics, fallback directly to LRCLIB
        }

        // 2. Direct fallback to LRCLIB (Directly works on-device even if local backend is offline)
        try {
            val cleanTitle = title.replace(Regex("""[\(\[\{].*?[\)\]\}]"""), "").trim()
                .replace(Regex("""\b(feat|ft)\.?\s+.*$""", RegexOption.IGNORE_CASE), "").trim()
            val cleanArtist = artist.replace(Regex("""[\(\[\{].*?[\)\]\}]"""), "").trim()

            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")

            val getUrl = "https://lrclib.net/api/get?track_name=$encodedTitle&artist_name=$encodedArtist"
            val request = Request.Builder()
                .url(getUrl)
                .header("User-Agent", "Veloura/1.0 (Android Music Player)")
                .build()

            lrclibHttpClient.newCall(request).execute().use { response ->
                val jsonStr = response.body?.string()
                if (response.isSuccessful && !jsonStr.isNullOrBlank()) {
                    val dto = Gson().fromJson(jsonStr, LyricsDto::class.java)
                    val lines = LyricsParser.parse(dto.syncedLyrics)
                    return@withContext Result.success(
                        Lyrics(
                            trackTitle = dto.trackName ?: title,
                            artistName = dto.artistName ?: artist,
                            plainLyrics = dto.plainLyrics,
                            syncedLyrics = dto.syncedLyrics,
                            lines = lines
                        )
                    )
                }
            }

            // 2b. Direct search fallback on LRCLIB
            val searchQuery = "$cleanArtist $cleanTitle".trim()
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            val searchUrl = "https://lrclib.net/api/search?q=$encodedQuery"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Veloura/1.0 (Android Music Player)")
                .build()

            lrclibHttpClient.newCall(searchRequest).execute().use { searchResponse ->
                val jsonStr = searchResponse.body?.string()
                if (searchResponse.isSuccessful && !jsonStr.isNullOrBlank()) {
                    val listType = object : TypeToken<List<LyricsDto>>() {}.type
                    val dtoList: List<LyricsDto>? = Gson().fromJson(jsonStr, listType)
                    val first = dtoList?.firstOrNull()
                    if (first != null) {
                        val lines = LyricsParser.parse(first.syncedLyrics)
                        return@withContext Result.success(
                            Lyrics(
                                trackTitle = first.trackName ?: title,
                                artistName = first.artistName ?: artist,
                                plainLyrics = first.plainLyrics,
                                syncedLyrics = first.syncedLyrics,
                                lines = lines
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        Result.failure(Exception("Lyrics not available for $title"))
    }
}
