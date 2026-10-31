package com.juliancitou.timbra.audio

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Cruces por cero de cada lado del filtro: da unos −74 dB de rechazo y deja intacto hasta ~18 kHz. */
private const val ZERO_CROSSINGS = 16

/**
 * Cambia la frecuencia de muestreo con interpolación sinc con ventana de Blackman. Una interpolación
 * lineal apagaría los agudos, justo lo que el usuario debe juzgar. La tabla usa la razón exacta entre
 * frecuencias (por ejemplo 160/147 de 44.1 a 48 kHz) y al bajar de frecuencia el corte baja con ella
 * para no plegar agudos.
 */
fun resample(input: FloatArray, fromRate: Int, toRate: Int): FloatArray {
    if (fromRate == toRate) return input
    val divisor = greatestCommonDivisor(fromRate, toRate)
    val phases = toRate / divisor
    val stride = fromRate / divisor
    val cutoff = min(1.0, toRate.toDouble() / fromRate)
    val halfWidth = ceil(ZERO_CROSSINGS / cutoff).toInt()
    val taps = 2 * halfWidth
    val table = kernelTable(phases, taps, halfWidth, cutoff)

    return FloatArray((input.size.toLong() * phases / stride).toInt()) { index ->
        val position = index.toLong() * stride
        val base = (position / phases).toInt()
        val row = (position % phases).toInt() * taps
        var sum = 0f
        for (tap in 0 until taps) {
            val sourceIndex = base - halfWidth + 1 + tap
            if (sourceIndex in input.indices) sum += input[sourceIndex] * table[row + tap]
        }
        sum
    }
}

/** Una fila por fase, normalizada a ganancia 1 para que el nivel no ondule entre fases. */
private fun kernelTable(phases: Int, taps: Int, halfWidth: Int, cutoff: Double): FloatArray {
    val table = FloatArray(phases * taps)
    for (phase in 0 until phases) {
        val row = DoubleArray(taps) { tap ->
            windowedSinc(phase.toDouble() / phases - (tap - halfWidth + 1), halfWidth, cutoff)
        }
        val total = row.sum()
        for (tap in 0 until taps) table[phase * taps + tap] = (row[tap] / total).toFloat()
    }
    return table
}

private fun windowedSinc(distance: Double, halfWidth: Int, cutoff: Double): Double {
    if (distance == 0.0) return cutoff
    val angle = PI * distance / halfWidth
    val window = 0.42 + 0.5 * cos(angle) + 0.08 * cos(2 * angle)
    return sin(PI * cutoff * distance) / (PI * distance) * window
}

private tailrec fun greatestCommonDivisor(a: Int, b: Int): Int = if (b == 0) a else greatestCommonDivisor(b, a % b)
