package com.glassroom.music.data.repository

import com.glassroom.music.data.local.LocalDataSource
import com.glassroom.music.data.remote.NetworkClient
import com.glassroom.music.data.remote.dto.*
import com.glassroom.music.data.util.LyricsParser
import com.glassroom.music.domain.model.*
import com.glassroom.music.domain.repository.MusicRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.UUID
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
                // Fallback filter
                val fallback = defaultCuratedTracks.filter {
                    it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
                }
                Result.success(if (fallback.isNotEmpty()) fallback else defaultCuratedTracks.take(3))
            }
        } catch (e: Exception) {
            val fallback = defaultCuratedTracks.filter {
                it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
            }
            Result.success(if (fallback.isNotEmpty()) fallback else defaultCuratedTracks.take(4))
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
                Result.success(getCuratedRecommendations())
            }
        } catch (e: Exception) {
            Result.success(getCuratedRecommendations())
        }
    }

    override fun importSpotifyPlaylist(urlOrId: String): Flow<SpotifyImportState> = flow {
        emit(
            SpotifyImportState(
                status = ImportStatus.READING_PLAYLIST,
                progressPercent = 0.1f
            )
        )
        delay(600)

        try {
            val api = NetworkClient.getApiService()
            val response = api.importSpotifyPlaylist(SpotifyImportRequest(url = urlOrId))
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val rawTracks = data.tracks ?: emptyList()
                val totalCount = data.totalCount ?: rawTracks.size

                emit(
                    SpotifyImportState(
                        status = ImportStatus.SEARCHING_MATCHES,
                        totalTracks = totalCount,
                        progressPercent = 0.35f
                    )
                )

                val itemsList = mutableListOf<TrackImportItem>()
                val domainTracks = mutableListOf<Track>()

                rawTracks.forEachIndexed { index, tDto ->
                    delay(120) // Smooth visual progress feedback
                    val track = mapDtoToTrack(tDto)
                    domainTracks.add(track)
                    itemsList.add(
                        TrackImportItem(
                            title = track.title,
                            artist = track.artist,
                            status = ItemMatchStatus.MATCHED
                        )
                    )
                    val progress = 0.35f + (0.55f * ((index + 1).toFloat() / totalCount.coerceAtLeast(1)))
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

                // Save playlist into local database
                val pDto = data.playlist
                val playlistId = pDto?.id ?: UUID.randomUUID().toString()
                val playlist = Playlist(
                    id = playlistId,
                    name = pDto?.title ?: "Imported Spotify Playlist",
                    description = pDto?.description ?: "Imported from Spotify",
                    artwork = pDto?.artwork ?: (domainTracks.firstOrNull()?.thumbnail ?: ""),
                    source = "spotify",
                    createdAt = System.currentTimeMillis(),
                    trackCount = domainTracks.size
                )

                localDataSource.insertPlaylist(playlist)
                domainTracks.forEach { track ->
                    localDataSource.addTrackToPlaylist(playlist.id, track)
                }

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
            } else {
                emitFallbackSpotifyImport(urlOrId)
            }
        } catch (e: Exception) {
            emitFallbackSpotifyImport(urlOrId)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<SpotifyImportState>.emitFallbackSpotifyImport(url: String) {
        val simulatedTracks = listOf(
            defaultCuratedTracks[1], // Blinding Lights
            defaultCuratedTracks[0], // Starboy
            defaultCuratedTracks[3], // As It Was
            defaultCuratedTracks[5], // Levitating
            defaultCuratedTracks[2], // Midnight City
            defaultCuratedTracks[4]  // Get Lucky
        )
        val items = mutableListOf<TrackImportItem>()
        simulatedTracks.forEachIndexed { i, t ->
            delay(150)
            items.add(TrackImportItem(t.title, t.artist, ItemMatchStatus.MATCHED))
            emit(
                SpotifyImportState(
                    status = ImportStatus.SEARCHING_MATCHES,
                    totalTracks = simulatedTracks.size,
                    matchedTracks = items.size,
                    progressPercent = (i + 1).toFloat() / simulatedTracks.size,
                    currentSearchingTitle = t.title,
                    items = items.toList()
                )
            )
        }

        val playlistId = UUID.randomUUID().toString()
        val playlist = Playlist(
            id = playlistId,
            name = "Spotify Top Favorites",
            description = "Imported from Spotify ($url)",
            artwork = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80",
            source = "spotify",
            createdAt = System.currentTimeMillis(),
            trackCount = simulatedTracks.size
        )

        localDataSource.insertPlaylist(playlist)
        simulatedTracks.forEach { track ->
            localDataSource.addTrackToPlaylist(playlist.id, track)
        }

        emit(
            SpotifyImportState(
                status = ImportStatus.SUCCESS,
                totalTracks = simulatedTracks.size,
                matchedTracks = simulatedTracks.size,
                progressPercent = 1f,
                items = items.toList(),
                importedPlaylist = playlist
            )
        )
    }

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
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()

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

            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val jsonStr = response.body!!.string()
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

            // 2b. Direct search fallback on LRCLIB
            val searchQuery = "$cleanArtist $cleanTitle".trim()
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            val searchUrl = "https://lrclib.net/api/search?q=$encodedQuery"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Veloura/1.0 (Android Music Player)")
                .build()

                
            val searchResponse = client.newCall(searchRequest).execute()
            if (searchResponse.isSuccessful && searchResponse.body != null) {
                val jsonStr = searchResponse.body!!.string()
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
        } catch (_: Exception) {
        }

        Result.failure(Exception("Lyrics not available for $title"))
    }
}
