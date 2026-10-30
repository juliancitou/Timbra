package com.juliancitou.timbra.audio

import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.tan

/**
 * Ponderación K de ITU-R BS.1770 (la misma base que usan las plataformas de música para igualar
 * volumen): un estante de +4 dB sobre ~1.7 kHz que imita el efecto de la cabeza, y un paso alto
 * en ~38 Hz porque el oído casi no percibe la energía más grave. Parámetros de la norma, válidos
 * para cualquier frecuencia de muestreo.
 */
private const val K_SHELF_HZ = 1681.974450955533
private const val K_SHELF_GAIN_DB = 3.999843853973347
private const val K_SHELF_Q = 0.7071752369554196
private const val K_HIGH_PASS_HZ = 38.13547087602444
private const val K_HIGH_PASS_Q = 0.5003270373238773

/** Desfase de la fórmula de sonoridad de BS.1770, en LU. */
private const val LOUDNESS_OFFSET = -0.691

/** Exponente de la ganancia de banda media del estante en la derivación de la norma. */
private const val K_SHELF_MID_EXPONENT = 0.4996667741545416

/**
 * Los dos filtros de la ponderación K. Usan la transformación bilineal con la que se obtienen los
 * coeficientes publicados en BS.1770 (no las fórmulas del "Cookbook", que dan otro estante).
 */
fun kWeightingFilters(sampleRate: Int): List<BiquadCoefficients> = listOf(kShelf(sampleRate), kHighPass(sampleRate))

private fun kShelf(sampleRate: Int): BiquadCoefficients {
    val k = tan(PI * K_SHELF_HZ / sampleRate)
    val highGain = 10.0.pow(K_SHELF_GAIN_DB / 20)
    val midGain = highGain.pow(K_SHELF_MID_EXPONENT)
    val a0 = 1 + k / K_SHELF_Q + k * k
    return BiquadCoefficients(
        b0 = (highGain + midGain * k / K_SHELF_Q + k * k) / a0,
        b1 = 2 * (k * k - highGain) / a0,
        b2 = (highGain - midGain * k / K_SHELF_Q + k * k) / a0,
        a1 = 2 * (k * k - 1) / a0,
        a2 = (1 - k / K_SHELF_Q + k * k) / a0,
    )
}

private fun kHighPass(sampleRate: Int): BiquadCoefficients {
    val k = tan(PI * K_HIGH_PASS_HZ / sampleRate)
    val a0 = 1 + k / K_HIGH_PASS_Q + k * k
    return BiquadCoefficients(
        b0 = 1.0,
        b1 = -2.0,
        b2 = 1.0,
        a1 = 2 * (k * k - 1) / a0,
        a2 = (1 - k / K_HIGH_PASS_Q + k * k) / a0,
    )
}

/**
 * Sonoridad de una señal mono en LUFS, sin compuertas: sirve para fragmentos cortos y continuos
 * como los del test. Un seno de 1 kHz a escala completa mide ~−3 LUFS.
 */
fun loudnessLufs(samples: DoubleArray, sampleRate: Int): Double {
    val filters = kWeightingFilters(sampleRate).map { Biquad(it) }
    var sumOfSquares = 0.0
    for (sample in samples) {
        var weighted = sample
        for (filter in filters) weighted = filter.process(weighted)
        sumOfSquares += weighted * weighted
    }
    return LOUDNESS_OFFSET + 10 * log10(sumOfSquares / samples.size)
}

/**
 * Iguala el volumen percibido entre ecualizaciones. Si no se hiciera, el oído preferiría la
 * opción que suena más fuerte aunque su tono no sea mejor. Mide [reference] (el fragmento que
 * sonará) con cada ecualización y corrige su preamp de vista previa para que todas suenen como
 * la señal sin ecualizar.
 */
class LoudnessMatcher(private val sampleRate: Int, private val reference: DoubleArray) {
    private val referenceLoudness = loudnessLufs(reference, sampleRate)

    /** Diferencia en dB que hay que sumar a [settings] para que suene igual que sin ecualizar. */
    fun compensationDb(settings: EqualizerSettings): Double {
        val equalizer = GraphicEqualizer(sampleRate, settings)
        val processed = DoubleArray(reference.size) { equalizer.process(reference[it]) }
        return referenceLoudness - loudnessLufs(processed, sampleRate)
    }

    /** [settings] con el preamp ajustado solo para escuchar; el preset exportado no cambia. */
    fun compensated(settings: EqualizerSettings): EqualizerSettings =
        settings.copy(preampDb = settings.preampDb + compensationDb(settings).toFloat())
}
