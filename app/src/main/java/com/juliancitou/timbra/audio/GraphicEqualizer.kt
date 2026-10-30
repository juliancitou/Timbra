package com.juliancitou.timbra.audio

import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import kotlin.math.pow

/**
 * Ancho de cada banda del ecualizador gráfico. Poweramp no publica el suyo. Las bandas están a
 * ~0.42 octavas una de otra y Q = 2 da la curva más pareja posible (1.4 dB de ondulación con
 * todas las bandas iguales). Las bandas vecinas se suman: todas en +3 dB dan ~+7 dB reales, así
 * que las gráficas deben mostrar [GraphicEqualizer.magnitudeDb] y no solo los valores de los
 * controles. Se calibra en la Fase 4 comparando la vista previa con Poweramp.
 */
const val POWERAMP_BAND_Q = 2.0

/** No se filtran bandas por encima de esta fracción de la frecuencia de muestreo (cerca de Nyquist). */
private const val MAX_BAND_FRACTION_OF_SAMPLE_RATE = 0.45

/** Ganancias en dB de las 25 bandas de Poweramp, en orden, más el preamp en dB. */
data class EqualizerSettings(val bandGainsDb: List<Float>, val preampDb: Float = 0f) {
    init {
        require(bandGainsDb.size == POWERAMP_BAND_FREQUENCIES_HZ.size) {
            "Se esperaban ${POWERAMP_BAND_FREQUENCIES_HZ.size} bandas"
        }
    }

    companion object {
        val Flat = EqualizerSettings(List(POWERAMP_BAND_FREQUENCIES_HZ.size) { 0f })
    }
}

/**
 * Ecualizador gráfico que imita al de Poweramp: una campana por banda en las mismas 25
 * frecuencias, en serie, y el preamp como ganancia final. Las bandas en 0 dB no se calculan.
 */
class GraphicEqualizer(private val sampleRate: Int, val settings: EqualizerSettings) {
    private val bandCoefficients: List<BiquadCoefficients> =
        POWERAMP_BAND_FREQUENCIES_HZ.zip(settings.bandGainsDb)
            .filter { (frequencyHz, gainDb) -> gainDb != 0f && isFilterable(frequencyHz) }
            .map { (frequencyHz, gainDb) ->
                BiquadCoefficients.peaking(frequencyHz.toDouble(), gainDb.toDouble(), POWERAMP_BAND_Q, sampleRate)
            }
    private val filters = bandCoefficients.map { Biquad(it) }
    private val preampGain = 10.0.pow(settings.preampDb / 20.0)

    fun process(sample: Double): Double {
        var output = sample
        for (filter in filters) output = filter.process(output)
        return output * preampGain
    }

    /** Respuesta total en dB a [frequencyHz]: suma de todas las bandas más el preamp. */
    fun magnitudeDb(frequencyHz: Double): Double =
        bandCoefficients.sumOf { it.magnitudeDb(frequencyHz, sampleRate) } + settings.preampDb

    fun reset() = filters.forEach { it.reset() }

    private fun isFilterable(frequencyHz: Int) = frequencyHz < sampleRate * MAX_BAND_FRACTION_OF_SAMPLE_RATE
}
