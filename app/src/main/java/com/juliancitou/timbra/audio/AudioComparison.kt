package com.juliancitou.timbra.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A 48 kHz da ~3 Hz por punto del espectro: alcanza para separar las bandas graves. */
private const val FFT_SIZE = 16_384
private const val RENDER_BLOCK_FRAMES = 1_024

/** Lo que tardan en terminar la entrada suave y el fundido: esa parte no se compara. */
private const val SETTLE_SECONDS = 0.25
private const val MEASURE_SECONDS = 8.0

/** Con menos de dos puntos del espectro por debajo, la energía de la banda se mezcla con la vecina. */
private const val LOWEST_MEASURABLE_BINS = 2

/** Una banda tan por debajo de la más fuerte casi no tiene música: compararla solo mediría ruido. */
private const val SILENT_BAND_BELOW_STRONGEST_DB = 70.0
private const val MAX_MEASURABLE_FRACTION_OF_SAMPLE_RATE = 0.45
private const val NOTICEABLE_BAND_CHANGE_DB = 0.5f

/** Por debajo de esto, en la banda que más cambia, es normal que cueste oír la diferencia. */
private const val SUBTLE_CHANGE_DB = 1.5f

/** Cuánto cambia un preset respecto a dejar todo en 0. */
data class EqualizerDifference(val changedBands: Int, val strongestHz: Int, val strongestDb: Float) {
    val isSubtle: Boolean get() = abs(strongestDb) < SUBTLE_CHANGE_DB
}

fun equalizerDifference(settings: EqualizerSettings): EqualizerDifference {
    val strongest = settings.bandGainsDb.indices.maxBy { abs(settings.bandGainsDb[it]) }
    return EqualizerDifference(
        changedBands = settings.bandGainsDb.count { abs(it) >= NOTICEABLE_BAND_CHANGE_DB },
        strongestHz = settings.bandFrequenciesHz[strongest],
        strongestDb = settings.bandGainsDb[strongest],
    )
}

/**
 * Pasa [samples] por la cadena real de reproducción con y sin ecualizar y devuelve cuánto cambió cada
 * banda, en dB. Se descuenta la diferencia de preamp, que solo iguala el volumen: lo que queda es el
 * cambio de tono. `null` en las bandas donde no se puede medir porque ahí no hay música.
 */
fun measureEqualizerDifference(
    samples: DoubleArray,
    sampleRate: Int,
    flat: EqualizerSettings,
    equalized: EqualizerSettings,
): List<Float?> {
    val before = renderThroughChain(samples, sampleRate, flat)
    val after = renderThroughChain(samples, sampleRate, equalized)
    val preampChangeDb = equalized.preampDb - flat.preampDb
    return bandDifferenceDb(before, after, sampleRate, equalized.bandFrequenciesHz)
        .map { difference -> difference?.let { it - preampChangeDb } }
}

private fun renderThroughChain(samples: DoubleArray, sampleRate: Int, settings: EqualizerSettings): DoubleArray {
    val renderer = AudioRenderer(sampleRate, LoopSource(samples))
    renderer.changeEqualization(settings)
    renderer.play()
    val settleFrames = (SETTLE_SECONDS * sampleRate).toInt()
    val output = DoubleArray((MEASURE_SECONDS * sampleRate).toInt())
    val block = FloatArray(2 * RENDER_BLOCK_FRAMES)
    var frame = -settleFrames
    while (frame < output.size) {
        renderer.render(block, RENDER_BLOCK_FRAMES)
        for (index in 0 until RENDER_BLOCK_FRAMES) {
            val position = frame + index
            if (position in output.indices) output[position] = block[2 * index].toDouble()
        }
        frame += RENDER_BLOCK_FRAMES
    }
    return output
}

/** Diferencia de nivel de [after] respecto a [before] en la zona de cada banda de [centersHz], en dB. */
fun bandDifferenceDb(
    before: DoubleArray,
    after: DoubleArray,
    sampleRate: Int,
    centersHz: List<Int>,
): List<Float?> {
    val beforeSpectrum = averagePowerSpectrum(before)
    val afterSpectrum = averagePowerSpectrum(after)
    val binHz = sampleRate.toDouble() / FFT_SIZE
    val ranges = bandBinRanges(centersHz, binHz)
    val beforePower = ranges.map { range -> range.sumOf { beforeSpectrum[it] } }
    val silenceFloor = (beforePower.maxOrNull() ?: 0.0) / power10(SILENT_BAND_BELOW_STRONGEST_DB)
    return centersHz.indices.map { band ->
        val measurable = centersHz[band] >= LOWEST_MEASURABLE_BINS * binHz &&
            centersHz[band] < sampleRate * MAX_MEASURABLE_FRACTION_OF_SAMPLE_RATE &&
            beforePower[band] > silenceFloor
        if (measurable) {
            val afterPower = ranges[band].sumOf { afterSpectrum[it] }
            (10 * log10(afterPower / beforePower[band])).toFloat()
        } else {
            null
        }
    }
}

/** Sube o baja toda la curva para comparar solo su forma: el nivel general lo cambian el preamp y el DVC. */
fun List<Float>.alignedToItsMedian(): List<Float> {
    val median = sorted()[size / 2]
    return map { it - median }
}

/** Cuánto más bajas están las bandas que el preset corta, en promedio, que las que deja igual. */
fun cutDepthDb(curveDb: List<Float>, isCut: List<Boolean>): Float {
    val cut = curveDb.filterIndexed { band, _ -> isCut[band] }
    val kept = curveDb.filterIndexed { band, _ -> !isCut[band] }
    if (cut.isEmpty() || kept.isEmpty()) return 0f
    return kept.average().toFloat() - cut.average().toFloat()
}

private fun power10(decibels: Double) = Math.pow(10.0, decibels / 10)

/** Cada banda llega hasta la media geométrica con la siguiente; las muy angostas usan su punto más cercano. */
private fun bandBinRanges(centersHz: List<Int>, binHz: Double): List<IntRange> = centersHz.indices.map { band ->
    val center = centersHz[band].toDouble()
    val lowerHz = centersHz.getOrNull(band - 1)?.let { sqrt(it * center) } ?: center
    val upperHz = centersHz.getOrNull(band + 1)?.let { sqrt(it * center) } ?: center
    val lastBin = FFT_SIZE / 2
    val first = (lowerHz / binHz).roundToInt().coerceIn(1, lastBin)
    val last = (upperHz / binHz).roundToInt().coerceIn(first, lastBin)
    first..last
}

/** Espectro de potencia promediado de Welch: ventanas de Hann traslapadas a la mitad. */
private fun averagePowerSpectrum(samples: DoubleArray): DoubleArray {
    val window = DoubleArray(FFT_SIZE) { 0.5 - 0.5 * cos(2 * PI * it / FFT_SIZE) }
    val power = DoubleArray(FFT_SIZE / 2 + 1)
    val real = DoubleArray(FFT_SIZE)
    val imaginary = DoubleArray(FFT_SIZE)
    var start = 0
    while (start + FFT_SIZE <= samples.size) {
        for (index in 0 until FFT_SIZE) real[index] = samples[start + index] * window[index]
        imaginary.fill(0.0)
        fourierTransform(real, imaginary)
        for (bin in power.indices) power[bin] += real[bin] * real[bin] + imaginary[bin] * imaginary[bin]
        start += FFT_SIZE / 2
    }
    return power
}

/** FFT radix-2 en el mismo arreglo. El tamaño debe ser potencia de 2. */
private fun fourierTransform(real: DoubleArray, imaginary: DoubleArray) {
    val size = real.size
    var reversed = 0
    for (index in 1 until size) {
        var bit = size shr 1
        while (reversed and bit != 0) {
            reversed = reversed xor bit
            bit = bit shr 1
        }
        reversed = reversed or bit
        if (index < reversed) {
            real[index] = real[reversed].also { real[reversed] = real[index] }
            imaginary[index] = imaginary[reversed].also { imaginary[reversed] = imaginary[index] }
        }
    }
    var length = 2
    while (length <= size) {
        val angle = -2 * PI / length
        for (offset in 0 until length / 2) {
            val twiddleReal = cos(angle * offset)
            val twiddleImaginary = sin(angle * offset)
            var lower = offset
            while (lower < size) {
                val upper = lower + length / 2
                val productReal = real[upper] * twiddleReal - imaginary[upper] * twiddleImaginary
                val productImaginary = real[upper] * twiddleImaginary + imaginary[upper] * twiddleReal
                real[upper] = real[lower] - productReal
                imaginary[upper] = imaginary[lower] - productImaginary
                real[lower] += productReal
                imaginary[lower] += productImaginary
                lower += length
            }
        }
        length = length shl 1
    }
}
