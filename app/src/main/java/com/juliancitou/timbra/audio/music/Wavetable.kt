package com.juliancitou.timbra.audio.music

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/** Frecuencia en Hz de una nota MIDI (69 = La 440 Hz). */
fun midiToHz(note: Int): Double = 440.0 * 2.0.pow((note - 69) / 12.0)

/**
 * Un ciclo de onda precalculado como suma de armónicos. Solo lleva los armónicos que caben bajo
 * la frecuencia de corte, así que no genera aliasing (los "silbidos" metálicos de las ondas de
 * diente de sierra ingenuas). Leer la tabla es mucho más barato que sumar senos en cada muestra.
 */
class Wavetable private constructor(private val table: DoubleArray) {

    /** Muestra en la fase [phase] (0 a 1), con interpolación lineal entre puntos de la tabla. */
    fun sample(phase: Double): Double {
        val position = phase * SIZE
        val index = position.toInt() % SIZE
        val fraction = position - position.toInt()
        val next = table[(index + 1) % SIZE]
        return table[index] + (next - table[index]) * fraction
    }

    companion object {
        private const val SIZE = 2048

        /**
         * Tabla para una nota de [fundamentalHz] con armónicos hasta [cutoffHz]. [amplitude] da el
         * peso del armónico n (1 = fundamental): `1/n` suena brillante como un diente de sierra,
         * `1/n²` suave como una flauta o una voz.
         */
        fun forNote(fundamentalHz: Double, cutoffHz: Double, amplitude: (Int) -> Double): Wavetable {
            val harmonics = (cutoffHz / fundamentalHz).toInt().coerceIn(1, SIZE / 2 - 1)
            val table = DoubleArray(SIZE) { index ->
                (1..harmonics).sumOf { n -> amplitude(n) * sin(2 * PI * n * index / SIZE) }
            }
            val peak = table.maxOf { abs(it) }
            return Wavetable(DoubleArray(SIZE) { table[it] / peak })
        }
    }
}
