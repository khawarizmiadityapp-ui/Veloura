package com.glassroom.music.data.remote

import com.glassroom.music.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface MusicApiService {
    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("category") category: String = "songs"
    ): Response<SearchResponseDto>

    @GET("api/track/{id}")
    suspend fun getTrack(
        @Path("id") trackId: String
    ): Response<TrackDto>

    @GET("api/stream/{id}")
    suspend fun getStream(
        @Path("id") trackId: String
    ): Response<StreamResponseDto>

    @GET("api/recommendations")
    suspend fun getRecommendations(): Response<RecommendationsResponseDto>

    @POST("api/spotify/import")
    suspend fun importSpotifyPlaylist(
        @Body request: SpotifyImportRequest
    ): Response<SpotifyImportResponseDto>

    @GET("api/health")
    suspend fun healthCheck(): Response<Map<String, String>>
}
