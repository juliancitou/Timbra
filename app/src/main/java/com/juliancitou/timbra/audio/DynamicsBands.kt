package com.juliancitou.timbra.audio

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Cómo se convierte un preset en sonido durante la vista previa en vivo. No se pueden leer los ajustes
 * internos de Poweramp, así que cada quien elige de oído la forma que suena igual a su Poweramp.
 * [Direct] da a `DynamicsProcessing` una banda por cada banda del preset; las demás calculan la curva
 * de campanas de [GraphicEqualizer] con un ancho multiplicado por [widthFactor] (mayor es más angosta).
 */
enum class BandShape(val widthFactor: Double?) {
    Direct(null),
    Narrow(1.4),
    Medium(1.0),
    Wide(0.7),
}

/** Una banda de `DynamicsProcessing`: llega hasta [upperEdgeHz] y aplica [gainDb]. */
data class PreviewBand(val upperEdgeHz: Float, val gainDb: Float)

private const val DENSE_LOWEST_HZ = 20.0
private const val DENSE_HIGHEST_HZ = 20_000.0

/** Un octavo de octava: bastante más fino que cualquier banda que permite Poweramp. */
private const val DENSE_BANDS_PER_OCTAVE = 8

/** Más allá de lo que cualquier teléfono reproduce. */
private const val NO_UPPER_LIMIT_HZ = 24_000f

/** Las bandas que reproducen [settings] con [shape], sin el preamp. */
fun previewBands(settings: EqualizerSettings, shape: BandShape, sampleRate: Int): List<PreviewBand> {
    val widthFactor = shape.widthFactor
        ?: return bandUpperEdgesHz(settings.bandFrequenciesHz.map { it.toDouble() })
            .zip(settings.bandGainsDb) { edge, gain -> PreviewBand(edge, gain) }
    val equalizer = GraphicEqualizer(sampleRate, settings.copy(preampDb = 0f, widthFactor = widthFactor))
    val centers = generateSequence(0) { it + 1 }
        .map { DENSE_LOWEST_HZ * 2.0.pow(it.toDouble() / DENSE_BANDS_PER_OCTAVE) }
        .takeWhile { it <= DENSE_HIGHEST_HZ }
        .toList()
    return bandUpperEdgesHz(centers).zip(centers) { edge, center ->
        PreviewBand(edge, equalizer.magnitudeDb(center).toFloat())
    }
}

/**
 * `DynamicsProcessing` define cada banda por dónde termina y aplica su ganancia hasta ahí, no por su
 * centro. El límite entre dos bandas va en la media geométrica de sus centros, que es el punto medio
 * al oído. La última cubre todo lo que quede arriba de ella.
 */
fun bandUpperEdgesHz(centersHz: List<Double>): List<Float> =
    centersHz.zipWithNext { lower, upper -> sqrt(lower * upper).toFloat() } +
        maxOf(NO_UPPER_LIMIT_HZ, centersHz.last().toFloat())
