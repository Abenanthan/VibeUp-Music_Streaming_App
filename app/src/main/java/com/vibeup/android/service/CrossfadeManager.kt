package com.vibeup.android.service

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrossfadeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context
        .getSharedPreferences("crossfade_prefs", Context.MODE_PRIVATE)

    private val _isEnabled = MutableStateFlow(
        prefs.getBoolean("crossfade_enabled", false)
    )
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _durationSeconds = MutableStateFlow(
        prefs.getInt("crossfade_duration", 5)
    )
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private var monitoringJob: Job? = null
    private var volumeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun toggle() {
        val enabled = !_isEnabled.value
        _isEnabled.value = enabled
        prefs.edit().putBoolean("crossfade_enabled", enabled).apply()
        if (!enabled) {
            monitoringJob?.cancel()
            volumeJob?.cancel()
        }
    }

    fun setDuration(seconds: Int) {
        _durationSeconds.value = seconds
        prefs.edit().putInt("crossfade_duration", seconds).apply()
    }

    // ✅ Start monitoring for crossfade trigger
    fun startMonitoring(
        player: ExoPlayer,
        onCrossfadeNeeded: () -> Unit
    ) {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (true) {
                delay(500)
                if (!_isEnabled.value) continue
                
                val duration = player.duration
                val position = player.currentPosition
                if (duration <= 0) continue

                val timeRemaining = duration - position
                val crossfadeDurationMs = _durationSeconds.value * 1000L

                // ✅ Trigger crossfade when near end
                if (timeRemaining in 1..(crossfadeDurationMs + 500) &&
                    player.isPlaying
                ) {
                    onCrossfadeNeeded()
                    break
                }
            }
        }
    }

    // ✅ Fade out current player
    fun fadeOut(
        player: ExoPlayer,
        onComplete: () -> Unit
    ) {
        volumeJob?.cancel()
        val durationMs = _durationSeconds.value * 1000L
        val steps = 20
        val stepDelay = durationMs / steps
        var currentStep = 0

        volumeJob = scope.launch {
            while (currentStep <= steps) {
                val volume = 1f - (currentStep.toFloat() / steps)
                player.volume = volume.coerceIn(0f, 1f)
                currentStep++
                delay(stepDelay)
            }
            player.volume = 0f
            onComplete()
        }
    }

    // ✅ Fade in next song
    fun fadeIn(
        player: ExoPlayer,
        onComplete: () -> Unit
    ) {
        volumeJob?.cancel()
        val durationMs = _durationSeconds.value * 1000L
        val steps = 20
        val stepDelay = durationMs / steps
        var currentStep = 0

        player.volume = 0f

        volumeJob = scope.launch {
            while (currentStep <= steps) {
                val volume = currentStep.toFloat() / steps
                player.volume = volume.coerceIn(0f, 1f)
                currentStep++
                delay(stepDelay)
            }
            player.volume = 1f
            onComplete()
        }
    }

    fun cancelFade(player: ExoPlayer) {
        monitoringJob?.cancel()
        volumeJob?.cancel()
        player.volume = 1f
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
    }
}