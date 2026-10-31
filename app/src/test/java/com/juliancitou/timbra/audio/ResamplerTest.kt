package com.juliancitou.timbra.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class ResamplerTest {

    private fun tone(hz: Double, rate: Int, seconds: Int = 1) =
        FloatArray(rate * seconds) { (0.5 * sin(2 * PI * hz * it / rate)).toFloat() }

    /** Nivel en dB respecto a la entrada, sin contar las puntas donde el filtro aún no se llena. */
    private fun levelChangeDb(input: FloatArray, output: FloatArray): Double {
        fun rms(samples: FloatArray): Double {
            val middle = samples.copyOfRange(samples.size / 4, samples.size * 3 / 4)
            return sqrt(middle.sumOf { it.toDouble() * it } / middle.size)
        }
        return 20 * log10(rms(output) / rms(input))
    }

    @Test
    fun sameRateReturnsTheSameSamples() {
        val input = tone(440.0, 48_000)
        assertSame(input, resample(input, 48_000, 48_000))
    }

    @Test
    fun outputLengthFollowsTheRatio() {
        assertEquals(48_000, resample(tone(440.0, 44_100), 44_100, 48_000).size)
        assertEquals(44_100, resample(tone(440.0, 48_000), 48_000, 44_100).size)
    }

    @Test
    fun audibleTonesKeepTheirLevelFrom44100To48000() {
        listOf(100.0, 1_000.0, 10_000.0, 16_000.0).forEach { hz ->
            val input = tone(hz, 44_100)
            assertEquals("$hz Hz", 0.0, levelChangeDb(input, resample(input, 44_100, 48_000)), 0.1)
        }
    }

    @Test
    fun toneKeepsItsFrequencyAfterResampling() {
        val output = resample(tone(1_000.0, 44_100), 44_100, 48_000)
        val expected = tone(1_000.0, 48_000)
        val offset = 12_000
        val error = (offset until offset + 2_000).maxOf { kotlin.math.abs(output[it] - expected[it]) }
        assertTrue("error $error", error < 0.01f)
    }

    @Test
    fun downsamplingRejectsWhatNoLongerFits() {
        val input = tone(30_000.0, 96_000)
        assertTrue(levelChangeDb(input, resample(input, 96_000, 48_000)) < -50)
    }
}
