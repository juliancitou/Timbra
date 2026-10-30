package com.juliancitou.timbra.audio

import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerShapesTest {

    private fun gainAt(curve: List<Double>, hz: Int) = curve[POWERAMP_BAND_FREQUENCIES_HZ.indexOf(hz)]

    @Test
    fun bellPeaksAtItsCenterAndHalvesAtItsWidth() {
        val bell = EqualizerShapes.bell(centerHz = 2000.0, gainDb = 4.0, widthOctaves = 2.0)
        assertEquals(4.0, gainAt(bell, 2000), 1e-9)
        val oneOctaveAway = EqualizerShapes.bell(centerHz = 1000.0, gainDb = 4.0, widthOctaves = 2.0)
        assertEquals(2.0, gainAt(oneOctaveAway, 2000), 0.15)
        assertEquals(0.0, gainAt(bell, 20), 0.01)
    }

    @Test
    fun shelvesReachTheirGainOnlyOnTheirSide() {
        val low = EqualizerShapes.lowShelf(cornerHz = 150.0, gainDb = 3.0)
        assertEquals(3.0, gainAt(low, 20), 0.05)
        assertEquals(0.0, gainAt(low, 2000), 0.05)
        val high = EqualizerShapes.highShelf(cornerHz = 6000.0, gainDb = -4.0)
        assertEquals(-4.0, gainAt(high, 20000), 0.1)
        assertEquals(0.0, gainAt(high, 474), 0.05)
    }

    @Test
    fun curvesAddUp() {
        val total = EqualizerShapes.sum(EqualizerShapes.lowShelf(150.0, 3.0), EqualizerShapes.highShelf(6000.0, 2.0))
        assertEquals(3.0, gainAt(total, 20), 0.05)
        assertEquals(2.0, gainAt(total, 20000), 0.1)
    }

    @Test
    fun powerampGainsAreRoundedAndClamped() {
        val gains = listOf(1.26, -0.24, 5.0, -12.0, 2.74).toPowerampGains()
        assertEquals(listOf(1.5f, 0f, 3f, -8f, 2.5f), gains)
    }

    @Test
    fun preampLeavesThreeDecibelsOverTheHighestBoost() {
        assertEquals(-5.5f, powerampPreampDb(listOf(-2f, 2.5f, 0f)))
        assertEquals(-6f, powerampPreampDb(listOf(3f, 1f)))
    }

    @Test
    fun preampIsNeverAboveMinusFourAndAHalf() {
        assertEquals(-4.5f, powerampPreampDb(listOf(-3f, -1f, 0f)))
        assertEquals(-4.5f, powerampPreampDb(listOf(1.5f)))
    }

    @Test
    fun presetFromCurveFollowsTheContract() {
        val preset = presetFromCurve(EqualizerShapes.bell(2000.0, 6.0))
        assertTrue(preset.bandGainsDb.all { it in POWERAMP_MIN_GAIN_DB..POWERAMP_MAX_GAIN_DB })
        assertEquals(-6f, preset.preampDb)
    }
}
