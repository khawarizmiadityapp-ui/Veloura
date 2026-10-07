package com.glassroom.music.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.domain.model.Track
import com.glassroom.music.domain.repository.MusicRepository
import com.glassroom.music.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val greeting: String = "Good evening",
    val isLoading: Boolean = true,
    val popularSongs: List<Track> = emptyList(),
    val madeForYou: List<Track> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList(),
    val trending: List<Track> = emptyList(),
    val userPlaylists: List<Playlist> = emptyList(),
    val importedPlaylists: List<Playlist> = emptyList()
)

class HomeViewModel(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        updateGreeting()
        loadRecommendations()
        observePlaylists()
        observeRecentlyPlayed()
    }

    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
        _uiState.update { it.copy(greeting = greeting) }
    }

    fun loadRecommendations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = musicRepository.getRecommendations()
            if (result.isSuccess) {
                val data = result.getOrNull() ?: emptyMap()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        popularSongs = data["popular"] ?: emptyList(),
                        madeForYou = data["madeForYou"] ?: emptyList(),
                        trending = data["trending"] ?: emptyList()
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { playlists ->
                val user = playlists.filter { !it.isSpotifyImport }
                val imported = playlists.filter { it.isSpotifyImport }
                _uiState.update {
                    it.copy(
                        userPlaylists = user,
                        importedPlaylists = imported
                    )
                }
            }
        }
    }

    private fun observeRecentlyPlayed() {
        viewModelScope.launch {
            playlistRepository.getRecentlyPlayedTracks().collect { history ->
                if (history.isNotEmpty()) {
                    _uiState.update { it.copy(recentlyPlayed = history) }
                }
            }
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            playlistRepository.toggleFavorite(track)
        }
    }
}
