package com.juliancitou.timbra.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientNoiseTest {
    @Test
    fun silenceIsBelowTheQuietLimit() {
        assertTrue(rmsDbfs(ShortArray(1000)) < QUIET_ROOM_MAX_DBFS)
    }

    @Test
    fun fullScaleIsZeroDecibels() {
        assertEquals(0f, rmsDbfs(ShortArray(1000) { Short.MAX_VALUE }), 0.01f)
    }

    @Test
    fun tenPercentIsMinusTwentyDecibels() {
        assertEquals(-20f, rmsDbfs(ShortArray(1000) { (Short.MAX_VALUE / 10).toShort() }), 0.05f)
    }
}
