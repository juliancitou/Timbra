package com.juliancitou.timbra.audio

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/** Nivel promedio máximo de la app, en dBFS RMS. No subirlo sin que el usuario lo pida. */
const val MAX_OUTPUT_RMS_DBFS = -18.0

/** Pico máximo absoluto de cualquier muestra, en dBFS. No subirlo sin que el usuario lo pida. */
const val MAX_OUTPUT_PEAK_DBFS = -6.0

/** Ventana del medidor de nivel promedio: parecida a cómo el oído integra la sonoridad. */
private const val RMS_WINDOW_SECONDS = 0.4
private const val RMS_ATTACK_SECONDS = 0.02
private const val RMS_RELEASE_SECONDS = 0.5
private const val PEAK_RELEASE_SECONDS = 0.1

fun decibelsToGain(decibels: Double): Double = 10.0.pow(decibels / 20)

/**
 * Último paso antes de la salida de audio; todo buffer pasa por aquí. Dos etapas:
 *
 * 1. **Nivel:** mide el RMS en una ventana de ~400 ms y, si pasa de [MAX_OUTPUT_RMS_DBFS], baja
 *    la ganancia con ataque de 20 ms y liberación de 500 ms (sin bombeo audible).
 * 2. **Picos:** si una muestra pasaría de [MAX_OUTPUT_PEAK_DBFS], la ganancia baja en esa misma
 *    muestra (ataque instantáneo) y se recupera en 100 ms. Un recorte duro final garantiza el
 *    techo aunque fallara todo lo anterior.
 *
 * Las muestras no finitas (NaN, ∞) salen como silencio. En estéreo los dos canales comparten la misma
 * ganancia, para que limitar no mueva el sonido hacia un lado.
 */
class SafetyLimiter(sampleRate: Int) {
    private val rmsCeiling = decibelsToGain(MAX_OUTPUT_RMS_DBFS)
    private val peakCeiling = decibelsToGain(MAX_OUTPUT_PEAK_DBFS)

    private val rmsSmoothing = smoothingFactor(RMS_WINDOW_SECONDS, sampleRate)
    private val rmsAttack = smoothingFactor(RMS_ATTACK_SECONDS, sampleRate)
    private val rmsRelease = smoothingFactor(RMS_RELEASE_SECONDS, sampleRate)
    private val peakRelease = smoothingFactor(PEAK_RELEASE_SECONDS, sampleRate)

    private var meanSquare = 0.0
    private var levelGain = 1.0
    private var peakGain = 1.0

    fun process(sample: Double): Float {
        if (!sample.isFinite()) return 0f
        val gain = updatedGain(sample * sample, abs(sample))
        return (sample * gain).coerceIn(-peakCeiling, peakCeiling).toFloat()
    }

    /** Escribe el cuadro limitado en [output], en [offset] (izquierdo) y `offset + 1` (derecho). */
    fun processStereo(left: Double, right: Double, output: FloatArray, offset: Int) {
        if (!left.isFinite() || !right.isFinite()) {
            output[offset] = 0f
            output[offset + 1] = 0f
            return
        }
        val gain = updatedGain((left * left + right * right) / 2, maxOf(abs(left), abs(right)))
        output[offset] = (left * gain).coerceIn(-peakCeiling, peakCeiling).toFloat()
        output[offset + 1] = (right * gain).coerceIn(-peakCeiling, peakCeiling).toFloat()
    }

    fun reset() {
        meanSquare = 0.0
        levelGain = 1.0
        peakGain = 1.0
    }

    private fun updatedGain(power: Double, peak: Double): Double {
        meanSquare += (power - meanSquare) * rmsSmoothing
        val rms = sqrt(meanSquare)
        val targetLevelGain = if (rms > rmsCeiling) rmsCeiling / rms else 1.0
        val levelCoefficient = if (targetLevelGain < levelGain) rmsAttack else rmsRelease
        levelGain += (targetLevelGain - levelGain) * levelCoefficient

        val leveledPeak = peak * levelGain
        peakGain += (1.0 - peakGain) * peakRelease
        if (leveledPeak * peakGain > peakCeiling) peakGain = peakCeiling / leveledPeak
        return levelGain * peakGain
    }

    /** Coeficiente de un suavizado exponencial con constante de tiempo [seconds]. */
    private fun smoothingFactor(seconds: Double, sampleRate: Int) = 1 - exp(-1.0 / (seconds * sampleRate))
}
