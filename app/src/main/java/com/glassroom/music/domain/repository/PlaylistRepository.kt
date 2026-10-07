package com.glassroom.music.domain.repository

import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.domain.model.PlaylistWithTracks
import com.glassroom.music.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getPlaylistWithTracks(playlistId: String): Flow<PlaylistWithTracks?>
    suspend fun createPlaylist(name: String, description: String = "", source: String = "local", artwork: String = ""): Playlist
    suspend fun deletePlaylist(playlistId: String)
    suspend fun renamePlaylist(playlistId: String, newName: String, newDescription: String = "")
    suspend fun addTrackToPlaylist(playlistId: String, track: Track)
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
    suspend fun reorderPlaylistTracks(playlistId: String, trackIdsInOrder: List<String>)
    
    // Favorites
    fun getFavoriteTracks(): Flow<List<Track>>
    suspend fun toggleFavorite(track: Track): Boolean
    fun isFavorite(trackId: String): Flow<Boolean>

    // Recently played
    fun getRecentlyPlayedTracks(): Flow<List<Track>>
    suspend fun addToRecentlyPlayed(track: Track)
}
