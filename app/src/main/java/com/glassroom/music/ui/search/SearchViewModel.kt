package com.glassroom.music.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassroom.music.domain.model.Track
import com.glassroom.music.domain.repository.MusicRepository
import com.glassroom.music.domain.repository.PlaylistRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedCategory: String = "Songs",
    val isLoading: Boolean = false,
    val results: List<Track> = emptyList(),
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

    @Volatile
    private var favoriteIds: Set<String> = emptySet()

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
            searchQueryFlow
                .debounce(350)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isNotBlank()) {
                        executeSearch(query, _uiState.value.selectedCategory)
                    } else {
                        _uiState.update { it.copy(results = emptyList(), isLoading = false) }
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchQueryFlow.value = query
    }

    fun onCategorySelect(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        val currentQ = _uiState.value.query
        if (currentQ.isNotBlank()) {
            executeSearch(currentQ, category)
        }
    }

    private fun executeSearch(query: String, category: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = musicRepository.search(query, category.lowercase())
            if (result.isSuccess) {
                val favIds = favoriteIds
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        results = (result.getOrNull() ?: emptyList()).map { track ->
                            track.copy(isFavorite = track.id in favIds)
                        }
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
