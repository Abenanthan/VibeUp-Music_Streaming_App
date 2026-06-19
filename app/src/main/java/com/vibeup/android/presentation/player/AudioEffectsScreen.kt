package com.vibeup.android.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.vibeup.android.service.AudioEffectsManager
import com.vibeup.android.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.media.audiofx.PresetReverb

// ── ViewModel ──
@HiltViewModel
class AudioEffectsViewModel @Inject constructor(
    val audioEffectsManager: AudioEffectsManager
) : ViewModel()

// ── Screen ──
@Composable
fun AudioEffectsScreen(
    navController: NavController,
    viewModel: AudioEffectsViewModel = hiltViewModel()
) {
    val efx = viewModel.audioEffectsManager

    val eqEnabled by efx.equalizerEnabled.collectAsState()
    val bassEnabled by efx.bassBoostEnabled.collectAsState()
    val virtEnabled by efx.virtualizerEnabled.collectAsState()
    val reverbEnabled by efx.reverbEnabled.collectAsState()
    val loudnessEnabled by efx.loudnessEnabled.collectAsState()
    val bassStrength by efx.bassStrength.collectAsState()
    val virtStrength by efx.virtualizerStrength.collectAsState()
    val reverbPreset by efx.reverbPreset.collectAsState()
    val loudnessGain by efx.loudnessGain.collectAsState()
    val eqBands by efx.eqBandLevels.collectAsState()
    val eqPreset by efx.eqPreset.collectAsState()

    val frequencies = efx.getEqBandFrequencies()

    val reverbPresets = listOf(
        "None" to PresetReverb.PRESET_NONE.toInt(),
        "Small Room" to PresetReverb.PRESET_SMALLROOM.toInt(),
        "Medium Room" to PresetReverb.PRESET_MEDIUMROOM.toInt(),
        "Large Room" to PresetReverb.PRESET_LARGEROOM.toInt(),
        "Medium Hall" to PresetReverb.PRESET_MEDIUMHALL.toInt(),
        "Large Hall" to PresetReverb.PRESET_LARGEHALL.toInt(),
        "Plate" to PresetReverb.PRESET_PLATE.toInt()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
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
                        "Audio Effects",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PurpleLight, BlueLight)
                            )
                        )
                    )
                    Text(
                        "Customize your sound",
                        fontSize = 11.sp,
                        color = Color(0xFF4B5563)
                    )
                }
            }

            // ── Equalizer ──
            EffectCard(
                title = "🎚️ Equalizer",
                subtitle = "Adjust frequency bands",
                enabled = eqEnabled,
                onToggle = { efx.toggleEqualizer() }
            ) {
                // EQ Presets
                Text(
                    "Presets",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(efx.eqPresets.keys.toList()) { preset ->
                        val index = efx.eqPresets.keys.indexOf(preset)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (eqPreset == index)
                                        Brush.linearGradient(
                                            colors = listOf(
                                                PurplePrimary,
                                                BluePrimary
                                            )
                                        )
                                    else
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF1C1C3A),
                                                Color(0xFF1C1C3A)
                                            )
                                        )
                                )
                                .clickable { efx.applyEqPreset(preset) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                preset,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (eqPreset == index)
                                    Color.White
                                else
                                    Color(0xFF6B7280)
                            )
                        }
                    }
                }

                // EQ Band Sliders
                frequencies.forEachIndexed { index, freq ->
                    if (index < eqBands.size) {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    freq,
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                                Text(
                                    "${eqBands[index] / 100} dB",
                                    fontSize = 11.sp,
                                    color = PurplePrimary
                                )
                            }
                            Slider(
                                value = eqBands[index].toFloat(),
                                onValueChange = { level ->
                                    efx.setEqBandLevel(index, level.toInt())
                                },
                                valueRange = -1500f..1500f,
                                colors = SliderDefaults.colors(
                                    thumbColor = PurplePrimary,
                                    activeTrackColor = PurplePrimary,
                                    inactiveTrackColor = Color(0xFF2A2A4A)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Bass Boost ──
            EffectCard(
                title = "🔊 Bass Boost",
                subtitle = "Enhance low frequencies",
                enabled = bassEnabled,
                onToggle = { efx.toggleBassBoost() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Strength", fontSize = 12.sp, color = Color(0xFF6B7280))
                    Text(
                        "${bassStrength / 10}%",
                        fontSize = 12.sp,
                        color = PurplePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = bassStrength.toFloat(),
                    onValueChange = { efx.setBassStrength(it.toInt()) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = PurplePrimary,
                        activeTrackColor = PurplePrimary,
                        inactiveTrackColor = Color(0xFF2A2A4A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Virtualizer ──
            EffectCard(
                title = "🎧 Virtualizer",
                subtitle = "Surround sound effect",
                enabled = virtEnabled,
                onToggle = { efx.toggleVirtualizer() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Strength", fontSize = 12.sp, color = Color(0xFF6B7280))
                    Text(
                        "${virtStrength / 10}%",
                        fontSize = 12.sp,
                        color = BluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = virtStrength.toFloat(),
                    onValueChange = { efx.setVirtualizerStrength(it.toInt()) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = BluePrimary,
                        activeTrackColor = BluePrimary,
                        inactiveTrackColor = Color(0xFF2A2A4A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Reverb ──
            EffectCard(
                title = "🏛️ Reverb",
                subtitle = "Room and space effects",
                enabled = reverbEnabled,
                onToggle = { efx.toggleReverb() }
            ) {
                Text(
                    "Environment",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(reverbPresets) { (name, value) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (reverbPreset == value)
                                        Brush.linearGradient(
                                            colors = listOf(
                                                PinkAccent,
                                                PurplePrimary
                                            )
                                        )
                                    else
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF1C1C3A),
                                                Color(0xFF1C1C3A)
                                            )
                                        )
                                )
                                .clickable { efx.setReverbPreset(value) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (reverbPreset == value)
                                    Color.White
                                else
                                    Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Loudness Enhancer ──
            EffectCard(
                title = "📢 Loudness Enhancer",
                subtitle = "Boost overall volume",
                enabled = loudnessEnabled,
                onToggle = { efx.toggleLoudness() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gain", fontSize = 12.sp, color = Color(0xFF6B7280))
                    Text(
                        "+${loudnessGain / 100} dB",
                        fontSize = 12.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = loudnessGain.toFloat(),
                    onValueChange = { efx.setLoudnessGain(it.toInt()) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF10B981),
                        activeTrackColor = Color(0xFF10B981),
                        inactiveTrackColor = Color(0xFF2A2A4A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Effect Card Component ──
@Composable
fun EffectCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0D2B))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (enabled) 16.dp else 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF3F4F6)
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF4B5563)
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PurplePrimary,
                    uncheckedThumbColor = Color(0xFF6B7280),
                    uncheckedTrackColor = Color(0xFF1C1C3A)
                )
            )
        }
        if (enabled) {
            content()
        }
    }
}