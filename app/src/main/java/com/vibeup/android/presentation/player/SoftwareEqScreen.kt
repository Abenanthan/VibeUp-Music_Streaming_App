package com.vibeup.android.presentation.player

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import AutoEq Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste AutoEq parametric EQ text:", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
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
                        placeholder = { Text("Filter 1: ON PK Fc 32 Hz Gain 3.5 dB Q 0.7...", color = Color(0xFF374151), fontSize = 11.sp) }
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
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancel", color = TextSecondary) } },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepDark)) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(36.dp).background(DarkSurface, CircleShape)) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Software Equalizer", fontSize = 20.sp, fontWeight = FontWeight.Bold, style = androidx.compose.ui.text.TextStyle(brush = Brush.horizontalGradient(colors = listOf(PurpleLight, BlueLight))))
                        Text("10-band high-precision DSP", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(checked = isEnabled, onCheckedChange = { eq.toggle() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PurplePrimary, uncheckedThumbColor = Color(0xFF6B7280), uncheckedTrackColor = DarkCard))
                }
            }

            // Presets
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    items(SoftwareEqualizer.PRESETS.keys.toList()) { preset ->
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (currentPreset == preset) PurplePrimary else DarkCard).clickable { eq.applyPreset(preset) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(preset, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (currentPreset == preset) Color.White else TextSecondary)
                        }
                    }
                }
            }

            // Pre-amp
            item {
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DarkCard).padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pre-amp Gain", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("${if(preAmp >= 0) "+" else ""}${String.format("%.1f", preAmp)} dB", fontSize = 12.sp, color = PurplePrimary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = preAmp.toFloat(),
                        onValueChange = { eq.setPreAmp(it.toDouble()) },
                        valueRange = -15f..15f,
                        colors = SliderDefaults.colors(thumbColor = PurplePrimary, activeTrackColor = PurplePrimary, inactiveTrackColor = DarkElevated)
                    )
                    Text("Lower pre-amp if you notice distortion when boosting bands.", fontSize = 10.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // EQ Visualizer
            item {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp)).background(DarkCard).padding(8.dp)) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        bands.forEach { band ->
                            val normalizedGain = ((band.gainDb + 12) / 24).coerceIn(0.0, 1.0)
                            Box(modifier = Modifier.width(12.dp).height((normalizedGain * 60).dp.coerceAtLeast(4.dp)).clip(RoundedCornerShape(2.dp)).background(if (band.gainDb == 0.0) DarkElevated else if (band.gainDb > 0) PurplePrimary else Color(0xFFEC4899)))
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.Center).background(Color.White.copy(alpha = 0.1f)))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sliders
            item {
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DarkCard).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    bands.forEachIndexed { index, band ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = formatFreq(band.frequency), fontSize = 10.sp, color = TextSecondary, modifier = Modifier.width(40.dp))
                            Slider(
                                value = band.gainDb.toFloat(),
                                onValueChange = { gain -> eq.setBandGain(index, (gain * 10).roundToInt() / 10.0) },
                                valueRange = -12f..12f,
                                colors = SliderDefaults.colors(thumbColor = PurplePrimary, activeTrackColor = PurplePrimary, inactiveTrackColor = DarkElevated),
                                modifier = Modifier.weight(1f)
                            )
                            Text(text = "${if (band.gainDb >= 0) "+" else ""}${String.format("%.1f", band.gainDb)}", fontSize = 10.sp, color = if (band.gainDb > 0) PurplePrimary else TextSecondary, modifier = Modifier.width(36.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Actions
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { eq.resetAll() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset All", fontSize = 13.sp)
                    }
                    Button(onClick = { showImportDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary.copy(alpha = 0.2f)), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AutoEq Import", fontSize = 12.sp, color = PurplePrimary)
                    }
                }
            }
        }
    }
}

private fun formatFreq(hz: Double): String {
    return if (hz >= 1000) "${(hz / 1000).toInt()}k" else "${hz.toInt()}"
}
