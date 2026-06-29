package com.vibeup.android.service.audio

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        // Optimized frequencies and Q for 10-band EQ
        val DEFAULT_BANDS = listOf(
            SoftwareEqBand(0,  31.0,   type = BiquadFilter.Type.LOW_SHELF),
            SoftwareEqBand(1,  62.0),
            SoftwareEqBand(2,  125.0),
            SoftwareEqBand(3,  250.0),
            SoftwareEqBand(4,  500.0),
            SoftwareEqBand(5,  1000.0),
            SoftwareEqBand(6,  2000.0),
            SoftwareEqBand(7,  4000.0),
            SoftwareEqBand(8,  8000.0),
            SoftwareEqBand(9,  16000.0, type = BiquadFilter.Type.HIGH_SHELF)
        )

        val PRESETS = mapOf(
            "Flat"          to listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            "Rock"          to listOf(4.5, 3.5, 2.0, 0.5, -1.5, -0.5, 1.0, 3.0, 4.0, 4.5),
            "Pop"           to listOf(-1.5, 0.0, 2.0, 3.5, 4.5, 3.5, 1.5, 0.0, -1.0, -1.5),
            "Jazz"          to listOf(3.5, 2.5, 1.0, 2.0, -1.5, -1.5, 0.0, 1.5, 2.5, 3.5),
            "Electronic"    to listOf(5.0, 4.0, 2.0, 0.0, -2.0, 0.0, 1.5, 2.5, 4.5, 5.0),
            "Bass Boost"    to listOf(6.0, 5.0, 3.5, 1.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            "Treble Boost"  to listOf(0.0, 0.0, 0.0, 0.0, 0.0, 1.5, 3.0, 4.5, 5.5, 6.5),
            "Vocal"         to listOf(-3.0, -2.0, -1.0, 1.5, 3.5, 4.5, 3.0, 1.5, 0.5, -0.5)
        )
    }

    private val prefs: SharedPreferences = context
        .getSharedPreferences("software_eq", Context.MODE_PRIVATE)

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

    private var inputFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputBuffer = ByteBuffer.allocate(0)
    private var inputEnded = false
    private var sampleRate = 44100.0

    init {
        updateFilters()
    }

    override fun configure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
             throw AudioProcessor.UnhandledAudioFormatException(inputFormat)
        }
        this.inputFormat = inputFormat
        sampleRate = inputFormat.sampleRate.toDouble()
        updateFilters()
        return inputFormat
    }

    override fun isActive(): Boolean = _isEnabled.value

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!_isEnabled.value || inputFormat == AudioProcessor.AudioFormat.NOT_SET) {
            outputBuffer = inputBuffer
            return
        }

        val remaining = inputBuffer.remaining()
        if (outputBuffer.capacity() < remaining) {
            outputBuffer = ByteBuffer.allocate(remaining).order(ByteOrder.nativeOrder())
        }
        outputBuffer.clear()

        val preAmp = 10.0.pow(_preAmpGain.value / 20.0)

        while (inputBuffer.hasRemaining()) {
            // Process stereo 16-bit
            val leftShort = inputBuffer.short
            val rightShort = inputBuffer.short

            var left = (leftShort.toDouble() / 32768.0) * preAmp
            var right = (rightShort.toDouble() / 32768.0) * preAmp

            for (filter in filters) {
                filter.process(left, right, stereoOut)
                left = stereoOut[0]
                right = stereoOut[1]
            }

            // Clip and scale back
            val outL = (left.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
            val outR = (right.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()

            outputBuffer.putShort(outL)
            outputBuffer.putShort(outR)
        }

        outputBuffer.flip()
    }

    override fun queueEndOfStream() { inputEnded = true }
    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return out
    }
    override fun isEnded(): Boolean = inputEnded && outputBuffer == AudioProcessor.EMPTY_BUFFER
    override fun flush() {
        filters.forEach { it.reset() }
        outputBuffer = AudioProcessor.EMPTY_BUFFER
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
        _preAmpGain.value = gainDb
        prefs.edit().putFloat("sw_preamp", gainDb.toFloat()).apply()
    }

    fun setBandGain(bandIndex: Int, gainDb: Double) {
        val current = _bands.value.toMutableList()
        if (bandIndex >= current.size) return
        current[bandIndex] = current[bandIndex].copy(gainDb = gainDb)
        _bands.value = current
        _currentPreset.value = "Custom"
        prefs.edit().putFloat("sw_band_$bandIndex", gainDb.toFloat()).apply()
        updateFilters()
    }

    fun applyPreset(presetName: String) {
        val gains = PRESETS[presetName] ?: return
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
                    q = 1.41, // Suitable for 10-band octave EQ
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

    fun importAutoEqProfile(text: String) {
        val gains = MutableList(10) { 0.0 }
        val regex = Regex("""Fc\s+([\d.]+)\s+Hz\s+Gain\s+([-\d.]+)\s+dB""", RegexOption.IGNORE_CASE)
        regex.findAll(text).forEach { match ->
            val freq = match.groupValues[1].toDoubleOrNull() ?: return@forEach
            val gain = match.groupValues[2].toDoubleOrNull() ?: return@forEach
            val nearestBand = DEFAULT_BANDS.minByOrNull { Math.abs(Math.log10(it.frequency) - Math.log10(freq)) }
            nearestBand?.let { gains[it.id] = gain }
        }
        val current = _bands.value.toMutableList()
        gains.forEachIndexed { i, gain -> if (i < current.size) current[i] = current[i].copy(gainDb = gain) }
        _bands.value = current
        _currentPreset.value = "AutoEq Import"
        updateFilters()
    }
}
