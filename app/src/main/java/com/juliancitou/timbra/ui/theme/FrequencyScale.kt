package com.juliancitou.timbra.ui.theme

import kotlin.math.log10

const val CHART_MIN_FREQUENCY_HZ = 20f
const val CHART_MAX_FREQUENCY_HZ = 20_000f

/**
 * Posición horizontal (0 a 1) de una frecuencia en escala logarítmica: cada octava ocupa el
 * mismo ancho, como la percibe el oído. 20 Hz queda en 0 y 20 kHz en 1.
 */
fun frequencyToFraction(frequencyHz: Float): Float {
    val clamped = frequencyHz.coerceIn(CHART_MIN_FREQUENCY_HZ, CHART_MAX_FREQUENCY_HZ)
    return (log10(clamped / CHART_MIN_FREQUENCY_HZ) / log10(CHART_MAX_FREQUENCY_HZ / CHART_MIN_FREQUENCY_HZ))
}

/** Posición vertical (0 arriba, 1 abajo) de una ganancia en dB dentro de [minDb]..[maxDb]. */
fun gainToFraction(gainDb: Float, minDb: Float, maxDb: Float): Float =
    ((maxDb - gainDb) / (maxDb - minDb)).coerceIn(0f, 1f)
