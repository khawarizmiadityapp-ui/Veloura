package com.glassroom.music.domain.repository

import com.glassroom.music.domain.model.SpotifyImportState
import com.glassroom.music.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    suspend fun search(query: String, category: String = "songs"): Result<List<Track>>
    suspend fun getTrack(trackId: String): Result<Track>
    suspend fun resolveStreamUrl(trackId: String): Result<String>
    suspend fun getRecommendations(): Result<Map<String, List<Track>>>
    fun importSpotifyPlaylist(urlOrId: String): Flow<SpotifyImportState>
}
