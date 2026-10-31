package com.juliancitou.timbra.audio

import com.juliancitou.timbra.export.BandLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicsBandsTest {

    @Test
    fun edgeBetweenTwoBandsIsTheirGeometricMean() {
        assertEquals(200f, bandUpperEdgesHz(listOf(100, 400))[0], 0.01f)
    }

    @Test
    fun everyCenterStaysInsideItsOwnBand() {
        listOf(BandLayout.Default, BandLayout(5), BandLayout(32, 5, 14_000)).forEach { layout ->
            val centers = layout.frequenciesHz
            val edges = bandUpperEdgesHz(centers)
            assertEquals(centers.size, edges.size)
            centers.forEachIndexed { band, center ->
                assertTrue(center <= edges[band])
                if (band > 0) assertTrue(center >= edges[band - 1])
            }
        }
    }
}
