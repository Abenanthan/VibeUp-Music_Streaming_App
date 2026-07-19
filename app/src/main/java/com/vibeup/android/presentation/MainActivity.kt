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

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            Column {
                                currentSong?.let { song ->
                                    MiniPlayer(
                                        song = song,
                                        isPlaying = isPlaying,
                                        currentPosition = playerViewModel.currentPosition
                                            .collectAsState().value,
                                        duration = playerViewModel.duration
                                            .collectAsState().value,
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
                                    containerColor = MaterialTheme.colorScheme.surface
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
                                            selected = isSelected,
                                            onClick = {
                                                if (currentRoute == item.route) return@NavigationBarItem

                                                navController.navigate(item.route) {
                                                    // Pop everything up to the root of the graph to avoid building up a large stack
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = false
                                                    }
                                                    // Avoid multiple copies of the same destination
                                                    launchSingleTop = true
                                                    // Don't restore state so we always land on the root of the tab
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