package com.juliancitou.timbra.audio.music

/** Lo que una canción necesita para tocar una pista: instrumentos, la pista y la conversión de tiempos. */
class Performance(
    val instruments: Instruments,
    val track: DoubleArray,
    private val sampleRate: Int,
    private val beatSeconds: Double,
    private val loopSamples: Int,
) {
    /** Muestra donde cae el pulso [beat]. */
    fun at(beat: Double): Int = (beat * beatSeconds * sampleRate).toInt()

    /** Muestra [seconds] antes del pulso [beat]; si cae antes del inicio, da la vuelta al final del bucle. */
    fun before(beat: Double, seconds: Double): Int = (at(beat) - (seconds * sampleRate).toInt()).mod(loopSamples)

    fun seconds(beats: Double): Double = beats * beatSeconds
}

data class Note(val beat: Double, val lengthBeats: Double, val midiNote: Int)

data class SungNote(
    val beat: Double,
    val lengthBeats: Double,
    val midiNote: Int,
    val vowel: Vowel,
    val sibilant: Boolean,
)

/**
 * Una pieza del test. Todas usan las mismas pistas ([MusicStem]), así que las rondas resaltan
 * graves, voz o platillos en cualquier género. Los tiempos van en pulsos (negras) y las notas en
 * MIDI. Las melodías son originales.
 */
abstract class Song(val tempoBpm: Double, val bars: Int = 4, val beatsPerBar: Int = 4) {
    val beatSeconds: Double get() = 60.0 / tempoBpm
    val loopSeconds: Double get() = beatSeconds * beatsPerBar * bars

    /** Volumen base de cada pista en dB; las pistas que no aparecen no suenan en esta pieza. */
    abstract val levelsDb: Map<MusicStem, Double>
    abstract val reverbSends: Map<MusicStem, Double>
    open val reverbWet: Double = 0.25

    abstract fun play(stem: MusicStem, performance: Performance)

    protected fun barStart(bar: Int) = (bar * beatsPerBar).toDouble()
    protected fun everyBar(pattern: List<Double>) = (0 until bars).flatMap { bar -> pattern.map { barStart(bar) + it } }

    protected fun Performance.sing(melody: List<SungNote>) = melody.forEach {
        instruments.voice(track, at(it.beat), it.midiNote, seconds(it.lengthBeats), it.vowel)
    }

    protected fun Performance.sibilants(melody: List<SungNote>) = melody.filter { it.sibilant }.forEach {
        instruments.sibilant(track, before(it.beat, Instruments.SIBILANT_SECONDS * 0.8))
    }
}

/** Pop / balada: 96 BPM, La menor – Fa – Do – Sol. */
object PopSong : Song(tempoBpm = 96.0) {
    private val chords = listOf(listOf(57, 60, 64), listOf(53, 57, 60), listOf(55, 60, 64), listOf(55, 59, 62))
    private val bassRoots = listOf(33, 29, 36, 31)
    private val bassPattern = listOf(0, null, 0, 0, null, 0, 12, 0)
    private val melody = listOf(
        SungNote(0.0, 1.8, 76, Vowel.A, sibilant = true),
        SungNote(2.0, 1.8, 72, Vowel.O, sibilant = false),
        SungNote(4.0, 1.8, 69, Vowel.E, sibilant = false),
        SungNote(6.0, 1.8, 72, Vowel.A, sibilant = true),
        SungNote(8.0, 1.8, 67, Vowel.O, sibilant = true),
        SungNote(10.0, 1.8, 76, Vowel.E, sibilant = false),
        SungNote(12.0, 1.8, 74, Vowel.A, sibilant = false),
        SungNote(14.0, 1.8, 71, Vowel.O, sibilant = true),
    )

    override val levelsDb = mapOf(
        MusicStem.Kick to -2.0, MusicStem.Bass to -4.0, MusicStem.Snare to -8.0, MusicStem.HiHat to -15.0,
        MusicStem.Cymbals to -19.0, MusicStem.Harmony to -11.0, MusicStem.Lead to -6.0, MusicStem.Sibilance to -16.0,
    )
    override val reverbSends = mapOf(
        MusicStem.Snare to 0.2, MusicStem.HiHat to 0.05, MusicStem.Cymbals to 0.1,
        MusicStem.Harmony to 0.35, MusicStem.Lead to 0.3, MusicStem.Sibilance to 0.2,
    )

    override fun play(stem: MusicStem, performance: Performance) = with(performance) {
        when (stem) {
            MusicStem.Kick -> (0 until bars).flatMap { bar ->
                listOfNotNull(0.0, 2.0, if (bar % 2 == 1) 2.5 else null).map { barStart(bar) + it }
            }.forEach { instruments.kick(track, at(it)) }
            MusicStem.Snare -> everyBar(listOf(1.0, 3.0)).forEach { instruments.snare(track, at(it)) }
            MusicStem.HiHat -> (0 until bars * 8).forEach { step ->
                instruments.hiHat(track, at(step * 0.5), open = step % 8 == 7)
            }
            MusicStem.Cymbals -> {
                (0 until bars * beatsPerBar).forEach { instruments.ride(track, at(it.toDouble())) }
                instruments.crash(track, at(0.0))
            }
            MusicStem.Bass -> bassRoots.forEachIndexed { bar, root ->
                bassPattern.forEachIndexed { step, interval ->
                    if (interval != null) {
                        instruments.bass(track, at(barStart(bar) + step * 0.5), root + interval, seconds(0.45))
                    }
                }
            }
            MusicStem.Harmony -> chords.forEachIndexed { bar, notes ->
                instruments.pad(track, at(barStart(bar)), notes, seconds(beatsPerBar.toDouble()))
            }
            MusicStem.Lead -> sing(melody)
            MusicStem.Sibilance -> sibilants(melody)
        }
    }
}

/** Rock: 120 BPM, Mi menor – Do – Sol – Re, con el bombo al frente y guitarras distorsionadas. */
object RockSong : Song(tempoBpm = 120.0) {
    private val powerChords = listOf(listOf(40, 47, 52), listOf(48, 55, 60), listOf(43, 50, 55), listOf(50, 57, 62))
    private val bassRoots = listOf(28, 36, 31, 38)
    private val melody = listOf(
        SungNote(0.0, 1.5, 71, Vowel.A, sibilant = false),
        SungNote(2.0, 1.5, 67, Vowel.O, sibilant = true),
        SungNote(4.0, 1.5, 72, Vowel.E, sibilant = true),
        SungNote(6.0, 1.5, 67, Vowel.A, sibilant = false),
        SungNote(8.0, 1.5, 74, Vowel.A, sibilant = false),
        SungNote(10.0, 1.5, 71, Vowel.O, sibilant = true),
        SungNote(12.0, 1.5, 69, Vowel.E, sibilant = false),
        SungNote(14.0, 1.5, 66, Vowel.A, sibilant = true),
    )

    override val levelsDb = mapOf(
        MusicStem.Kick to 0.0, MusicStem.Bass to -6.0, MusicStem.Snare to -4.0, MusicStem.HiHat to -15.0,
        MusicStem.Cymbals to -16.0, MusicStem.Harmony to -10.0, MusicStem.Lead to -8.0, MusicStem.Sibilance to -16.0,
    )
    override val reverbSends = mapOf(
        MusicStem.Snare to 0.15, MusicStem.Cymbals to 0.1, MusicStem.Harmony to 0.1,
        MusicStem.Lead to 0.25, MusicStem.Sibilance to 0.15,
    )
    override val reverbWet = 0.2

    override fun play(stem: MusicStem, performance: Performance) = with(performance) {
        when (stem) {
            MusicStem.Kick -> (0 until bars).flatMap { bar ->
                listOfNotNull(0.0, 2.0, 2.5, if (bar == bars - 1) 3.5 else null).map { barStart(bar) + it }
            }.forEach { instruments.kick(track, at(it), KickStyle.Punchy) }
            MusicStem.Snare -> everyBar(listOf(1.0, 3.0)).forEach { instruments.snare(track, at(it), crack = 1.4) }
            MusicStem.HiHat -> (0 until bars * 8).forEach { instruments.hiHat(track, at(it * 0.5)) }
            MusicStem.Cymbals -> listOf(0, 2).forEach { instruments.crash(track, at(barStart(it))) }
            MusicStem.Bass -> bassRoots.forEachIndexed { bar, root ->
                (0 until 8).forEach { step ->
                    instruments.bass(track, at(barStart(bar) + step * 0.5), root, seconds(0.45), brightness = 1.6)
                }
            }
            MusicStem.Harmony -> powerChords.forEachIndexed { bar, notes ->
                instruments.distortedGuitar(track, at(barStart(bar)), notes, seconds(0.95), palmMuted = false)
                (2 until 8).forEach { step ->
                    val chug = at(barStart(bar) + step * 0.5)
                    instruments.distortedGuitar(track, chug, notes, seconds(0.4), palmMuted = true)
                }
            }
            MusicStem.Lead -> sing(melody)
            MusicStem.Sibilance -> sibilants(melody)
        }
    }
}

/**
 * Clásica: 72 BPM, Re – La – Si menor – Sol, orquesta de cuerdas en una sala amplia. Timbales en
 * lugar de bombo y un platillo suspendido que crece hacia el inicio del bucle.
 */
object ClassicalSong : Song(tempoBpm = 72.0) {
    private val bassRoots = listOf(38, 33, 35, 31)
    private val chords = listOf(listOf(57, 62, 66), listOf(57, 61, 64), listOf(59, 62, 66), listOf(59, 62, 67))
    private val violin = listOf(
        Note(0.0, 2.0, 78), Note(2.0, 1.0, 81), Note(3.0, 1.0, 79),
        Note(4.0, 2.0, 76), Note(6.0, 2.0, 73),
        Note(8.0, 1.0, 74), Note(9.0, 1.0, 78), Note(10.0, 2.0, 83),
        Note(12.0, 2.0, 81), Note(14.0, 1.0, 79), Note(15.0, 1.0, 78),
    )

    override val levelsDb = mapOf(
        MusicStem.Kick to -6.0, MusicStem.Bass to -6.0, MusicStem.Cymbals to -14.0,
        MusicStem.Harmony to -10.0, MusicStem.Lead to -6.0,
    )
    override val reverbSends = mapOf(
        MusicStem.Kick to 0.3, MusicStem.Bass to 0.3, MusicStem.Cymbals to 0.4,
        MusicStem.Harmony to 0.5, MusicStem.Lead to 0.45,
    )
    override val reverbWet = 0.45

    override fun play(stem: MusicStem, performance: Performance) = with(performance) {
        when (stem) {
            MusicStem.Kick -> listOf(0 to 38, 1 to 33, 2 to 38).forEach { (bar, note) ->
                instruments.timpani(track, at(barStart(bar)), note)
            }
            MusicStem.Bass -> bassRoots.forEachIndexed { bar, root ->
                instruments.strings(track, at(barStart(bar)), root, seconds(3.8), attackSeconds = 0.2)
                instruments.strings(track, at(barStart(bar)), root + 12, seconds(3.8), attackSeconds = 0.2)
            }
            MusicStem.Harmony -> chords.forEachIndexed { bar, notes ->
                notes.forEach { instruments.strings(track, at(barStart(bar)), it, seconds(3.8), attackSeconds = 0.4) }
            }
            MusicStem.Lead -> violin.forEach {
                val length = seconds(it.lengthBeats * 0.95)
                instruments.strings(track, at(it.beat), it.midiNote, length, attackSeconds = 0.08)
            }
            MusicStem.Cymbals -> instruments.cymbalSwell(track, at(barStart(3)), swellSeconds = seconds(4.0))
            else -> Unit
        }
    }
}

/** Electrónica (house): 124 BPM, bombo en cada pulso, aplausos, bajo a contratiempo y arpegio. */
object ElectronicSong : Song(tempoBpm = 124.0) {
    private val chords = listOf(listOf(57, 60, 64), listOf(53, 57, 60), listOf(55, 60, 64), listOf(55, 59, 62))
    private val bassRoots = listOf(33, 29, 36, 31)

    override val levelsDb = mapOf(
        MusicStem.Kick to -1.0, MusicStem.Bass to -6.0, MusicStem.Snare to -6.0, MusicStem.HiHat to -15.0,
        MusicStem.Cymbals to -16.0, MusicStem.Harmony to -12.0, MusicStem.Lead to -12.0,
    )
    override val reverbSends = mapOf(MusicStem.Snare to 0.2, MusicStem.Harmony to 0.3, MusicStem.Lead to 0.25)

    override fun play(stem: MusicStem, performance: Performance) = with(performance) {
        when (stem) {
            MusicStem.Kick -> (0 until bars * beatsPerBar).forEach {
                instruments.kick(track, at(it.toDouble()), KickStyle.Punchy)
            }
            MusicStem.Snare -> everyBar(listOf(1.0, 3.0)).forEach { instruments.clap(track, at(it)) }
            MusicStem.HiHat -> (0 until bars * beatsPerBar).forEach { beat ->
                instruments.hiHat(track, at(beat + 0.5), open = true)
                instruments.hiHat(track, at(beat + 0.25))
                instruments.hiHat(track, at(beat + 0.75))
            }
            MusicStem.Cymbals -> instruments.crash(track, at(0.0))
            MusicStem.Bass -> bassRoots.forEachIndexed { bar, root ->
                (0 until beatsPerBar).forEach { beat ->
                    instruments.bass(track, at(barStart(bar) + beat + 0.5), root + 12, seconds(0.4), brightness = 1.8)
                }
            }
            MusicStem.Harmony -> chords.forEachIndexed { bar, notes ->
                instruments.pad(track, at(barStart(bar)), notes, seconds(beatsPerBar.toDouble()), detuneCents = 15.0)
            }
            MusicStem.Lead -> chords.forEachIndexed { bar, notes ->
                val arpeggio = notes.map { it + 12 } + (notes.first() + 24)
                (0 until 8).forEach { step ->
                    instruments.synthLead(track, at(barStart(bar) + step * 0.5), arpeggio[step % 4], seconds(0.4))
                }
            }
            else -> Unit
        }
    }
}

/** Hip-hop: 88 BPM, Do menor – La♭ – Mi♭ – Si♭, bombo largo, 808 y voz rítmica con muchas "s". */
object HipHopSong : Song(tempoBpm = 88.0) {
    private val chords = listOf(listOf(60, 63, 67), listOf(56, 60, 63), listOf(55, 58, 63), listOf(58, 62, 65))
    private val subRoots = listOf(36, 32, 39, 34)
    private val flowBeats = listOf(0.0, 0.5, 1.0, 1.5, 2.5, 3.0)
    private val flowNotes = listOf(67, 67, 65, 63, 65, 67)
    private val vowels = listOf(Vowel.A, Vowel.E, Vowel.O, Vowel.A, Vowel.U, Vowel.E)
    private val flow = (0 until 4).flatMap { bar ->
        flowBeats.mapIndexed { index, beat ->
            val vowel = vowels[(index + bar) % vowels.size]
            SungNote(bar * 4 + beat, 0.4, flowNotes[index], vowel, sibilant = index % 2 == 0)
        }
    }

    override val levelsDb = mapOf(
        MusicStem.Kick to -2.0, MusicStem.Bass to -5.0, MusicStem.Snare to -5.0, MusicStem.HiHat to -15.0,
        MusicStem.Harmony to -12.0, MusicStem.Lead to -8.0, MusicStem.Sibilance to -15.0,
    )
    override val reverbSends = mapOf(
        MusicStem.Snare to 0.15, MusicStem.Harmony to 0.2, MusicStem.Lead to 0.15, MusicStem.Sibilance to 0.1,
    )
    override val reverbWet = 0.2

    override fun play(stem: MusicStem, performance: Performance) = with(performance) {
        when (stem) {
            MusicStem.Kick -> everyBar(listOf(0.0, 1.75, 2.5)).forEach {
                instruments.kick(track, at(it), KickStyle.Boom)
            }
            MusicStem.Snare -> everyBar(listOf(1.0, 3.0)).forEach { instruments.snare(track, at(it), crack = 1.1) }
            MusicStem.HiHat -> (0 until bars).forEach { bar ->
                val steps = (0 until 7).map { it * 0.5 } + listOf(3.5, 3.625, 3.75, 3.875)
                steps.forEach { instruments.hiHat(track, at(barStart(bar) + it)) }
            }
            MusicStem.Bass -> subRoots.forEachIndexed { bar, root ->
                instruments.sub808(track, at(barStart(bar)), root, seconds(1.5))
                instruments.sub808(track, at(barStart(bar) + 2.5), root, seconds(1.2))
            }
            MusicStem.Harmony -> chords.forEachIndexed { bar, notes ->
                listOf(0.0, 2.0).forEach {
                    instruments.piano(track, at(barStart(bar) + it), notes, seconds(1.6), velocity = 0.8)
                }
            }
            MusicStem.Lead -> sing(flow)
            MusicStem.Sibilance -> sibilants(flow)
            else -> Unit
        }
    }
}

/** Jazz: 120 BPM con swing, Sol m7 – Do7 – Fa maj7 – Re m7, contrabajo caminante y piano. */
object JazzSong : Song(tempoBpm = 120.0) {
    /** Corchea con swing: la segunda cae a 2/3 del pulso, no a la mitad. */
    private const val SWING = 2.0 / 3.0
    private val chords = listOf(
        listOf(53, 58, 62, 67),
        listOf(52, 58, 62, 64),
        listOf(52, 57, 60, 64),
        listOf(53, 57, 60, 64),
    )
    private val walkingBass = listOf(43, 45, 46, 47, 48, 46, 45, 43, 41, 45, 48, 52, 50, 48, 45, 44)
    private val scat = listOf(
        SungNote(0.0, 1.0, 70, Vowel.U, false), SungNote(1 + SWING, 0.3, 69, Vowel.A, false),
        SungNote(2.0, 1.5, 67, Vowel.O, false), SungNote(4.0, 1.0, 64, Vowel.U, false),
        SungNote(5 + SWING, 0.3, 65, Vowel.A, false), SungNote(6.0, 1.5, 67, Vowel.O, false),
        SungNote(8.0, 2.0, 69, Vowel.U, false), SungNote(10 + SWING, 1.0, 72, Vowel.A, false),
        SungNote(12.0, 1.0, 74, Vowel.U, false), SungNote(13 + SWING, 0.3, 72, Vowel.A, false),
        SungNote(14.0, 1.8, 69, Vowel.O, false),
    )

    override val levelsDb = mapOf(
        MusicStem.Kick to -14.0, MusicStem.Bass to -4.0, MusicStem.Snare to -12.0, MusicStem.HiHat to -16.0,
        MusicStem.Cymbals to -14.0, MusicStem.Harmony to -10.0, MusicStem.Lead to -8.0,
    )
    override val reverbSends = mapOf(
        MusicStem.Snare to 0.2, MusicStem.Cymbals to 0.1, MusicStem.Harmony to 0.25, MusicStem.Lead to 0.3,
    )

    override fun play(stem: MusicStem, performance: Performance) = with(performance) {
        when (stem) {
            MusicStem.Kick -> (0 until bars * beatsPerBar).forEach { instruments.kick(track, at(it.toDouble())) }
            MusicStem.Snare -> everyBar(listOf(1.0, 3.0)).forEach { instruments.brush(track, at(it)) }
            MusicStem.HiHat -> everyBar(listOf(1.0, 3.0)).forEach { instruments.hiHat(track, at(it)) }
            MusicStem.Cymbals -> everyBar(listOf(0.0, 1.0, 1 + SWING, 2.0, 3.0, 3 + SWING)).forEach {
                instruments.ride(track, at(it))
            }
            MusicStem.Bass -> walkingBass.forEachIndexed { beat, note ->
                instruments.uprightBass(track, at(beat.toDouble()), note, seconds(0.9))
            }
            MusicStem.Harmony -> chords.forEachIndexed { bar, notes ->
                instruments.piano(track, at(barStart(bar)), notes, seconds(0.6), velocity = 0.9)
                instruments.piano(track, at(barStart(bar) + 1 + SWING), notes, seconds(0.5), velocity = 0.7)
            }
            MusicStem.Lead -> sing(scat)
            else -> Unit
        }
    }
}

/** Géneros disponibles para el test. */
enum class Genre(val song: Song) {
    Pop(PopSong),
    Rock(RockSong),
    Classical(ClassicalSong),
    Electronic(ElectronicSong),
    HipHop(HipHopSong),
    Jazz(JazzSong),
}
