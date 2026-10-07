package com.glassroom.music.di

import android.content.Context
import com.glassroom.music.data.local.GlassroomDbHelper
import com.glassroom.music.data.local.LocalDataSource
import com.glassroom.music.data.local.PreferencesManager
import com.glassroom.music.data.repository.MusicRepositoryImpl
import com.glassroom.music.data.repository.PlaylistRepositoryImpl
import com.glassroom.music.domain.repository.MusicRepository
import com.glassroom.music.domain.repository.PlaylistRepository
import com.glassroom.music.playback.PlayerManager

class GlassroomContainer(context: Context) {
    val dbHelper = GlassroomDbHelper(context)
    val localDataSource = LocalDataSource(dbHelper)
    val preferencesManager = PreferencesManager(context)

    val musicRepository: MusicRepository = MusicRepositoryImpl(localDataSource)
    val playlistRepository: PlaylistRepository = PlaylistRepositoryImpl(localDataSource)

    val playerManager = PlayerManager(
        context = context,
        musicRepository = musicRepository,
        playlistRepository = playlistRepository
    )
}
