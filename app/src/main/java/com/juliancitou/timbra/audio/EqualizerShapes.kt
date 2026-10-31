package com.juliancitou.timbra.audio

import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.round
import kotlin.math.sqrt

/** Relación entre el ancho total a media ganancia y la desviación de una campana gaussiana. */
private val HALF_GAIN_WIDTH_TO_SIGMA = 2 * sqrt(2 * ln(2.0))

/** Pendiente de los estantes: con este divisor, el 10–90 % del cambio ocupa ~1 octava por octava de transición. */
private const val SHELF_STEEPNESS = 4.4

/**
 * Curvas suaves sobre las bandas de Poweramp, en dB por banda. Trabajan en escala de octavas
 * (como el oído) y se suman entre sí. Son la base de los presets de prueba y de las "dimensiones"
 * que mueven las esferas del test.
 */
object EqualizerShapes {
    /** Campana centrada en [centerHz]: [gainDb] en el centro y la mitad a [widthOctaves]/2 de cada lado. */
    fun bell(
        centerHz: Double,
        gainDb: Double,
        widthOctaves: Double = 1.0,
        frequenciesHz: List<Int> = POWERAMP_BAND_FREQUENCIES_HZ,
    ): List<Double> {
        val sigma = widthOctaves / HALF_GAIN_WIDTH_TO_SIGMA
        val center = log2(centerHz)
        return frequenciesHz.map { frequencyHz ->
            val distance = (log2(frequencyHz.toDouble()) - center) / sigma
            gainDb * exp(-0.5 * distance * distance)
        }
    }

    /** Estante bajo: [gainDb] en todo lo que queda bajo [cornerHz] (la mitad justo en [cornerHz]). */
    fun lowShelf(
        cornerHz: Double,
        gainDb: Double,
        transitionOctaves: Double = 1.0,
        frequenciesHz: List<Int> = POWERAMP_BAND_FREQUENCIES_HZ,
    ): List<Double> = shelf(cornerHz, gainDb, transitionOctaves, rising = false, frequenciesHz)

    /** Estante alto: [gainDb] en todo lo que queda sobre [cornerHz] (la mitad justo en [cornerHz]). */
    fun highShelf(
        cornerHz: Double,
        gainDb: Double,
        transitionOctaves: Double = 1.0,
        frequenciesHz: List<Int> = POWERAMP_BAND_FREQUENCIES_HZ,
    ): List<Double> = shelf(cornerHz, gainDb, transitionOctaves, rising = true, frequenciesHz)

    fun sum(vararg curves: List<Double>): List<Double> =
        curves.first().indices.map { band -> curves.sumOf { it[band] } }

    private fun shelf(
        cornerHz: Double,
        gainDb: Double,
        transitionOctaves: Double,
        rising: Boolean,
        frequenciesHz: List<Int>,
    ): List<Double> {
        val corner = log2(cornerHz)
        val steepness = transitionOctaves / SHELF_STEEPNESS
        return frequenciesHz.map { frequencyHz ->
            val distance = (log2(frequencyHz.toDouble()) - corner) / steepness
            gainDb / (1 + exp(if (rising) -distance else distance))
        }
    }
}

const val POWERAMP_MIN_GAIN_DB = -8f
const val POWERAMP_MAX_GAIN_DB = 3f

/**
 * Ganancias listas para Poweramp: redondeadas a 0.5 dB y dentro de −8 a +3 dB. Sumar `0f` convierte
 * el cero negativo (−0.24 redondea a −0.0) en cero normal, para que el JSON no diga `-0.0`.
 */
fun List<Double>.toPowerampGains(): List<Float> =
    map { (round(it * 2) / 2).toFloat().coerceIn(POWERAMP_MIN_GAIN_DB, POWERAMP_MAX_GAIN_DB) + 0f }

/**
 * Preamp del formato de Poweramp: −(ganancia positiva máxima + 3 dB), nunca mayor que −4.5 dB. El
 * margen evita que el limitador de Poweramp actúe y "bombee" el volumen cuando suben los graves.
 */
fun powerampPreampDb(gainsDb: List<Float>): Float = minOf(-4.5f, -((gainsDb.maxOrNull() ?: 0f).coerceAtLeast(0f) + 3f))

/** Preset completo a partir de una curva: ganancias de Poweramp y su preamp. */
fun presetFromCurve(
    curveDb: List<Double>,
    frequenciesHz: List<Int> = POWERAMP_BAND_FREQUENCIES_HZ,
): EqualizerSettings {
    val gains = curveDb.toPowerampGains()
    return EqualizerSettings(gains, preampDb = powerampPreampDb(gains), bandFrequenciesHz = frequenciesHz)
}
