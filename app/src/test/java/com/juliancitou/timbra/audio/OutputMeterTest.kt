package com.juliancitou.timbra.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OutputMeterTest {
    @Test
    fun readsPowerFromTheVisualizerFormat() {
        val fft = byteArrayOf(9, 9, 3, 4, 0, 0)
        val power = DoubleArray(3)
        addFftPower(fft, power)
        assertEquals(listOf(0.0, 25.0, 0.0), power.toList())
    }

    @Test
    fun comparesEachBandInDecibels() {
        val before = MeasuredSpectrum(List(512) { 100.0 }, binHz = 47.0)
        val after = MeasuredSpectrum(List(512) { 10.0 }, binHz = 47.0)
        val difference = spectrumDifferenceDb(before, after, listOf(1000, 2000, 4000))
        difference.forEach { assertEquals(-10f, requireNotNull(it), 0.01f) }
    }

    @Test
    fun aFlatLineDoesNotCountAsSeeingTheCuts() {
        val isCut = listOf(false, true, false, false, true, false)
        assertEquals(0f, cutDepthDb(List(6) { 0.5f }, isCut), 0.001f)
        assertEquals(8f, cutDepthDb(listOf(0f, -8f, 0f, 0f, -8f, 0f), isCut), 0.001f)
    }

    @Test
    fun alignmentKeepsTheShape() {
        assertEquals(listOf(-3f, 0f, 2f), listOf(-5f, -2f, 0f).alignedToItsMedian())
    }

    @Test
    fun lowBandsCannotBeMeasured() {
        val spectrum = MeasuredSpectrum(List(512) { 1.0 }, binHz = 47.0)
        assertNull(spectrumDifferenceDb(spectrum, spectrum, listOf(20, 1000))[0])
    }
}
