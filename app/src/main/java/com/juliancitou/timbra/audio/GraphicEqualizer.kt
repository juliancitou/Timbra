package com.juliancitou.timbra.audio

import com.juliancitou.timbra.export.BandLayout
import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import java.util.Collections
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Las dos opciones de "Superposición de bandas" de Poweramp. [Envelope]: la curva pasa por el valor de
 * cada banda y las vecinas no se suman. [Cascade]: las bandas se suman, y todas en +3 dB dan ~+7 dB.
 */
enum class BandOverlap { Envelope, Cascade }

/**
 * Ancho de cada banda del ecualizador gráfico. Poweramp no publica el suyo. Las bandas están a
 * ~0.42 octavas una de otra y Q = 2 da la curva más pareja posible (1.4 dB de ondulación con
 * todas las bandas iguales).
 */
const val POWERAMP_BAND_Q = 2.0

/** No se filtran bandas por encima de esta fracción de la frecuencia de muestreo (cerca de Nyquist). */
private const val MAX_BAND_FRACTION_OF_SAMPLE_RATE = 0.45

/** Separación en octavas de las bandas con las que se eligió [POWERAMP_BAND_Q]. */
private val REFERENCE_SPACING_OCTAVES = BandLayout.Default.spacingOctaves

/**
 * Q de una banda según la separación entre bandas, en octavas. Conserva la misma superposición que
 * [POWERAMP_BAND_Q] tiene con 25 bandas: con menos bandas, cada una es más ancha y la curva sigue
 * sin huecos. Usa la relación estándar `√r / (r − 1)`, con `r` la razón de frecuencias vecinas.
 */
fun powerampBandQ(spacingOctaves: Double): Double {
    fun proportionalQ(octaves: Double): Double {
        val ratio = 2.0.pow(octaves)
        return sqrt(ratio) / (ratio - 1)
    }
    return POWERAMP_BAND_Q * proportionalQ(spacingOctaves) / proportionalQ(REFERENCE_SPACING_OCTAVES)
}

/** Ganancias en dB de cada banda de [bandFrequenciesHz], en orden, más el preamp en dB. */
data class EqualizerSettings(
    val bandGainsDb: List<Float>,
    val preampDb: Float = 0f,
    val bandFrequenciesHz: List<Int> = POWERAMP_BAND_FREQUENCIES_HZ,
    /** Solo para escuchar: debe coincidir con lo elegido en Poweramp. No se exporta. */
    val overlap: BandOverlap = BandOverlap.Envelope,
) {
    init {
        require(bandGainsDb.size == bandFrequenciesHz.size) {
            "Se esperaban ${bandFrequenciesHz.size} bandas"
        }
    }

    /** Separación promedio entre bandas, en octavas. */
    val spacingOctaves: Double
        get() = log2(bandFrequenciesHz.last().toDouble() / bandFrequenciesHz.first()) / (bandFrequenciesHz.size - 1)

    companion object {
        val Flat = EqualizerSettings(List(POWERAMP_BAND_FREQUENCIES_HZ.size) { 0f })
    }
}

/** Se usa desde el hilo de audio: los mismos ajustes ya se calcularon antes, al igualar el volumen. */
private val envelopeGainCache: MutableMap<EnvelopeRequest, List<Double>> = Collections.synchronizedMap(
    object : LinkedHashMap<EnvelopeRequest, List<Double>>(ENVELOPE_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<EnvelopeRequest, List<Double>>) =
            size > ENVELOPE_CACHE_SIZE
    },
)

private const val ENVELOPE_CACHE_SIZE = 256
private const val INTERACTION_REFERENCE_DB = 6.0
private const val ENVELOPE_REFINEMENTS = 2

/** Con una curva disparatada, las bandas podrían pedirse ganancias enormes y opuestas entre sí. */
private const val MAX_ENVELOPE_FILTER_DB = 18.0
private const val SKIPPED_FILTER_DB = 0.01

private data class EnvelopeRequest(val gainsDb: List<Float>, val frequenciesHz: List<Int>, val sampleRate: Int)

/**
 * Las ganancias que deben tener las campanas en serie para que la respuesta total valga [targetsDb] en
 * el centro de cada banda, como en el modo "Envolvente" de Poweramp. Cada campana también afecta a sus
 * vecinas: es un sistema de ecuaciones donde cada fila dice cuánto aporta cada banda a esa frecuencia.
 * El aporte en dB casi no depende de la ganancia, y un par de correcciones cubre lo que falta.
 */
private fun envelopeFilterGainsDb(targetsDb: List<Double>, frequenciesHz: List<Double>, q: Double, sampleRate: Int): List<Double> {
    fun responseDb(hz: Double, centerHz: Double, gainDb: Double) =
        BiquadCoefficients.peaking(centerHz, gainDb, q, sampleRate).magnitudeDb(hz, sampleRate)
    val interaction = frequenciesHz.map { hz ->
        DoubleArray(frequenciesHz.size) { band ->
            responseDb(hz, frequenciesHz[band], INTERACTION_REFERENCE_DB) / INTERACTION_REFERENCE_DB
        }
    }
    var gains = solveLinearSystem(interaction, targetsDb)
    repeat(ENVELOPE_REFINEMENTS) {
        val errors = frequenciesHz.indices.map { row ->
            targetsDb[row] - frequenciesHz.indices.sumOf { band -> responseDb(frequenciesHz[row], frequenciesHz[band], gains[band]) }
        }
        gains = gains.zip(solveLinearSystem(interaction, errors)) { gain, correction -> gain + correction }
    }
    return gains.map { it.coerceIn(-MAX_ENVELOPE_FILTER_DB, MAX_ENVELOPE_FILTER_DB) }
}

/** Eliminación de Gauss con pivoteo parcial. */
private fun solveLinearSystem(matrix: List<DoubleArray>, values: List<Double>): List<Double> {
    val rows = matrix.map { it.copyOf() }.toMutableList()
    val right = values.toMutableList()
    for (pivot in rows.indices) {
        val best = (pivot..rows.lastIndex).maxBy { abs(rows[it][pivot]) }
        rows[pivot] = rows[best].also { rows[best] = rows[pivot] }
        right[pivot] = right[best].also { right[best] = right[pivot] }
        for (row in pivot + 1..rows.lastIndex) {
            val factor = rows[row][pivot] / rows[pivot][pivot]
            for (column in pivot..rows.lastIndex) rows[row][column] -= factor * rows[pivot][column]
            right[row] -= factor * right[pivot]
        }
    }
    val solution = DoubleArray(rows.size)
    for (row in rows.indices.reversed()) {
        val known = (row + 1..rows.lastIndex).sumOf { rows[row][it] * solution[it] }
        solution[row] = (right[row] - known) / rows[row][row]
    }
    return solution.toList()
}

/**
 * Ecualizador gráfico que imita al de Poweramp: una campana por banda en las mismas frecuencias, en
 * serie, y el preamp como ganancia final. Las campanas casi sin ganancia no se calculan.
 */
class GraphicEqualizer(private val sampleRate: Int, val settings: EqualizerSettings) {
    private val bandQ = powerampBandQ(settings.spacingOctaves)
    private val bandCoefficients: List<BiquadCoefficients> = filterBands()
        .filter { (_, gainDb) -> abs(gainDb) >= SKIPPED_FILTER_DB }
        .map { (frequencyHz, gainDb) -> BiquadCoefficients.peaking(frequencyHz, gainDb, bandQ, sampleRate) }
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

    /** Frecuencia y ganancia de cada campana. */
    private fun filterBands(): List<Pair<Double, Double>> {
        val bands = settings.bandFrequenciesHz.zip(settings.bandGainsDb).filter { (hz, _) -> isFilterable(hz) }
        val frequencies = bands.map { (hz, _) -> hz.toDouble() }
        val targets = bands.map { (_, gainDb) -> gainDb.toDouble() }
        if (settings.overlap == BandOverlap.Cascade || targets.all { it == 0.0 }) return frequencies.zip(targets)
        val request = EnvelopeRequest(bands.map { it.second }, bands.map { it.first }, sampleRate)
        val gains = envelopeGainCache[request]
            ?: envelopeFilterGainsDb(targets, frequencies, bandQ, sampleRate).also { envelopeGainCache[request] = it }
        return frequencies.zip(gains)
    }

    private fun isFilterable(frequencyHz: Int) = frequencyHz < sampleRate * MAX_BAND_FRACTION_OF_SAMPLE_RATE
}
