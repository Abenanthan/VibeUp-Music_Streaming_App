package com.vibeup.android.service.audio

data class SoftwareEqBand(
    val id: Int,
    val frequency: Double,   // Hz
    var gainDb: Double = 0.0,// -12 to +12 dB
    var q: Double = 1.0,     // bandwidth
    val type: BiquadFilter.Type = BiquadFilter.Type.PEAK_EQ,
    var enabled: Boolean = true
)
