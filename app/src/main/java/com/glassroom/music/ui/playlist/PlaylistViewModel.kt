package com.glassroom.music.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassroom.music.domain.model.*
import com.glassroom.music.domain.repository.MusicRepository
import com.glassroom.music.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val showCreateDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val importUrlInput: String = "",
    val importState: SpotifyImportState = SpotifyImportState(),
    val isCreatingPlaylist: Boolean = false
)

class PlaylistViewModel(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        observePlaylists()
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { list ->
                _uiState.update { it.copy(playlists = list) }
            }
        }
    }

    fun openImportDialog() {
        _uiState.update {
            it.copy(
                showImportDialog = true,
                importUrlInput = "",
                importState = SpotifyImportState()
            )
        }
    }

    fun closeImportDialog() {
        _uiState.update { it.copy(showImportDialog = false) }
    }

    fun onImportUrlChange(url: String) {
        _uiState.update { it.copy(importUrlInput = url) }
    }

    fun startSpotifyImport() {
        val url = _uiState.value.importUrlInput.trim()
        if (url.isBlank()) return

        viewModelScope.launch {
            musicRepository.importSpotifyPlaylist(url).collect { progress ->
                _uiState.update { it.copy(importState = progress) }
            }
        }
    }

    fun openCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun closeCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun createNewPlaylist(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistRepository.createPlaylist(name, description)
            _uiState.update { it.copy(showCreateDialog = false) }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }
}
