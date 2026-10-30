package com.juliancitou.timbra.audio.music

import com.juliancitou.timbra.audio.Biquad
import com.juliancitou.timbra.audio.BiquadCoefficients
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Instrumentos sintetizados. Cada función suma una nota a [target] desde la muestra [start]; el
 * arreglo debe tener espacio para la cola de la nota. Todo es determinista: el ruido sale de un
 * [Random] con semilla, así que el bucle suena idéntico cada vez.
 */
class Instruments(private val sampleRate: Int, seed: Int = 2026) {
    private val random = Random(seed)

    private fun seconds(samples: Int) = samples.toDouble() / sampleRate
    private fun samples(seconds: Double) = (seconds * sampleRate).toInt()
    private fun noise() = random.nextDouble(-1.0, 1.0)

    /** Bombo: seno que cae de ~160 a 48 Hz (el "golpe") con un clic corto de ataque. */
    fun kick(target: DoubleArray, start: Int) {
        var phase = 0.0
        for (n in 0 until samples(0.6)) {
            val t = seconds(n)
            val frequency = 48 + 110 * exp(-t / 0.035)
            phase += 2 * PI * frequency / sampleRate
            val body = sin(phase) * exp(-t / 0.28) * attack(t, 0.001)
            val click = noise() * exp(-t / 0.002) * 0.25
            target[start + n] += body + click
        }
    }

    /** Caja: tono de ~185 Hz más ruido filtrado entre 1 y 5 kHz (el "chasquido" del parche). */
    fun snare(target: DoubleArray, start: Int) {
        val band = Biquad(BiquadCoefficients.bandPass(2500.0, q = 0.8, sampleRate))
        val highPass = Biquad(BiquadCoefficients.highPass(1000.0, q = 0.707, sampleRate))
        var phase = 0.0
        for (n in 0 until samples(0.35)) {
            val t = seconds(n)
            phase += 2 * PI * (175 + 10 * exp(-t / 0.02)) / sampleRate
            val tone = sin(phase) * exp(-t / 0.06) * 0.6
            val rattle = highPass.process(band.process(noise())) * exp(-t / 0.12) * 1.2
            target[start + n] += (tone + rattle) * attack(t, 0.0005)
        }
    }

    /** Hi-hat: ruido sobre 7 kHz. Cerrado dura ~35 ms; abierto, [open], suena casi un cuarto de segundo. */
    fun hiHat(target: DoubleArray, start: Int, open: Boolean = false) {
        val filters = List(2) { Biquad(BiquadCoefficients.highPass(7000.0, q = 0.707, sampleRate)) }
        val decay = if (open) 0.18 else 0.035
        for (n in 0 until samples(if (open) 0.6 else 0.15)) {
            var sample = noise()
            for (filter in filters) sample = filter.process(sample)
            target[start + n] += sample * exp(-seconds(n) / decay)
        }
    }

    /** Ride: platillo suave sobre 9 kHz con una campana en 5 kHz; da el "aire". */
    fun ride(target: DoubleArray, start: Int) {
        val filters = List(2) { Biquad(BiquadCoefficients.highPass(9000.0, q = 0.707, sampleRate)) }
        val bell = Biquad(BiquadCoefficients.bandPass(5200.0, q = 3.0, sampleRate))
        for (n in 0 until samples(1.2)) {
            val white = noise()
            var shimmer = white
            for (filter in filters) shimmer = filter.process(shimmer)
            val ring = bell.process(white) * exp(-seconds(n) / 0.15) * 0.4
            target[start + n] += (shimmer + ring) * exp(-seconds(n) / 0.35)
        }
    }

    /** Crash: platillo largo sobre 5 kHz que marca el inicio del bucle. */
    fun crash(target: DoubleArray, start: Int) {
        val filter = Biquad(BiquadCoefficients.highPass(5000.0, q = 0.707, sampleRate))
        for (n in 0 until samples(3.0)) {
            val t = seconds(n)
            target[start + n] += filter.process(noise()) * exp(-t / 0.9) * attack(t, 0.002)
        }
    }

    /** Bajo: onda brillante (armónicos `1/n` hasta 1.5 kHz) con ataque rápido y algo de caída. */
    fun bass(target: DoubleArray, start: Int, midiNote: Int, durationSeconds: Double) {
        val frequency = midiToHz(midiNote)
        val wave = Wavetable.forNote(frequency, cutoffHz = 1500.0) { 1.0 / it }
        var phase = 0.0
        for (n in 0 until samples(durationSeconds + RELEASE_SECONDS)) {
            val t = seconds(n)
            phase = (phase + frequency / sampleRate) % 1.0
            val sustain = 0.7 + 0.3 * exp(-t / 0.1)
            target[start + n] += wave.sample(phase) * sustain * gate(t, 0.005, durationSeconds)
        }
    }

    /**
     * Acorde de fondo ("pad"): cada nota con dos osciladores desafinados ±7 cents, que crean el
     * movimiento suave de un ensamble de cuerdas. Entrada lenta de 0.3 s.
     */
    fun pad(target: DoubleArray, start: Int, midiNotes: List<Int>, durationSeconds: Double) {
        midiNotes.forEach { note ->
            listOf(-7.0, 7.0).forEach { cents ->
                val frequency = midiToHz(note) * 2.0.pow(cents / 1200)
                val wave = Wavetable.forNote(frequency, cutoffHz = 2500.0) { 1.0 / it.toDouble().pow(1.3) }
                var phase = random.nextDouble()
                for (n in 0 until samples(durationSeconds + PAD_RELEASE_SECONDS)) {
                    phase = (phase + frequency / sampleRate) % 1.0
                    val envelope = gate(seconds(n), 0.3, durationSeconds, PAD_RELEASE_SECONDS)
                    target[start + n] += wave.sample(phase) * envelope / (midiNotes.size * 2)
                }
            }
        }
    }

    /**
     * Voz sintética: una fuente tipo cuerdas vocales (armónicos `1/n^1.6`, con vibrato que entra
     * poco a poco) pasada por tres resonancias, los formantes, que hacen que suene a vocal.
     */
    fun voice(target: DoubleArray, start: Int, midiNote: Int, durationSeconds: Double, vowel: Vowel) {
        val frequency = midiToHz(midiNote)
        val source = Wavetable.forNote(frequency, cutoffHz = 8000.0) { 1.0 / it.toDouble().pow(1.6) }
        val formants = vowel.formants.map { formant ->
            formant.gain to Biquad(BiquadCoefficients.bandPass(formant.frequencyHz, formant.q, sampleRate))
        }
        var phase = 0.0
        for (n in 0 until samples(durationSeconds + RELEASE_SECONDS)) {
            val t = seconds(n)
            val vibratoDepth = VIBRATO_SEMITONES * (t / VIBRATO_ONSET_SECONDS).coerceAtMost(1.0)
            val vibrato = 2.0.pow(vibratoDepth * sin(2 * PI * VIBRATO_HZ * t) / 12)
            phase = (phase + frequency * vibrato / sampleRate) % 1.0
            val glottal = source.sample(phase)
            val sung = formants.sumOf { (gain, filter) -> gain * filter.process(glottal) }
            target[start + n] += sung * gate(t, 0.06, durationSeconds, 0.12)
        }
    }

    /** Consonante "s": ruido concentrado entre 6 y 9 kHz, ~110 ms, justo antes de una vocal. */
    fun sibilant(target: DoubleArray, start: Int) {
        val low = Biquad(BiquadCoefficients.bandPass(6500.0, q = 1.2, sampleRate))
        val high = Biquad(BiquadCoefficients.bandPass(9000.0, q = 1.5, sampleRate))
        for (n in 0 until samples(SIBILANT_SECONDS)) {
            val white = noise()
            val hiss = low.process(white) + 0.7 * high.process(white)
            target[start + n] += hiss * gate(seconds(n), 0.02, SIBILANT_SECONDS - 0.04, 0.04)
        }
    }

    /** Subida lineal en [attackSeconds] para que ninguna nota empiece con un salto. */
    private fun attack(t: Double, attackSeconds: Double) = (t / attackSeconds).coerceAtMost(1.0)

    /**
     * Envolvente de nota: sube en [attackSeconds], se sostiene hasta [durationSeconds] y baja en
     * [releaseSeconds]. Empieza y termina en cero, sin clics.
     */
    private fun gate(
        t: Double,
        attackSeconds: Double,
        durationSeconds: Double,
        releaseSeconds: Double = RELEASE_SECONDS,
    ): Double = when {
        t < attackSeconds -> t / attackSeconds
        t < durationSeconds -> 1.0
        else -> (1 - (t - durationSeconds) / releaseSeconds).coerceAtLeast(0.0)
    }

    companion object {
        const val RELEASE_SECONDS = 0.03
        const val PAD_RELEASE_SECONDS = 0.4
        const val SIBILANT_SECONDS = 0.11
        private const val VIBRATO_HZ = 5.5
        private const val VIBRATO_SEMITONES = 0.35
        private const val VIBRATO_ONSET_SECONDS = 0.4
    }
}

data class Formant(val frequencyHz: Double, val q: Double, val gain: Double)

/** Formantes de una voz aguda (soprano/contralto) para tres vocales, en Hz. */
enum class Vowel(val formants: List<Formant>) {
    A(listOf(Formant(850.0, 8.0, 1.0), Formant(1220.0, 10.0, 0.5), Formant(2810.0, 15.0, 0.3))),
    O(listOf(Formant(550.0, 7.0, 1.0), Formant(1000.0, 9.0, 0.45), Formant(2830.0, 15.0, 0.2))),
    E(listOf(Formant(610.0, 7.0, 1.0), Formant(2330.0, 12.0, 0.45), Formant(2990.0, 15.0, 0.3))),
}
