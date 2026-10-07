package com.glassroom.music.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val thumbnail: String = "",
    val duration: Int = 0, // seconds
    val streamUrl: String = "",
    val source: String = "youtube",
    val isFavorite: Boolean = false
) {
    val durationFormatted: String
        get() {
            if (duration <= 0) return "--:--"
            val minutes = duration / 60
            val seconds = duration % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
