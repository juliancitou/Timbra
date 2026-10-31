package com.juliancitou.timbra.audio

import com.juliancitou.timbra.export.BandLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val RATE = 48_000

class DynamicsBandsTest {

    private val oneBandDown = EqualizerSettings(List(25) { if (it == 14) -8f else 0f })

    private fun gainNear(bands: List<PreviewBand>, hz: Float) = bands.first { it.upperEdgeHz >= hz }.gainDb

    @Test
    fun edgeBetweenTwoBandsIsTheirGeometricMean() {
        assertEquals(200f, bandUpperEdgesHz(listOf(100.0, 400.0))[0], 0.01f)
    }

    @Test
    fun everyCenterStaysInsideItsOwnBand() {
        listOf(BandLayout.Default, BandLayout(5), BandLayout(32, 5, 14_000)).forEach { layout ->
            val centers = layout.frequenciesHz.map { it.toDouble() }
            val edges = bandUpperEdgesHz(centers)
            assertEquals(centers.size, edges.size)
            centers.forEachIndexed { band, center ->
                assertTrue(center <= edges[band])
                if (band > 0) assertTrue(center >= edges[band - 1])
            }
        }
    }

    @Test
    fun directShapeKeepsThePresetBands() {
        val bands = previewBands(oneBandDown, BandShape.Direct, RATE)
        assertEquals(oneBandDown.bandGainsDb, bands.map { it.gainDb })
    }

    @Test
    fun denseShapesFollowTheCurveOfTheModel() {
        val bands = previewBands(oneBandDown, BandShape.Medium, RATE)
        assertTrue(bands.size > 25)
        val model = GraphicEqualizer(RATE, oneBandDown)
        assertEquals(model.magnitudeDb(1125.0).toFloat(), gainNear(bands, 1125f), 0.5f)
        assertEquals(0f, gainNear(bands, 100f), 0.1f)
    }

    @Test
    fun wideShapeReachesFurtherThanNarrow() {
        val wide = gainNear(previewBands(oneBandDown, BandShape.Wide, RATE), 1700f)
        val narrow = gainNear(previewBands(oneBandDown, BandShape.Narrow, RATE), 1700f)
        assertTrue("ancha $wide, angosta $narrow", wide < narrow)
    }

    @Test
    fun presetGainIsNotMixedWithThePreamp() {
        val withPreamp = oneBandDown.copy(preampDb = -6f)
        assertEquals(
            previewBands(oneBandDown, BandShape.Medium, RATE),
            previewBands(withPreamp, BandShape.Medium, RATE),
        )
    }
}
