package com.juliancitou.timbra.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Frecuencia baja para que las pruebas de 20 minutos corran rápido. */
private const val SAMPLE_RATE = 8_000
private const val FRAMES = 512

class AudioRendererTest {

    private fun renderSeconds(renderer: AudioRenderer, seconds: Double): FloatArray {
        val frames = (seconds * SAMPLE_RATE).toInt()
        val output = FloatArray(frames * 2)
        val buffer = FloatArray(FRAMES * 2)
        var written = 0
        while (written < frames) {
            val chunk = minOf(FRAMES, frames - written)
            renderer.render(buffer, chunk)
            buffer.copyInto(output, written * 2, 0, chunk * 2)
            written += chunk
        }
        return output
    }

    private val constantLoud = SignalSource { 5.0 }

    @Test
    fun staysSilentUntilPlayIsCalled() {
        val renderer = AudioRenderer(SAMPLE_RATE, PinkNoise())
        assertTrue(renderSeconds(renderer, 0.5).all { it == 0f })
        assertTrue(renderer.isSilent)
    }

    @Test
    fun everySampleGoesThroughTheLimiter() {
        val renderer = AudioRenderer(SAMPLE_RATE, constantLoud)
        renderer.changeEqualization(EqualizerSettings.Flat.copy(preampDb = 3f))
        renderer.play()
        val ceiling = decibelsToGain(MAX_OUTPUT_PEAK_DBFS).toFloat()
        assertTrue(renderSeconds(renderer, 2.0).all { abs(it) <= ceiling })
    }

    @Test
    fun bothChannelsCarryTheSameSignal() {
        val renderer = AudioRenderer(SAMPLE_RATE, PinkNoise())
        renderer.play()
        val output = renderSeconds(renderer, 0.2)
        for (frame in 0 until output.size / 2) assertEquals(output[2 * frame], output[2 * frame + 1])
    }

    @Test
    fun stopFadesOutAndThenReportsSilence() {
        val renderer = AudioRenderer(SAMPLE_RATE, PinkNoise())
        renderer.play()
        renderSeconds(renderer, 0.3)
        renderer.stop()
        renderSeconds(renderer, FADE_SECONDS / 2)
        assertFalse("A media salida aún suena", renderer.isSilent)
        renderSeconds(renderer, FADE_SECONDS * 2)
        assertTrue(renderer.isSilent)
        assertTrue(renderSeconds(renderer, 0.1).all { it == 0f })
    }

    @Test
    fun stopsByItselfAfterTwentyMinutes() {
        val renderer = AudioRenderer(SAMPLE_RATE, PinkNoise())
        renderer.play()
        renderSeconds(renderer, AUTO_STOP_SECONDS - 1.0)
        assertFalse(renderer.stoppedByTimeLimit)
        renderSeconds(renderer, 2.0)
        assertTrue(renderer.stoppedByTimeLimit)
        assertTrue(renderer.isSilent)
    }

    @Test
    fun playingAgainClearsTheTimeLimitFlag() {
        val renderer = AudioRenderer(SAMPLE_RATE, PinkNoise())
        renderer.play()
        renderSeconds(renderer, AUTO_STOP_SECONDS + 1.0)
        renderer.play()
        assertFalse(renderer.stoppedByTimeLimit)
    }

    @Test
    fun sourceChangeDipsToSilenceAndComesBack() {
        val renderer = AudioRenderer(SAMPLE_RATE, SignalSource { 0.1 })
        renderer.play()
        renderSeconds(renderer, 0.5)
        renderer.changeSource(SignalSource { -0.1 })
        val transition = renderSeconds(renderer, 0.5)
        assertTrue("Debe pasar por silencio", transition.any { it == 0f })
        assertTrue("Debe sonar la fuente nueva", transition.takeLast(10).all { it < 0f })
    }
}
