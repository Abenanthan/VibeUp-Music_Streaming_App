package com.vibeup.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vibeup.android.presentation.auth.AuthScreen
import com.vibeup.android.presentation.auth.ProfileScreen
import com.vibeup.android.presentation.home.HomeScreen
import com.vibeup.android.presentation.library.LibraryScreen
import com.vibeup.android.presentation.player.PlayerScreen
import com.vibeup.android.presentation.search.SearchScreen
import com.vibeup.android.presentation.library.PlaylistDetailScreen
import com.vibeup.android.presentation.local.LocalMusicScreen
import com.vibeup.android.presentation.library.AddSongsScreen


sealed class Screen(val route: String) {
    object Auth    : Screen("auth")
    object Home    : Screen("home")
    object Search  : Screen("search")
    object Library : Screen("library")
    object Player  : Screen("player")
    object Profile : Screen("profile")

    object Playlist : Screen("playlist")

    object AddSongs : Screen("add_songs")
    object Local : Screen("local")
}

@Composable
fun VibeUpNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Auth.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Search.route) {
            SearchScreen(navController = navController)
        }
        composable(Screen.Library.route) {
            LibraryScreen(navController = navController)
        }
        composable(Screen.Player.route) {
            PlayerScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }

        composable(Screen.Local.route) {
            LocalMusicScreen(navController = navController)
        }

        composable("${Screen.Playlist.route}/{playlistId}") { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            PlaylistDetailScreen(
                navController = navController,
                playlistId = playlistId
            )
        }

        composable("${Screen.AddSongs.route}/{playlistId}") { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            AddSongsScreen(
                navController = navController,
                playlistId = playlistId
            )
        }
    }
}