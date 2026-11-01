package com.juliancitou.timbra.audio

import com.juliancitou.timbra.export.BandLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerampPresetDataTest {
    /** "Huawei FreeBuds Pro 4 (ANC on)", tal como lo guarda Poweramp Equalizer build 1033. */
    private val realBlob = "50615001220000000000000000000000000000005A00000001000000000000001027000002500000989919BF1400000002500000C0CCCC3D19000000025000006766E63E1F000000025000006666863F2700000002500000343323403100000002500000666686403D000000025000000100A8404C0000000250000086EBB1405F000000025000008988B84077000000025000006666B440940000000250000049E19240B90000000250000049AD3440E70000000250000015AE07402101000002500000CB6BE83F69010000025000007977A73FC301000002500000972CBF3F340200000250000048E15A40C1020000025000006AF278407003000002500000FE7287404C04000002500000C6A98F405E05000002500000CDCC9C40B5060000025000000F6289406108000002500000B1096140780A000002500000FDA16F40140D00000250000051237240561000000250000012C3A23E6914000002500000C23EAB3F801900000250000033335340DC1F00000250000041F40D40CE27000002500000DE1ACABEBB3100000250000081DF93C0213E000002500000000028C19F4D0000"
        .chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private val bands = requireNotNull(graphicBandsOf(realBlob))

    @Test
    fun readsOnlyTheGraphicBands() {
        assertEquals(32, bands.size)
        assertEquals(20, bands.first().frequencyHz)
        assertEquals(-0.6f, bands.first().gainDb, 0.01f)
        assertEquals(5.77f, bands.first { it.frequencyHz == 119 }.gainDb, 0.01f)
    }

    @Test
    fun aCopyMadeByPowerampKeepsTheName() {
        assertTrue(isSamePresetName("Timbra - Bocina", "Timbra - Bocina"))
        assertTrue(isSamePresetName("Timbra - Bocina (2)", "Timbra - Bocina"))
        assertTrue(isSamePresetName("Timbra - A+B (Cable) (12)", "Timbra - A+B (Cable)"))
    }

    @Test
    fun anotherPresetWithASimilarNameIsNotACopy() {
        assertFalse(isSamePresetName("Timbra - Bocina (Bluetooth)", "Timbra - Bocina"))
        assertFalse(isSamePresetName("Timbra - Bocina 2", "Timbra - Bocina"))
        assertFalse(isSamePresetName("Timbra - Bocina (2) (3)", "Timbra - Bocina"))
    }

    @Test
    fun aRepeatedTestWithAnotherCurveDiffersFromTheStoredPreset() {
        val stored = bands.map { it.gainDb }
        assertFalse(blobDiffersFrom(realBlob, stored))
        assertTrue(blobDiffersFrom(realBlob, stored.map { it - 0.5f }))
        assertTrue(blobDiffersFrom(realBlob, stored.drop(1)))
    }

    @Test
    fun unreadableDataDoesNotContradictThePreset() {
        assertFalse(blobDiffersFrom(null, listOf(0f)))
        assertFalse(blobDiffersFrom(ByteArray(420), listOf(0f)))
    }

    @Test
    fun rejectsDataThatIsNotAPreset() {
        assertNull(graphicBandsOf(ByteArray(420)))
        assertNull(graphicBandsOf(realBlob.copyOf(100)))
    }

    @Test
    fun interpolatesBetweenBandsInOctaves() {
        val line = listOf(PresetBand(100, 0f), PresetBand(400, 4f))
        assertEquals(listOf(0.0, 2.0, 4.0, 4.0), gainsAt(line, listOf(50, 200, 400, 800)))
    }

    @Test
    fun startingCurveKeepsTheShapeAndLeavesRoomToBoost() {
        val profile = AutoEqProfile("x", null, bands)
        val frequencies = BandLayout(32, 20, 20_000).frequenciesHz
        val curve = profile.startingCurveDb(frequencies)
        val original = gainsAt(bands, frequencies)
        assertEquals(1.5, curve.max(), 1e-6)
        val shift = original[0] - curve[0]
        original.zip(curve).forEach { (before, after) -> assertEquals(before - shift, after, 1e-6) }
        assertTrue(shift > 0)
    }

    @Test
    fun aQuietCurveIsNotMoved() {
        val profile = AutoEqProfile("x", null, listOf(PresetBand(20, -2f), PresetBand(20_000, 1f)))
        assertEquals(listOf(-2.0, 1.0), profile.startingCurveDb(listOf(20, 20_000)))
    }
}
