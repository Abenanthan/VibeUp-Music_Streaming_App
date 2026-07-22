package com.vibeup.android.presentation.ringtone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vibeup.android.data.repository.RingtoneType
import com.vibeup.android.presentation.player.LyricLine
import com.vibeup.android.presentation.player.LyricsState
import com.vibeup.android.presentation.player.LyricsViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

private const val MIN_CLIP_SEC = 1f

@Composable
fun RingtoneTrimmerScreen(
    navController: NavController,
    viewModel: RingtoneViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.start() }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Auto-close a moment after success.
    LaunchedEffect(state.stage) {
        if (state.stage is RingtoneStage.Done) {
            delay(900)
            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.White)
                }
                Text("Set as Ringtone", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            val song = state.song
            if (song != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = song.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, color = Color(0xFF9CA3AF), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when (val stage = state.stage) {
                is RingtoneStage.Loading -> CenterStatus("Preparing audio…", showSpinner = true)
                is RingtoneStage.Working -> CenterStatus(stage.label, showSpinner = true)
                is RingtoneStage.Failed -> CenterStatus(stage.message, showSpinner = false, error = true)
                is RingtoneStage.Done -> CenterStatus("Done ✅", showSpinner = false)
                is RingtoneStage.Ready -> TrimmerBody(
                    sourcePath = stage.sourcePath,
                    durationMs = state.durationMs,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CenterStatus(
    message: String,
    showSpinner: Boolean,
    error: Boolean = false
) {
    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            if (showSpinner) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(message, color = if (error) Color(0xFFEF4444) else Color(0xFF9CA3AF), fontSize = 15.sp)
        }
    }
}

@Composable
private fun ColumnScope.TrimmerBody(
    sourcePath: String,
    durationMs: Long,
    viewModel: RingtoneViewModel
) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary

    val durationSec = (durationMs / 1000f).let { if (it < MIN_CLIP_SEC) 30f else it }

    var range by remember { mutableStateOf(0f..minOf(30f, durationSec)) }
    var selectedType by remember { mutableStateOf(RingtoneType.RINGTONE) }
    var playheadFrac by remember { mutableStateOf(0f) }
    var contactUri by remember { mutableStateOf<Uri?>(null) }

    // ── Lyrics (reuse the existing LyricsViewModel) ──
    val lyricsVm: LyricsViewModel = hiltViewModel()
    val lyricsState by lyricsVm.lyricsState.collectAsState()
    val song = viewModel.state.collectAsState().value.song
    LaunchedEffect(song?.id) { song?.let { lyricsVm.loadLyrics(it) } }

    // ── Preview player: loops the current selection ──
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(normalizeUri(sourcePath)))
            prepare()
        }
    }
    var isPlaying by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(isPlaying, range) {
        if (isPlaying) {
            player.seekTo((range.start * 1000).toLong())
            player.playWhenReady = true
            while (isPlaying) {
                val posMs = player.currentPosition
                if (posMs >= (range.endInclusive * 1000).toLong()) player.seekTo((range.start * 1000).toLong())
                playheadFrac = if (durationSec > 0f) (posMs / 1000f) / durationSec else 0f
                delay(40)
            }
        } else {
            player.playWhenReady = false
        }
    }

    // ── Contact picker + permission ──
    val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        uri?.let {
            contactUri = it
            Toast.makeText(context, "Contact selected", Toast.LENGTH_SHORT).show()
        }
    }
    val requestContactPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pickContact.launch(null)
        else Toast.makeText(context, "Contacts permission denied", Toast.LENGTH_SHORT).show()
    }
    fun chooseContact() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (granted) pickContact.launch(null) else requestContactPerm.launch(Manifest.permission.WRITE_CONTACTS)
    }

    // ── WRITE_SETTINGS gate ──
    var pendingApply by remember { mutableStateOf(false) }
    val writeSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (pendingApply) {
            pendingApply = false
            if (viewModel.canWriteSettings()) {
                viewModel.applyRingtone(sourcePath, (range.start * 1000).toLong(), (range.endInclusive * 1000).toLong(), selectedType, contactUri)
            } else {
                Toast.makeText(context, "Permission needed to set ringtone", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun doApply() {
        isPlaying = false
        if (viewModel.canWriteSettings()) {
            viewModel.applyRingtone(sourcePath, (range.start * 1000).toLong(), (range.endInclusive * 1000).toLong(), selectedType, contactUri)
        } else {
            pendingApply = true
            writeSettingsLauncher.launch(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }
            )
        }
    }

    fun nudge(startSide: Boolean, delta: Float) {
        range = if (startSide) clampRange((range.start + delta)..range.endInclusive, range, durationSec)
        else clampRange(range.start..(range.endInclusive + delta), range, durationSec)
    }

    // Content fills remaining height: scrollable list + sticky button.
    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Readout
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Start ${formatSec(range.start)}", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                    Text("Length ${formatSec(range.endInclusive - range.start)}", color = accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("End ${formatSec(range.endInclusive)}", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                }
            }

            // Waveform
            item {
                WaveformTrimmer(
                    seedKey = sourcePath,
                    durationSec = durationSec,
                    range = range,
                    playheadFrac = playheadFrac,
                    accent = accent,
                    onRangeChange = { new -> range = clampRange(new, range, durationSec) }
                )
            }

            // Fine-tune ±1s
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NudgeGroup("Start", accent, onMinus = { nudge(true, -1f) }, onPlus = { nudge(true, +1f) }, modifier = Modifier.weight(1f))
                    NudgeGroup("End", accent, onMinus = { nudge(false, -1f) }, onPlus = { nudge(false, +1f) }, modifier = Modifier.weight(1f))
                }
            }

            // Preview
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilledTonalButton(onClick = { isPlaying = !isPlaying }) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isPlaying) "Stop preview" else "Preview clip")
                    }
                }
            }

            // Type chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Set as", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TypeChip("Ringtone", Icons.Default.MusicNote, selectedType == RingtoneType.RINGTONE) { selectedType = RingtoneType.RINGTONE }
                        TypeChip("Notification", Icons.Default.Notifications, selectedType == RingtoneType.NOTIFICATION) { selectedType = RingtoneType.NOTIFICATION }
                        TypeChip("Alarm", Icons.Default.Alarm, selectedType == RingtoneType.ALARM) { selectedType = RingtoneType.ALARM }
                    }
                }
            }

            // Contact assignment
            item {
                OutlinedButton(
                    onClick = { chooseContact() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (contactUri != null) Icons.Default.CheckCircle else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (contactUri != null) Color(0xFF10B981) else Color(0xFF9CA3AF)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (contactUri != null) "Contact selected — tap to change" else "Also set for a contact (optional)")
                }
            }

            // Lyrics seek panel
            item {
                Text("Seek by lyrics", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            when (val ls = lyricsState) {
                is LyricsState.SyncedLoaded -> {
                    items(ls.lines) { line ->
                        LyricRow(
                            line = line,
                            selected = abs(line.timeMs / 1000f - range.start) < 0.5f,
                            accent = accent,
                            onClick = {
                                val startSec = (line.timeMs / 1000f).coerceIn(0f, durationSec)
                                val len = (range.endInclusive - range.start).coerceAtLeast(MIN_CLIP_SEC)
                                range = clampRange(startSec..(startSec + len).coerceAtMost(durationSec), range, durationSec)
                            }
                        )
                    }
                }
                is LyricsState.Loading -> item {
                    Text("Loading lyrics…", color = Color(0xFF6B7280), fontSize = 12.sp)
                }
                else -> item {
                    Text("Synced lyrics not available for this song", color = Color(0xFF6B7280), fontSize = 12.sp)
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        // Sticky action button — never cropped.
        Button(
            onClick = { doApply() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp, top = 8.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.ContentCut, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Trim & Set", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun NudgeGroup(
    label: String,
    accent: Color,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF2A2A45), RoundedCornerShape(10.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        NudgeButton(Icons.Default.Remove, accent, onMinus)
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        NudgeButton(Icons.Default.Add, accent, onPlus)
    }
}

@Composable
private fun NudgeButton(icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LyricRow(
    line: LyricLine,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accent.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            formatSec(line.timeMs / 1000f),
            color = if (selected) accent else Color(0xFF6B7280),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            line.text,
            color = if (selected) Color.White else Color(0xFFB0B0C0),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TypeChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF2A2A45)
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (selected) Color.White else Color(0xFF9CA3AF), fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun WaveformTrimmer(
    seedKey: String,
    durationSec: Float,
    range: ClosedFloatingPointRange<Float>,
    playheadFrac: Float,
    accent: Color,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    val barCount = 70
    val bars = remember(seedKey) {
        val seed = seedKey.hashCode()
        List(barCount) { i ->
            val jitter = (((seed / (i + 1)) % 100) / 100f)
            val v = 0.5f + 0.5f * sin(i * 0.55f) * abs(sin(i * 0.17f + 1.3f))
            (0.12f + 0.88f * (0.55f * v + 0.45f * jitter)).coerceIn(0.10f, 1f)
        }
    }

    var widthPx by remember { mutableStateOf(1f) }
    val rangeState = remember { mutableStateOf(range) }
    rangeState.value = range
    var activeHandle by remember { mutableStateOf(0) }
    val dim = Color(0xFF2E2E4D)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(durationSec) {
                detectDragGestures(
                    onDragStart = { pos ->
                        val cur = rangeState.value
                        val startX = (cur.start / durationSec) * widthPx
                        val endX = (cur.endInclusive / durationSec) * widthPx
                        activeHandle = if (abs(pos.x - startX) <= abs(pos.x - endX)) 1 else 2
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val frac = (change.position.x / widthPx).coerceIn(0f, 1f)
                        val sec = frac * durationSec
                        val cur = rangeState.value
                        val next = if (activeHandle == 1) sec..cur.endInclusive else cur.start..sec
                        onRangeChange(next)
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val startX = (range.start / durationSec).coerceIn(0f, 1f) * w
        val endX = (range.endInclusive / durationSec).coerceIn(0f, 1f) * w

        drawRoundRect(
            color = accent.copy(alpha = 0.12f),
            topLeft = Offset(startX, 0f),
            size = Size((endX - startX).coerceAtLeast(0f), h),
            cornerRadius = CornerRadius(8f, 8f)
        )

        val slot = w / barCount
        val barW = slot * 0.55f
        bars.forEachIndexed { i, amp ->
            val cx = i * slot + slot / 2f
            val barH = h * amp
            val top = (h - barH) / 2f
            val inSel = cx in startX..endX
            drawRoundRect(
                color = if (inSel) accent else dim,
                topLeft = Offset(cx - barW / 2f, top),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }

        val handleW = 8f
        listOf(startX, endX).forEach { x ->
            drawRoundRect(color = accent, topLeft = Offset(x - handleW / 2f, 0f), size = Size(handleW, h), cornerRadius = CornerRadius(4f, 4f))
        }

        if (playheadFrac > 0f) {
            val px = playheadFrac.coerceIn(0f, 1f) * w
            drawRoundRect(color = Color.White, topLeft = Offset(px - 1.5f, 0f), size = Size(3f, h), cornerRadius = CornerRadius(2f, 2f))
        }
    }
}

/** Enforces min length + bounds. No maximum length — the clip can be as long as the song. */
private fun clampRange(
    new: ClosedFloatingPointRange<Float>,
    old: ClosedFloatingPointRange<Float>,
    max: Float
): ClosedFloatingPointRange<Float> {
    var start = new.start.coerceIn(0f, max)
    var end = new.endInclusive.coerceIn(0f, max)
    if (end - start < MIN_CLIP_SEC) {
        if (start != old.start) start = (end - MIN_CLIP_SEC).coerceAtLeast(0f)
        else end = (start + MIN_CLIP_SEC).coerceAtMost(max)
    }
    return start..end
}

private fun normalizeUri(path: String): String =
    if (path.startsWith("content://") || path.startsWith("file://")) path else "file://$path"

private fun formatSec(sec: Float): String {
    val total = sec.toInt()
    return "%d:%02d".format(total / 60, total % 60)
}
