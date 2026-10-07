package com.glassroom.music.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.glassroom.music.GlassroomApplication
import com.glassroom.music.domain.model.Track
import com.glassroom.music.ui.components.GlassBottomBar
import com.glassroom.music.ui.components.GlassMiniPlayer
import com.glassroom.music.ui.home.HomeScreen
import com.glassroom.music.ui.home.HomeViewModel
import com.glassroom.music.ui.library.LibraryScreen
import com.glassroom.music.ui.library.LibraryViewModel
import com.glassroom.music.ui.navigation.Screen
import com.glassroom.music.ui.player.FullPlayerScreen
import com.glassroom.music.ui.playlist.PlaylistDetailScreen
import com.glassroom.music.ui.search.SearchScreen
import com.glassroom.music.ui.search.SearchViewModel
import com.glassroom.music.ui.playlist.PlaylistViewModel
import com.glassroom.music.ui.playlist.PlaylistsScreen
import com.glassroom.music.ui.settings.SettingsScreen
import com.glassroom.music.ui.theme.GlassroomBackgroundBrush
import kotlinx.coroutines.launch

@Composable
fun GlassroomApp(
    app: GlassroomApplication,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val playerManager = app.container.playerManager
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPosition by playerManager.currentPosition.collectAsState()
    val duration by playerManager.duration.collectAsState()

    var isFullPlayerExpanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val isCurrentFavorite by if (currentTrack != null) {
        app.container.playlistRepository.isFavorite(currentTrack!!.id).collectAsState(initial = false)
    } else {
        remember { mutableStateOf(false) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(com.glassroom.music.ui.theme.LocalGlassConfig.current.backgroundBrush)
    ) {
        // Main Navigation Content
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel {
                    HomeViewModel(app.container.musicRepository, app.container.playlistRepository)
                }
                HomeScreen(
                    viewModel = homeViewModel,
                    onTrackClick = { track, queue ->
                        playerManager.playTrack(track, queue)
                    },
                    onPlaylistClick = { playlist ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id))
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    },
                    currentPlayingTrack = currentTrack,
                    isPlaying = isPlaying
                )
            }

            composable(Screen.Search.route) {
                val searchViewModel: SearchViewModel = viewModel {
                    SearchViewModel(app.container.musicRepository, app.container.playlistRepository)
                }
                com.glassroom.music.ui.search.SearchScreen(
                    viewModel = searchViewModel,
                    onTrackClick = { track, queue ->
                        playerManager.playTrack(track, queue)
                    },
                    currentPlayingTrack = currentTrack,
                    isPlaying = isPlaying
                )
            }

            composable(Screen.Library.route) {
                val libraryViewModel: LibraryViewModel = viewModel {
                    LibraryViewModel(app.container.playlistRepository)
                }
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onTrackClick = { track, queue ->
                        playerManager.playTrack(track, queue)
                    },
                    onPlaylistClick = { playlist ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id))
                    },
                    currentPlayingTrack = currentTrack,
                    isPlaying = isPlaying
                )
            }

            composable(Screen.Playlists.route) {
                val playlistViewModel: PlaylistViewModel = viewModel {
                    PlaylistViewModel(app.container.playlistRepository, app.container.musicRepository)
                }
                PlaylistsScreen(
                    viewModel = playlistViewModel,
                    onPlaylistClick = { playlist ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id))
                    }
                )
            }

            composable(
                route = Screen.PlaylistDetail.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    playlistRepository = app.container.playlistRepository,
                    onBack = { navController.popBackStack() },
                    onPlayTrack = { track, queue ->
                        playerManager.playTrack(track, queue)
                    },
                    onPlayAll = { tracks ->
                        playerManager.playQueue(tracks)
                    },
                    currentPlayingTrack = currentTrack,
                    isPlaying = isPlaying
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    preferencesManager = app.container.preferencesManager
                )
            }
        }

        // Floating Bottom Controls: MiniPlayer + BottomBar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Floating MiniPlayer (Visible when track is loaded and FullPlayer is collapsed)
            AnimatedVisibility(
                visible = currentTrack != null && !isFullPlayerExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                currentTrack?.let { track ->
                    GlassMiniPlayer(
                        track = track,
                        isPlaying = isPlaying,
                        currentPositionMs = currentPosition,
                        durationMs = duration,
                        onPlayPause = { playerManager.togglePlayPause() },
                        onNext = { playerManager.skipNext() },
                        onClick = { isFullPlayerExpanded = true },
                        onSwipeUp = { isFullPlayerExpanded = true }
                    )
                }
            }

            // Glass Bottom Bar (Visible on top-level tabs)
            val showBottomBar = currentRoute in listOf(
                Screen.Home.route,
                Screen.Search.route,
                Screen.Library.route,
                Screen.Playlists.route,
                Screen.Settings.route
            )
            AnimatedVisibility(
                visible = showBottomBar && !isFullPlayerExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                GlassBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }

        // Animated Full Screen Music Player Overlay
        AnimatedVisibility(
            visible = isFullPlayerExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 350)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut()
        ) {
            FullPlayerScreen(
                playerManager = playerManager,
                isFavorite = isCurrentFavorite,
                onToggleFavorite = {
                    currentTrack?.let { t ->
                        coroutineScope.launch {
                            app.container.playlistRepository.toggleFavorite(t)
                        }
                    }
                },
                onCollapse = { isFullPlayerExpanded = false }
            )
        }
    }
}
