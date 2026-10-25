package com.glassroom.music.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    // Production Cloud Server on Vercel
    const val CLOUD_SERVER_URL = "https://veloura-lilac-sigma.vercel.app/"

    // Development Presets for explicit user selection
    const val WIFI_SERVER_URL = "http://172.30.189.56:5000/"
    const val ADB_REVERSE_URL = "http://127.0.0.1:5000/"
    const val EMULATOR_URL = "http://10.0.2.2:5000/"

    // Default base URL strictly points to cloud production
    const val DEFAULT_BASE_URL = CLOUD_SERVER_URL

    @Volatile
    private var currentBaseUrl: String = DEFAULT_BASE_URL

    @Volatile
    private var cachedApiService: MusicApiService? = null

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    /**
     * Normalizes a base URL ensuring correct scheme (http/https) and trailing slash.
     */
    fun normalizeBaseUrl(raw: String): String {
        var url = raw.trim()
        if (url.isBlank()) return DEFAULT_BASE_URL
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "https://$url"
        }
        if (!url.endsWith("/")) {
            url = "$url/"
        }
        return url
    }

    /**
     * Returns the Retrofit service for the active backend server.
     * Caches the instance thread-safely until updateBaseUrl is invoked.
     */
    @Synchronized
    fun getApiService(baseUrl: String? = null): MusicApiService {
        val targetUrl = if (!baseUrl.isNullOrBlank()) normalizeBaseUrl(baseUrl) else currentBaseUrl

        val cached = cachedApiService
        if (cached != null && currentBaseUrl == targetUrl) {
            return cached
        }

        currentBaseUrl = targetUrl
        val retrofit = Retrofit.Builder()
            .baseUrl(targetUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MusicApiService::class.java)
        cachedApiService = service
        return service
    }

    /**
     * Creates an isolated service instance without mutating the global active currentBaseUrl.
     * Used by Settings to test a candidate server before applying it.
     */
    fun createServiceForUrl(url: String): MusicApiService {
        val targetUrl = normalizeBaseUrl(url)
        val retrofit = Retrofit.Builder()
            .baseUrl(targetUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(MusicApiService::class.java)
    }

    /**
     * Updates the active base URL thread-safely and invalidates the cached Retrofit instance.
     */
    @Synchronized
    fun updateBaseUrl(newUrl: String) {
        val normalized = normalizeBaseUrl(newUrl)
        if (currentBaseUrl != normalized) {
            currentBaseUrl = normalized
            cachedApiService = null
        }
    }

    fun getBaseUrl(): String = currentBaseUrl
}
