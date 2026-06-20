package com.vibeup.android.service

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEffectsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context
        .getSharedPreferences("audio_effects", Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null
    private var loudness: LoudnessEnhancer? = null

    // ── State flows ──
    private val _equalizerEnabled = MutableStateFlow(
        prefs.getBoolean("eq_enabled", false)
    )
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _bassBoostEnabled = MutableStateFlow(
        prefs.getBoolean("bass_enabled", false)
    )
    val bassBoostEnabled: StateFlow<Boolean> = _bassBoostEnabled.asStateFlow()

    private val _virtualizerEnabled = MutableStateFlow(
        prefs.getBoolean("virt_enabled", false)
    )
    val virtualizerEnabled: StateFlow<Boolean> = _virtualizerEnabled.asStateFlow()

    private val _reverbEnabled = MutableStateFlow(
        prefs.getBoolean("reverb_enabled", false)
    )
    val reverbEnabled: StateFlow<Boolean> = _reverbEnabled.asStateFlow()

    private val _loudnessEnabled = MutableStateFlow(
        prefs.getBoolean("loudness_enabled", false)
    )
    val loudnessEnabled: StateFlow<Boolean> = _loudnessEnabled.asStateFlow()

    private val _bassStrength = MutableStateFlow(
        prefs.getInt("bass_strength", 500)
    )
    val bassStrength: StateFlow<Int> = _bassStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(
        prefs.getInt("virt_strength", 500)
    )
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    private val _reverbPreset = MutableStateFlow(
        prefs.getInt("reverb_preset", PresetReverb.PRESET_NONE.toInt())
    )
    val reverbPreset: StateFlow<Int> = _reverbPreset.asStateFlow()

    private val _loudnessGain = MutableStateFlow(
        prefs.getInt("loudness_gain", 300)
    )
    val loudnessGain: StateFlow<Int> = _loudnessGain.asStateFlow()

    private val _eqBandLevels = MutableStateFlow(
        loadEqBands()
    )
    val eqBandLevels: StateFlow<List<Int>> = _eqBandLevels.asStateFlow()

    private val _eqPreset = MutableStateFlow(
        prefs.getInt("eq_preset", -1)
    )
    val eqPreset: StateFlow<Int> = _eqPreset.asStateFlow()

    // EQ presets in millibels per band (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
    val eqPresets = mapOf(
        "Normal"     to listOf(0, 0, 0, 0, 0),
        "Rock"       to listOf(400, 200, -100, 200, 400),
        "Pop"        to listOf(-100, 200, 400, 200, -100),
        "Jazz"       to listOf(200, 100, -200, 200, 300),
        "Classical"  to listOf(300, 200, -200, 200, 300),
        "Dance"      to listOf(500, 300, 0, 200, 100),
        "Hip Hop"    to listOf(500, 400, 100, 0, 200),
        "Bass Boost" to listOf(600, 400, 0, 0, 0),
        "Voice Clarity" to listOf(-200, -100, 300, 500, 200),
        "Surround"   to listOf(300, 100, 0, 100, 400)
    )

    fun initialize(audioSessionId: Int) {
        try {
            releaseEffects()

            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _equalizerEnabled.value
                val bands = _eqBandLevels.value
                for (i in 0 until numberOfBands) {
                    if (i < bands.size) {
                        setBandLevel(i.toShort(), bands[i].toShort())
                    }
                }
            }

            bassBoost = BassBoost(0, audioSessionId).apply {
                setStrength(_bassStrength.value.toShort())
                enabled = _bassBoostEnabled.value
            }

            virtualizer = Virtualizer(0, audioSessionId).apply {
                setStrength(_virtualizerStrength.value.toShort())
                enabled = _virtualizerEnabled.value
            }

            reverb = PresetReverb(0, audioSessionId).apply {
                preset = _reverbPreset.value.toShort()
                enabled = _reverbEnabled.value
            }

            loudness = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(_loudnessGain.value)
                enabled = _loudnessEnabled.value
            }

            android.util.Log.d("AudioEffects", "Initialized for session $audioSessionId")
        } catch (e: Exception) {
            android.util.Log.e("AudioEffects", "Init error: ${e.message}")
        }
    }

    // ── Equalizer ──
    fun toggleEqualizer() {
        val enabled = !_equalizerEnabled.value
        _equalizerEnabled.value = enabled
        equalizer?.enabled = enabled
        prefs.edit().putBoolean("eq_enabled", enabled).apply()
    }

    fun setEqBandLevel(band: Int, level: Int) {
        try {
            equalizer?.setBandLevel(band.toShort(), level.toShort())
            val bands = _eqBandLevels.value.toMutableList()
            if (band < bands.size) bands[band] = level
            _eqBandLevels.value = bands
            _eqPreset.value = -1
            saveEqBands(bands)
            prefs.edit().putInt("eq_preset", -1).apply()
        } catch (e: Exception) {
            android.util.Log.e("AudioEffects", "EQ band error: ${e.message}")
        }
    }

    fun applyEqPreset(presetName: String) {
        val levels = eqPresets[presetName] ?: return
        val index = eqPresets.keys.indexOf(presetName)
        _eqPreset.value = index
        prefs.edit().putInt("eq_preset", index).apply()

        levels.forEachIndexed { band, level ->
            val mb = (level * 100).toShort()
            try {
                equalizer?.setBandLevel(band.toShort(), mb)
            } catch (e: Exception) { }
        }
        val mbLevels = levels.map { it * 100 }
        _eqBandLevels.value = mbLevels
        saveEqBands(mbLevels)
    }

    fun getEqBandFrequencies(): List<String> {
        return try {
            val count = equalizer?.numberOfBands?.toInt() ?: 5
            (0 until count).map { band ->
                val freq = equalizer?.getCenterFreq(band.toShort()) ?: 0
                when {
                    freq >= 1_000_000 -> "${freq / 1_000_000}kHz"
                    else -> "${freq / 1_000}Hz"
                }
            }
        } catch (e: Exception) {
            listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
        }
    }

    // ── Bass Boost ──
    fun toggleBassBoost() {
        val enabled = !_bassBoostEnabled.value
        _bassBoostEnabled.value = enabled
        bassBoost?.enabled = enabled
        prefs.edit().putBoolean("bass_enabled", enabled).apply()
    }

    fun setBassStrength(strength: Int) {
        _bassStrength.value = strength
        bassBoost?.setStrength(strength.toShort())
        prefs.edit().putInt("bass_strength", strength).apply()
    }

    // ── Virtualizer ──
    fun toggleVirtualizer() {
        val enabled = !_virtualizerEnabled.value
        _virtualizerEnabled.value = enabled
        virtualizer?.enabled = enabled
        prefs.edit().putBoolean("virt_enabled", enabled).apply()
    }

    fun setVirtualizerStrength(strength: Int) {
        _virtualizerStrength.value = strength
        virtualizer?.setStrength(strength.toShort())
        prefs.edit().putInt("virt_strength", strength).apply()
    }

    // ── Reverb ──
    fun toggleReverb() {
        val enabled = !_reverbEnabled.value
        _reverbEnabled.value = enabled
        reverb?.enabled = enabled
        prefs.edit().putBoolean("reverb_enabled", enabled).apply()
    }

    fun setReverbPreset(preset: Int) {
        _reverbPreset.value = preset
        reverb?.preset = preset.toShort()
        prefs.edit().putInt("reverb_preset", preset).apply()
    }

    // ── Loudness ──
    fun toggleLoudness() {
        val enabled = !_loudnessEnabled.value
        _loudnessEnabled.value = enabled
        loudness?.enabled = enabled
        prefs.edit().putBoolean("loudness_enabled", enabled).apply()
    }

    fun setLoudnessGain(gain: Int) {
        _loudnessGain.value = gain
        loudness?.setTargetGain(gain)
        prefs.edit().putInt("loudness_gain", gain).apply()
    }

    private fun saveEqBands(bands: List<Int>) {
        prefs.edit().apply {
            bands.forEachIndexed { i, level ->
                putInt("eq_band_$i", level)
            }
            apply()
        }
    }

    private fun loadEqBands(): List<Int> {
        return (0..4).map { i -> prefs.getInt("eq_band_$i", 0) }
    }

    fun releaseEffects() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            reverb?.release()
            loudness?.release()
        } catch (e: Exception) { }
        equalizer = null
        bassBoost = null
        virtualizer = null
        reverb = null
        loudness = null
    }
}