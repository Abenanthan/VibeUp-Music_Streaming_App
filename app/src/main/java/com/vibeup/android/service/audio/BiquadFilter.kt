package com.vibeup.android.service.audio

import kotlin.math.*

/**
 * Optimized High-Quality Biquad Filter (Transposed Direct Form II).
 * Features per-sample coefficient smoothing to eliminate zipper noise.
 */
class BiquadFilter {

    enum class Type {
        PEAK_EQ, LOW_SHELF, HIGH_SHELF
    }

    // Target coefficients
    private var b0 = 1.0; private var b1 = 0.0; private var b2 = 0.0
    private var a1 = 0.0; private var a2 = 0.0

    // Current coefficients (for smoothing)
    private var curB0 = 1.0; private var curB1 = 0.0; private var curB2 = 0.0
    private var curA1 = 0.0; private var curA2 = 0.0

    // Smoothing steps
    private var stepB0 = 0.0; private var stepB1 = 0.0; private var stepB2 = 0.0
    private var stepA1 = 0.0; private var stepA2 = 0.0
    private var framesRemaining = 0

    // State variables (TDF2)
    private var s1L = 0.0; private var s2L = 0.0
    private var s1R = 0.0; private var s2R = 0.0

    var isEnabled = true

    /**
     * Configure filter with smoothing over a specific number of frames.
     */
    fun configure(
        type: Type,
        freq: Double,
        gainDb: Double,
        q: Double,
        sampleRate: Double,
        smoothFrames: Int = 1024
    ) {
        val w0 = 2.0 * PI * freq / sampleRate
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val A = 10.0.pow(gainDb / 40.0)
        val alpha = sinW0 / (2.0 * q)

        val b0Tmp: Double
        val b1Tmp: Double
        val b2Tmp: Double
        val a0Tmp: Double
        val a1Tmp: Double
        val a2Tmp: Double

        when (type) {
            Type.PEAK_EQ -> {
                b0Tmp = 1.0 + alpha * A
                b1Tmp = -2.0 * cosW0
                b2Tmp = 1.0 - alpha * A
                a0Tmp = 1.0 + alpha / A
                a1Tmp = -2.0 * cosW0
                a2Tmp = 1.0 - alpha / A
            }
            Type.LOW_SHELF -> {
                val sqrtA = sqrt(A)
                b0Tmp = A * ((A + 1.0) - (A - 1.0) * cosW0 + 2.0 * sqrtA * alpha)
                b1Tmp = 2.0 * A * ((A - 1.0) - (A + 1.0) * cosW0)
                b2Tmp = A * ((A + 1.0) - (A - 1.0) * cosW0 - 2.0 * sqrtA * alpha)
                a0Tmp = (A + 1.0) + (A - 1.0) * cosW0 + 2.0 * sqrtA * alpha
                a1Tmp = -2.0 * ((A - 1.0) + (A + 1.0) * cosW0)
                a2Tmp = (A + 1.0) + (A - 1.0) * cosW0 - 2.0 * sqrtA * alpha
            }
            Type.HIGH_SHELF -> {
                val sqrtA = sqrt(A)
                b0Tmp = A * ((A + 1.0) + (A - 1.0) * cosW0 + 2.0 * sqrtA * alpha)
                b1Tmp = -2.0 * A * ((A - 1.0) + (A + 1.0) * cosW0)
                b2Tmp = A * ((A + 1.0) + (A - 1.0) * cosW0 - 2.0 * sqrtA * alpha)
                a0Tmp = (A + 1.0) - (A - 1.0) * cosW0 + 2.0 * sqrtA * alpha
                a1Tmp = 2.0 * ((A - 1.0) - (A + 1.0) * cosW0)
                a2Tmp = (A + 1.0) - (A - 1.0) * cosW0 - 2.0 * sqrtA * alpha
            }
        }

        // New Target
        this.b0 = b0Tmp / a0Tmp
        this.b1 = b1Tmp / a0Tmp
        this.b2 = b2Tmp / a0Tmp
        this.a1 = a1Tmp / a0Tmp
        this.a2 = a2Tmp / a0Tmp

        // Calculate steps for smoothing
        if (smoothFrames > 0) {
            stepB0 = (this.b0 - curB0) / smoothFrames
            stepB1 = (this.b1 - curB1) / smoothFrames
            stepB2 = (this.b2 - curB2) / smoothFrames
            stepA1 = (this.a1 - curA1) / smoothFrames
            stepA2 = (this.a2 - curA2) / smoothFrames
            framesRemaining = smoothFrames
        } else {
            curB0 = b0; curB1 = b1; curB2 = b2; curA1 = a1; curA2 = a2
            framesRemaining = 0
        }
    }

    fun reset() {
        s1L = 0.0; s2L = 0.0
        s1R = 0.0; s2R = 0.0
        // Coefficients should not be reset to 1.0/0.0 but kept at current targets
    }

    /**
     * Process stereo sample using TDF2.
     * Direct modification of the input variables for performance.
     */
    fun process(left: Double, right: Double, out: DoubleArray) {
        if (!isEnabled) {
            out[0] = left
            out[1] = right
            return
        }

        // Apply smoothing
        if (framesRemaining > 0) {
            curB0 += stepB0
            curB1 += stepB1
            curB2 += stepB2
            curA1 += stepA1
            curA2 += stepA2
            framesRemaining--
        }

        // Left Channel (TDF2)
        val outL = left * curB0 + s1L
        s1L = left * curB1 - outL * curA1 + s2L
        s2L = left * curB2 - outL * curA2

        // Right Channel (TDF2)
        val outR = right * curB0 + s1R
        s1R = right * curB1 - outR * curA1 + s2R
        s2R = right * curB2 - outR * curA2

        out[0] = outL
        out[1] = outR
    }
}
