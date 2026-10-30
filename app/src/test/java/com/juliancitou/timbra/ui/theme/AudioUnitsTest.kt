package com.juliancitou.timbra.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioUnitsTest {

    @Test
    fun positiveGainHasPlusSign() {
        assertEquals("+1.5 dB", formatDecibels(1.5f))
    }

    @Test
    fun negativeGainUsesTypographicMinus() {
        assertEquals("−3.0 dB", formatDecibels(-3f))
    }

    @Test
    fun tinyNegativeGainRoundsToUnsignedZero() {
        assertEquals("0.0 dB", formatDecibels(-0.04f))
    }

    @Test
    fun gainRoundsToOneDecimal() {
        assertEquals("+2.3 dB", formatDecibels(2.26f))
    }

    @Test
    fun frequenciesBelowOneKilohertzStayInHertz() {
        assertEquals("20 Hz", formatHertz(20))
        assertEquals("843 Hz", formatHertz(843))
    }

    @Test
    fun kilohertzKeepOneDecimalWhenNeeded() {
        assertEquals("1.1 kHz", formatHertz(1125))
        assertEquals("8.4 kHz", formatHertz(8434))
        assertEquals("11.2 kHz", formatHertz(11247))
    }

    @Test
    fun roundKilohertzDropTheDecimal() {
        assertEquals("2 kHz", formatHertz(2000))
        assertEquals("15 kHz", formatHertz(14998))
        assertEquals("20 kHz", formatHertz(20000))
    }
}
