package com.juliancitou.timbra.ui.theme

import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrequencyScaleTest {

    @Test
    fun chartEdgesMapToZeroAndOne() {
        assertEquals(0f, frequencyToFraction(20f), 1e-6f)
        assertEquals(1f, frequencyToFraction(20_000f), 1e-6f)
    }

    @Test
    fun geometricMidpointIsCentered() {
        assertEquals(0.5f, frequencyToFraction(632.456f), 1e-4f)
    }

    @Test
    fun frequenciesOutsideTheChartAreClamped() {
        assertEquals(0f, frequencyToFraction(5f), 1e-6f)
        assertEquals(1f, frequencyToFraction(40_000f), 1e-6f)
    }

    @Test
    fun powerampBandsAreEvenlySpacedOnTheChart() {
        val positions = POWERAMP_BAND_FREQUENCIES_HZ.map { frequencyToFraction(it.toFloat()) }
        val gaps = positions.zipWithNext { a, b -> b - a }
        val expectedGap = 1f / (POWERAMP_BAND_FREQUENCIES_HZ.size - 1)
        gaps.forEach { assertEquals(expectedGap, it, 0.004f) }
    }

    @Test
    fun gainMapsTopToBottom() {
        assertEquals(0f, gainToFraction(12f, -12f, 12f), 1e-6f)
        assertEquals(0.5f, gainToFraction(0f, -12f, 12f), 1e-6f)
        assertEquals(1f, gainToFraction(-12f, -12f, 12f), 1e-6f)
    }

    @Test
    fun gainOutsideRangeIsClamped() {
        assertTrue(gainToFraction(20f, -12f, 12f) == 0f)
        assertTrue(gainToFraction(-20f, -12f, 12f) == 1f)
    }
}
