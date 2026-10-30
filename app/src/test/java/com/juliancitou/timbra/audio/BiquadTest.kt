package com.juliancitou.timbra.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

private const val SAMPLE_RATE = 48_000

class BiquadTest {

    @Test
    fun peakingReachesItsGainAtTheCenter() {
        val filter = BiquadCoefficients.peaking(1000.0, gainDb = 6.0, q = 1.0, SAMPLE_RATE)
        assertEquals(6.0, filter.magnitudeDb(1000.0, SAMPLE_RATE), 0.01)
    }

    @Test
    fun peakingLeavesDistantFrequenciesAlone() {
        val filter = BiquadCoefficients.peaking(1000.0, gainDb = 6.0, q = 2.0, SAMPLE_RATE)
        assertEquals(0.0, filter.magnitudeDb(50.0, SAMPLE_RATE), 0.1)
        assertEquals(0.0, filter.magnitudeDb(18_000.0, SAMPLE_RATE), 0.1)
    }

    @Test
    fun peakingCutIsSymmetricToBoost() {
        val boost = BiquadCoefficients.peaking(3000.0, gainDb = 4.0, q = 1.4, SAMPLE_RATE)
        val cut = BiquadCoefficients.peaking(3000.0, gainDb = -4.0, q = 1.4, SAMPLE_RATE)
        listOf(1000.0, 2500.0, 3000.0, 4000.0).forEach { hz ->
            assertEquals(-boost.magnitudeDb(hz, SAMPLE_RATE), cut.magnitudeDb(hz, SAMPLE_RATE), 0.01)
        }
    }

    @Test
    fun shelvesChangeOnlyTheirSide() {
        val low = BiquadCoefficients.lowShelf(150.0, gainDb = 5.0, q = 0.707, SAMPLE_RATE)
        assertEquals(5.0, low.magnitudeDb(20.0, SAMPLE_RATE), 0.1)
        assertEquals(0.0, low.magnitudeDb(5000.0, SAMPLE_RATE), 0.05)
        val high = BiquadCoefficients.highShelf(8000.0, gainDb = -5.0, q = 0.707, SAMPLE_RATE)
        assertEquals(-5.0, high.magnitudeDb(22_000.0, SAMPLE_RATE), 0.2)
        assertEquals(0.0, high.magnitudeDb(200.0, SAMPLE_RATE), 0.05)
    }

    @Test
    fun bandPassPeaksAtZeroDecibels() {
        val filter = BiquadCoefficients.bandPass(2000.0, q = 4.3, SAMPLE_RATE)
        assertEquals(0.0, filter.magnitudeDb(2000.0, SAMPLE_RATE), 0.01)
        assertTrue(filter.magnitudeDb(500.0, SAMPLE_RATE) < -20)
    }

    @Test
    fun processedSineMatchesTheComputedResponse() {
        val coefficients = BiquadCoefficients.peaking(1000.0, gainDb = 6.0, q = 1.0, SAMPLE_RATE)
        val filter = Biquad(coefficients)
        val settle = SAMPLE_RATE / 10
        val measure = SAMPLE_RATE / 2
        var inputEnergy = 0.0
        var outputEnergy = 0.0
        for (n in 0 until settle + measure) {
            val input = sin(2 * PI * 1000.0 * n / SAMPLE_RATE)
            val output = filter.process(input)
            if (n >= settle) {
                inputEnergy += input * input
                outputEnergy += output * output
            }
        }
        val measuredDb = 20 * log10(sqrt(outputEnergy / inputEnergy))
        assertEquals(6.0, measuredDb, 0.05)
    }

    @Test
    fun identityPassesSignalUnchanged() {
        val filter = Biquad()
        listOf(0.5, -0.25, 1.0).forEach { assertEquals(it, filter.process(it), 0.0) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun frequencyAboveNyquistIsRejected() {
        BiquadCoefficients.peaking(30_000.0, gainDb = 3.0, q = 1.0, SAMPLE_RATE)
    }
}
