package com.juliancitou.timbra.audio

import com.juliancitou.timbra.export.BandLayout
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
    fun envelopePassesThroughEveryBandWithoutAddingNeighbours() {
        val gains = POWERAMP_BAND_FREQUENCIES_HZ.map { if (it in 200..2000) -3f else 0f }
        val equalizer = GraphicEqualizer(SAMPLE_RATE, EqualizerSettings(gains))
        POWERAMP_BAND_FREQUENCIES_HZ.subList(1, 23).zip(gains.subList(1, 23)).forEach { (hz, gain) ->
            assertEquals("$hz Hz", gain.toDouble(), equalizer.magnitudeDb(hz.toDouble()), 0.05)
        }
    }

    @Test
    fun previewStrengthScalesTheCurveButNotThePreset() {
        val gains = POWERAMP_BAND_FREQUENCIES_HZ.map { if (it in 200..2000) -6f else 0f }
        val settings = EqualizerSettings(gains, previewStrength = 0.5)
        assertEquals(-3.0, GraphicEqualizer(SAMPLE_RATE, settings).magnitudeDb(632.0), 0.1)
        assertEquals(gains, settings.bandGainsDb)
    }

    @Test
    fun cascadeAddsNeighbouringBands() {
        val settings = EqualizerSettings(List(POWERAMP_BAND_FREQUENCIES_HZ.size) { 3f }, overlap = BandOverlap.Cascade)
        assertTrue(GraphicEqualizer(SAMPLE_RATE, settings).magnitudeDb(1125.0) > 6.0)
    }

    @Test
    fun envelopeWorksWithEveryBandLayoutAndARealisticCurve() {
        listOf(BandLayout.Default, BandLayout(32, 5, 20_000), BandLayout(5)).forEach { layout ->
            val gains = layout.frequenciesHz.map { if (it in 100..300) -4f else if (it > 5000) 1.5f else 0f }
            val settings = EqualizerSettings(gains, bandFrequenciesHz = layout.frequenciesHz)
            val equalizer = GraphicEqualizer(SAMPLE_RATE, settings)
            layout.frequenciesHz.zip(gains).filter { (hz, _) -> hz < 18_000 }.forEach { (hz, gain) ->
                assertEquals("$layout $hz Hz", gain.toDouble(), equalizer.magnitudeDb(hz.toDouble()), 0.2)
            }
        }
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

    @Test
    fun defaultBandsKeepTheCalibratedWidth() {
        assertEquals(POWERAMP_BAND_Q, powerampBandQ(BandLayout.Default.spacingOctaves), 1e-9)
    }

    @Test
    fun fewerBandsAreWiderSoTheCurveHasNoGaps() {
        assertTrue(powerampBandQ(BandLayout(count = 10).spacingOctaves) < POWERAMP_BAND_Q)
        assertTrue(powerampBandQ(BandLayout(count = 32).spacingOctaves) > POWERAMP_BAND_Q)
    }

    @Test
    fun anyLayoutReachesItsGainAtItsBand() {
        val layout = BandLayout(count = 10, firstHz = 31, lastHz = 16_000)
        val gains = List(10) { if (it == 5) -4f else 0f }
        val settings = EqualizerSettings(gains, bandFrequenciesHz = layout.frequenciesHz)
        val equalizer = GraphicEqualizer(SAMPLE_RATE, settings)
        assertEquals(-4.0, equalizer.magnitudeDb(layout.frequenciesHz[5].toDouble()), 0.01)
    }

    @Test(expected = IllegalArgumentException::class)
    fun gainsMustMatchTheBands() {
        EqualizerSettings(List(25) { 0f }, bandFrequenciesHz = BandLayout(count = 10).frequenciesHz)
    }
}
