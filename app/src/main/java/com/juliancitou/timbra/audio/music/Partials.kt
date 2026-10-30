package com.juliancitou.timbra.audio.music

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/** Un componente senoidal de un sonido: frecuencia en Hz, amplitud y tiempo de caída en segundos. */
data class Partial(val frequencyHz: Double, val amplitude: Double, val decaySeconds: Double)

/** Los parciales por encima de esta fracción de la frecuencia de muestreo se omiten (sin aliasing). */
private const val MAX_PARTIAL_FRACTION = 0.45

/**
 * Suma [partials] a [target] desde [start] durante [lengthSamples], cada uno apagándose a su
 * propio ritmo y todo multiplicado por [envelope] (en función del tiempo en segundos). Así suenan
 * los instrumentos de cuerda pulsada o golpeada: los armónicos agudos mueren antes que los graves,
 * y eso es lo que el oído reconoce como "piano" o "contrabajo".
 *
 * Cada parcial es un oscilador por rotación (un número complejo que gira un ángulo fijo por
 * muestra), mucho más barato que llamar a `sin` en cada muestra.
 */
fun addPartials(
    target: DoubleArray,
    start: Int,
    lengthSamples: Int,
    sampleRate: Int,
    partials: List<Partial>,
    envelope: (Double) -> Double = { 1.0 },
) {
    val audible = partials.filter { it.frequencyHz < sampleRate * MAX_PARTIAL_FRACTION && it.amplitude > 0 }
    val stepCos = DoubleArray(audible.size) { cos(2 * PI * audible[it].frequencyHz / sampleRate) }
    val stepSin = DoubleArray(audible.size) { sin(2 * PI * audible[it].frequencyHz / sampleRate) }
    val decay = DoubleArray(audible.size) { exp(-1.0 / (audible[it].decaySeconds * sampleRate)) }
    val real = DoubleArray(audible.size) { 1.0 }
    val imaginary = DoubleArray(audible.size)
    val amplitude = DoubleArray(audible.size) { audible[it].amplitude }

    for (n in 0 until lengthSamples) {
        var sum = 0.0
        for (k in audible.indices) {
            val rotatedReal = real[k] * stepCos[k] - imaginary[k] * stepSin[k]
            imaginary[k] = real[k] * stepSin[k] + imaginary[k] * stepCos[k]
            real[k] = rotatedReal
            sum += imaginary[k] * amplitude[k]
            amplitude[k] *= decay[k]
        }
        target[start + n] += sum * envelope(n.toDouble() / sampleRate)
    }
}
