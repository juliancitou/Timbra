package com.juliancitou.timbra.audio.music

import com.juliancitou.timbra.audio.Biquad
import com.juliancitou.timbra.audio.BiquadCoefficients
import com.juliancitou.timbra.audio.decibelsToGain
import com.juliancitou.timbra.audio.loudnessLufs
import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

private const val SAMPLE_RATE = 48_000

class MusicLoopTest {

    companion object {
        private val loop by lazy { MusicLoop(SAMPLE_RATE) }
        private val balanced by lazy { loop.mix(MusicFocus.Balanced) }
    }

    /**
     * Energía en dB alrededor de [centerHz] (filtro de ~0.7 octavas). El bucle es continuo, así que
     * una primera vuelta deja el filtro en régimen antes de medir.
     */
    private fun bandEnergyDb(samples: DoubleArray, centerHz: Double): Double {
        val filter = Biquad(BiquadCoefficients.bandPass(centerHz, q = 2.0, sampleRate = SAMPLE_RATE))
        samples.forEach { filter.process(it) }
        val energy = samples.sumOf { val filtered = filter.process(it); filtered * filtered }
        return 10 * log10(energy / samples.size)
    }

    private fun zoneEnergyDb(samples: DoubleArray, range: IntRange): Double {
        val bands = POWERAMP_BAND_FREQUENCIES_HZ.filter { it in range }
        return 10 * log10(bands.sumOf { 10.0.pow(bandEnergyDb(samples, it.toDouble()) / 10) })
    }

    @Test
    fun loopLastsExactlyFourBarsAt96Bpm() {
        assertEquals(10.0, Arrangement.LOOP_SECONDS, 1e-9)
        assertEquals(SAMPLE_RATE * 10, balanced.size)
    }

    @Test
    fun mixIsFiniteAndSafelyBelowTheLimiter() {
        assertTrue(balanced.all { it.isFinite() })
        assertTrue(balanced.maxOf { abs(it) } <= decibelsToGain(MUSIC_MAX_PEAK_DBFS) + 1e-9)
        assertTrue(loudnessLufs(balanced, SAMPLE_RATE) <= MUSIC_TARGET_LUFS + 0.1)
    }

    @Test
    fun loopRepeatsWithoutAJump() {
        val steps = balanced.toList().zipWithNext { a, b -> abs(b - a) }.sorted()
        val typicalLargeStep = steps[(steps.size * 0.999).toInt()]
        val seam = abs(balanced.first() - balanced.last())
        assertTrue("Costura de $seam contra $typicalLargeStep", seam <= typicalLargeStep)
    }

    @Test
    fun everyAudibleBandHasMusicalEnergy() {
        val levels = POWERAMP_BAND_FREQUENCIES_HZ.associateWith { bandEnergyDb(balanced, it.toDouble()) }
        val loudest = levels.values.max()
        val weak = levels.filter { (hz, db) -> hz in 27..14998 && db < loudest - 45 }
        assertTrue("Bandas casi vacías: $weak", weak.isEmpty())
    }

    /**
     * Cuánto gana [zone] frente a [reference] al pasar de la mezcla equilibrada a [focus], en dB.
     * Cada mezcla se normaliza al mismo volumen, así que el énfasis se mide como balance relativo.
     */
    private fun emphasisDb(focus: MusicFocus, zone: IntRange, reference: IntRange): Double {
        val mix = loop.mix(focus)
        val focused = zoneEnergyDb(mix, zone) - zoneEnergyDb(mix, reference)
        val normal = zoneEnergyDb(balanced, zone) - zoneEnergyDb(balanced, reference)
        return focused - normal
    }

    @Test
    fun bassFocusRaisesTheLowEnd() {
        val gain = emphasisDb(MusicFocus.Bass, zone = 36..112, reference = 843..4743)
        assertTrue("Graves +$gain dB frente a los medios", gain >= 3.0)
    }

    @Test
    fun sibilanceFocusRaisesTheEssZone() {
        val gain = emphasisDb(MusicFocus.Sibilance, zone = 4743..11247, reference = 200..2000)
        assertTrue("Zona de la S +$gain dB frente a los medios", gain >= 3.0)
    }

    @Test
    fun voiceFocusRaisesPresence() {
        val gain = emphasisDb(MusicFocus.Voice, zone = 843..2667, reference = 47..200)
        assertTrue("Voz +$gain dB frente a los graves", gain >= 2.0)
    }

    @Test
    fun renderingIsDeterministic() {
        assertArrayEquals(balanced, MusicLoop(SAMPLE_RATE).mix(MusicFocus.Balanced), 0.0)
    }
}
