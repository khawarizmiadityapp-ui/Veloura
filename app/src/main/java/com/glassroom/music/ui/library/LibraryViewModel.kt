package com.glassroom.music.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassroom.music.data.local.OfflineAudioManager
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.domain.model.Track
import com.glassroom.music.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LibraryUiState(
    val selectedTab: String = "Liked Songs",
    val favoriteTracks: List<Track> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList(),
    val offlineTracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList()
)

class LibraryViewModel(
    private val playlistRepository: PlaylistRepository,
    private val offlineAudioManager: OfflineAudioManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val tabs = listOf("Liked Songs", "Recently Played", "Offline", "Artists", "Playlists")

    init {
        observeFavorites()
        observeHistory()
        observeOfflineTracks()
        observePlaylists()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            playlistRepository.getFavoriteTracks().collect { list ->
                _uiState.update { it.copy(favoriteTracks = list) }
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            playlistRepository.getRecentlyPlayedTracks().collect { list ->
                _uiState.update { it.copy(recentlyPlayed = list) }
            }
        }
    }

    private fun observeOfflineTracks() {
        val manager = offlineAudioManager ?: return
        viewModelScope.launch {
            combine(
                manager.downloadedTrackIds,
                playlistRepository.getFavoriteTrackIds()
            ) { downloadedIds, favIds ->
                downloadedIds to favIds
            }.collect { (downloadedIds, favIds) ->
                val resolved = playlistRepository.getTracksByIds(downloadedIds)
                    .map { it.copy(isFavorite = it.id in favIds) }
                _uiState.update { it.copy(offlineTracks = resolved) }
            }
        }
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { list ->
                _uiState.update { it.copy(playlists = list) }
            }
        }
    }

    fun selectTab(tab: String) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            playlistRepository.toggleFavorite(track)
        }
    }
}
