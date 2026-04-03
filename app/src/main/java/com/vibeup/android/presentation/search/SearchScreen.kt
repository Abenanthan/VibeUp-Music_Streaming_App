package com.vibeup.android.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vibeup.android.domain.model.Playlist
import com.vibeup.android.domain.model.Song
import com.vibeup.android.presentation.home.SongOptionsDialog
import com.vibeup.android.presentation.library.LibraryViewModel
import com.vibeup.android.presentation.player.PlayerViewModel
import com.vibeup.android.ui.theme.DarkCard
import com.vibeup.android.ui.theme.DarkSurface
import com.vibeup.android.ui.theme.TextSecondary
import com.vibeup.android.ui.theme.VibeUpGreen

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val playlists by libraryViewModel.playlists.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Search 🔍",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onQueryChange(it) },
            placeholder = {
                Text(
                    text = "Search Tamil, Telugu, Hindi songs...",
                    color = TextSecondary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextSecondary
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = DarkCard,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { keyboardController?.hide() }
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                query.isEmpty() -> {
                    // Show search history
                    if (searchHistory.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🎵", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Search for your favourite songs",
                                fontSize = 16.sp,
                                color = TextSecondary
                            )
                        }
                    } else {
                        LazyColumn {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent Searches",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    TextButton(
                                        onClick = { viewModel.clearHistory() }
                                    ) {
                                        Text(
                                            text = "Clear All",
                                            color = TextSecondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            items(searchHistory) { history ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.searchFromHistory(history.query)
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = history.query,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteHistoryItem(history)
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    color = DarkSurface,
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
                searchResults.isEmpty() && query.isNotEmpty() && !isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "😔", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No results found for \"$query\"",
                            fontSize = 16.sp,
                            color = TextSecondary
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "${searchResults.size} results",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(searchResults) { song ->
                            SearchSongItem(
                                song = song,
                                playlists = playlists,
                                onClick = {
                                    playerViewModel.playSong(
                                        song,
                                        searchResults
                                    )
                                    keyboardController?.hide()
                                },
                                onLike = {
                                    libraryViewModel.likeSong(song)
                                },
                                onAddToPlaylist = { playlistId ->
                                    libraryViewModel.addSongToPlaylist(
                                        playlistId,
                                        song
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSongItem(
    song: Song,
    playlists: List<Playlist> = emptyList(),
    onClick: () -> Unit,
    onLike: () -> Unit = {},
    onAddToPlaylist: (String) -> Unit = {}
) {
    var showOptions by remember { mutableStateOf(false) }

    if (showOptions) {
        SongOptionsDialog(
            song = song,
            playlists = playlists,
            onDismiss = { showOptions = false },
            onLike = onLike,
            onAddToPlaylist = onAddToPlaylist
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.imageUrl,
            contentDescription = song.title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.album,
                fontSize = 11.sp,
                color = TextSecondary.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { showOptions = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = TextSecondary
            )
        }
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}