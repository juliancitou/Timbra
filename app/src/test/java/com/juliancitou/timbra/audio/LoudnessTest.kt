package com.juliancitou.timbra.audio

import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.log10

private const val SAMPLE_RATE = 48_000

class LoudnessTest {

    private fun pinkNoise(seconds: Double): DoubleArray {
        val noise = PinkNoise(seed = 3)
        return DoubleArray((seconds * SAMPLE_RATE).toInt()) { noise.next() }
    }

    /** Coeficientes oficiales de BS.1770-4 a 48 kHz, para comparar la derivación propia. */
    private val officialShelf = BiquadCoefficients(
        1.53512485958697, -2.69169618940638, 1.19839281085285, -1.69065929318241, 0.73248077421585,
    )
    private val officialHighPass = BiquadCoefficients(1.0, -2.0, 1.0, -1.99004745483398, 0.99007225036621)

    @Test
    fun kWeightingMatchesTheStandardAt48k() {
        val (shelf, highPass) = kWeightingFilters(SAMPLE_RATE)
        listOf(30.0, 100.0, 1000.0, 3000.0, 10_000.0).forEach { hz ->
            val ours = shelf.magnitudeDb(hz, SAMPLE_RATE) + highPass.magnitudeDb(hz, SAMPLE_RATE)
            val official = officialShelf.magnitudeDb(hz, SAMPLE_RATE) + officialHighPass.magnitudeDb(hz, SAMPLE_RATE)
            assertEquals("A $hz Hz", official, ours, 0.001)
        }
    }

    @Test
    fun fullScaleSineAtOneKilohertzMeasuresAboutMinusThreeLufs() {
        val tone = SineTone(997.0, amplitude = 1.0, sampleRate = SAMPLE_RATE)
        val samples = DoubleArray(SAMPLE_RATE * 2) { tone.next() }
        assertEquals(-3.01, loudnessLufs(samples, SAMPLE_RATE), 0.1)
    }

    @Test
    fun pinkNoiseHasEqualEnergyPerOctave() {
        val noise = pinkNoise(seconds = 10.0)
        val octaveCentersHz = listOf(125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0)
        val bandLevelsDb = octaveCentersHz.map { center ->
            val filter = Biquad(BiquadCoefficients.bandPass(center, q = 1.414, sampleRate = SAMPLE_RATE))
            10 * log10(noise.sumOf { val y = filter.process(it); y * y })
        }
        assertTrue("Niveles por octava: $bandLevelsDb", bandLevelsDb.max() - bandLevelsDb.min() < 1.5)
    }

    @Test
    fun broadBoostIsCompensatedByTheSameAmount() {
        val matcher = LoudnessMatcher(SAMPLE_RATE, pinkNoise(seconds = 3.0))
        assertEquals(-4.0, matcher.compensationDb(EqualizerSettings.Flat.copy(preampDb = 4f)), 0.01)
    }

    @Test
    fun compensatedSettingsSoundAsLoudAsFlat() {
        val reference = pinkNoise(seconds = 3.0)
        val matcher = LoudnessMatcher(SAMPLE_RATE, reference)
        val vocalBoost = EqualizerSettings(
            POWERAMP_BAND_FREQUENCIES_HZ.map { if (it in 1000..4000) 3f else 0f },
        )
        val compensated = GraphicEqualizer(SAMPLE_RATE, matcher.compensated(vocalBoost))
        val processed = DoubleArray(reference.size) { compensated.process(reference[it]) }
        assertEquals(loudnessLufs(reference, SAMPLE_RATE), loudnessLufs(processed, SAMPLE_RATE), 0.05)
    }

    @Test
    fun earWeightsDeepBassLessThanPresence() {
        val matcher = LoudnessMatcher(SAMPLE_RATE, pinkNoise(seconds = 3.0))
        val boostBand = { range: IntRange ->
            EqualizerSettings(POWERAMP_BAND_FREQUENCIES_HZ.map { if (it in range) 4f else 0f })
        }
        val subBassCompensation = matcher.compensationDb(boostBand(20..47))
        val presenceCompensation = matcher.compensationDb(boostBand(2000..4743))
        assertTrue(subBassCompensation > presenceCompensation)
    }
}
