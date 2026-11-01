package com.juliancitou.timbra.audio

import android.annotation.SuppressLint
import android.media.audiofx.Visualizer
import kotlinx.coroutines.delay
import kotlin.math.log10
import kotlin.math.sqrt

private const val CAPTURE_INTERVAL_MS = 50L
private const val SILENCE_POWER = 1e-3
private const val MILLIHERTZ_PER_HERTZ = 1000.0
private const val LOWEST_USABLE_BIN = 2

/**
 * Espectro de lo que sale por el teléfono, con el medidor global de Android (`Visualizer` en la sesión 0).
 * Es lo único que ve el audio después del ecualizador de otra app. Es tosco: 8 bits y ~47 Hz por punto, así
 * que solo sirve promediando mucho y arriba de unos 100 Hz. Android lo marca como obsoleto y puede no
 * funcionar en todos los teléfonos. Requiere `RECORD_AUDIO`.
 */
class OutputMeter {
    /** Espectro de potencia promedio durante [durationMs], y sus Hz por punto. `null` si no captó sonido. */
    @SuppressLint("MissingPermission")
    suspend fun capture(durationMs: Long): MeasuredSpectrum? {
        val visualizer = runCatching { Visualizer(0) }.getOrNull() ?: return null
        try {
            visualizer.enabled = false
            visualizer.captureSize = Visualizer.getCaptureSizeRange().last()
            visualizer.scalingMode = Visualizer.SCALING_MODE_AS_PLAYED
            visualizer.enabled = true
            val bytes = ByteArray(visualizer.captureSize)
            val power = DoubleArray(bytes.size / 2)
            var frames = 0
            val frameCount = (durationMs / CAPTURE_INTERVAL_MS).toInt()
            repeat(frameCount) {
                delay(CAPTURE_INTERVAL_MS)
                if (visualizer.getFft(bytes) == Visualizer.SUCCESS) {
                    addFftPower(bytes, power)
                    frames++
                }
            }
            if (frames == 0 || power.sum() / frames < SILENCE_POWER) return null
            val binHz = visualizer.samplingRate / MILLIHERTZ_PER_HERTZ / bytes.size
            return MeasuredSpectrum(power.map { it / frames }, binHz)
        } catch (_: RuntimeException) {
            return null
        } finally {
            visualizer.release()
        }
    }
}

data class MeasuredSpectrum(val power: List<Double>, val binHz: Double)

/** Suma a [power] la potencia de una captura. Formato de `getFft`: reales e imaginarios alternados desde el byte 2. */
fun addFftPower(fft: ByteArray, power: DoubleArray) {
    for (bin in 1 until power.size) {
        val real = fft[2 * bin].toDouble()
        val imaginary = fft[2 * bin + 1].toDouble()
        power[bin] += real * real + imaginary * imaginary
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

/** Cuánto cambió [after] respecto a [before] alrededor de cada banda, en dB. `null` debajo de lo que se puede medir. */
fun spectrumDifferenceDb(before: MeasuredSpectrum, after: MeasuredSpectrum, centersHz: List<Int>): List<Float?> =
    centersHz.indices.map { band ->
        val center = centersHz[band].toDouble()
        if (center < LOWEST_USABLE_BIN * before.binHz || center / before.binHz >= before.power.size) {
            return@map null
        }
        val lowerHz = centersHz.getOrNull(band - 1)?.let { sqrt(it * center) } ?: center
        val upperHz = centersHz.getOrNull(band + 1)?.let { sqrt(it * center) } ?: center
        val first = (lowerHz / before.binHz).toInt().coerceAtLeast(1)
        val last = (upperHz / before.binHz).toInt().coerceIn(first, before.power.lastIndex)
        val beforePower = (first..last).sumOf { before.power[it] }
        val afterPower = (first..last).sumOf { after.power[it] }
        if (beforePower <= 0.0 || afterPower <= 0.0) null else (10 * log10(afterPower / beforePower)).toFloat()
    }
