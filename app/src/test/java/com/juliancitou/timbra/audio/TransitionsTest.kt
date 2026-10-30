package com.juliancitou.timbra.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val SAMPLE_RATE = 48_000
private val FADE_SAMPLES = (FADE_SECONDS * SAMPLE_RATE).toInt()

class TransitionsTest {

    @Test
    fun fadeInTakesFiftyMilliseconds() {
        val ramp = GainRamp(SAMPLE_RATE)
        ramp.fadeIn()
        val gains = List(FADE_SAMPLES + 10) { ramp.next() }
        assertTrue(gains[FADE_SAMPLES - 200] < 1.0)
        assertEquals(1.0, gains[FADE_SAMPLES], 1e-9)
    }

    @Test
    fun fadeHasNoJumps() {
        val ramp = GainRamp(SAMPLE_RATE)
        ramp.fadeIn()
        val gains = listOf(0.0) + List(FADE_SAMPLES) { ramp.next() }
        val largestStep = gains.zipWithNext { a, b -> abs(b - a) }.max()
        // La pendiente máxima del coseno es π/2 veces la de una rampa lineal.
        assertTrue(largestStep <= (PI / 2) / FADE_SAMPLES + 1e-9)
    }

    @Test
    fun fadeOutEndsSilentAndReportsIt() {
        val ramp = GainRamp(SAMPLE_RATE, startsAudible = true)
        assertTrue(!ramp.isSilent)
        ramp.fadeOut()
        repeat(FADE_SAMPLES) { ramp.next() }
        assertEquals(0.0, ramp.next(), 0.0)
        assertTrue(ramp.isSilent)
    }

    @Test
    fun equalizerChangeReachesTheNewGainWithoutClicks() {
        val crossfader = EqualizerCrossfader(SAMPLE_RATE)
        val sine = { n: Int -> 0.1 * sin(2 * PI * 440.0 * n / SAMPLE_RATE) }
        var n = 0
        repeat(SAMPLE_RATE / 10) { crossfader.process(sine(n++)) }

        crossfader.change(EqualizerSettings.Flat.copy(preampDb = 6f))
        val output = List(FADE_SAMPLES * 3) { crossfader.process(sine(n++)) }

        val newAmplitude = 0.1 * decibelsToGain(6.0)
        val steadyMaxStep = newAmplitude * 2 * PI * 440.0 / SAMPLE_RATE
        val largestStep = output.zipWithNext { a, b -> abs(b - a) }.max()
        assertTrue("Salto de $largestStep", largestStep <= steadyMaxStep * 1.05)
        assertEquals(newAmplitude, output.takeLast(FADE_SAMPLES).maxOf { abs(it) }, newAmplitude * 0.01)
    }

    @Test
    fun repeatedSameSettingsDoNotRestartTheCrossfade() {
        val target = EqualizerSettings.Flat.copy(preampDb = -3f)
        val crossfader = EqualizerCrossfader(SAMPLE_RATE, target)
        crossfader.change(target)
        assertEquals(0.5 * decibelsToGain(-3.0), crossfader.process(0.5), 1e-9)
    }
}
