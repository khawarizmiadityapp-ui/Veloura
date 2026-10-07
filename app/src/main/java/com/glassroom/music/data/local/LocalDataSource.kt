package com.glassroom.music.data.local

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.domain.model.PlaylistWithTracks
import com.glassroom.music.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalDataSource(private val dbHelper: GlassroomDbHelper) {

    suspend fun upsertTrack(track: Track) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("id", track.id)
            put("title", track.title)
            put("artist", track.artist)
            put("album", track.album)
            put("thumbnail", track.thumbnail)
            put("duration", track.duration)
            put("source", track.source)
            put("streamReference", track.streamUrl)
        }
        db.insertWithOnConflict(GlassroomDbHelper.TABLE_TRACKS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun getTrackById(trackId: String): Track? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            GlassroomDbHelper.TABLE_TRACKS,
            null,
            "id = ?",
            arrayOf(trackId),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) mapCursorToTrack(it) else null
        }
    }

    fun getAllPlaylistsFlow(): Flow<List<Playlist>> {
        return dbHelper.playlistsVersion.map {
            withContext(Dispatchers.IO) {
                val db = dbHelper.readableDatabase
                val list = mutableListOf<Playlist>()
                val query = """
                    SELECT p.*, COUNT(pt.trackId) AS trackCount 
                    FROM ${GlassroomDbHelper.TABLE_PLAYLISTS} p
                    LEFT JOIN ${GlassroomDbHelper.TABLE_PLAYLIST_TRACKS} pt ON p.id = pt.playlistId
                    GROUP BY p.id
                    ORDER BY p.createdAt DESC
                """.trimIndent()

                val cursor = db.rawQuery(query, null)
                cursor.use {
                    while (it.moveToNext()) {
                        val count = it.getInt(it.getColumnIndexOrThrow("trackCount"))
                        list.add(mapCursorToPlaylist(it, count))
                    }
                }
                list
            }
        }
    }

    fun getPlaylistWithTracksFlow(playlistId: String): Flow<PlaylistWithTracks?> {
        return dbHelper.playlistsVersion.map {
            withContext(Dispatchers.IO) {
                val db = dbHelper.readableDatabase
                val playlistCursor = db.query(
                    GlassroomDbHelper.TABLE_PLAYLISTS,
                    null,
                    "id = ?",
                    arrayOf(playlistId),
                    null,
                    null,
                    null
                )
                val playlist = playlistCursor.use {
                    if (it.moveToFirst()) mapCursorToPlaylist(it, 0) else null
                } ?: return@withContext null

                val tracksQuery = """
                    SELECT t.* FROM ${GlassroomDbHelper.TABLE_TRACKS} t
                    INNER JOIN ${GlassroomDbHelper.TABLE_PLAYLIST_TRACKS} pt ON t.id = pt.trackId
                    WHERE pt.playlistId = ?
                    ORDER BY pt.position ASC
                """.trimIndent()

                val tracksCursor = db.rawQuery(tracksQuery, arrayOf(playlistId))
                val tracks = mutableListOf<Track>()
                tracksCursor.use {
                    while (it.moveToNext()) {
                        tracks.add(mapCursorToTrack(it))
                    }
                }
                PlaylistWithTracks(
                    playlist = playlist.copy(trackCount = tracks.size),
                    tracks = tracks
                )
            }
        }
    }

    suspend fun insertPlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("id", playlist.id)
            put("name", playlist.name)
            put("description", playlist.description)
            put("artwork", playlist.artwork)
            put("source", playlist.source)
            put("createdAt", playlist.createdAt)
            put("isFavorite", if (playlist.isFavorite) 1 else 0)
        }
        db.insertWithOnConflict(GlassroomDbHelper.TABLE_PLAYLISTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        dbHelper.notifyPlaylistsChanged()
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(GlassroomDbHelper.TABLE_PLAYLIST_TRACKS, "playlistId = ?", arrayOf(playlistId))
            db.delete(GlassroomDbHelper.TABLE_PLAYLISTS, "id = ?", arrayOf(playlistId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        dbHelper.notifyPlaylistsChanged()
    }

    suspend fun renamePlaylist(playlistId: String, newName: String, newDescription: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", newName)
            if (newDescription.isNotBlank()) {
                put("description", newDescription)
            }
        }
        db.update(GlassroomDbHelper.TABLE_PLAYLISTS, cv, "id = ?", arrayOf(playlistId))
        dbHelper.notifyPlaylistsChanged()
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: Track) = withContext(Dispatchers.IO) {
        upsertTrack(track)
        val db = dbHelper.writableDatabase
        
        // Find current max position
        var nextPos = 0
        val posCursor = db.rawQuery(
            "SELECT MAX(position) FROM ${GlassroomDbHelper.TABLE_PLAYLIST_TRACKS} WHERE playlistId = ?",
            arrayOf(playlistId)
        )
        posCursor.use {
            if (it.moveToFirst() && !it.isNull(0)) {
                nextPos = it.getInt(0) + 1
            }
        }

        val cv = ContentValues().apply {
            put("playlistId", playlistId)
            put("trackId", track.id)
            put("position", nextPos)
        }
        db.insertWithOnConflict(GlassroomDbHelper.TABLE_PLAYLIST_TRACKS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        dbHelper.notifyPlaylistsChanged()
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(
            GlassroomDbHelper.TABLE_PLAYLIST_TRACKS,
            "playlistId = ? AND trackId = ?",
            arrayOf(playlistId, trackId)
        )
        dbHelper.notifyPlaylistsChanged()
    }

    suspend fun reorderPlaylistTracks(playlistId: String, trackIdsInOrder: List<String>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            trackIdsInOrder.forEachIndexed { index, trackId ->
                val cv = ContentValues().apply {
                    put("position", index)
                }
                db.update(
                    GlassroomDbHelper.TABLE_PLAYLIST_TRACKS,
                    cv,
                    "playlistId = ? AND trackId = ?",
                    arrayOf(playlistId, trackId)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        dbHelper.notifyPlaylistsChanged()
    }

    // FAVORITES
    fun getFavoriteTracksFlow(): Flow<List<Track>> {
        return dbHelper.favoritesVersion.map {
            withContext(Dispatchers.IO) {
                val db = dbHelper.readableDatabase
                val list = mutableListOf<Track>()
                val query = """
                    SELECT t.* FROM ${GlassroomDbHelper.TABLE_TRACKS} t
                    INNER JOIN ${GlassroomDbHelper.TABLE_FAVORITES} f ON t.id = f.trackId
                    ORDER BY f.addedAt DESC
                """.trimIndent()
                val cursor = db.rawQuery(query, null)
                cursor.use {
                    while (it.moveToNext()) {
                        list.add(mapCursorToTrack(it).copy(isFavorite = true))
                    }
                }
                list
            }
        }
    }

    fun isFavoriteFlow(trackId: String): Flow<Boolean> {
        return dbHelper.favoritesVersion.map {
            withContext(Dispatchers.IO) {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    GlassroomDbHelper.TABLE_FAVORITES,
                    arrayOf("trackId"),
                    "trackId = ?",
                    arrayOf(trackId),
                    null,
                    null,
                    null
                )
                cursor.use { it.moveToFirst() }
            }
        }
    }

    suspend fun toggleFavorite(track: Track): Boolean = withContext(Dispatchers.IO) {
        upsertTrack(track)
        val db = dbHelper.writableDatabase
        val cursor = db.query(
            GlassroomDbHelper.TABLE_FAVORITES,
            arrayOf("trackId"),
            "trackId = ?",
            arrayOf(track.id),
            null,
            null,
            null
        )
        val isCurrentlyFav = cursor.use { it.moveToFirst() }
        if (isCurrentlyFav) {
            db.delete(GlassroomDbHelper.TABLE_FAVORITES, "trackId = ?", arrayOf(track.id))
        } else {
            val cv = ContentValues().apply {
                put("trackId", track.id)
                put("addedAt", System.currentTimeMillis())
            }
            db.insert(GlassroomDbHelper.TABLE_FAVORITES, null, cv)
        }
        dbHelper.notifyFavoritesChanged()
        !isCurrentlyFav
    }

    // HISTORY
    fun getRecentlyPlayedFlow(): Flow<List<Track>> {
        return dbHelper.historyVersion.map {
            withContext(Dispatchers.IO) {
                val db = dbHelper.readableDatabase
                val list = mutableListOf<Track>()
                val query = """
                    SELECT t.* FROM ${GlassroomDbHelper.TABLE_TRACKS} t
                    INNER JOIN ${GlassroomDbHelper.TABLE_HISTORY} h ON t.id = h.trackId
                    ORDER BY h.playedAt DESC
                    LIMIT 30
                """.trimIndent()
                val cursor = db.rawQuery(query, null)
                cursor.use {
                    while (it.moveToNext()) {
                        list.add(mapCursorToTrack(it))
                    }
                }
                list
            }
        }
    }

    suspend fun recordPlayed(track: Track) = withContext(Dispatchers.IO) {
        upsertTrack(track)
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("trackId", track.id)
            put("playedAt", System.currentTimeMillis())
        }
        db.insertWithOnConflict(GlassroomDbHelper.TABLE_HISTORY, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        dbHelper.notifyHistoryChanged()
    }

    private fun mapCursorToTrack(c: Cursor): Track {
        return Track(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            title = c.getString(c.getColumnIndexOrThrow("title")),
            artist = c.getString(c.getColumnIndexOrThrow("artist")),
            album = c.getString(c.getColumnIndexOrThrow("album")) ?: "",
            thumbnail = c.getString(c.getColumnIndexOrThrow("thumbnail")) ?: "",
            duration = c.getInt(c.getColumnIndexOrThrow("duration")),
            source = c.getString(c.getColumnIndexOrThrow("source")) ?: "youtube",
            streamUrl = c.getString(c.getColumnIndexOrThrow("streamReference")) ?: ""
        )
    }

    private fun mapCursorToPlaylist(c: Cursor, trackCount: Int): Playlist {
        return Playlist(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            name = c.getString(c.getColumnIndexOrThrow("name")),
            description = c.getString(c.getColumnIndexOrThrow("description")) ?: "",
            artwork = c.getString(c.getColumnIndexOrThrow("artwork")) ?: "",
            source = c.getString(c.getColumnIndexOrThrow("source")) ?: "local",
            createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt")),
            trackCount = trackCount,
            isFavorite = c.getInt(c.getColumnIndexOrThrow("isFavorite")) == 1
        )
    }
}
