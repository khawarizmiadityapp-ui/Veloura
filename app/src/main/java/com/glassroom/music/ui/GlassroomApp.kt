package com.glassroom.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
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

@OptIn(ExperimentalSharedTransitionApi::class)
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
    val isLoading by playerManager.isLoading.collectAsState()

    var isFullPlayerExpanded by remember { mutableStateOf(false) }

    // Intercept back button when Full Player is expanded
    BackHandler(enabled = isFullPlayerExpanded) {
        isFullPlayerExpanded = false
    }

    val coroutineScope = rememberCoroutineScope()
    val isCurrentFavorite by (currentTrack?.let {
        app.container.playlistRepository.isFavorite(it.id)
    } ?: kotlinx.coroutines.flow.flowOf(false)).collectAsState(initial = false)

    SharedTransitionLayout(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Atmospheric ambient backdrop for authentic optical glass refraction
            com.glassroom.music.ui.components.VelouraBackdrop(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                isFullPlayer = isFullPlayerExpanded
            )

            // Main Navigation Content with fluid directional transitions
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it / 8 },
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 8 },
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(180))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 8 },
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(200))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it / 8 },
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(180))
                }
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
                    SearchScreen(
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
                // When expanding, in-place fade-out keeps coordinates steady for Shared Element artwork travel
                AnimatedVisibility(
                    visible = currentTrack != null && !isFullPlayerExpanded,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = 0.86f, stiffness = 380f)
                    ) + fadeIn(tween(220)),
                    exit = fadeOut(tween(160))
                ) {
                    currentTrack?.let { track ->
                        GlassMiniPlayer(
                            track = track,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPosition,
                            durationMs = duration,
                            isLoading = isLoading,
                            onPlayPause = { playerManager.togglePlayPause() },
                            onNext = { playerManager.skipNext() },
                            onClick = { isFullPlayerExpanded = true },
                            onSwipeUp = { isFullPlayerExpanded = true },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility
                        )
                    }
                }

                // Glass Bottom Bar (Visible on top-level tabs when player is collapsed)
                val showBottomBar = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Search.route,
                    Screen.Library.route,
                    Screen.Playlists.route,
                    Screen.Settings.route
                )
                AnimatedVisibility(
                    visible = showBottomBar && !isFullPlayerExpanded,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(160))
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

            // Animated Full Screen Music Player Overlay with Shared Element transition
            AnimatedVisibility(
                visible = isFullPlayerExpanded,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 380f)
                ) + fadeIn(tween(220)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.90f, stiffness = 380f)
                ) + fadeOut(tween(200))
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
                    onCollapse = { isFullPlayerExpanded = false },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility
                )
            }
        }
    }
}
