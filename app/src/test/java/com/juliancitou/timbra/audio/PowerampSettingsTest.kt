package com.juliancitou.timbra.audio

import com.juliancitou.timbra.export.BandLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerampSettingsTest {
    /** Los valores que dio Poweramp Equalizer build 1033 en el teléfono del usuario. */
    private val realValues = mapOf<String, Any?>(
        "peq_enable" to true,
        "equ_custom_bands" to true,
        "equ_custom_bands_num" to 32,
        "equ_custom_bands_first_fr" to 20,
        "equ_custom_bands_last_fr" to 20000,
        "equ_bands_num" to 10,
        "equ_overlap" to 0,
        "dsp_border_gain" to false,
        "no_dvc_gain_mb" to 0,
        "peq_new_pipeline_model" to 0,
    )

    @Test
    fun readsTheBandsAndOverlap() {
        val settings = requireNotNull(powerampSettingsFrom(realValues))
        assertTrue(settings.audioProcessingOn)
        assertEquals(BandLayout(32, 20, 20_000), settings.layout)
        assertEquals(BandOverlap.Envelope, settings.overlap)
        assertTrue(settings.issues.isEmpty())
    }

    @Test
    fun fixedBandsAreNotCopied() {
        assertNull(requireNotNull(powerampSettingsFrom(realValues + ("equ_custom_bands" to false))).layout)
    }

    @Test
    fun bandsOutsideTheAllowedLimitsAreNotCopied() {
        assertNull(requireNotNull(powerampSettingsFrom(realValues + ("equ_custom_bands_num" to 64))).layout)
    }

    @Test
    fun findsWhatChangesTheSound() {
        val values = realValues + mapOf(
            "dsp_border_gain" to true,
            "no_dvc_gain_mb" to -800,
            "peq_new_pipeline_model" to 1,
            "equ_overlap" to 1,
            "peq_enable" to false,
        )
        val settings = requireNotNull(powerampSettingsFrom(values))
        assertEquals(PowerampIssue.entries.toSet(), settings.issues)
        assertEquals(BandOverlap.Cascade, settings.overlap)
        assertFalse(settings.audioProcessingOn)
    }

    @Test
    fun unknownVersionsReturnNothing() {
        assertNull(powerampSettingsFrom(mapOf("otra_clave" to 1)))
    }
}
