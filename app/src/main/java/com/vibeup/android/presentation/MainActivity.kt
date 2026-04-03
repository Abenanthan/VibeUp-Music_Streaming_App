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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vibeup.android.presentation.auth.AuthViewModel
import com.vibeup.android.presentation.player.PlayerViewModel
import com.vibeup.android.ui.components.MiniPlayer
import com.vibeup.android.ui.theme.VibeUpTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)



        // ✅ Permission request INSIDE onCreate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        setContent {
            VibeUpTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val playerViewModel: PlayerViewModel = hiltViewModel()
                val authViewModel: AuthViewModel = hiltViewModel()

                val currentSong by playerViewModel.currentSong.collectAsState()
                val isPlaying by playerViewModel.isPlaying.collectAsState()
                val currentUser by authViewModel.currentUser.collectAsState()

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
                                        currentPosition = playerViewModel.currentPosition.collectAsState().value,
                                        duration = playerViewModel.duration.collectAsState().value,
                                        onTogglePlayPause = { playerViewModel.togglePlayPause() },
                                        onNext = { playerViewModel.playNext() },
                                        onPrevious = { playerViewModel.playPrevious() },
                                        onExpand = { navController.navigate(Screen.Player.route) }
                                    )
                                }
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    bottomNavItems.forEach { item ->
                                        NavigationBarItem(
                                            selected = currentRoute == item.route,
                                            onClick = {
                                                navController.navigate(item.route) {
                                                    popUpTo(Screen.Home.route) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
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
                        startDestination = if (currentUser != null)
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