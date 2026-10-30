package com.juliancitou.timbra.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Coeficientes de un filtro de segundo orden, ya normalizados (a0 = 1). Las fórmulas son las del
 * "Audio EQ Cookbook" de Robert Bristow-Johnson. Frecuencias en Hz, ganancias en dB.
 */
data class BiquadCoefficients(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double,
) {
    /** Ganancia en dB del filtro a [frequencyHz], evaluando H(z) en el círculo unitario. */
    fun magnitudeDb(frequencyHz: Double, sampleRate: Int): Double {
        val w = 2 * PI * frequencyHz / sampleRate
        val cos1 = cos(w)
        val sin1 = sin(w)
        val cos2 = cos(2 * w)
        val sin2 = sin(2 * w)
        val numeratorReal = b0 + b1 * cos1 + b2 * cos2
        val numeratorImaginary = -(b1 * sin1 + b2 * sin2)
        val denominatorReal = 1 + a1 * cos1 + a2 * cos2
        val denominatorImaginary = -(a1 * sin1 + a2 * sin2)
        val numerator = numeratorReal * numeratorReal + numeratorImaginary * numeratorImaginary
        val denominator = denominatorReal * denominatorReal + denominatorImaginary * denominatorImaginary
        return 10 * log10(numerator / denominator)
    }

    companion object {
        val Identity = BiquadCoefficients(1.0, 0.0, 0.0, 0.0, 0.0)

        /** Campana centrada en [frequencyHz]; sube o baja [gainDb] sin tocar el resto. */
        fun peaking(frequencyHz: Double, gainDb: Double, q: Double, sampleRate: Int): BiquadCoefficients {
            val a = 10.0.pow(gainDb / 40)
            val (cosW, alpha) = angularTerms(frequencyHz, q, sampleRate)
            return normalized(
                b0 = 1 + alpha * a,
                b1 = -2 * cosW,
                b2 = 1 - alpha * a,
                a0 = 1 + alpha / a,
                a1 = -2 * cosW,
                a2 = 1 - alpha / a,
            )
        }

        /** Estante bajo: cambia [gainDb] todo lo que queda por debajo de [frequencyHz]. */
        fun lowShelf(frequencyHz: Double, gainDb: Double, q: Double, sampleRate: Int): BiquadCoefficients {
            val a = 10.0.pow(gainDb / 40)
            val (cosW, alpha) = angularTerms(frequencyHz, q, sampleRate)
            val shelf = 2 * sqrt(a) * alpha
            return normalized(
                b0 = a * ((a + 1) - (a - 1) * cosW + shelf),
                b1 = 2 * a * ((a - 1) - (a + 1) * cosW),
                b2 = a * ((a + 1) - (a - 1) * cosW - shelf),
                a0 = (a + 1) + (a - 1) * cosW + shelf,
                a1 = -2 * ((a - 1) + (a + 1) * cosW),
                a2 = (a + 1) + (a - 1) * cosW - shelf,
            )
        }

        /** Estante alto: cambia [gainDb] todo lo que queda por encima de [frequencyHz]. */
        fun highShelf(frequencyHz: Double, gainDb: Double, q: Double, sampleRate: Int): BiquadCoefficients {
            val a = 10.0.pow(gainDb / 40)
            val (cosW, alpha) = angularTerms(frequencyHz, q, sampleRate)
            val shelf = 2 * sqrt(a) * alpha
            return normalized(
                b0 = a * ((a + 1) + (a - 1) * cosW + shelf),
                b1 = -2 * a * ((a - 1) + (a + 1) * cosW),
                b2 = a * ((a + 1) + (a - 1) * cosW - shelf),
                a0 = (a + 1) - (a - 1) * cosW + shelf,
                a1 = 2 * ((a - 1) - (a + 1) * cosW),
                a2 = (a + 1) - (a - 1) * cosW - shelf,
            )
        }

        fun highPass(frequencyHz: Double, q: Double, sampleRate: Int): BiquadCoefficients {
            val (cosW, alpha) = angularTerms(frequencyHz, q, sampleRate)
            return normalized(
                b0 = (1 + cosW) / 2,
                b1 = -(1 + cosW),
                b2 = (1 + cosW) / 2,
                a0 = 1 + alpha,
                a1 = -2 * cosW,
                a2 = 1 - alpha,
            )
        }

        fun lowPass(frequencyHz: Double, q: Double, sampleRate: Int): BiquadCoefficients {
            val (cosW, alpha) = angularTerms(frequencyHz, q, sampleRate)
            return normalized(
                b0 = (1 - cosW) / 2,
                b1 = 1 - cosW,
                b2 = (1 - cosW) / 2,
                a0 = 1 + alpha,
                a1 = -2 * cosW,
                a2 = 1 - alpha,
            )
        }

        /** Pasa-banda con ganancia de 0 dB en [frequencyHz]. */
        fun bandPass(frequencyHz: Double, q: Double, sampleRate: Int): BiquadCoefficients {
            val (cosW, alpha) = angularTerms(frequencyHz, q, sampleRate)
            return normalized(
                b0 = alpha,
                b1 = 0.0,
                b2 = -alpha,
                a0 = 1 + alpha,
                a1 = -2 * cosW,
                a2 = 1 - alpha,
            )
        }

        private fun angularTerms(frequencyHz: Double, q: Double, sampleRate: Int): Pair<Double, Double> {
            require(frequencyHz > 0 && frequencyHz < sampleRate / 2.0) { "La frecuencia debe estar bajo Nyquist" }
            require(q > 0) { "Q debe ser positivo" }
            val w0 = 2 * PI * frequencyHz / sampleRate
            return cos(w0) to sin(w0) / (2 * q)
        }

        private fun normalized(b0: Double, b1: Double, b2: Double, a0: Double, a1: Double, a2: Double) =
            BiquadCoefficients(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }
}

/**
 * Filtro biquad en forma directa II transpuesta: el estado en `Double` evita que el ruido de
 * redondeo crezca en filtros graves (20 Hz a 48 kHz tiene polos muy cerca del círculo unitario).
 */
class Biquad(var coefficients: BiquadCoefficients = BiquadCoefficients.Identity) {
    private var state1 = 0.0
    private var state2 = 0.0

    fun process(input: Double): Double {
        val c = coefficients
        val output = c.b0 * input + state1
        state1 = c.b1 * input - c.a1 * output + state2
        state2 = c.b2 * input - c.a2 * output
        return output
    }

    fun reset() {
        state1 = 0.0
        state2 = 0.0
    }
}
