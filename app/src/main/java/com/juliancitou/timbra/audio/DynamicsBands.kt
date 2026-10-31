package com.juliancitou.timbra.audio

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/** Una banda de `DynamicsProcessing`: llega hasta [upperEdgeHz] y aplica [gainDb]. */
data class PreviewBand(val upperEdgeHz: Float, val gainDb: Float)

private const val DENSE_LOWEST_HZ = 20.0
private const val DENSE_HIGHEST_HZ = 20_000.0

/** Un sexto de octava: más fino que cualquier banda de Poweramp, y cada banda es una llamada entre procesos. */
private const val DENSE_BANDS_PER_OCTAVE = 6

/** Más allá de lo que cualquier teléfono reproduce. */
private const val NO_UPPER_LIMIT_HZ = 24_000f

/**
 * Las bandas de `DynamicsProcessing` que reproducen [settings], sin el preamp. Se usa una curva densa y
 * no una banda por cada banda del preset, porque esa daría escalones donde Poweramp da una curva.
 */
fun previewBands(settings: EqualizerSettings, overlap: BandOverlap, sampleRate: Int): List<PreviewBand> {
    val equalizer = GraphicEqualizer(sampleRate, settings.copy(preampDb = 0f, overlap = overlap))
    val centers = generateSequence(0) { it + 1 }
        .map { DENSE_LOWEST_HZ * 2.0.pow(it.toDouble() / DENSE_BANDS_PER_OCTAVE) }
        .takeWhile { it <= DENSE_HIGHEST_HZ }
        .toList()
    return bandUpperEdgesHz(centers).zip(centers) { edge, center ->
        PreviewBand(edge, equalizer.magnitudeDb(center).toFloat())
    }
}

/** Lo que el efecto dice tener aplicado, leído de él mismo y no de lo que Timbra le pidió. */
data class EffectReading(val enabled: Boolean, val hasControl: Boolean, val bands: List<PreviewBand>)

private const val READ_TOLERANCE_DB = 0.1f

/** Cuántas bandas de [wanted] tiene el efecto con la ganancia pedida. */
fun confirmedBandCount(reading: EffectReading, wanted: List<PreviewBand>): Int =
    wanted.indices.count { index ->
        val read = reading.bands.getOrNull(index)
        read != null && abs(read.gainDb - wanted[index].gainDb) <= READ_TOLERANCE_DB
    }

/** La ganancia que aplican [bands] en cada frecuencia de [frequenciesHz]. */
fun gainsAtFrequencies(bands: List<PreviewBand>, frequenciesHz: List<Int>): List<Float> =
    frequenciesHz.map { hz -> (bands.firstOrNull { hz <= it.upperEdgeHz } ?: bands.last()).gainDb }

/**
 * `DynamicsProcessing` define cada banda por dónde termina y aplica su ganancia hasta ahí, no por su
 * centro. El límite entre dos bandas va en la media geométrica de sus centros, que es el punto medio
 * al oído. La última cubre todo lo que quede arriba de ella.
 */
fun bandUpperEdgesHz(centersHz: List<Double>): List<Float> =
    centersHz.zipWithNext { lower, upper -> sqrt(lower * upper).toFloat() } +
        maxOf(NO_UPPER_LIMIT_HZ, centersHz.last().toFloat())
