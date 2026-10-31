package com.juliancitou.timbra.audio

import com.juliancitou.timbra.audio.music.MUSIC_MAX_PEAK_DBFS
import com.juliancitou.timbra.audio.music.MUSIC_TARGET_LUFS
import kotlin.math.abs

private const val REFERENCE_SECONDS = 4
private const val SEAM_FADE_SECONDS = 0.05

/** Una canción del usuario lista para repetirse. [reference] es su parte más fuerte, en mono. */
class PreparedSong(val left: FloatArray, val right: FloatArray, val reference: DoubleArray)

/**
 * Deja una canción comercial al mismo nivel que la música de la app. Suelen venir mucho más fuertes
 * que [MUSIC_TARGET_LUFS], y sin esto el limitador las estaría aplastando todo el tiempo. Se mide en
 * su parte más fuerte, para que ningún tramo llegue al limitador y para que el volumen entre opciones
 * se iguale donde más se nota, no en una introducción suave.
 */
fun prepareSong(left: FloatArray, right: FloatArray, sampleRate: Int): PreparedSong {
    require(left.size == right.size) { "Los canales deben medir lo mismo" }
    require(left.size >= sampleRate) { "La canción es demasiado corta" }
    val mono = DoubleArray(left.size) { (left[it] + right[it]) / 2.0 }
    val windowLength = minOf(mono.size, REFERENCE_SECONDS * sampleRate)
    val start = loudestWindowStart(mono, windowLength, sampleRate)
    val reference = mono.copyOfRange(start, start + windowLength)

    val loudness = loudnessLufs(reference, sampleRate)
    val peak = maxOf(left.maxOf { abs(it) }, right.maxOf { abs(it) }).toDouble()
    require(loudness.isFinite() && peak > 0.0) { "La canción está en silencio" }
    val gain = minOf(decibelsToGain(MUSIC_TARGET_LUFS - loudness), decibelsToGain(MUSIC_MAX_PEAK_DBFS) / peak)

    return PreparedSong(
        left = scaledWithFadedEnds(left, gain, sampleRate),
        right = scaledWithFadedEnds(right, gain, sampleRate),
        reference = DoubleArray(windowLength) { reference[it] * gain },
    )
}

/** Busca avanzando de segundo en segundo, que basta para ubicar la parte fuerte. */
private fun loudestWindowStart(samples: DoubleArray, windowLength: Int, hop: Int): Int =
    (0..samples.size - windowLength step hop).maxBy { start ->
        var energy = 0.0
        for (index in start until start + windowLength) energy += samples[index] * samples[index]
        energy
    }

/** Las puntas se desvanecen para que la vuelta del bucle no suene como un clic. */
private fun scaledWithFadedEnds(samples: FloatArray, gain: Double, sampleRate: Int): FloatArray {
    val fadeLength = (SEAM_FADE_SECONDS * sampleRate).toInt().coerceAtMost(samples.size / 2)
    return FloatArray(samples.size) { index ->
        val fromEdge = minOf(index, samples.lastIndex - index)
        val fade = if (fromEdge < fadeLength) raisedCosine(fromEdge.toDouble() / fadeLength) else 1.0
        (samples[index] * gain * fade).toFloat()
    }
}
