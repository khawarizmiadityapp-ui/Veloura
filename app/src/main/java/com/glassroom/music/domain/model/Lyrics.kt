package com.glassroom.music.domain.model

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class Lyrics(
    val trackTitle: String,
    val artistName: String,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
    val lines: List<LyricLine> = emptyList(),
    val isSynced: Boolean = lines.isNotEmpty()
)
