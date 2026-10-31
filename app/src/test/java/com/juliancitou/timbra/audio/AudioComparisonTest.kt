package com.juliancitou.timbra.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class AudioComparisonTest {
    private val sampleRate = 48_000
    private val flat = EqualizerSettings.Flat

    private fun noise(seconds: Int = 9): DoubleArray {
        val random = Random(7)
        return DoubleArray(seconds * sampleRate) { (random.nextDouble() - 0.5) * 0.1 }
    }

    @Test
    fun sameSettingsMeasureNoChange() {
        val difference = measureEqualizerDifference(noise(), sampleRate, flat, flat)
        assertTrue(difference.filterNotNull().all { abs(it) < 0.01f })
    }

    @Test
    fun measuredChangeFollowsTheEqualizerAndIgnoresThePreamp() {
        val gains = flat.bandGainsDb.toMutableList()
        val band = flat.bandFrequenciesHz.indexOf(1125)
        gains[band] = -6f
        val equalized = flat.copy(bandGainsDb = gains, preampDb = -4.5f)
        val difference = measureEqualizerDifference(noise(), sampleRate, flat, equalized)
        assertEquals(-6f, requireNotNull(difference[band]), 1.5f)
        assertEquals(0f, requireNotNull(difference[flat.bandFrequenciesHz.indexOf(112)]), 0.5f)
    }

    @Test
    fun bandsWithoutMusicAreNotMeasured() {
        val tone = DoubleArray(4 * sampleRate) { 0.1 * sin(2 * PI * 1000 * it / sampleRate) }
        val difference = bandDifferenceDb(tone, tone, sampleRate, flat.bandFrequenciesHz)
        assertNull(difference.first())
        assertNotNull(difference[flat.bandFrequenciesHz.indexOf(1125)])
    }

    @Test
    fun differenceCountsBandsAndFindsTheStrongest() {
        val gains = flat.bandGainsDb.toMutableList()
        gains[3] = -4.5f
        gains[10] = 1f
        val difference = equalizerDifference(flat.copy(bandGainsDb = gains))
        assertEquals(2, difference.changedBands)
        assertEquals(flat.bandFrequenciesHz[3], difference.strongestHz)
        assertEquals(-4.5f, difference.strongestDb, 0f)
        assertFalse(difference.isSubtle)
    }

    @Test
    fun smallChangesAreCalledSubtle() {
        val gains = flat.bandGainsDb.toMutableList()
        gains[10] = -1f
        assertTrue(equalizerDifference(flat.copy(bandGainsDb = gains)).isSubtle)
    }
}
