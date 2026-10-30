package com.juliancitou.timbra.audio.music

import com.juliancitou.timbra.audio.decibelsToGain
import com.juliancitou.timbra.audio.loudnessLufs
import kotlin.math.abs

/** Pistas por separado: se generan una vez y cada mezcla solo cambia sus volúmenes. */
enum class MusicStem(val levelDb: Double, val reverbSend: Double) {
    Kick(levelDb = -2.0, reverbSend = 0.0),
    Bass(levelDb = -6.0, reverbSend = 0.0),
    Snare(levelDb = -8.0, reverbSend = 0.2),
    HiHat(levelDb = -15.0, reverbSend = 0.05),
    Cymbals(levelDb = -19.0, reverbSend = 0.1),
    Pad(levelDb = -11.0, reverbSend = 0.35),
    Voice(levelDb = -6.0, reverbSend = 0.3),
    Sibilance(levelDb = -16.0, reverbSend = 0.2),
}

/**
 * Qué zona resalta la mezcla en cada ronda del test, en dB sobre el nivel base de cada pista.
 * [Balanced] es la mezcla normal; las demás suben lo que la ronda necesita escuchar.
 */
enum class MusicFocus(private val boostsDb: Map<MusicStem, Double>) {
    Balanced(emptyMap()),
    Bass(mapOf(MusicStem.Kick to 5.0, MusicStem.Bass to 6.0)),
    Clarity(mapOf(MusicStem.Pad to 5.0, MusicStem.Snare to 3.0, MusicStem.Bass to 1.0)),
    Voice(mapOf(MusicStem.Voice to 5.0, MusicStem.Pad to -2.0)),
    Sibilance(mapOf(MusicStem.Sibilance to 8.0, MusicStem.HiHat to 6.0, MusicStem.Cymbals to 3.0)),
    Air(mapOf(MusicStem.Cymbals to 8.0, MusicStem.HiHat to 3.0));

    fun levelDb(stem: MusicStem): Double = stem.levelDb + (boostsDb[stem] ?: 0.0)
}

/**
 * Sonoridad de cada mezcla, en LUFS. Deja margen para que el test suba bandas sin que actúe el
 * limitador (que corta a −18 dBFS RMS, unos −15 LUFS).
 */
const val MUSIC_TARGET_LUFS = -24.0

/** Pico máximo de la mezcla, en dBFS: si la sonoridad objetivo lo pasara, se baja más. */
const val MUSIC_MAX_PEAK_DBFS = -8.0

private const val REVERB_WET = 0.25
private const val TAIL_SECONDS = 3.5

/**
 * Bucle musical de [Arrangement.LOOP_SECONDS] generado por la app. Cada pista se sintetiza con
 * espacio para las colas y luego se "enrolla": lo que suena después del final se suma al
 * principio. Así el bucle se repite sin costura, como si la pieza nunca se hubiera detenido.
 */
class MusicLoop(private val sampleRate: Int) {
    val loopSamples = (Arrangement.LOOP_SECONDS * sampleRate).toInt()
    private val stems: Map<MusicStem, DoubleArray> = renderStems()

    /** La mezcla para [focus], normalizada a [MUSIC_TARGET_LUFS]. */
    fun mix(focus: MusicFocus = MusicFocus.Balanced): DoubleArray {
        val dry = DoubleArray(loopSamples)
        val send = DoubleArray(loopSamples)
        stems.forEach { (stem, samples) ->
            val gain = decibelsToGain(focus.levelDb(stem))
            for (i in 0 until loopSamples) {
                dry[i] += samples[i] * gain
                send[i] += samples[i] * gain * stem.reverbSend
            }
        }
        val wet = loopedReverb(send)
        return normalized(DoubleArray(loopSamples) { dry[it] + wet[it] * REVERB_WET })
    }

    private fun renderStems(): Map<MusicStem, DoubleArray> {
        val instruments = Instruments(sampleRate)
        val beat = { beats: Double -> (beats * Arrangement.BEAT_SECONDS * sampleRate).toInt() }
        val beatsToSeconds = { beats: Double -> beats * Arrangement.BEAT_SECONDS }
        return MusicStem.entries.associateWith { stem ->
            val track = DoubleArray(loopSamples + (TAIL_SECONDS * sampleRate).toInt())
            when (stem) {
                MusicStem.Kick -> Arrangement.kickBeats.forEach { instruments.kick(track, beat(it)) }
                MusicStem.Snare -> Arrangement.snareBeats.forEach { instruments.snare(track, beat(it)) }
                MusicStem.HiHat -> Arrangement.hiHats.forEach { (at, open) -> instruments.hiHat(track, beat(at), open) }
                MusicStem.Cymbals -> {
                    Arrangement.rideBeats.forEach { instruments.ride(track, beat(it)) }
                    Arrangement.crashBeats.forEach { instruments.crash(track, beat(it)) }
                }
                MusicStem.Bass -> Arrangement.bassNotes.forEach {
                    instruments.bass(track, beat(it.beat), it.midiNote, beatsToSeconds(it.lengthBeats))
                }
                MusicStem.Pad -> Arrangement.padChords.forEach { (at, notes) ->
                    instruments.pad(track, beat(at), notes, beatsToSeconds(Arrangement.BEATS_PER_BAR.toDouble()))
                }
                MusicStem.Voice -> Arrangement.melody.forEach {
                    instruments.voice(track, beat(it.beat), it.midiNote, beatsToSeconds(it.lengthBeats), it.vowel)
                }
                MusicStem.Sibilance -> Arrangement.melody.filter { it.sibilant }.forEach {
                    val before = (Instruments.SIBILANT_SECONDS * 0.8 * sampleRate).toInt()
                    instruments.sibilant(track, (beat(it.beat) - before).mod(loopSamples))
                }
            }
            wrapped(track)
        }
    }

    /** Suma la cola (lo que pasa del final) al principio del bucle. */
    private fun wrapped(track: DoubleArray): DoubleArray {
        val loop = DoubleArray(loopSamples)
        for (i in track.indices) loop[i % loopSamples] += track[i]
        return loop
    }

    /** Reverberación en régimen continuo: se pasan dos vueltas y se conserva la segunda. */
    private fun loopedReverb(send: DoubleArray): DoubleArray {
        val reverb = Reverb(sampleRate)
        repeat(loopSamples) { reverb.process(send[it]) }
        return DoubleArray(loopSamples) { reverb.process(send[it]) }
    }

    private fun normalized(samples: DoubleArray): DoubleArray {
        val loudnessGain = decibelsToGain(MUSIC_TARGET_LUFS - loudnessLufs(samples, sampleRate))
        val peak = samples.maxOf { abs(it) } * loudnessGain
        val peakGain = minOf(1.0, decibelsToGain(MUSIC_MAX_PEAK_DBFS) / peak)
        val gain = loudnessGain * peakGain
        return DoubleArray(samples.size) { samples[it] * gain }
    }
}
