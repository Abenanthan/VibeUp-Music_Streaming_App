package com.vibeup.android.presentation.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vibeup.android.domain.model.Song
import com.vibeup.android.ui.theme.*
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun QueueScreen(
    navController: NavController,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val activeQueue by viewModel.activeQueue.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    // Local mutable copy so drag reordering feels instant; PlayerManager is
    // the source of truth and gets updated on drag-end, not on every frame.
    var localQueue by remember(activeQueue) { mutableStateOf(activeQueue) }

    val currentIndex = remember(localQueue, currentSong) {
        localQueue.indexOfFirst { it.id == currentSong?.id }.coerceAtLeast(0)
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Drag state: which item index is being dragged and by how much (px)
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var itemHeightPx by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF12122A), CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        "Up Next",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.horizontalGradient(listOf(PurpleLight, BlueLight))
                        )
                    )
                    Text(
                        "${localQueue.size} songs in queue",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            if (localQueue.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Queue is empty", color = Color(0xFF6B7280), fontSize = 13.sp)
                    }
                }
                return@Column
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                itemsIndexed(localQueue, key = { _, song -> song.id }) { index, song ->
                    val isCurrent = index == currentIndex
                    val isDragging = draggingIndex == index

                    val offsetY = if (isDragging) dragOffsetY else 0f

                    QueueRow(
                        song = song,
                        index = index,
                        isCurrent = isCurrent,
                        isDragging = isDragging,
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragging) 1f else 0f)
                            .onGloballyPositioned { coords ->
                                if (itemHeightPx == 0f) itemHeightPx = coords.size.height.toFloat()
                            }
                            .graphicsLayerOffsetY(offsetY),
                        onClick = {
                            if (!isCurrent) {
                                viewModel.jumpToQueueIndex(index)
                            }
                        },
                        onRemove = {
                            if (!isCurrent) {
                                val updated = localQueue.toMutableList().apply { removeAt(index) }
                                localQueue = updated
                                viewModel.removeFromQueue(index)
                            }
                        },
                        onDragStart = {
                            draggingIndex = index
                            dragOffsetY = 0f
                        },
                        onDrag = { delta ->
                            dragOffsetY += delta

                            // Compute how many rows we've crossed and swap
                            // the local list live, so the UI reflows as the
                            // user drags — classic reorderable-list feel.
                            val rows = (dragOffsetY / itemHeightPx).roundToInt()
                            if (rows != 0) {
                                val from = index
                                val to = (index + rows).coerceIn(0, localQueue.lastIndex)
                                if (from != to && to != index) {
                                    val updated = localQueue.toMutableList()
                                    val item = updated.removeAt(from)
                                    updated.add(to, item)
                                    localQueue = updated
                                    draggingIndex = to
                                    dragOffsetY -= rows * itemHeightPx
                                }
                            }
                        },
                        onDragEnd = { finalIndex ->
                            draggingIndex = null
                            dragOffsetY = 0f
                            // Reconcile PlayerManager's real queue with the
                            // locally-reordered list in one shot by replaying
                            // the move from the original index to final index.
                            val originalIndex = activeQueue.indexOfFirst { it.id == song.id }
                            if (originalIndex != -1 && originalIndex != finalIndex) {
                                viewModel.moveQueueItem(originalIndex, finalIndex)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    song: Song,
    index: Int,
    isCurrent: Boolean,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: (Int) -> Unit
) {
    var swipeOffsetX by remember { mutableStateOf(0f) }
    val swipeThresholdPx = 180f

    Box(
        modifier = modifier
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isCurrent) PurplePrimary.copy(alpha = 0.18f)
                else if (isDragging) Color(0xFF1C1C3A)
                else Color(0xFF12122A)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (!isCurrent) {
                        it.pointerInput(song.id) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (swipeOffsetX < -swipeThresholdPx) onRemove()
                                    swipeOffsetX = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    swipeOffsetX = (swipeOffsetX + dragAmount).coerceAtMost(0f)
                                }
                            )
                        }
                    } else it
                }
                .clickable { onClick() }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Drag handle — only this area initiates reorder, so taps and
            // swipes elsewhere in the row aren't accidentally hijacked.
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .let { base ->
                        if (!isCurrent) {
                            base.pointerInput(song.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { onDragStart() },
                                    onDragEnd = { onDragEnd(index) },
                                    onDragCancel = { onDragEnd(index) },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDrag(dragAmount.y)
                                    }
                                )
                            }
                        } else base
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!isCurrent) {
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = Color(0xFF4B5563),
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PurplePrimary)
                    )
                }
            }

            AsyncImage(
                model = song.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    fontSize = 13.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrent) PurpleLight else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isCurrent) {
                Text(
                    "Now playing",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PurplePrimary
                )
            } else {
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove from queue",
                        tint = Color(0xFF4B5563),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Small helper to translate a composable vertically by [offsetY] pixels
 * during an active drag, without needing a separate animatable per row.
 */
private fun Modifier.graphicsLayerOffsetY(offsetY: Float): Modifier =
    this.then(
        Modifier.offset { androidx.compose.ui.unit.IntOffset(0, offsetY.roundToInt()) }
    )