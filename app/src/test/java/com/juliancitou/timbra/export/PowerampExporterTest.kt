package com.juliancitou.timbra.export

import com.juliancitou.timbra.audio.EqualizerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerampExporterTest {

    /** Ganancias del preset v5, hecho a mano por el usuario y probado en Poweramp. */
    private val vocesV5 = EqualizerSettings(
        bandGainsDb = listOf(
            -1.5f, -1.5f, -1f, -0.5f, 0f, 0f, -1f, -2f, -2.5f, -2.5f, -2f, -1f, -0.5f,
            0f, 0.5f, 1f, 1.5f, 1f, 0f, -1.5f, -3.5f, -4.5f, -3.5f, -2.5f, -2.5f,
        ),
        preampDb = -4.5f,
    )

    private fun resource(path: String): String =
        requireNotNull(javaClass.classLoader?.getResource(path)) { "Falta $path" }.readText()

    /**
     * Prueba golden: el exportador debe producir, carácter por carácter, el mismo archivo que un
     * preset que Poweramp ya importa bien. Si esta prueba falla, el formato cambió.
     */
    @Test
    fun matchesAPresetThatPowerampAlreadyImports() {
        assertEquals(resource("golden/poweramp-voces-v5.json"), PowerampExporter.toJson("Voces Perfectas v5", vocesV5))
    }

    @Test
    fun hasTwoNeutralToneBandsAndTwentyFiveGraphicBands() {
        val json = PowerampExporter.toJson("Prueba", EqualizerSettings.Flat)
        assertEquals(1, Regex("\"type\": 0").findAll(json).count())
        assertEquals(1, Regex("\"type\": 1").findAll(json).count())
        assertEquals(25, Regex("\"type\": 2").findAll(json).count())
        assertEquals(27, Regex("\"gain\": 0.0").findAll(json).count())
    }

    @Test
    fun neverWritesNegativeZero() {
        val settings = EqualizerSettings(List(25) { -0f }, preampDb = -0f)
        assertFalse(PowerampExporter.toJson("Ceros", settings).contains("-0.0"))
    }

    @Test
    fun escapesQuotesAndBackslashesInTheName() {
        val json = PowerampExporter.toJson("Mi \"preset\" \\ uno", EqualizerSettings.Flat)
        assertTrue(json.contains("\"name\": \"Timbra - Mi \\\"preset\\\" \\\\ uno\""))
    }

    @Test
    fun suggestsASafeFileName() {
        assertEquals("timbra-mi-ecualizacion.json", PowerampExporter.fileName("  Mi Ecualización "))
        assertEquals("timbra-freebuds-pro-4-anc.json", PowerampExporter.fileName("FreeBuds Pro 4 (ANC)"))
        assertEquals("timbra-preset.json", PowerampExporter.fileName("¿¿??"))
    }

    /** No viene de Poweramp: fija el formato con otra cantidad de bandas para que no cambie sin querer. */
    @Test
    fun exportsACustomLayoutWithItsOwnBands() {
        val layout = BandLayout(count = 10)
        val gains = listOf(-3f, -2f, -1f, 0f, 0.5f, 1f, 0f, -1.5f, -3f, -2f)
        val settings = EqualizerSettings(gains, preampDb = -4.5f, bandFrequenciesHz = layout.frequenciesHz)
        assertEquals(resource("golden/poweramp-10-bands.json"), PowerampExporter.toJson("Diez bandas", settings))
    }
}
