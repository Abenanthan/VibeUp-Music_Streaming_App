package com.vibeup.android.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.vibeup.android.service.audio.SoftwareEqualizer
import com.vibeup.android.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class SoftwareEqViewModel @Inject constructor(
    val softwareEqualizer: SoftwareEqualizer
) : ViewModel()

@Composable
fun SoftwareEqScreen(
    navController: NavController,
    viewModel: SoftwareEqViewModel = hiltViewModel()
) {
    val eq = viewModel.softwareEqualizer
    val bands by eq.bands.collectAsState()
    val isEnabled by eq.isEnabled.collectAsState()
    val currentPreset by eq.currentPreset.collectAsState()
    val preAmp by eq.preAmpGain.collectAsState()
    val customPresets by eq.customPresets.collectAsState()
    val isBypassed by eq.isBypassed.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }

    val allPresetNames = remember(customPresets) {
        SoftwareEqualizer.PRESETS.keys.toList() + customPresets.keys.toList()
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import AutoEq Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Paste AutoEq parametric EQ text:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkElevated,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard,
                            cursorColor = PurplePrimary
                        ),
                        placeholder = {
                            Text(
                                "Filter 1: ON PK Fc 32 Hz Gain 3.5 dB Q 0.7...",
                                color = Color(0xFF374151),
                                fontSize = 11.sp
                            )
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isNotBlank()) {
                            eq.importAutoEqProfile(importText)
                            showImportDialog = false
                            importText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Custom Preset", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Save your current 32-band settings under a name:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkElevated,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard,
                            cursorColor = PurplePrimary
                        ),
                        placeholder = { Text("e.g. My Earphones", color = Color(0xFF374151), fontSize = 12.sp) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetNameInput.isNotBlank()) {
                            eq.saveCustomPreset(presetNameInput.trim())
                            showSavePresetDialog = false
                            presetNameInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepDark)) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {

            // ── Header ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(36.dp).background(DarkSurface, CircleShape)
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
                            "Software Equalizer",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            style = androidx.compose.ui.text.TextStyle(
                                brush = Brush.horizontalGradient(colors = listOf(PurpleLight, BlueLight))
                            )
                        )
                        Text("32-band high-precision DSP", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { eq.toggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PurplePrimary,
                            uncheckedThumbColor = Color(0xFF6B7280),
                            uncheckedTrackColor = DarkCard
                        )
                    )
                }
            }

            // ── A/B Bypass button ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isBypassed) Color(0xFFEC4899).copy(alpha = 0.18f)
                            else DarkCard
                        )
                        .clickable(enabled = isEnabled) { eq.toggleBypassPreview() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        if (isBypassed) Icons.Default.HearingDisabled else Icons.Default.CompareArrows,
                        contentDescription = null,
                        tint = if (isBypassed) Color(0xFFEC4899) else PurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isBypassed) "Playing unprocessed (A)" else "A/B Compare",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            if (isBypassed) "Tap to restore EQ early" else "Briefly hear raw audio without EQ",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    if (!isEnabled) {
                        Text("EQ off", fontSize = 10.sp, color = Color(0xFF4B5563))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Presets (built-in + custom) ──
            item {
                Text(
                    "Presets",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(allPresetNames) { preset ->
                        val isCustom = customPresets.containsKey(preset)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (currentPreset == preset) PurplePrimary else DarkCard)
                                .clickable { eq.applyPreset(preset) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isCustom) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (currentPreset == preset) Color.White else PurpleLight,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Text(
                                    preset,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (currentPreset == preset) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PurplePrimary.copy(alpha = 0.15f))
                                .clickable { showSavePresetDialog = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Save preset",
                                    tint = PurpleLight,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text("Save current", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PurpleLight)
                            }
                        }
                    }
                }

                // Delete row for custom presets — only shown if any exist
                if (customPresets.isNotEmpty()) {
                    Text(
                        "Long-press a saved preset name below to delete it",
                        fontSize = 10.sp,
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // ── Pre-amp ──
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pre-amp Gain", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text(
                            "${if (preAmp >= 0) "+" else ""}${String.format("%.1f", preAmp)} dB",
                            fontSize = 12.sp,
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = preAmp.toFloat(),
                        onValueChange = { eq.setPreAmp(it.toDouble()) },
                        valueRange = -15f..15f,
                        colors = SliderDefaults.colors(
                            thumbColor = PurplePrimary,
                            activeTrackColor = PurplePrimary,
                            inactiveTrackColor = DarkElevated
                        )
                    )
                    Text(
                        "Lower pre-amp if you notice distortion when boosting bands.",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Bezier curve visualizer ──
            item {
                EqCurveVisualizer(bands = bands)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── 32 sliders ──
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    bands.forEachIndexed { index, band ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatFreq(band.frequency),
                                fontSize = 9.sp,
                                color = TextSecondary,
                                modifier = Modifier.width(36.dp)
                            )
                            Slider(
                                value = band.gainDb.toFloat(),
                                onValueChange = { gain ->
                                    eq.setBandGain(index, (gain * 10).roundToInt() / 10.0)
                                },
                                valueRange = -12f..12f,
                                colors = SliderDefaults.colors(
                                    thumbColor = PurplePrimary,
                                    activeTrackColor = PurplePrimary,
                                    inactiveTrackColor = DarkElevated
                                ),
                                modifier = Modifier.weight(1f).height(28.dp)
                            )
                            Text(
                                text = "${if (band.gainDb >= 0) "+" else ""}${String.format("%.1f", band.gainDb)}",
                                fontSize = 9.sp,
                                color = if (band.gainDb > 0) PurplePrimary else TextSecondary,
                                modifier = Modifier.width(30.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Actions ──
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { eq.resetAll() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset All", fontSize = 13.sp)
                    }
                    Button(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AutoEq Import", fontSize = 12.sp, color = PurplePrimary)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Smooth bezier-curve spectrum visualizer. Plots a cubic curve through all
 * 32 band gain points instead of discrete bars, giving the classic
 * "spectrum analyzer" look found in dedicated EQ apps.
 */
@Composable
private fun EqCurveVisualizer(bands: List<com.vibeup.android.service.audio.SoftwareEqBand>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val midY = height / 2f
            val maxGain = 12f

            // Zero-line
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, midY),
                end = Offset(width, midY),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )

            if (bands.isEmpty()) return@Canvas

            val stepX = width / (bands.size - 1).coerceAtLeast(1)
            val points = bands.mapIndexed { i, band ->
                val x = i * stepX
                val normalized = (band.gainDb.toFloat() / maxGain).coerceIn(-1f, 1f)
                val y = midY - (normalized * midY * 0.85f)
                Offset(x, y)
            }

            // Build a smooth path through all points using cubic bezier
            // segments, each control point derived from neighboring points
            // (Catmull-Rom-ish smoothing) so the curve doesn't overshoot.
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val controlOffset = (p1.x - p0.x) / 2f
                    cubicTo(
                        p0.x + controlOffset, p0.y,
                        p1.x - controlOffset, p1.y,
                        p1.x, p1.y
                    )
                }
            }

            // Filled area under the curve, gradient-tinted by boost/cut
            val fillPath = Path().apply {
                addPath(path)
                lineTo(points.last().x, midY)
                lineTo(points.first().x, midY)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PurplePrimary.copy(alpha = 0.35f),
                        PurplePrimary.copy(alpha = 0.02f)
                    )
                )
            )

            // The curve stroke itself
            drawPath(
                path = path,
                color = PurplePrimary,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )

            // Dots at each band point, brighter where gain is non-zero
            points.forEachIndexed { i, point ->
                val gain = bands[i].gainDb
                drawCircle(
                    color = if (gain == 0.0) Color.White.copy(alpha = 0.25f) else PurpleLight,
                    radius = if (gain == 0.0) 1.5f else 2.5f,
                    center = point
                )
            }
        }
    }
}

private fun formatFreq(hz: Double): String {
    return if (hz >= 1000) {
        val k = hz / 1000
        if (k == k.toInt().toDouble()) "${k.toInt()}k" else String.format("%.1fk", k)
    } else {
        "${hz.toInt()}"
    }
}