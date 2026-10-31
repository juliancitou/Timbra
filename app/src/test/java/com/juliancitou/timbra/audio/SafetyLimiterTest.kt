package com.juliancitou.timbra.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val SAMPLE_RATE = 48_000
private val PEAK_CEILING = decibelsToGain(MAX_OUTPUT_PEAK_DBFS).toFloat()

class SafetyLimiterTest {

    private fun sine(amplitude: Double, frequencyHz: Double, seconds: Double) =
        DoubleArray((seconds * SAMPLE_RATE).toInt()) { amplitude * sin(2 * PI * frequencyHz * it / SAMPLE_RATE) }

    private fun rmsDb(samples: List<Float>) = 20 * log10(sqrt(samples.sumOf { it.toDouble() * it } / samples.size))

    @Test
    fun noSampleEverExceedsThePeakCeiling() {
        val limiter = SafetyLimiter(SAMPLE_RATE)
        val random = Random(seed = 7)
        val brutalInput = DoubleArray(SAMPLE_RATE * 3) { random.nextDouble(-50.0, 50.0) }
        val output = brutalInput.map { limiter.process(it) }
        assertTrue(output.all { abs(it) <= PEAK_CEILING })
    }

    @Test
    fun suddenImpulseAfterSilenceIsCapped() {
        val limiter = SafetyLimiter(SAMPLE_RATE)
        repeat(SAMPLE_RATE) { limiter.process(0.0) }
        assertTrue(abs(limiter.process(1.0)) <= PEAK_CEILING)
        assertTrue(abs(limiter.process(-8.0)) <= PEAK_CEILING)
    }

    @Test
    fun loudSteadySignalSettlesAtTheRmsCeiling() {
        val limiter = SafetyLimiter(SAMPLE_RATE)
        val output = sine(amplitude = 1.0, frequencyHz = 440.0, seconds = 4.0).map { limiter.process(it) }
        val lastSecond = output.takeLast(SAMPLE_RATE)
        assertTrue("RMS final ${rmsDb(lastSecond)} dBFS", rmsDb(lastSecond) <= MAX_OUTPUT_RMS_DBFS + 0.5)
    }

    @Test
    fun quietSignalPassesUnchanged() {
        val limiter = SafetyLimiter(SAMPLE_RATE)
        val input = sine(amplitude = 0.03, frequencyHz = 1000.0, seconds = 1.0)
        val output = input.map { limiter.process(it) }
        input.zip(output).forEach { (original, processed) -> assertEquals(original, processed.toDouble(), 1e-6) }
    }

    @Test
    fun invalidSamplesBecomeSilence() {
        val limiter = SafetyLimiter(SAMPLE_RATE)
        assertEquals(0f, limiter.process(Double.NaN))
        assertEquals(0f, limiter.process(Double.POSITIVE_INFINITY))
        assertEquals(0f, limiter.process(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun stereoNeverExceedsTheCeilingOnEitherChannel() {
        val limiter = SafetyLimiter(SAMPLE_RATE)
        val frame = FloatArray(2)
        repeat(SAMPLE_RATE) {
            limiter.processStereo(5.0, -3.0, frame, 0)
            assertTrue(abs(frame[0]) <= PEAK_CEILING && abs(frame[1]) <= PEAK_CEILING)
        }
    }

    @Test
    fun stereoKeepsTheBalanceBetweenChannelsWhileLimiting() {
        val limiter = SafetyLimiter(SAMPLE_RATE)
        val frame = FloatArray(2)
        repeat(SAMPLE_RATE) { limiter.processStereo(0.8, 0.4, frame, 0) }
        assertEquals(2.0, (frame[0] / frame[1]).toDouble(), 1e-3)
    }

    @Test
    fun stereoSilencesInvalidSamples() {
        val frame = floatArrayOf(1f, 1f)
        SafetyLimiter(SAMPLE_RATE).processStereo(Double.NaN, 0.5, frame, 0)
        assertEquals(0f, frame[0])
        assertEquals(0f, frame[1])
    }
}
