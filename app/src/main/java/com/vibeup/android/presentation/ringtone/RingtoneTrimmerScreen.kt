package com.vibeup.android.presentation.ringtone

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vibeup.android.data.repository.RingtoneType
import kotlinx.coroutines.delay

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
                if (player.currentPosition >= (range.endInclusive * 1000).toLong()) {
                    player.seekTo((range.start * 1000).toLong())
                }
                delay(100)
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

        RangeSlider(
            value = range,
            onValueChange = { new ->
                range = clampRange(new, range, durationSec)
            },
            valueRange = 0f..durationSec,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color(0xFF2A2A45)
            )
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
