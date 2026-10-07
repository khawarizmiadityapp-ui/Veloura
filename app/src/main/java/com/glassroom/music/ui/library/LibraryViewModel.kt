package com.glassroom.music.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.domain.model.Track
import com.glassroom.music.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LibraryUiState(
    val selectedTab: String = "Liked Songs",
    val favoriteTracks: List<Track> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList()
)

class LibraryViewModel(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val tabs = listOf("Liked Songs", "Recently Played", "Artists", "Playlists")

    init {
        observeFavorites()
        observeHistory()
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
