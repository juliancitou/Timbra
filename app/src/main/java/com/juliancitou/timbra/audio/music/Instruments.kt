package com.juliancitou.timbra.audio.music

import com.juliancitou.timbra.audio.Biquad
import com.juliancitou.timbra.audio.BiquadCoefficients
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Carácter del bombo. Frecuencias en Hz, tiempos en segundos.
 * - [Soft]: redondo, para pop y jazz.
 * - [Punchy]: el "golpe" del rock y la electrónica, con más mazo (ataque) y cuerpo grave.
 * - [Boom]: largo y profundo, de hip-hop.
 */
enum class KickStyle(
    val startHz: Double,
    val endHz: Double,
    val pitchDecay: Double,
    val bodyDecay: Double,
    val beaterLevel: Double,
) {
    Soft(startHz = 160.0, endHz = 50.0, pitchDecay = 0.035, bodyDecay = 0.28, beaterLevel = 0.15),
    Punchy(startHz = 230.0, endHz = 55.0, pitchDecay = 0.025, bodyDecay = 0.32, beaterLevel = 0.5),
    Boom(startHz = 140.0, endHz = 45.0, pitchDecay = 0.05, bodyDecay = 0.5, beaterLevel = 0.12),
}

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
    private fun filter(coefficients: BiquadCoefficients) = Biquad(coefficients)

    // ---------- Percusión ----------

    /** Bombo: seno que cae de tono (el "golpe") más el clic del mazo contra el parche. */
    fun kick(target: DoubleArray, start: Int, style: KickStyle = KickStyle.Soft) {
        val beater = filter(BiquadCoefficients.bandPass(3500.0, q = 1.0, sampleRate))
        var phase = 0.0
        for (n in 0 until samples(style.bodyDecay * 2.5)) {
            val t = seconds(n)
            val frequency = style.endHz + (style.startHz - style.endHz) * exp(-t / style.pitchDecay)
            phase += 2 * PI * frequency / sampleRate
            val body = sin(phase) * exp(-t / style.bodyDecay) * attack(t, 0.001)
            val click = beater.process(noise()) * exp(-t / 0.004) * style.beaterLevel * 3
            target[start + n] += body + click
        }
    }

    /** Caja: tono de ~185 Hz más ruido entre 1 y 5 kHz (el "chasquido" de la bordonera). */
    fun snare(target: DoubleArray, start: Int, crack: Double = 1.0) {
        val band = filter(BiquadCoefficients.bandPass(2500.0, q = 0.8, sampleRate))
        val highPass = filter(BiquadCoefficients.highPass(1000.0, q = 0.707, sampleRate))
        var phase = 0.0
        for (n in 0 until samples(0.35)) {
            val t = seconds(n)
            phase += 2 * PI * (175 + 10 * exp(-t / 0.02)) / sampleRate
            val tone = sin(phase) * exp(-t / 0.06) * 0.6
            val rattle = highPass.process(band.process(noise())) * exp(-t / 0.12) * 1.2 * crack
            target[start + n] += (tone + rattle) * attack(t, 0.0005)
        }
    }

    /** Aplauso: tres ráfagas de ruido muy seguidas y una cola corta, como varias manos. */
    fun clap(target: DoubleArray, start: Int) {
        val band = filter(BiquadCoefficients.bandPass(1300.0, q = 1.0, sampleRate))
        for (n in 0 until samples(0.3)) {
            val t = seconds(n)
            val bursts = listOf(0.0, 0.011, 0.022).sumOf { at -> if (t >= at) exp(-(t - at) / 0.004) else 0.0 }
            val tail = exp(-t / 0.09)
            target[start + n] += band.process(noise()) * (bursts * 0.7 + tail) * 1.5
        }
    }

    /** Escobillas de jazz: un barrido suave de ruido sobre el parche. */
    fun brush(target: DoubleArray, start: Int) {
        val band = filter(BiquadCoefficients.bandPass(3800.0, q = 0.7, sampleRate))
        for (n in 0 until samples(0.4)) {
            val t = seconds(n)
            target[start + n] += band.process(noise()) * attack(t, 0.03) * exp(-t / 0.16) * 0.8
        }
    }

    /** Hi-hat: ruido sobre 7 kHz. Cerrado dura ~35 ms; abierto, casi un cuarto de segundo. */
    fun hiHat(target: DoubleArray, start: Int, open: Boolean = false) {
        val filters = List(2) { filter(BiquadCoefficients.highPass(7000.0, q = 0.707, sampleRate)) }
        val decay = if (open) 0.18 else 0.035
        for (n in 0 until samples(if (open) 0.6 else 0.15)) {
            var sample = noise()
            for (stage in filters) sample = stage.process(sample)
            target[start + n] += sample * exp(-seconds(n) / decay)
        }
    }

    /** Ride: platillo suave sobre 9 kHz con una campana en 5 kHz; da el "aire". */
    fun ride(target: DoubleArray, start: Int) {
        val filters = List(2) { filter(BiquadCoefficients.highPass(9000.0, q = 0.707, sampleRate)) }
        val bell = filter(BiquadCoefficients.bandPass(5200.0, q = 3.0, sampleRate))
        for (n in 0 until samples(1.2)) {
            val white = noise()
            var shimmer = white
            for (stage in filters) shimmer = stage.process(shimmer)
            val ring = bell.process(white) * exp(-seconds(n) / 0.15) * 0.4
            target[start + n] += (shimmer + ring) * exp(-seconds(n) / 0.35)
        }
    }

    /** Crash: platillo largo sobre 5 kHz que marca el inicio de una sección. */
    fun crash(target: DoubleArray, start: Int) {
        val highPass = filter(BiquadCoefficients.highPass(5000.0, q = 0.707, sampleRate))
        for (n in 0 until samples(3.0)) {
            val t = seconds(n)
            target[start + n] += highPass.process(noise()) * exp(-t / 0.9) * attack(t, 0.002)
        }
    }

    /** Platillo suspendido con baqueta suave: crece durante [swellSeconds], como en una orquesta. */
    fun cymbalSwell(target: DoubleArray, start: Int, swellSeconds: Double) {
        val highPass = filter(BiquadCoefficients.highPass(6000.0, q = 0.707, sampleRate))
        for (n in 0 until samples(swellSeconds + 1.5)) {
            val t = seconds(n)
            val envelope = if (t < swellSeconds) (t / swellSeconds).pow(2) else exp(-(t - swellSeconds) / 0.5)
            target[start + n] += highPass.process(noise()) * envelope
        }
    }

    /** Timbal: parche afinado; sus parciales no son armónicos y eso le da su "bum" orquestal. */
    fun timpani(target: DoubleArray, start: Int, midiNote: Int) {
        val fundamental = midiToHz(midiNote)
        val partials = listOf(1.0 to 1.0, 1.5 to 0.6, 1.99 to 0.4, 2.44 to 0.25, 2.89 to 0.15).map { (ratio, gain) ->
            Partial(fundamental * ratio, gain, decaySeconds = 1.6 / ratio)
        }
        addPartials(target, start, samples(3.0), sampleRate, partials) { attack(it, 0.004) }
    }

    // ---------- Graves ----------

    /**
     * Bajo pulsado: los armónicos agudos se apagan antes que la fundamental (eso da la
     * "definición" del ataque) y un sub-grave sostenido da el cuerpo. [brightness] sube los
     * armónicos para un bajo de rock o de sintetizador.
     */
    fun bass(target: DoubleArray, start: Int, midiNote: Int, durationSeconds: Double, brightness: Double = 1.0) {
        val frequency = midiToHz(midiNote)
        // Con brillo 1 los armónicos caen como 1/n^1.2; con más brillo caen más lento.
        val rolloff = 1.2 / brightness
        val partials = (1..30).map { n ->
            Partial(frequency * n, 1.0 / n.toDouble().pow(rolloff), decaySeconds = 0.9 / n.toDouble().pow(0.6))
        }.filter { it.frequencyHz < 2500 }
        val length = samples(durationSeconds + RELEASE_SECONDS)
        addPartials(target, start, length, sampleRate, partials) { gate(it, 0.004, durationSeconds) }
        val sub = listOf(Partial(frequency, 0.6, decaySeconds = 4.0))
        addPartials(target, start, length, sampleRate, sub) { gate(it, 0.01, durationSeconds) }
    }

    /** Contrabajo de jazz: pulsado con los dedos, cálido y con poca sostenida. */
    fun uprightBass(target: DoubleArray, start: Int, midiNote: Int, durationSeconds: Double) {
        val frequency = midiToHz(midiNote)
        val partials = (1..12).map { n ->
            Partial(frequency * n, 1.0 / n.toDouble().pow(1.5), decaySeconds = 1.2 / n.toDouble().pow(0.8))
        }
        addPartials(target, start, samples(durationSeconds + 0.08), sampleRate, partials) {
            gate(it, 0.003, durationSeconds, releaseSeconds = 0.08)
        }
    }

    /** 808: el sub-grave del hip-hop, un seno que baja de tono al inicio y suena largo con algo de saturación. */
    fun sub808(target: DoubleArray, start: Int, midiNote: Int, durationSeconds: Double) {
        val frequency = midiToHz(midiNote)
        var phase = 0.0
        for (n in 0 until samples(durationSeconds + 0.1)) {
            val t = seconds(n)
            phase += 2 * PI * frequency * (1 + 0.5 * exp(-t / 0.03)) / sampleRate
            val tone = tanh(1.6 * sin(phase)) / tanh(1.6)
            target[start + n] += tone * exp(-t / 1.1) * gate(t, 0.002, durationSeconds, releaseSeconds = 0.1)
        }
    }

    // ---------- Armonía ----------

    /**
     * Acorde de fondo ("pad"): cada nota con dos osciladores desafinados ±[detuneCents], que crean
     * el movimiento suave de un ensamble. Entrada lenta de 0.3 s.
     */
    fun pad(target: DoubleArray, start: Int, midiNotes: List<Int>, durationSeconds: Double, detuneCents: Double = 7.0) {
        midiNotes.forEach { note ->
            listOf(-detuneCents, detuneCents).forEach { cents ->
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
     * Piano: cada nota con parciales ligeramente más agudos que los armónicos exactos
     * (inarmonicidad de una cuerda tensa) y agudos que se apagan antes. Al soltar, el apagador
     * corta en 0.15 s.
     */
    fun piano(target: DoubleArray, start: Int, midiNotes: List<Int>, durationSeconds: Double, velocity: Double = 1.0) {
        midiNotes.forEach { note ->
            val frequency = midiToHz(note)
            val partials = (1..24).map { n ->
                val stretched = frequency * n * sqrt(1 + PIANO_INHARMONICITY * n * n)
                Partial(stretched, velocity / n.toDouble().pow(1.2), decaySeconds = 2.0 / n.toDouble().pow(0.7))
            }
            addPartials(target, start, samples(durationSeconds + 0.15), sampleRate, partials) {
                gate(it, 0.002, durationSeconds, releaseSeconds = 0.15) / midiNotes.size
            }
        }
    }

    /**
     * Guitarra eléctrica distorsionada tocando un acorde de quinta (power chord), con la mano
     * apagando las cuerdas ([palmMuted]). La saturación con `tanh` crea los armónicos del
     * amplificador y los filtros imitan su bocina (nada bajo 90 Hz ni sobre ~4.5 kHz).
     */
    fun distortedGuitar(
        target: DoubleArray,
        start: Int,
        midiNotes: List<Int>,
        durationSeconds: Double,
        palmMuted: Boolean,
    ) {
        val waves = midiNotes.map { note ->
            val frequency = midiToHz(note)
            frequency to Wavetable.forNote(frequency, cutoffHz = 5000.0) { 1.0 / it }
        }
        val phases = DoubleArray(waves.size) { random.nextDouble() }
        val cabinetLow = filter(BiquadCoefficients.highPass(90.0, q = 0.707, sampleRate))
        val cabinetHigh = filter(BiquadCoefficients.lowPass(4500.0, q = 0.9, sampleRate))
        val presence = filter(BiquadCoefficients.peaking(1800.0, gainDb = 4.0, q = 1.0, sampleRate))
        val decay = if (palmMuted) 0.12 else 1.5
        for (n in 0 until samples(durationSeconds + RELEASE_SECONDS)) {
            val t = seconds(n)
            var string = 0.0
            waves.forEachIndexed { index, (frequency, wave) ->
                phases[index] = (phases[index] + frequency / sampleRate) % 1.0
                string += wave.sample(phases[index]) / waves.size
            }
            val driven = tanh(GUITAR_DRIVE * string * exp(-t / decay))
            val amp = cabinetHigh.process(presence.process(cabinetLow.process(driven)))
            target[start + n] += amp * gate(t, 0.002, durationSeconds)
        }
    }

    /**
     * Sección de cuerdas: tres osciladores desafinados por nota (varios músicos), vibrato de arco y
     * entrada lenta, más un filtro de "caja de resonancia" y un poco de ruido de arco en los agudos.
     */
    fun strings(target: DoubleArray, start: Int, midiNote: Int, durationSeconds: Double, attackSeconds: Double = 0.25) {
        val frequency = midiToHz(midiNote)
        val wave = Wavetable.forNote(frequency, cutoffHz = 7000.0) { 1.0 / it }
        val body = filter(BiquadCoefficients.peaking(300.0, gainDb = 3.0, q = 1.0, sampleRate))
        val bright = filter(BiquadCoefficients.peaking(2500.0, gainDb = 2.0, q = 1.0, sampleRate))
        val soften = filter(BiquadCoefficients.lowPass(7000.0, q = 0.707, sampleRate))
        val bow = filter(BiquadCoefficients.bandPass(5000.0, q = 0.8, sampleRate))
        val detunes = listOf(-6.0, 0.0, 6.0).map { 2.0.pow(it / 1200) }
        val phases = DoubleArray(detunes.size) { random.nextDouble() }
        for (n in 0 until samples(durationSeconds + STRINGS_RELEASE_SECONDS)) {
            val t = seconds(n)
            val vibrato = 2.0.pow(0.12 * sin(2 * PI * 5.0 * t) * (t / 0.5).coerceAtMost(1.0) / 12)
            var ensemble = 0.0
            detunes.forEachIndexed { index, detune ->
                phases[index] = (phases[index] + frequency * detune * vibrato / sampleRate) % 1.0
                ensemble += wave.sample(phases[index]) / detunes.size
            }
            val tone = soften.process(bright.process(body.process(ensemble)))
            val rosin = bow.process(noise()) * 0.04
            target[start + n] += (tone + rosin) * gate(t, attackSeconds, durationSeconds, STRINGS_RELEASE_SECONDS)
        }
    }

    // ---------- Melodía ----------

    /**
     * Voz sintética: una fuente tipo cuerdas vocales (armónicos `1/n^1.6`, con vibrato que entra
     * poco a poco) pasada por tres resonancias, los formantes, que hacen que suene a vocal.
     */
    fun voice(target: DoubleArray, start: Int, midiNote: Int, durationSeconds: Double, vowel: Vowel) {
        val frequency = midiToHz(midiNote)
        val source = Wavetable.forNote(frequency, cutoffHz = 8000.0) { 1.0 / it.toDouble().pow(1.6) }
        val formants = vowel.formants.map { formant ->
            formant.gain to filter(BiquadCoefficients.bandPass(formant.frequencyHz, formant.q, sampleRate))
        }
        var phase = 0.0
        for (n in 0 until samples(durationSeconds + RELEASE_SECONDS)) {
            val t = seconds(n)
            val vibratoDepth = VIBRATO_SEMITONES * (t / VIBRATO_ONSET_SECONDS).coerceAtMost(1.0)
            val vibrato = 2.0.pow(vibratoDepth * sin(2 * PI * VIBRATO_HZ * t) / 12)
            phase = (phase + frequency * vibrato / sampleRate) % 1.0
            val glottal = source.sample(phase)
            val sung = formants.sumOf { (gain, resonance) -> gain * resonance.process(glottal) }
            target[start + n] += sung * gate(t, 0.06, durationSeconds, 0.12)
        }
    }

    /** Sintetizador principal: onda cuadrada suave (solo armónicos impares) con ataque de "pulsación". */
    fun synthLead(target: DoubleArray, start: Int, midiNote: Int, durationSeconds: Double) {
        val frequency = midiToHz(midiNote)
        val wave = Wavetable.forNote(frequency, cutoffHz = 9000.0) { if (it % 2 == 1) 1.0 / it else 0.0 }
        val tone = filter(BiquadCoefficients.lowPass(5000.0, q = 0.8, sampleRate))
        var phase = 0.0
        for (n in 0 until samples(durationSeconds + RELEASE_SECONDS)) {
            val t = seconds(n)
            phase = (phase + frequency / sampleRate) % 1.0
            val pluck = 0.6 + 0.4 * exp(-t / 0.15)
            target[start + n] += tone.process(wave.sample(phase)) * pluck * gate(t, 0.005, durationSeconds)
        }
    }

    /** Consonante "s": ruido concentrado entre 6 y 9 kHz, ~110 ms, justo antes de una vocal. */
    fun sibilant(target: DoubleArray, start: Int) {
        val low = filter(BiquadCoefficients.bandPass(6500.0, q = 1.2, sampleRate))
        val high = filter(BiquadCoefficients.bandPass(9000.0, q = 1.5, sampleRate))
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
        const val STRINGS_RELEASE_SECONDS = 0.5
        const val SIBILANT_SECONDS = 0.11
        private const val VIBRATO_HZ = 5.5
        private const val VIBRATO_SEMITONES = 0.35
        private const val VIBRATO_ONSET_SECONDS = 0.4
        private const val PIANO_INHARMONICITY = 0.0004
        private const val GUITAR_DRIVE = 6.0
    }
}

data class Formant(val frequencyHz: Double, val q: Double, val gain: Double)

/** Formantes de una voz aguda para cuatro vocales, en Hz. */
enum class Vowel(val formants: List<Formant>) {
    A(listOf(Formant(850.0, 8.0, 1.0), Formant(1220.0, 10.0, 0.5), Formant(2810.0, 15.0, 0.3))),
    O(listOf(Formant(550.0, 7.0, 1.0), Formant(1000.0, 9.0, 0.45), Formant(2830.0, 15.0, 0.2))),
    E(listOf(Formant(610.0, 7.0, 1.0), Formant(2330.0, 12.0, 0.45), Formant(2990.0, 15.0, 0.3))),
    U(listOf(Formant(400.0, 7.0, 1.0), Formant(800.0, 9.0, 0.35), Formant(2700.0, 15.0, 0.12))),
}
