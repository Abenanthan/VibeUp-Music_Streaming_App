package com.vibeup.android

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.material3.NavigationBarItemDefaults
import com.vibeup.android.ui.theme.AppTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vibeup.android.presentation.auth.AuthViewModel
import com.vibeup.android.presentation.auth.AuthState
import com.vibeup.android.presentation.player.PlayerViewModel
import com.vibeup.android.ui.components.MiniPlayer
import com.vibeup.android.ui.theme.VibeUpTheme
import com.vibeup.android.ui.theme.ThemeManager
import com.vibeup.android.presentation.player.LyricsViewModel
import com.vibeup.android.presentation.player.activityViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.vibeup.android.service.PlayerManager
import javax.inject.Inject


data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var playerManager: PlayerManager

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // System bar colours/icon appearance are driven by VibeUpTheme, which
        // re-applies them whenever the user switches theme (a light theme needs dark
        // bar icons). themes.xml supplies the dark default for the pre-Compose frame.
        if (savedInstanceState == null) {
            playerManager.restoreState()
        }



        // ✅ Permission request INSIDE onCreate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        setContent {
            VibeUpTheme(themeManager = themeManager) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val playerViewModel: PlayerViewModel = hiltViewModel()
                val authViewModel: AuthViewModel = hiltViewModel()
                val lyricsViewModel: LyricsViewModel = activityViewModel()

                val currentSong by playerViewModel.currentSong.collectAsState()

                // Observe song changes → load lyrics immediately
                LaunchedEffect(currentSong?.id) {
                    currentSong?.let { lyricsViewModel.loadLyrics(it) }
                }
                val isPlaying by playerViewModel.isPlaying.collectAsState()
                val currentUser by authViewModel.currentUser.collectAsState()
                val authState by authViewModel.authState.collectAsState()

                val bottomNavItems = listOf(
                    BottomNavItem("Home", Icons.Default.Home, Screen.Home.route),
                    BottomNavItem("Search", Icons.Default.Search, Screen.Search.route),
                    BottomNavItem("Local", Icons.Default.FolderOpen, Screen.Local.route),
                    BottomNavItem("Library", Icons.Default.LibraryMusic, Screen.Library.route)
                )

                val showBottomBar = currentRoute != Screen.Auth.route &&
                        currentRoute != Screen.Player.route

                // Collected here (cheap: just creates the State objects and
                // subscribes). Crucially `.value` is NOT read in this scope — see
                // the `progress` lambda below.
                val positionState = playerViewModel.currentPosition.collectAsState()
                val durationState = playerViewModel.duration.collectAsState()

                // Hoisted out of the draw lambda below (AppTheme.colors is a
                // @Composable getter and can't be read inside drawBehind).
                val navDivider = AppTheme.divider
                val navSelectedColors = NavigationBarItemDefaults.colors(
                    // Material's default selected indicator is the baseline lavender
                    // (#E8DEF8) because secondaryContainer isn't part of our palette —
                    // off-brand in every theme. Tie it to the app's own colours.
                    selectedIconColor = AppTheme.colors.onAccent,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = AppTheme.colors.textSecondary,
                    unselectedTextColor = AppTheme.colors.textSecondary
                )

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            Column {
                                currentSong?.let { song ->
                                    MiniPlayer(
                                        song = song,
                                        isPlaying = isPlaying,
                                        // Deferred state read. The flows are still
                                        // collected into Compose State here, but
                                        // `.value` is only read inside the lambda,
                                        // which runs in MiniPlayer's draw phase — so
                                        // a position tick invalidates only the draw,
                                        // not this scope. Reading `.value` here
                                        // instead would recompose the whole bottomBar
                                        // (mini player + NavigationBar + items)
                                        // twice a second on every screen.
                                        progress = {
                                            val dur = durationState.value
                                            if (dur > 0L)
                                                positionState.value.toFloat() / dur.toFloat()
                                            else 0f
                                        },
                                        onTogglePlayPause = {
                                            playerViewModel.togglePlayPause()
                                        },
                                        onNext = { playerViewModel.playNext() },
                                        onPrevious = { playerViewModel.playPrevious() },
                                        onExpand = {
                                            navController.navigate(Screen.Player.route)
                                        }
                                    )
                                }
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    // In the light themes `surface` is the same as (or
                                    // within 2% of) `background`, so the bar merged into
                                    // the page. A hairline separator is what YouTube
                                    // Music and Apple Music both use here.
                                    modifier = Modifier.drawBehind {
                                        drawRect(
                                            color = navDivider,
                                            size = Size(size.width, 1.dp.toPx())
                                        )
                                    }
                                ) {
                                    bottomNavItems.forEach { item ->
                                        val isSelected = when (item.route) {
                                            Screen.Home.route -> currentRoute == Screen.Home.route || 
                                                                currentRoute?.startsWith(Screen.Playlist.route) == true ||
                                                                currentRoute == Screen.Profile.route ||
                                                                currentRoute == Screen.Settings.route ||
                                                                currentRoute == Screen.Stats.route ||
                                                                currentRoute?.startsWith("artist") == true ||
                                                                currentRoute == Screen.AudioEffects.route ||
                                                                currentRoute == Screen.SoftwareEq.route ||
                                                                currentRoute == Screen.Queue.route ||
                                                                currentRoute == Screen.Lyrics.route
                                            Screen.Library.route -> currentRoute == Screen.Library.route || 
                                                                   currentRoute == Screen.Downloads.route ||
                                                                   currentRoute?.startsWith(Screen.AddSongs.route) == true
                                            else -> currentRoute == item.route
                                        }

                                        NavigationBarItem(
                                            colors = navSelectedColors,
                                            selected = isSelected,
                                            onClick = {
                                                if (currentRoute == item.route) return@NavigationBarItem

                                                navController.navigate(item.route) {
                                                    // Clear everything above the graph root so a tab
                                                    // press always lands on that tab's own page, from
                                                    // anywhere in the app — including deep screens like
                                                    // playlist detail, artist, settings or the queue.
                                                    //
                                                    // saveState/restoreState are deliberately OFF: with
                                                    // them on, Navigation restores the tab's previously
                                                    // saved back stack, so pressing a tab could drop you
                                                    // back onto a sub-page instead of the tab root. That
                                                    // is especially unpredictable here because the graph
                                                    // is flat (no nested navigation{} per tab).
                                                    //
                                                    // The reason they were enabled — avoiding ViewModel
                                                    // churn on every tab switch — is now handled by
                                                    // scoping the tab ViewModels to the Activity instead
                                                    // (see activityViewModel() in the screen signatures),
                                                    // so state survives without hijacking navigation.
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = false
                                                    }
                                                    // Avoid stacking duplicates of the same destination
                                                    launchSingleTop = true
                                                    restoreState = false
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = item.label
                                                )
                                            },
                                            label = { Text(item.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    VibeUpNavHost(
                        navController = navController,
                        startDestination = if (currentUser != null || authState is AuthState.Guest)
                            Screen.Home.route
                        else
                            Screen.Auth.route,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}