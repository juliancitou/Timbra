package com.juliancitou.timbra.ui.theme

import kotlin.math.abs
import kotlin.math.roundToInt

private const val MINUS_SIGN = "−"

/**
 * Ganancia con signo y un decimal: "+1.5 dB", "−3.0 dB", "0.0 dB". Usa el signo menos tipográfico
 * y no depende del idioma del teléfono, para que el ancho no cambie entre valores.
 */
fun formatDecibels(gainDb: Float): String {
    val tenths = (gainDb * 10).roundToInt()
    val sign = when {
        tenths > 0 -> "+"
        tenths < 0 -> MINUS_SIGN
        else -> ""
    }
    val magnitude = abs(tenths)
    return "$sign${magnitude / 10}.${magnitude % 10} dB"
}

/** Frecuencia legible: "843 Hz", "1.1 kHz", "8.4 kHz", "20 kHz". */
fun formatHertz(frequencyHz: Int): String {
    if (frequencyHz < 1000) return "$frequencyHz Hz"
    val tenthsOfKilohertz = (frequencyHz / 100.0).roundToInt()
    val whole = tenthsOfKilohertz / 10
    val decimal = tenthsOfKilohertz % 10
    return if (decimal == 0) "$whole kHz" else "$whole.$decimal kHz"
}
