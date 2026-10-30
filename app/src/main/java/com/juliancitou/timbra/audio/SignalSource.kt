package com.juliancitou.timbra.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Fuente de sonido mono: entrega una muestra (−1 a 1) cada vez que se le pide. */
fun interface SignalSource {
    fun next(): Double
}

/** Nivel del ruido rosa: deja margen para subir bandas antes de que actúe el limitador. */
private const val PINK_NOISE_SCALE = 0.05

/**
 * Ruido rosa: misma energía en cada octava, como la música, a diferencia del ruido blanco que
 * suena chillón. Usa el filtro de Paul Kellet ("refined"), con error menor a ±0.05 dB sobre
 * 9 Hz. [seed] hace que la secuencia sea repetible en las pruebas.
 */
class PinkNoise(seed: Int = 1) : SignalSource {
    private val random = Random(seed)
    private var b0 = 0.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var b3 = 0.0
    private var b4 = 0.0
    private var b5 = 0.0
    private var b6 = 0.0

    override fun next(): Double {
        val white = random.nextDouble(-1.0, 1.0)
        b0 = 0.99886 * b0 + white * 0.0555179
        b1 = 0.99332 * b1 + white * 0.0750759
        b2 = 0.96900 * b2 + white * 0.1538520
        b3 = 0.86650 * b3 + white * 0.3104856
        b4 = 0.55000 * b4 + white * 0.5329522
        b5 = -0.7616 * b5 - white * 0.0168980
        val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
        b6 = white * 0.115926
        return pink * PINK_NOISE_SCALE
    }
}

/** Tono puro de [frequencyHz] con amplitud de pico [amplitude]. */
class SineTone(
    private val frequencyHz: Double,
    private val amplitude: Double,
    private val sampleRate: Int,
) : SignalSource {
    private var phase = 0.0
    private val phaseStep = 2 * PI * frequencyHz / sampleRate

    override fun next(): Double {
        val sample = amplitude * sin(phase)
        phase += phaseStep
        if (phase >= 2 * PI) phase -= 2 * PI
        return sample
    }
}
