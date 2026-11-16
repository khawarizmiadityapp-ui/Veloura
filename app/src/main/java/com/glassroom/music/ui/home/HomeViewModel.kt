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

    @Volatile
    private var favoriteIds: Set<String> = emptySet()

    init {
        updateGreeting()
        observeFavoriteIds()
        loadRecommendations()
        observePlaylists()
        observeRecentlyPlayed()
    }

    private fun applyFavorites(tracks: List<Track>, favIds: Set<String>): List<Track> {
        return tracks.map { track -> track.copy(isFavorite = track.id in favIds) }
    }

    private fun observeFavoriteIds() {
        viewModelScope.launch {
            playlistRepository.getFavoriteTrackIds().collect { favIds ->
                favoriteIds = favIds
                _uiState.update { state ->
                    state.copy(
                        popularSongs = applyFavorites(state.popularSongs, favIds),
                        madeForYou = applyFavorites(state.madeForYou, favIds),
                        recentlyPlayed = applyFavorites(state.recentlyPlayed, favIds),
                        trending = applyFavorites(state.trending, favIds)
                    )
                }
            }
        }
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
                val favIds = favoriteIds
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        popularSongs = applyFavorites(data["popular"] ?: emptyList(), favIds),
                        madeForYou = applyFavorites(data["madeForYou"] ?: emptyList(), favIds),
                        trending = applyFavorites(data["trending"] ?: emptyList(), favIds)
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
                    _uiState.update { it.copy(recentlyPlayed = applyFavorites(history, favoriteIds)) }
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
