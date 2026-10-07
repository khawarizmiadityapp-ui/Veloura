package com.glassroom.music.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val artwork: String = "",
    val source: String = "local", // "local" or "spotify"
    val createdAt: Long = System.currentTimeMillis(),
    val trackCount: Int = 0,
    val isFavorite: Boolean = false
) {
    val isSpotifyImport: Boolean
        get() = source.equals("spotify", ignoreCase = true)
}

data class PlaylistWithTracks(
    val playlist: Playlist,
    val tracks: List<Track>
)
