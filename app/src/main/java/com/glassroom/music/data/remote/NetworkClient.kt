package com.glassroom.music.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    // Default emulator local address to backend
    const val DEFAULT_BASE_URL = "http://10.0.2.2:5000/"

    private var currentBaseUrl: String = DEFAULT_BASE_URL
    private var cachedApiService: MusicApiService? = null

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    @Synchronized
    fun getApiService(baseUrl: String = currentBaseUrl): MusicApiService {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (cachedApiService != null && currentBaseUrl == sanitizedUrl) {
            return cachedApiService!!
        }

        currentBaseUrl = sanitizedUrl
        val retrofit = Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MusicApiService::class.java)
        cachedApiService = service
        return service
    }

    fun updateBaseUrl(newUrl: String) {
        val sanitized = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        if (currentBaseUrl != sanitized) {
            currentBaseUrl = sanitized
            cachedApiService = null
        }
    }

    fun getBaseUrl(): String = currentBaseUrl
}
