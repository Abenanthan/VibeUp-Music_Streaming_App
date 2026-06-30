package com.vibeup.android.service.audio

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@OptIn(UnstableApi::class)
@Singleton
class SoftwareEqualizer @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioProcessor {

    companion object {

        /**
         * 32-band layout. Spacing follows a roughly 1/3-octave curve from
         * 20 Hz to 20 kHz, which is the same density used by professional
         * studio graphic equalizers. First and last bands are shelf filters,
         * everything in between is a peaking filter.
         */
        val DEFAULT_BANDS: List<SoftwareEqBand> = buildList {
            val freqs = listOf(
                20.0, 25.0, 31.5, 40.0, 50.0, 63.0, 80.0, 100.0,
                125.0, 160.0, 200.0, 250.0, 315.0, 400.0, 500.0, 630.0,
                800.0, 1000.0, 1250.0, 1600.0, 2000.0, 2500.0, 3150.0, 4000.0,
                5000.0, 6300.0, 8000.0, 10000.0, 12500.0, 16000.0, 18000.0, 20000.0
            )
            freqs.forEachIndexed { i, f ->
                val type = when (i) {
                    0 -> BiquadFilter.Type.LOW_SHELF
                    freqs.lastIndex -> BiquadFilter.Type.HIGH_SHELF
                    else -> BiquadFilter.Type.PEAK_EQ
                }
                add(SoftwareEqBand(id = i, frequency = f, q = 1.41, type = type))
            }
        }

        private const val BAND_COUNT = 32

        /**
         * Built-in presets, expressed as 32 gain values matching DEFAULT_BANDS
         * order. Indian-music presets are tuned for vocal-forward, percussion
         * heavy mixes typical of Tamil/Hindi/Telugu film and devotional music.
         */
        val PRESETS: Map<String, List<Double>> = mapOf(
            "Flat" to List(BAND_COUNT) { 0.0 },

            "Bass Boost" to gainCurve(BAND_COUNT) { i ->
                if (i < 8) 6.0 - i * 0.6 else 0.0
            },

            "Treble Boost" to gainCurve(BAND_COUNT) { i ->
                if (i > 22) (i - 22) * 0.7 else 0.0
            },

            "Rock" to gainCurve(BAND_COUNT) { i ->
                when {
                    i < 6 -> 4.0
                    i < 14 -> 1.0
                    i < 20 -> -1.0
                    i < 26 -> 1.5
                    else -> 4.0
                }
            },

            "Pop" to gainCurve(BAND_COUNT) { i ->
                when {
                    i < 6 -> -1.5
                    i < 14 -> 2.5
                    i < 22 -> 4.0
                    i < 28 -> 1.5
                    else -> -1.0
                }
            },

            "Jazz" to gainCurve(BAND_COUNT) { i ->
                when {
                    i < 8 -> 3.0
                    i < 16 -> -1.0
                    i < 24 -> 1.5
                    else -> 3.0
                }
            },

            "Electronic" to gainCurve(BAND_COUNT) { i ->
                when {
                    i < 8 -> 5.5
                    i < 16 -> -1.5
                    i < 24 -> 1.0
                    else -> 4.5
                }
            },

            "Vocal" to gainCurve(BAND_COUNT) { i ->
                when {
                    i < 8 -> -3.0
                    i < 14 -> -1.0
                    i < 22 -> 4.0
                    i < 28 -> 1.5
                    else -> -0.5
                }
            },

            // ── Indian music presets ──

            "Tamil Pop" to gainCurve(BAND_COUNT) { i ->
                when {
                    i < 7 -> 2.5            // sub/bass: kick + bass guitar
                    i < 14 -> 1.0           // low-mid: keep clean
                    i < 20 -> 3.0           // upper-mid: vocal presence
                    i < 26 -> 2.0           // presence: instrumental clarity
                    else -> 1.5             // air: cymbals, breath
                }
            },

            "Bollywood" to gainCurve(BAND_COUNT) { i ->
                when {
                    i < 7 -> 3.5            // dhol/bass-heavy low end
                    i < 14 -> 1.5
                    i < 20 -> 3.0           // lead vocal forward
                    i < 26 -> 1.5
                    else -> 1.0
                }
            },

            "Carnatic" to gainCurve(BAND_COUNT) { i ->
                when {
                    i < 7 -> 0.5            // minimal sub-bass, mridangam sits higher
                    i < 14 -> -0.5
                    i < 20 -> 4.0           // vocal/violin presence
                    i < 26 -> 2.5           // veena/violin harmonics
                    else -> 1.5
                }
            },

            "Devotional" to gainCurve(BAND_COUNT) { i ->
                when {
                    i < 7 -> 1.0
                    i < 14 -> 1.5
                    i < 20 -> 3.5           // chant/vocal clarity
                    i < 26 -> 2.0
                    else -> 1.0
                }
            },

            "Night Drive" to gainCurve(BAND_COUNT) { i ->
                when {
                    i < 7 -> 4.5            // deep sub
                    i < 14 -> 0.5
                    i < 20 -> -1.0          // dip harsh mids
                    i < 26 -> 2.0
                    else -> 4.0             // crisp highs
                }
            }
        )

        /** Helper to generate a 32-value gain list from a per-index function. */
        private fun gainCurve(count: Int, fn: (Int) -> Double): List<Double> =
            (0 until count).map { fn(it) }
    }

    private val prefs: SharedPreferences = context
        .getSharedPreferences("software_eq_v2", Context.MODE_PRIVATE)

    private val filters = Array(DEFAULT_BANDS.size) { BiquadFilter() }
    private val stereoOut = DoubleArray(2)

    private val _bands = MutableStateFlow(loadBands())
    val bands: StateFlow<List<SoftwareEqBand>> = _bands.asStateFlow()

    private val _isEnabled = MutableStateFlow(
        prefs.getBoolean("sw_eq_enabled", false)
    )
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _preAmpGain = MutableStateFlow(
        prefs.getFloat("sw_preamp", 0f).toDouble()
    )
    val preAmpGain: StateFlow<Double> = _preAmpGain.asStateFlow()

    private val _currentPreset = MutableStateFlow(
        prefs.getString("sw_eq_preset", "Flat") ?: "Flat"
    )
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    // ── Custom user-saved presets (name -> 32 gains) ──
    private val _customPresets = MutableStateFlow(loadCustomPresets())
    val customPresets: StateFlow<Map<String, List<Double>>> = _customPresets.asStateFlow()

    // ── A/B bypass state — true while temporarily flattened for comparison ──
    private val _isBypassed = MutableStateFlow(false)
    val isBypassed: StateFlow<Boolean> = _isBypassed.asStateFlow()
    private var bypassJob: Job? = null
    private val bypassScope = CoroutineScope(Dispatchers.Main)

    private var inputFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputBuffer = ByteBuffer.allocate(0)
    private var inputEnded = false
    private var sampleRate = 44100.0
    private var channelCount = 2

    init {
        updateFilters()
    }

    // ── AudioProcessor implementation ──

    override fun configure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputFormat)
        }
        this.inputFormat = inputFormat
        this.sampleRate = inputFormat.sampleRate.toDouble()
        this.channelCount = inputFormat.channelCount
        updateFilters()
        return inputFormat
    }

    override fun isActive(): Boolean = _isEnabled.value && !_isBypassed.value

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive() || inputFormat == AudioProcessor.AudioFormat.NOT_SET) {
            outputBuffer = inputBuffer
            return
        }

        val remaining = inputBuffer.remaining()
        if (outputBuffer.capacity() < remaining) {
            outputBuffer = ByteBuffer.allocate(remaining).order(ByteOrder.nativeOrder())
        }
        outputBuffer.clear()

        val preAmp = 10.0.pow(_preAmpGain.value / 20.0)
        val isMono = channelCount == 1

        while (inputBuffer.hasRemaining()) {
            val leftShort = inputBuffer.short
            val rightShort = if (isMono) leftShort else inputBuffer.short

            var left = (leftShort.toDouble() / 32768.0) * preAmp
            var right = (rightShort.toDouble() / 32768.0) * preAmp

            for (filter in filters) {
                filter.process(left, right, stereoOut)
                left = stereoOut[0]
                right = stereoOut[1]
            }

            outputBuffer.putShort((left.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort())
            if (!isMono) {
                outputBuffer.putShort((right.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort())
            }
        }

        outputBuffer.flip()
    }

    override fun queueEndOfStream() { inputEnded = true }

    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = ByteBuffer.allocate(0)
        return out
    }

    override fun isEnded(): Boolean = inputEnded && !outputBuffer.hasRemaining()

    override fun flush() {
        filters.forEach { it.reset() }
        outputBuffer = ByteBuffer.allocate(0)
        inputEnded = false
    }

    override fun reset() {
        flush()
        inputFormat = AudioProcessor.AudioFormat.NOT_SET
    }

    // ── Public API ──

    fun toggle() {
        val enabled = !_isEnabled.value
        _isEnabled.value = enabled
        prefs.edit().putBoolean("sw_eq_enabled", enabled).apply()
    }

    fun setPreAmp(gainDb: Double) {
        if (!_isEnabled.value) return
        _preAmpGain.value = gainDb
        prefs.edit().putFloat("sw_preamp", gainDb.toFloat()).apply()
    }

    fun setBandGain(bandIndex: Int, gainDb: Double) {
        if (!_isEnabled.value) return
        val current = _bands.value.toMutableList()
        if (bandIndex >= current.size) return
        current[bandIndex] = current[bandIndex].copy(gainDb = gainDb)
        _bands.value = current
        _currentPreset.value = "Custom"
        prefs.edit()
            .putFloat("sw_band_$bandIndex", gainDb.toFloat())
            .putString("sw_eq_preset", "Custom")
            .apply()
        updateFilters()
    }

    fun applyPreset(presetName: String) {
        if (!_isEnabled.value) return
        val gains = PRESETS[presetName] ?: _customPresets.value[presetName] ?: return
        val current = _bands.value.toMutableList()
        gains.forEachIndexed { i, gain ->
            if (i < current.size) {
                current[i] = current[i].copy(gainDb = gain)
                prefs.edit().putFloat("sw_band_$i", gain.toFloat()).apply()
            }
        }
        _bands.value = current
        _currentPreset.value = presetName
        prefs.edit().putString("sw_eq_preset", presetName).apply()
        updateFilters()
    }

    fun resetAll() {
        setPreAmp(0.0)
        applyPreset("Flat")
    }

    /**
     * Saves the current 32 band gains as a named custom preset. Overwrites
     * if a preset with the same name already exists.
     */
    fun saveCustomPreset(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val gains = _bands.value.map { it.gainDb }
        val updated = _customPresets.value.toMutableMap()
        updated[trimmed] = gains
        _customPresets.value = updated
        persistCustomPresets(updated)
        _currentPreset.value = trimmed
        prefs.edit().putString("sw_eq_preset", trimmed).apply()
    }

    fun deleteCustomPreset(name: String) {
        val updated = _customPresets.value.toMutableMap()
        updated.remove(name)
        _customPresets.value = updated
        persistCustomPresets(updated)
        if (_currentPreset.value == name) {
            applyPreset("Flat")
        }
    }

    /**
     * A/B bypass — temporarily flattens the EQ output for [durationMs] so the
     * user can hear the raw, unprocessed signal, then automatically restores
     * the active EQ settings. Calling this again while already bypassed
     * cancels the pending restore and starts a fresh comparison window.
     */
    fun toggleBypassPreview(durationMs: Long = 2500L) {
        bypassJob?.cancel()
        if (_isBypassed.value) {
            // Already mid-preview — restore immediately.
            _isBypassed.value = false
            return
        }
        _isBypassed.value = true
        bypassJob = bypassScope.launch {
            delay(durationMs)
            _isBypassed.value = false
        }
    }

    fun importAutoEqProfile(text: String) {
        val gains = MutableList(DEFAULT_BANDS.size) { 0.0 }
        val regex = Regex("""Fc\s+([\d.]+)\s+Hz\s+Gain\s+([-\d.]+)\s+dB""", RegexOption.IGNORE_CASE)
        regex.findAll(text).forEach { match ->
            val freq = match.groupValues[1].toDoubleOrNull() ?: return@forEach
            val gain = match.groupValues[2].toDoubleOrNull() ?: return@forEach
            val nearestBand = DEFAULT_BANDS.minByOrNull {
                Math.abs(Math.log10(it.frequency) - Math.log10(freq))
            }
            nearestBand?.let { gains[it.id] = gain }
        }
        val current = _bands.value.toMutableList()
        gains.forEachIndexed { i, gain ->
            if (i < current.size) current[i] = current[i].copy(gainDb = gain)
        }
        _bands.value = current
        _currentPreset.value = "AutoEq Import"
        prefs.edit().putString("sw_eq_preset", "AutoEq Import").apply()
        updateFilters()
    }

    // ── Internal helpers ──

    private fun updateFilters() {
        val currentBands = _bands.value
        filters.forEachIndexed { i, filter ->
            if (i < currentBands.size) {
                val band = currentBands[i]
                filter.isEnabled = band.enabled
                filter.configure(
                    type = band.type,
                    freq = band.frequency,
                    gainDb = band.gainDb,
                    q = band.q,
                    sampleRate = sampleRate
                )
            }
        }
    }

    private fun loadBands(): List<SoftwareEqBand> {
        return DEFAULT_BANDS.map { band ->
            band.copy(
                gainDb = prefs.getFloat("sw_band_${band.id}", 0f).toDouble()
            )
        }
    }

    private fun persistCustomPresets(presets: Map<String, List<Double>>) {
        val json = JSONObject()
        presets.forEach { (name, gains) ->
            json.put(name, JSONArray(gains))
        }
        prefs.edit().putString("sw_custom_presets", json.toString()).apply()
    }

    private fun loadCustomPresets(): Map<String, List<Double>> {
        val raw = prefs.getString("sw_custom_presets", null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            val result = mutableMapOf<String, List<Double>>()
            json.keys().forEach { name ->
                val arr = json.getJSONArray(name)
                result[name] = (0 until arr.length()).map { arr.getDouble(it) }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }
}