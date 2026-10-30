package com.juliancitou.timbra.audio

import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SAMPLE_RATE = 48_000

class GraphicEqualizerTest {

    private fun settingsWith(vararg bands: Pair<Int, Float>, preampDb: Float = 0f): EqualizerSettings {
        val gains = MutableList(POWERAMP_BAND_FREQUENCIES_HZ.size) { 0f }
        bands.forEach { (index, gain) -> gains[index] = gain }
        return EqualizerSettings(gains, preampDb)
    }

    @Test
    fun flatEqualizerIsTransparent() {
        val equalizer = GraphicEqualizer(SAMPLE_RATE, EqualizerSettings.Flat)
        POWERAMP_BAND_FREQUENCIES_HZ.forEach { hz ->
            assertEquals(0.0, equalizer.magnitudeDb(hz.toDouble()), 1e-9)
        }
        assertEquals(0.37, equalizer.process(0.37), 0.0)
    }

    @Test
    fun singleBandReachesItsGainAtItsFrequency() {
        val index = POWERAMP_BAND_FREQUENCIES_HZ.indexOf(8434)
        val equalizer = GraphicEqualizer(SAMPLE_RATE, settingsWith(index to -4.5f))
        assertEquals(-4.5, equalizer.magnitudeDb(8434.0), 0.01)
        assertEquals(0.0, equalizer.magnitudeDb(500.0), 0.05)
    }

    @Test
    fun preampShiftsTheWholeCurve() {
        val equalizer = GraphicEqualizer(SAMPLE_RATE, settingsWith(preampDb = -4.5f))
        listOf(20.0, 1000.0, 15_000.0).forEach { assertEquals(-4.5, equalizer.magnitudeDb(it), 1e-9) }
        assertEquals(0.5 * 0.5957, equalizer.process(0.5), 1e-3)
    }

    @Test
    fun neighbouringBandsBlendWithoutDeepRipple() {
        val allUp = EqualizerSettings(List(POWERAMP_BAND_FREQUENCIES_HZ.size) { 3f })
        val equalizer = GraphicEqualizer(SAMPLE_RATE, allUp)
        val centers = POWERAMP_BAND_FREQUENCIES_HZ.subList(3, 22).map { equalizer.magnitudeDb(it.toDouble()) }
        val betweenBands = POWERAMP_BAND_FREQUENCIES_HZ.subList(3, 22).zipWithNext { a, b ->
            equalizer.magnitudeDb(kotlin.math.sqrt(a.toDouble() * b))
        }
        // Con campanas en serie a ~0.42 octavas, 1.4 dB es el mínimo posible (se da con Q 2–2.5).
        val ripple = centers.max() - betweenBands.min()
        assertTrue("Ondulación de $ripple dB entre bandas", ripple < 1.5)
    }

    @Test
    fun bandsNearNyquistAreSkippedAtLowSampleRates() {
        val lastIndex = POWERAMP_BAND_FREQUENCIES_HZ.lastIndex
        val equalizer = GraphicEqualizer(32_000, settingsWith(lastIndex to 3f))
        assertEquals(0.0, equalizer.magnitudeDb(1000.0), 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun wrongNumberOfBandsIsRejected() {
        EqualizerSettings(listOf(1f, 2f, 3f))
    }
}
