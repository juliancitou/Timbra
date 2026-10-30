package com.juliancitou.timbra.audio.music

/**
 * La pieza del test: 4 compases de 4/4 a 96 BPM (10 s exactos) con la progresión
 * La menor – Fa – Do – Sol, muy común en pop y baladas: suena familiar sin ser ninguna canción.
 * Los tiempos van en pulsos (1 pulso = una negra); las notas, en MIDI.
 */
object Arrangement {
    const val TEMPO_BPM = 96.0
    const val BEATS_PER_BAR = 4
    const val BARS = 4
    const val BEAT_SECONDS = 60.0 / TEMPO_BPM
    const val LOOP_SECONDS = BEAT_SECONDS * BEATS_PER_BAR * BARS

    data class Note(val beat: Double, val lengthBeats: Double, val midiNote: Int)
    data class SungNote(
        val beat: Double,
        val lengthBeats: Double,
        val midiNote: Int,
        val vowel: Vowel,
        val sibilant: Boolean,
    )

    /** Acordes por compás (La3–Mi4 aprox.): Am, F, C/G, G. */
    private val chords = listOf(listOf(57, 60, 64), listOf(53, 57, 60), listOf(55, 60, 64), listOf(55, 59, 62))

    /** Fundamental del bajo por compás: La1, Fa1, Do2, Sol1 (55, 44, 65 y 49 Hz). */
    private val bassRoots = listOf(33, 29, 36, 31)

    /** Patrón de corcheas del bajo: 0 = fundamental, 12 = octava arriba, null = silencio. */
    private val bassPattern = listOf(0, null, 0, 0, null, 0, 12, 0)

    val padChords: List<Pair<Double, List<Int>>> = chords.mapIndexed { bar, notes -> barStart(bar) to notes }

    val bassNotes: List<Note> = bassRoots.flatMapIndexed { bar, root ->
        bassPattern.mapIndexedNotNull { step, interval ->
            interval?.let { Note(barStart(bar) + step * 0.5, lengthBeats = 0.45, midiNote = root + it) }
        }
    }

    /** Bombo en 1 y 3, y un golpe extra en el "y" del 3 en los compases pares. */
    val kickBeats: List<Double> = (0 until BARS).flatMap { bar ->
        listOfNotNull(0.0, 2.0, if (bar % 2 == 1) 2.5 else null).map { barStart(bar) + it }
    }

    val snareBeats: List<Double> = (0 until BARS).flatMap { bar -> listOf(1.0, 3.0).map { barStart(bar) + it } }

    /** Hi-hat en corcheas; el último del compás va abierto. */
    val hiHats: List<Pair<Double, Boolean>> = (0 until BARS).flatMap { bar ->
        (0 until 8).map { step -> barStart(bar) + step * 0.5 to (step == 7) }
    }

    val rideBeats: List<Double> = (0 until BARS * BEATS_PER_BAR).map { it.toDouble() }

    val crashBeats: List<Double> = listOf(0.0)

    /** Melodía: dos notas largas por compás sobre el acorde; algunas empiezan con "s" ("sa", "so"). */
    val melody: List<SungNote> = listOf(
        SungNote(barStart(0), 1.8, 76, Vowel.A, sibilant = true),
        SungNote(barStart(0) + 2, 1.8, 72, Vowel.O, sibilant = false),
        SungNote(barStart(1), 1.8, 69, Vowel.E, sibilant = false),
        SungNote(barStart(1) + 2, 1.8, 72, Vowel.A, sibilant = true),
        SungNote(barStart(2), 1.8, 67, Vowel.O, sibilant = true),
        SungNote(barStart(2) + 2, 1.8, 76, Vowel.E, sibilant = false),
        SungNote(barStart(3), 1.8, 74, Vowel.A, sibilant = false),
        SungNote(barStart(3) + 2, 1.8, 71, Vowel.O, sibilant = true),
    )

    private fun barStart(bar: Int) = (bar * BEATS_PER_BAR).toDouble()
}
