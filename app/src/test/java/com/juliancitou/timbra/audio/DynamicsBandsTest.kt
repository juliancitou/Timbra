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
    fun bandsFollowTheCurveOfTheModel() {
        val bands = previewBands(oneBandDown, BandOverlap.Envelope, RATE)
        assertTrue(bands.size > 25)
        val model = GraphicEqualizer(RATE, oneBandDown)
        assertEquals(model.magnitudeDb(1125.0).toFloat(), gainNear(bands, 1125f), 0.8f)
        assertEquals(0f, gainNear(bands, 100f), 0.1f)
    }

    @Test
    fun cascadeReachesFurtherThanEnvelope() {
        val settings = EqualizerSettings(List(25) { if (it in 12..16) -4f else 0f })
        val envelope = gainNear(previewBands(settings, BandOverlap.Envelope, RATE), 1125f)
        val cascade = gainNear(previewBands(settings, BandOverlap.Cascade, RATE), 1125f)
        assertEquals(-4f, envelope, 0.5f)
        assertTrue("cascada $cascade", cascade < -6f)
    }

    @Test
    fun presetGainIsNotMixedWithThePreamp() {
        val withPreamp = oneBandDown.copy(preampDb = -6f)
        assertEquals(
            previewBands(oneBandDown, BandOverlap.Envelope, RATE),
            previewBands(withPreamp, BandOverlap.Envelope, RATE),
        )
    }
}
