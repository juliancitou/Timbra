package com.juliancitou.timbra.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BandLayoutTest {

    /** Las 25 bandas del preset del usuario, tal como las muestra Poweramp. */
    private val userBands = listOf(
        20, 27, 36, 47, 63, 84, 112, 150, 200, 267, 356, 474, 632,
        843, 1125, 1500, 2000, 2667, 3557, 4743, 6325, 8434, 11247, 14998, 20000,
    )

    @Test
    fun defaultLayoutReproducesTheUsersPowerampBands() {
        assertEquals(userBands, BandLayout.Default.frequenciesHz)
    }

    @Test
    fun firstAndLastBandsAreTheLimits() {
        val layout = BandLayout(count = 10, firstHz = 31, lastHz = 16_000)
        assertEquals(10, layout.frequenciesHz.size)
        assertEquals(31, layout.frequenciesHz.first())
        assertEquals(16_000, layout.frequenciesHz.last())
    }

    @Test
    fun bandsAreEvenlySpacedInOctaves() {
        val layout = BandLayout(count = 12)
        val octaves = layout.frequenciesHz.map { kotlin.math.log2(it.toDouble()) }
        octaves.zipWithNext { a, b -> b - a }.forEach { assertEquals(layout.spacingOctaves, it, 0.02) }
    }

    @Test
    fun bandsAlwaysRise() {
        listOf(BandLayout(5, 5, 14_000), BandLayout(32, 200, 20_000), BandLayout(32, 5, 14_000)).forEach { layout ->
            assertTrue(layout.frequenciesHz.zipWithNext().all { (a, b) -> b > a })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMoreBandsThanPowerampAllows() {
        BandLayout(count = 33)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsALastFrequencyBelowFourteenKilohertz() {
        BandLayout(lastHz = 10_000)
    }
}
