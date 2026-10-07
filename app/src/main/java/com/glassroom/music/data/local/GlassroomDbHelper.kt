package com.glassroom.music.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.domain.model.PlaylistWithTracks
import com.glassroom.music.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class GlassroomDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "glassroom_music.db"
        const val DATABASE_VERSION = 1

        const val TABLE_TRACKS = "tracks"
        const val TABLE_PLAYLISTS = "playlists"
        const val TABLE_PLAYLIST_TRACKS = "playlist_tracks"
        const val TABLE_FAVORITES = "favorite_tracks"
        const val TABLE_HISTORY = "history_tracks"
    }

    // Invalidation signals for reactive Flows
    val playlistsVersion = MutableStateFlow(0L)
    val favoritesVersion = MutableStateFlow(0L)
    val historyVersion = MutableStateFlow(0L)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TRACKS (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT,
                thumbnail TEXT,
                duration INTEGER DEFAULT 0,
                source TEXT DEFAULT 'youtube',
                streamReference TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PLAYLISTS (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                artwork TEXT,
                source TEXT DEFAULT 'local',
                createdAt INTEGER DEFAULT 0,
                isFavorite INTEGER DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PLAYLIST_TRACKS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                playlistId TEXT NOT NULL,
                trackId TEXT NOT NULL,
                position INTEGER DEFAULT 0,
                UNIQUE(playlistId, trackId)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_FAVORITES (
                trackId TEXT PRIMARY KEY,
                addedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_HISTORY (
                trackId TEXT PRIMARY KEY,
                playedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLIST_TRACKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLISTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRACKS")
        onCreate(db)
    }

    fun notifyPlaylistsChanged() {
        playlistsVersion.value = System.currentTimeMillis()
    }

    fun notifyFavoritesChanged() {
        favoritesVersion.value = System.currentTimeMillis()
    }

    fun notifyHistoryChanged() {
        historyVersion.value = System.currentTimeMillis()
    }
}
