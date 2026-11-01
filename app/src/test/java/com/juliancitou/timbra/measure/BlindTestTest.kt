package com.juliancitou.timbra.measure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlindTestTest {
    private fun afterChoosingEqualization(times: Int): BlindTest {
        var test = BlindTest(equalizedOption = 0)
        repeat(BLIND_TRIALS) { trial -> test = test.choose(if (trial < times) 0 else 1, nextEqualizedOption = 0) }
        return test
    }

    @Test
    fun finishesAfterEveryTrial() {
        assertFalse(BlindTest(equalizedOption = 1).isFinished)
        assertTrue(afterChoosingEqualization(0).isFinished)
    }

    @Test
    fun countsOnlyTheOptionThatWasTheEqualization() {
        val test = BlindTest(equalizedOption = 1).choose(0, nextEqualizedOption = 0).choose(0, nextEqualizedOption = 1)
        assertEquals(1, test.choseEqualization)
    }

    @Test
    fun verdictNeedsAClearMajority() {
        assertEquals(BlindVerdict.PrefersEqualization, afterChoosingEqualization(4).verdict)
        assertEquals(BlindVerdict.PrefersOriginal, afterChoosingEqualization(1).verdict)
        assertEquals(BlindVerdict.NoClearPreference, afterChoosingEqualization(3).verdict)
        assertEquals(BlindVerdict.NoClearPreference, afterChoosingEqualization(2).verdict)
    }
}
