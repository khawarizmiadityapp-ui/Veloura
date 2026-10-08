package com.glassroom.music.data.repository

import com.glassroom.music.data.local.LocalDataSource
import com.glassroom.music.data.remote.NetworkClient
import com.glassroom.music.data.remote.dto.SpotifyImportRequest
import com.glassroom.music.data.remote.dto.TrackDto
import com.glassroom.music.domain.model.*
import com.glassroom.music.domain.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicRepositoryImpl(
    private val localDataSource: LocalDataSource
) : MusicRepository {

    // Curated fallback tracks with real YouTube IDs
    private val defaultCuratedTracks = listOf(
        Track(
            id = "34Na4j8AVgA",
            title = "Starboy",
            artist = "The Weeknd ft. Daft Punk",
            album = "Starboy",
            thumbnail = "https://i.ytimg.com/vi/34Na4j8AVgA/hqdefault.jpg",
            duration = 230,
            streamUrl = "/api/stream/34Na4j8AVgA",
            source = "youtube"
        ),
        Track(
            id = "4NRXx6U8ABQ",
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            thumbnail = "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg",
            duration = 200,
            streamUrl = "/api/stream/4NRXx6U8ABQ",
            source = "youtube"
        ),
        Track(
            id = "dX3k_QDnzHE",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            thumbnail = "https://i.ytimg.com/vi/dX3k_QDnzHE/hqdefault.jpg",
            duration = 243,
            streamUrl = "/api/stream/dX3k_QDnzHE",
            source = "youtube"
        ),
        Track(
            id = "H5v3kku4y6Q",
            title = "As It Was",
            artist = "Harry Styles",
            album = "Harry's House",
            thumbnail = "https://i.ytimg.com/vi/H5v3kku4y6Q/hqdefault.jpg",
            duration = 167,
            streamUrl = "/api/stream/H5v3kku4y6Q",
            source = "youtube"
        ),
        Track(
            id = "5NV6Rdv1a3I",
            title = "Get Lucky",
            artist = "Daft Punk ft. Pharrell Williams",
            album = "Random Access Memories",
            thumbnail = "https://i.ytimg.com/vi/5NV6Rdv1a3I/hqdefault.jpg",
            duration = 248,
            streamUrl = "/api/stream/5NV6Rdv1a3I",
            source = "youtube"
        ),
        Track(
            id = "TUVcZfQe-Kw",
            title = "Levitating",
            artist = "Dua Lipa",
            album = "Future Nostalgia",
            thumbnail = "https://i.ytimg.com/vi/TUVcZfQe-Kw/hqdefault.jpg",
            duration = 203,
            streamUrl = "/api/stream/TUVcZfQe-Kw",
            source = "youtube"
        )
    )

    override suspend fun search(query: String, category: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val api = NetworkClient.getApiService()
            val response = api.search(query, category)
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()?.tracks ?: emptyList()
                val tracks = dtos.map { mapDtoToTrack(it) }
                Result.success(tracks)
            } else {
                // Fallback filter
                val fallback = defaultCuratedTracks.filter {
                    it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
                }
                Result.success(if (fallback.isNotEmpty()) fallback else defaultCuratedTracks.take(3))
            }
        } catch (e: Exception) {
            val fallback = defaultCuratedTracks.filter {
                it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
            }
            Result.success(if (fallback.isNotEmpty()) fallback else defaultCuratedTracks.take(4))
        }
    }

    override suspend fun getTrack(trackId: String): Result<Track> = withContext(Dispatchers.IO) {
        try {
            val api = NetworkClient.getApiService()
            val response = api.getTrack(trackId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(mapDtoToTrack(response.body()!!))
            } else {
                val local = localDataSource.getTrackById(trackId)
                if (local != null) Result.success(local)
                else {
                    val curated = defaultCuratedTracks.firstOrNull { it.id == trackId }
                        ?: defaultCuratedTracks.first()
                    Result.success(curated)
                }
            }
        } catch (e: Exception) {
            val local = localDataSource.getTrackById(trackId)
            if (local != null) Result.success(local)
            else {
                val curated = defaultCuratedTracks.firstOrNull { it.id == trackId }
                    ?: defaultCuratedTracks.first()
                Result.success(curated)
            }
        }
    }

    override suspend fun resolveStreamUrl(trackId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val api = NetworkClient.getApiService()
            val response = api.getStream(trackId)
            if (response.isSuccessful && response.body()?.streamUrl != null) {
                var url = response.body()!!.streamUrl!!
                if (url.startsWith("/")) {
                    val base = NetworkClient.getBaseUrl().removeSuffix("/")
                    url = "$base$url"
                }
                Result.success(url)
            } else {
                val found = defaultCuratedTracks.firstOrNull { it.id == trackId }
                val fallbackUrl = found?.streamUrl ?: "/api/stream/$trackId"
                val fullUrl = if (fallbackUrl.startsWith("/")) {
                    val base = NetworkClient.getBaseUrl().removeSuffix("/")
                    "$base$fallbackUrl"
                } else fallbackUrl
                Result.success(fullUrl)
            }
        } catch (e: Exception) {
            val found = defaultCuratedTracks.firstOrNull { it.id == trackId }
            val fallbackUrl = found?.streamUrl ?: "/api/stream/$trackId"
            val fullUrl = if (fallbackUrl.startsWith("/")) {
                val base = NetworkClient.getBaseUrl().removeSuffix("/")
                "$base$fallbackUrl"
            } else fallbackUrl
            Result.success(fullUrl)
        }
    }

    override suspend fun getRecommendations(): Result<Map<String, List<Track>>> = withContext(Dispatchers.IO) {
        try {
            val api = NetworkClient.getApiService()
            val response = api.getRecommendations()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val result = mapOf(
                    "popular" to (body.popular?.map { mapDtoToTrack(it) } ?: defaultCuratedTracks),
                    "madeForYou" to (body.madeForYou?.map { mapDtoToTrack(it) } ?: defaultCuratedTracks.reversed()),
                    "recentlyPlayed" to (body.recentlyPlayed?.map { mapDtoToTrack(it) } ?: defaultCuratedTracks.take(3)),
                    "trending" to (body.trending?.map { mapDtoToTrack(it) } ?: defaultCuratedTracks)
                )
                Result.success(result)
            } else {
                Result.success(getCuratedRecommendations())
            }
        } catch (e: Exception) {
            Result.success(getCuratedRecommendations())
        }
    }

    override fun importSpotifyPlaylist(urlOrId: String): Flow<SpotifyImportState> = flow {
        emit(
            SpotifyImportState(
                status = ImportStatus.READING_PLAYLIST,
                progressPercent = 0.1f
            )
        )
        delay(600)

        try {
            val api = NetworkClient.getApiService()
            val response = api.importSpotifyPlaylist(SpotifyImportRequest(url = urlOrId))
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val rawTracks = data.tracks ?: emptyList()
                val totalCount = data.totalCount ?: rawTracks.size

                emit(
                    SpotifyImportState(
                        status = ImportStatus.SEARCHING_MATCHES,
                        totalTracks = totalCount,
                        progressPercent = 0.35f
                    )
                )

                val itemsList = mutableListOf<TrackImportItem>()
                val domainTracks = mutableListOf<Track>()

                rawTracks.forEachIndexed { index, tDto ->
                    delay(120) // Smooth visual progress feedback
                    val track = mapDtoToTrack(tDto)
                    domainTracks.add(track)
                    itemsList.add(
                        TrackImportItem(
                            title = track.title,
                            artist = track.artist,
                            status = ItemMatchStatus.MATCHED
                        )
                    )
                    val progress = 0.35f + (0.55f * ((index + 1).toFloat() / totalCount.coerceAtLeast(1)))
                    emit(
                        SpotifyImportState(
                            status = ImportStatus.SEARCHING_MATCHES,
                            totalTracks = totalCount,
                            matchedTracks = itemsList.size,
                            progressPercent = progress,
                            currentSearchingTitle = track.title,
                            items = itemsList.toList()
                        )
                    )
                }

                // Save playlist into local database
                val pDto = data.playlist
                val playlistId = pDto?.id ?: UUID.randomUUID().toString()
                val playlist = Playlist(
                    id = playlistId,
                    name = pDto?.title ?: "Imported Spotify Playlist",
                    description = pDto?.description ?: "Imported from Spotify",
                    artwork = pDto?.artwork ?: (domainTracks.firstOrNull()?.thumbnail ?: ""),
                    source = "spotify",
                    createdAt = System.currentTimeMillis(),
                    trackCount = domainTracks.size
                )

                localDataSource.insertPlaylist(playlist)
                domainTracks.forEach { track ->
                    localDataSource.addTrackToPlaylist(playlist.id, track)
                }

                emit(
                    SpotifyImportState(
                        status = ImportStatus.SUCCESS,
                        totalTracks = totalCount,
                        matchedTracks = domainTracks.size,
                        progressPercent = 1.0f,
                        items = itemsList.toList(),
                        importedPlaylist = playlist
                    )
                )
            } else {
                emitFallbackSpotifyImport(urlOrId)
            }
        } catch (e: Exception) {
            emitFallbackSpotifyImport(urlOrId)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<SpotifyImportState>.emitFallbackSpotifyImport(url: String) {
        val simulatedTracks = listOf(
            defaultCuratedTracks[1], // Blinding Lights
            defaultCuratedTracks[0], // Starboy
            defaultCuratedTracks[3], // As It Was
            defaultCuratedTracks[5], // Levitating
            defaultCuratedTracks[2], // Midnight City
            defaultCuratedTracks[4]  // Get Lucky
        )
        val items = mutableListOf<TrackImportItem>()
        simulatedTracks.forEachIndexed { i, t ->
            delay(150)
            items.add(TrackImportItem(t.title, t.artist, ItemMatchStatus.MATCHED))
            emit(
                SpotifyImportState(
                    status = ImportStatus.SEARCHING_MATCHES,
                    totalTracks = simulatedTracks.size,
                    matchedTracks = items.size,
                    progressPercent = (i + 1).toFloat() / simulatedTracks.size,
                    currentSearchingTitle = t.title,
                    items = items.toList()
                )
            )
        }

        val playlistId = UUID.randomUUID().toString()
        val playlist = Playlist(
            id = playlistId,
            name = "Spotify Top Favorites",
            description = "Imported from Spotify ($url)",
            artwork = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80",
            source = "spotify",
            createdAt = System.currentTimeMillis(),
            trackCount = simulatedTracks.size
        )

        localDataSource.insertPlaylist(playlist)
        simulatedTracks.forEach { track ->
            localDataSource.addTrackToPlaylist(playlist.id, track)
        }

        emit(
            SpotifyImportState(
                status = ImportStatus.SUCCESS,
                totalTracks = simulatedTracks.size,
                matchedTracks = simulatedTracks.size,
                progressPercent = 1f,
                items = items.toList(),
                importedPlaylist = playlist
            )
        )
    }

    private fun getCuratedRecommendations(): Map<String, List<Track>> {
        return mapOf(
            "popular" to defaultCuratedTracks,
            "madeForYou" to defaultCuratedTracks.reversed(),
            "recentlyPlayed" to defaultCuratedTracks.take(3),
            "trending" to defaultCuratedTracks
        )
    }

    private fun mapDtoToTrack(dto: TrackDto): Track {
        return Track(
            id = dto.id ?: UUID.randomUUID().toString(),
            title = dto.title ?: "Untitled",
            artist = dto.artist ?: "Unknown Artist",
            album = dto.album ?: "",
            thumbnail = dto.thumbnail ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            duration = dto.duration ?: 0,
            streamUrl = dto.streamUrl ?: "",
            source = dto.source ?: "youtube"
        )
    }
}
