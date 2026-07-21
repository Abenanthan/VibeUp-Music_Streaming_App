package com.vibeup.android.presentation.ringtone

import android.Manifest
import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vibeup.android.data.repository.RingtoneType
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

private const val MAX_CLIP_SEC = 40f
private const val MIN_CLIP_SEC = 3f

@Composable
fun RingtoneTrimmerScreen(
    navController: NavController,
    viewModel: RingtoneViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.start() }

    // Surface one-off toasts from the ViewModel.
    LaunchedEffect(state.toast) {
        state.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
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
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    "Set as Ringtone",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
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
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            song.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            song.artist,
                            color = Color(0xFF9CA3AF),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when (val stage = state.stage) {
                is RingtoneStage.Loading ->
                    CenterStatus("Preparing audio…", showSpinner = true)

                is RingtoneStage.Working ->
                    CenterStatus(stage.label, showSpinner = true)

                is RingtoneStage.Failed ->
                    CenterStatus(stage.message, showSpinner = false, error = true)

                is RingtoneStage.Ready ->
                    TrimmerBody(
                        sourcePath = stage.sourcePath,
                        durationMs = state.durationMs,
                        viewModel = viewModel
                    )

                is RingtoneStage.Done ->
                    DoneBody(viewModel = viewModel, onClose = { navController.popBackStack() })
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            if (showSpinner) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            Text(
                message,
                color = if (error) Color(0xFFEF4444) else Color(0xFF9CA3AF),
                fontSize = 15.sp
            )
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

    // Fall back to a 30s window if we somehow don't know the duration.
    val durationSec = (durationMs / 1000f).let { if (it < MIN_CLIP_SEC) 30f else it }

    var range by remember {
        mutableStateOf(0f..minOf(MAX_CLIP_SEC, durationSec))
    }
    var selectedType by remember { mutableStateOf(RingtoneType.RINGTONE) }
    var alsoContact by remember { mutableStateOf(false) }
    var playheadFrac by remember { mutableStateOf(0f) }

    // ── Preview player: loops the current selection ──
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(if (sourcePath.startsWith("content://") || sourcePath.startsWith("file://")) sourcePath else "file://$sourcePath"))
            prepare()
        }
    }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    // Keep playback confined to [start, end].
    LaunchedEffect(isPlaying, range) {
        if (isPlaying) {
            player.seekTo((range.start * 1000).toLong())
            player.playWhenReady = true
            while (isPlaying) {
                val posMs = player.currentPosition
                if (posMs >= (range.endInclusive * 1000).toLong()) {
                    player.seekTo((range.start * 1000).toLong())
                }
                playheadFrac = if (durationSec > 0f) (posMs / 1000f) / durationSec else 0f
                delay(40)
            }
        } else {
            player.playWhenReady = false
        }
    }

    // WRITE_SETTINGS gate + optional retry after returning from settings.
    var pendingApply by remember { mutableStateOf(false) }
    val writeSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (pendingApply) {
            pendingApply = false
            if (viewModel.canWriteSettings()) {
                viewModel.applyRingtone(sourcePath, (range.start * 1000).toLong(), (range.endInclusive * 1000).toLong(), selectedType)
            } else {
                Toast.makeText(context, "Permission needed to set ringtone", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun apply() {
        if (viewModel.canWriteSettings()) {
            viewModel.applyRingtone(sourcePath, (range.start * 1000).toLong(), (range.endInclusive * 1000).toLong(), selectedType)
        } else {
            pendingApply = true
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            writeSettingsLauncher.launch(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Selection readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Start ${formatSec(range.start)}", color = Color(0xFF9CA3AF), fontSize = 13.sp)
            Text(
                "Length ${formatSec(range.endInclusive - range.start)}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text("End ${formatSec(range.endInclusive)}", color = Color(0xFF9CA3AF), fontSize = 13.sp)
        }

        WaveformTrimmer(
            seedKey = sourcePath,
            durationSec = durationSec,
            range = range,
            playheadFrac = playheadFrac,
            accent = MaterialTheme.colorScheme.primary,
            onRangeChange = { new -> range = clampRange(new, range, durationSec) }
        )

        Text(
            "Drag the handles to pick your clip • max ${MAX_CLIP_SEC.toInt()}s",
            color = Color(0xFF6B7280),
            fontSize = 11.sp
        )

        // Preview button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilledTonalButton(onClick = { isPlaying = !isPlaying }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isPlaying) "Stop preview" else "Preview clip")
            }
        }

        Text("Set as", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TypeChip("Ringtone", Icons.Default.MusicNote, selectedType == RingtoneType.RINGTONE) {
                selectedType = RingtoneType.RINGTONE
            }
            TypeChip("Notification", Icons.Default.Notifications, selectedType == RingtoneType.NOTIFICATION) {
                selectedType = RingtoneType.NOTIFICATION
            }
            TypeChip("Alarm", Icons.Default.Alarm, selectedType == RingtoneType.ALARM) {
                selectedType = RingtoneType.ALARM
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF9CA3AF))
            Spacer(Modifier.width(8.dp))
            Text(
                "Also set for a contact",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = alsoContact, onCheckedChange = { alsoContact = it })
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                isPlaying = false
                viewModel.setAssignContactAfterSave(alsoContact)
                apply()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.ContentCut, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Trim & Set", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ColumnScope.DoneBody(
    viewModel: RingtoneViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { viewModel.assignToContact(it) }
    }
    val contactPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contactPicker.launch(null)
        else Toast.makeText(context, "Contacts permission denied", Toast.LENGTH_SHORT).show()
    }

    val wantsContact = viewModel.assignContactAfterSave

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Ringtone set ✅", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            if (wantsContact) {
                Button(onClick = {
                    contactPermission.launch(Manifest.permission.WRITE_CONTACTS)
                }) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pick a contact")
                }
            }

            OutlinedButton(onClick = onClose) { Text("Done") }
        }
    }
}

@Composable
private fun TypeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
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
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF9CA3AF),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (selected) Color.White else Color(0xFF9CA3AF),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * A waveform-style trimmer: vertical bars spanning the whole track, a highlighted
 * selection window with two draggable grip handles, and a live playhead line.
 * (Bar heights are a deterministic pseudo-waveform — the source has no amplitude data.)
 */
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
            // Layered sines + a per-bar jitter derived from the seed → organic but stable.
            val jitter = (((seed / (i + 1)) % 100) / 100f)
            val v = 0.5f + 0.5f * sin(i * 0.55f) * abs(sin(i * 0.17f + 1.3f))
            (0.12f + 0.88f * (0.55f * v + 0.45f * jitter)).coerceIn(0.10f, 1f)
        }
    }

    var widthPx by remember { mutableStateOf(1f) }
    // Read the freshest range/handle inside the gesture without re-keying pointerInput.
    val rangeState = remember { mutableStateOf(range) }
    rangeState.value = range
    var activeHandle by remember { mutableStateOf(0) } // 1 = start, 2 = end

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

        // Selection background band.
        drawRoundRect(
            color = accent.copy(alpha = 0.12f),
            topLeft = Offset(startX, 0f),
            size = Size((endX - startX).coerceAtLeast(0f), h),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Bars.
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

        // Edge handles.
        val handleW = 8f
        listOf(startX, endX).forEach { x ->
            drawRoundRect(
                color = accent,
                topLeft = Offset(x - handleW / 2f, 0f),
                size = Size(handleW, h),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Playhead.
        if (playheadFrac > 0f) {
            val px = playheadFrac.coerceIn(0f, 1f) * w
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(px - 1.5f, 0f),
                size = Size(3f, h),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}

private fun clampRange(
    new: ClosedFloatingPointRange<Float>,
    old: ClosedFloatingPointRange<Float>,
    max: Float
): ClosedFloatingPointRange<Float> {
    var start = new.start.coerceIn(0f, max)
    var end = new.endInclusive.coerceIn(0f, max)
    if (end - start < MIN_CLIP_SEC) {
        // Keep at least the minimum length.
        if (start != old.start) start = (end - MIN_CLIP_SEC).coerceAtLeast(0f)
        else end = (start + MIN_CLIP_SEC).coerceAtMost(max)
    }
    if (end - start > MAX_CLIP_SEC) {
        if (start != old.start) end = start + MAX_CLIP_SEC
        else start = end - MAX_CLIP_SEC
    }
    return start..end
}

private fun formatSec(sec: Float): String {
    val total = sec.toInt()
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
