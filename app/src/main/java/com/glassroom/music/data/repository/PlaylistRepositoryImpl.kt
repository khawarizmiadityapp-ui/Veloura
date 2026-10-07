package com.glassroom.music.data.repository

import com.glassroom.music.data.local.LocalDataSource
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.domain.model.PlaylistWithTracks
import com.glassroom.music.domain.model.Track
import com.glassroom.music.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PlaylistRepositoryImpl(
    private val localDataSource: LocalDataSource
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return localDataSource.getAllPlaylistsFlow()
    }

    override fun getPlaylistWithTracks(playlistId: String): Flow<PlaylistWithTracks?> {
        return localDataSource.getPlaylistWithTracksFlow(playlistId)
    }

    override suspend fun createPlaylist(
        name: String,
        description: String,
        source: String,
        artwork: String
    ): Playlist {
        val playlist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            artwork = artwork.ifBlank { "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80" },
            source = source,
            createdAt = System.currentTimeMillis(),
            trackCount = 0
        )
        localDataSource.insertPlaylist(playlist)
        return playlist
    }

    override suspend fun deletePlaylist(playlistId: String) {
        localDataSource.deletePlaylist(playlistId)
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String, newDescription: String) {
        localDataSource.renamePlaylist(playlistId, newName, newDescription)
    }

    override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        localDataSource.addTrackToPlaylist(playlistId, track)
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        localDataSource.removeTrackFromPlaylist(playlistId, trackId)
    }

    override suspend fun reorderPlaylistTracks(playlistId: String, trackIdsInOrder: List<String>) {
        localDataSource.reorderPlaylistTracks(playlistId, trackIdsInOrder)
    }

    override fun getFavoriteTracks(): Flow<List<Track>> {
        return localDataSource.getFavoriteTracksFlow()
    }

    override suspend fun toggleFavorite(track: Track): Boolean {
        return localDataSource.toggleFavorite(track)
    }

    override fun isFavorite(trackId: String): Flow<Boolean> {
        return localDataSource.isFavoriteFlow(trackId)
    }

    override fun getRecentlyPlayedTracks(): Flow<List<Track>> {
        return localDataSource.getRecentlyPlayedFlow()
    }

    override suspend fun addToRecentlyPlayed(track: Track) {
        localDataSource.recordPlayed(track)
    }
}
