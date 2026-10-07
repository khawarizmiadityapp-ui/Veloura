package com.glassroom.music.domain.model

sealed interface MusicSource {
    val id: String
    val displayName: String

    data object YouTube : MusicSource {
        override val id: String = "youtube"
        override val displayName: String = "YouTube Audio"
    }

    data object SpotifyMetadata : MusicSource {
        override val id: String = "spotify"
        override val displayName: String = "Spotify Playlist Import"
    }

    data object Curated : MusicSource {
        override val id: String = "curated"
        override val displayName: String = "Glassroom Curated"
    }

    data class CustomSource(override val id: String, override val displayName: String) : MusicSource
}
