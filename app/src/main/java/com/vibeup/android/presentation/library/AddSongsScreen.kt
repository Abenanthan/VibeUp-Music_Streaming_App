package com.vibeup.android.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vibeup.android.data.remote.api.SaavnApiService
import com.vibeup.android.data.remote.dto.toDomain
import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.repository.LibraryRepository
import com.vibeup.android.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ──
@HiltViewModel
class AddSongsViewModel @Inject constructor(
    private val api: SaavnApiService,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _recommendedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recommendedSongs: StateFlow<List<Song>> = _recommendedSongs.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _addedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val addedSongIds: StateFlow<Set<String>> = _addedSongIds.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadRecommendations()
    }

    private fun loadRecommendations() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val queries = listOf(
                    "trending tamil songs",
                    "trending hindi songs",
                    "trending telugu songs"
                )
                val allSongs = mutableListOf<Song>()
                queries.forEach { query ->
                    val result = api.searchSongs(query, limit = 8)
                        .data?.results?.map { it.toDomain() } ?: emptyList()
                    allSongs.addAll(result)
                }
                _recommendedSongs.value = allSongs.shuffled()
            } catch (e: Exception) {
                android.util.Log.e("AddSongsVM", "Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _isSearching.value = true
            try {
                val result = api.searchSongs(query, limit = 20)
                    .data?.results?.map { it.toDomain() } ?: emptyList()
                _searchResults.value = result
            } catch (e: Exception) {
                android.util.Log.e("AddSongsVM", "Search error: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            try {
                libraryRepository.addSongToPlaylist(playlistId, song)
                _addedSongIds.value = _addedSongIds.value + song.id
                _message.value = "Added: ${song.title} ✅"
            } catch (e: Exception) {
                _message.value = "Failed to add song!"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

// ── Screen ──
@Composable
fun AddSongsScreen(
    navController: NavController,
    playlistId: String,
    viewModel: AddSongsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val recommendedSongs by viewModel.recommendedSongs.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val addedSongIds by viewModel.addedSongIds.collectAsState()
    val message by viewModel.message.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var query by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (message != null) {
            delay(2000)
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF12122A), androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add Songs",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Search or pick from recommendations",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                // Done button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PurplePrimary, BluePrimary)
                            )
                        )
                        .clickable { navController.popBackStack() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Done",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Search Bar ──
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.search(it)
                },
                placeholder = {
                    Text(
                        "Search songs to add...",
                        color = Color(0xFF4B5563)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF6B7280)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            viewModel.search("")
                        }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = null,
                                tint = Color(0xFF6B7280)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = Color(0xFF2A2A4A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PurplePrimary,
                    focusedContainerColor = Color(0xFF12122A),
                    unfocusedContainerColor = Color(0xFF12122A)
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                ),
                singleLine = true
            )

            // ── Content ──
            when {
                isSearching || isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = PurplePrimary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                if (isLoading) "Loading recommendations..."
                                else "Searching...",
                                color = Color(0xFF6B7280),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                query.isNotEmpty() && searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("😔", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No results for \"$query\"",
                                color = Color(0xFF6B7280),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                else -> {
                    val songsToShow = if (query.isNotEmpty()) searchResults
                    else recommendedSongs
                    val sectionTitle = if (query.isNotEmpty())
                        "${songsToShow.size} results"
                    else
                        "🎵 Recommended for you"

                    Text(
                        text = sectionTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF3F4F6),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = songsToShow,
                            key = { it.id }
                        ) { song ->
                            val isAdded = song.id in addedSongIds
                            AddSongItem(
                                song = song,
                                context = context,
                                isAdded = isAdded,
                                onAdd = {
                                    if (!isAdded) {
                                        viewModel.addSongToPlaylist(
                                            playlistId, song
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── Snackbar ──
        message?.let {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PurplePrimary
                )
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(14.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ── Add Song Item ──
@Composable
fun AddSongItem(
    song: Song,
    context: android.content.Context,
    isAdded: Boolean,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D0D2B))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Album Art
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.imageUrl)
                .crossfade(true)
                .memoryCacheKey(song.id)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF3F4F6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Add / Added Button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    if (isAdded)
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF059669),
                                Color(0xFF10B981)
                            )
                        )
                    else
                        Brush.linearGradient(
                            colors = listOf(PurplePrimary, BluePrimary)
                        )
                )
                .clickable { onAdd() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isAdded) Icons.Default.Check else Icons.Default.Add,
                contentDescription = if (isAdded) "Added" else "Add",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}