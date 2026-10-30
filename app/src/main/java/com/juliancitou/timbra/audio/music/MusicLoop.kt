package com.juliancitou.timbra.audio.music

import com.juliancitou.timbra.audio.decibelsToGain
import com.juliancitou.timbra.audio.loudnessLufs
import kotlin.math.abs

/**
 * Pistas comunes a todos los géneros: cada canción decide qué instrumento toca cada una (en clásica,
 * [Kick] son timbales y [Lead] es el violín). Se generan una vez y cada mezcla solo cambia volúmenes.
 */
enum class MusicStem { Kick, Bass, Snare, HiHat, Cymbals, Harmony, Lead, Sibilance }

/**
 * Qué zona resalta la mezcla en cada ronda del test, en dB sobre el nivel base de cada pista.
 * [Balanced] es la mezcla normal; las demás suben lo que la ronda necesita escuchar.
 */
enum class MusicFocus(private val boostsDb: Map<MusicStem, Double>) {
    Balanced(emptyMap()),
    Bass(mapOf(MusicStem.Kick to 5.0, MusicStem.Bass to 6.0)),
    Clarity(mapOf(MusicStem.Harmony to 5.0, MusicStem.Snare to 3.0, MusicStem.Bass to 1.0)),
    Voice(mapOf(MusicStem.Lead to 5.0, MusicStem.Harmony to -2.0)),
    Sibilance(mapOf(MusicStem.Sibilance to 8.0, MusicStem.HiHat to 6.0, MusicStem.Cymbals to 3.0)),
    Air(mapOf(MusicStem.Cymbals to 8.0, MusicStem.HiHat to 3.0));

    fun boostDb(stem: MusicStem): Double = boostsDb[stem] ?: 0.0
}

/**
 * Sonoridad de cada mezcla, en LUFS. Deja margen para que el test suba bandas sin que actúe el
 * limitador (que corta a −18 dBFS RMS, unos −15 LUFS).
 */
const val MUSIC_TARGET_LUFS = -24.0

/** Pico máximo de la mezcla, en dBFS: si la sonoridad objetivo lo pasara, se baja más. */
const val MUSIC_MAX_PEAK_DBFS = -8.0

private const val TAIL_SECONDS = 4.0

/**
 * Bucle musical de [song] generado por la app. Cada pista se sintetiza con espacio para las colas y
 * luego se "enrolla": lo que suena después del final se suma al principio. Así el bucle se repite
 * sin costura, como si la pieza nunca se hubiera detenido.
 */
class MusicLoop(private val sampleRate: Int, val song: Song = PopSong) {
    val loopSamples = (song.loopSeconds * sampleRate).toInt()
    private val stems: Map<MusicStem, DoubleArray> = renderStems()

    /** La mezcla para [focus], normalizada a [MUSIC_TARGET_LUFS]. */
    fun mix(focus: MusicFocus = MusicFocus.Balanced): DoubleArray {
        val dry = DoubleArray(loopSamples)
        val send = DoubleArray(loopSamples)
        stems.forEach { (stem, samples) ->
            val gain = decibelsToGain(song.levelsDb.getValue(stem) + focus.boostDb(stem))
            val sendLevel = song.reverbSends[stem] ?: 0.0
            for (i in 0 until loopSamples) {
                dry[i] += samples[i] * gain
                send[i] += samples[i] * gain * sendLevel
            }
        }
        val wet = loopedReverb(send)
        return normalized(DoubleArray(loopSamples) { dry[it] + wet[it] * song.reverbWet })
    }

    private fun renderStems(): Map<MusicStem, DoubleArray> {
        val instruments = Instruments(sampleRate)
        return song.levelsDb.keys.associateWith { stem ->
            val track = DoubleArray(loopSamples + (TAIL_SECONDS * sampleRate).toInt())
            song.play(stem, Performance(instruments, track, sampleRate, song.beatSeconds, loopSamples))
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
