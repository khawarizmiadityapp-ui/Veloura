package com.glassroom.music.domain.model

enum class ImportStatus {
    IDLE,
    READING_PLAYLIST,
    SEARCHING_MATCHES,
    SUCCESS,
    ERROR
}

data class TrackImportItem(
    val title: String,
    val artist: String,
    val status: ItemMatchStatus // MATCHED, NOT_FOUND, SEARCHING
)

enum class ItemMatchStatus {
    MATCHED,
    NOT_FOUND,
    SEARCHING
}

data class SpotifyImportState(
    val status: ImportStatus = ImportStatus.IDLE,
    val totalTracks: Int = 0,
    val matchedTracks: Int = 0,
    val unavailableTracks: Int = 0,
    val progressPercent: Float = 0f,
    val currentSearchingTitle: String = "",
    val items: List<TrackImportItem> = emptyList(),
    val importedPlaylist: Playlist? = null,
    val errorMessage: String? = null
)
