package com.glassroom.music.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.domain.model.Track
import com.glassroom.music.domain.repository.MusicRepository
import com.glassroom.music.domain.repository.PlaylistRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedCategory: String = "Songs",
    val isLoading: Boolean = false,
    val results: List<Track> = emptyList(),
    val matchedPlaylists: List<Playlist> = emptyList(),
    val selectedArtist: String? = null,
    val selectedAlbum: String? = null,
    val errorMessage: String? = null
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    @Volatile
    private var favoriteIds: Set<String> = emptySet()

    @Volatile
    private var savedPlaylists: List<Playlist> = emptyList()

    val categories = listOf("Songs", "Artists", "Albums", "Playlists")

    init {
        viewModelScope.launch {
            playlistRepository.getFavoriteTrackIds().collect { favIds ->
                favoriteIds = favIds
                _uiState.update { state ->
                    if (state.results.isEmpty()) state
                    else state.copy(results = state.results.map { it.copy(isFavorite = it.id in favIds) })
                }
            }
        }
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { playlists ->
                savedPlaylists = playlists
                val q = _uiState.value.query
                _uiState.update { state ->
                    state.copy(matchedPlaylists = filterSavedPlaylists(playlists, q))
                }
            }
        }
        viewModelScope.launch {
            searchQueryFlow
                .debounce(350)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isNotBlank()) {
                        executeSearch(query, _uiState.value.selectedCategory)
                    } else {
                        _uiState.update {
                            it.copy(
                                results = emptyList(),
                                matchedPlaylists = emptyList(),
                                selectedArtist = null,
                                selectedAlbum = null,
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                selectedArtist = null,
                selectedAlbum = null,
                matchedPlaylists = filterSavedPlaylists(savedPlaylists, query)
            )
        }
        searchQueryFlow.value = query
    }

    fun onCategorySelect(category: String) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                selectedArtist = null,
                selectedAlbum = null
            )
        }
        val currentQ = _uiState.value.query
        if (currentQ.isNotBlank()) {
            executeSearch(currentQ, category)
        }
    }

    fun selectArtist(artist: String?) {
        _uiState.update { it.copy(selectedArtist = artist) }
    }

    fun selectAlbum(album: String?) {
        _uiState.update { it.copy(selectedAlbum = album) }
    }

    private fun filterSavedPlaylists(playlists: List<Playlist>, query: String): List<Playlist> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()
        return playlists.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.description.contains(trimmed, ignoreCase = true)
        }
    }

    private fun buildCategoryQuery(rawQuery: String, category: String): String {
        val q = rawQuery.trim()
        return when (category.lowercase()) {
            "albums" -> if (q.contains("album", ignoreCase = true)) q else "$q full album"
            "playlists" -> if (q.contains("playlist", ignoreCase = true) || q.contains("mix", ignoreCase = true)) q else "$q playlist mix"
            else -> q
        }
    }

    private fun filterTracksForCategory(tracks: List<Track>, category: String): List<Track> {
        if (!category.equals("Songs", ignoreCase = true)) return tracks
        val normalSongs = tracks.filter { it.duration <= 720 }
        return if (normalSongs.size >= 3) normalSongs else tracks
    }

    private fun executeSearch(query: String, category: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val queryToUse = buildCategoryQuery(query, category)
            val result = musicRepository.search(queryToUse, category.lowercase())
            if (result.isSuccess) {
                val favIds = favoriteIds
                val filtered = filterTracksForCategory(result.getOrNull() ?: emptyList(), category)
                    .map { track -> track.copy(isFavorite = track.id in favIds) }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        results = filtered,
                        matchedPlaylists = filterSavedPlaylists(savedPlaylists, query)
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to find tracks right now"
                    )
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
