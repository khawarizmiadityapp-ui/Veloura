package com.glassroom.music.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TrackDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("artist") val artist: String?,
    @SerializedName("album") val album: String?,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("streamUrl") val streamUrl: String?,
    @SerializedName("source") val source: String?
)

data class SearchResponseDto(
    @SerializedName("tracks") val tracks: List<TrackDto>?,
    @SerializedName("artists") val artists: List<String>?,
    @SerializedName("albums") val albums: List<String>?
)

data class RecommendationsResponseDto(
    @SerializedName("popular") val popular: List<TrackDto>?,
    @SerializedName("madeForYou") val madeForYou: List<TrackDto>?,
    @SerializedName("recentlyPlayed") val recentlyPlayed: List<TrackDto>?,
    @SerializedName("trending") val trending: List<TrackDto>?
)

data class StreamResponseDto(
    @SerializedName("id") val id: String?,
    @SerializedName("youtubeId") val youtubeId: String? = null,
    @SerializedName("streamUrl") val streamUrl: String?,
    @SerializedName("directUrl") val directUrl: String? = null,
    @SerializedName("format") val format: String?,
    @SerializedName("source") val source: String?,
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("detail") val detail: String? = null
)

data class BackendErrorDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("detail") val detail: String? = null,
    @SerializedName("id") val id: String? = null
)

data class HealthResponseDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("service") val service: String? = null,
    @SerializedName("version") val version: String? = null,
    @SerializedName("yt_dlp_version") val ytDlpVersion: String? = null,
    @SerializedName("environment") val environment: String? = null
)

data class SpotifyImportRequest(
    @SerializedName("url") val url: String
)

data class SpotifyPlaylistDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("artwork") val artwork: String?,
    @SerializedName("trackCount") val trackCount: Int?,
    @SerializedName("source") val source: String?
)

data class SpotifyImportResponseDto(
    @SerializedName("playlist") val playlist: SpotifyPlaylistDto?,
    @SerializedName("tracks") val tracks: List<TrackDto>?,
    @SerializedName("matchedCount") val matchedCount: Int?,
    @SerializedName("unavailableCount") val unavailableCount: Int?,
    @SerializedName("totalCount") val totalCount: Int?,
    @SerializedName("error") val error: String?
)
