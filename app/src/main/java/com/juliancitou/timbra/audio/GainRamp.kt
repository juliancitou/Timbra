package com.juliancitou.timbra.audio

import kotlin.math.PI
import kotlin.math.cos

/** Duración de cada entrada, salida o cambio de ecualización. No quitarla sin que el usuario lo pida. */
const val FADE_SECONDS = 0.05

/**
 * Curva de transición de 0 a 1 con forma de coseno: arranca y termina con pendiente cero, así
 * que no hay esquinas que el oído perciba como clic.
 */
fun raisedCosine(progress: Double): Double = 0.5 - 0.5 * cos(PI * progress.coerceIn(0.0, 1.0))

/**
 * Ganancia de entrada y salida del sonido: va de 0 a 1 (o al revés) en [FADE_SECONDS] siguiendo
 * [raisedCosine]. Se llama [next] una vez por muestra.
 */
class GainRamp(sampleRate: Int, startsAudible: Boolean = false) {
    private val step = 1.0 / (FADE_SECONDS * sampleRate)
    private var progress = if (startsAudible) 1.0 else 0.0
    private var rising = startsAudible

    /** `true` cuando terminó de bajar: ya se puede detener la salida sin cortar el sonido. */
    val isSilent: Boolean get() = !rising && progress == 0.0

    fun fadeIn() {
        rising = true
    }

    fun fadeOut() {
        rising = false
    }

    fun next(): Double {
        progress = if (rising) minOf(1.0, progress + step) else maxOf(0.0, progress - step)
        return raisedCosine(progress)
    }
}
