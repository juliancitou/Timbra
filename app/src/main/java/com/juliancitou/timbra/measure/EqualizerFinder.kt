package com.juliancitou.timbra.measure

import com.juliancitou.timbra.audio.EqualizerSettings
import com.juliancitou.timbra.audio.EqualizerShapes.bell
import com.juliancitou.timbra.audio.EqualizerShapes.highShelf
import com.juliancitou.timbra.audio.EqualizerShapes.lowShelf
import com.juliancitou.timbra.audio.EqualizerShapes.sum
import com.juliancitou.timbra.audio.music.MusicFocus
import com.juliancitou.timbra.audio.presetFromCurve
import com.juliancitou.timbra.export.BandLayout
import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import kotlin.math.abs

/** Opciones que se escuchan en cada ronda; la del centro es la elección actual. */
const val OPTIONS_PER_ROUND = 5

/**
 * Una zona del sonido que el test ajusta, con su música resaltada, el paso entre opciones en la
 * pasada gruesa (dB) y el rango permitido. Los rangos son conservadores: subir más de +3 dB en
 * graves hace que el limitador de Poweramp "bombee" el volumen.
 */
enum class FinderDimension(
    val focus: MusicFocus,
    val coarseStepDb: Double,
    val rangeDb: ClosedFloatingPointRange<Double>,
) {
    /** Inclinación de toda la curva: negativo es más cálido, positivo más brillante. */
    Balance(MusicFocus.Balanced, coarseStepDb = 1.5, rangeDb = -4.5..4.5),
    Bass(MusicFocus.Bass, coarseStepDb = 1.5, rangeDb = -6.0..3.0),

    /** El "lodo" entre 150 y 400 Hz: bajarlo limpia voces y piano. */
    Clarity(MusicFocus.Clarity, coarseStepDb = 1.0, rangeDb = -5.0..2.0),
    Voice(MusicFocus.Voice, coarseStepDb = 1.0, rangeDb = -3.0..3.0),

    /** La "S" y el "tss" de los platillos, entre 5 y 10 kHz. */
    Sibilance(MusicFocus.Sibilance, coarseStepDb = 1.5, rangeDb = -8.0..2.0),
    Air(MusicFocus.Air, coarseStepDb = 1.0, rangeDb = -5.0..3.0);

    /** Curva de esta zona sobre [frequenciesHz] cuando vale [valueDb]. */
    fun curve(valueDb: Double, frequenciesHz: List<Int> = POWERAMP_BAND_FREQUENCIES_HZ): List<Double> = when (this) {
        Balance -> sum(
            lowShelf(400.0, -valueDb / 2, transitionOctaves = 2.0, frequenciesHz = frequenciesHz),
            highShelf(2500.0, valueDb / 2, transitionOctaves = 2.0, frequenciesHz = frequenciesHz),
        )
        Bass -> lowShelf(120.0, valueDb, frequenciesHz = frequenciesHz)
        Clarity -> bell(250.0, valueDb, widthOctaves = 1.5, frequenciesHz = frequenciesHz)
        Voice -> bell(2000.0, valueDb, widthOctaves = 1.5, frequenciesHz = frequenciesHz)
        Sibilance -> bell(7000.0, valueDb, widthOctaves = 1.2, frequenciesHz = frequenciesHz)
        Air -> highShelf(11_000.0, valueDb, frequenciesHz = frequenciesHz)
    }
}

/** Una ronda: qué zona se ajusta y si es la pasada de afinado (pasos de la mitad de tamaño). */
data class FinderRound(val dimension: FinderDimension, val fine: Boolean) {
    val stepDb: Double get() = if (fine) dimension.coarseStepDb / 2 else dimension.coarseStepDb
}

/**
 * Estado del test "Encuentra tu ecualización". Es inmutable: cada elección devuelve un estado
 * nuevo, y [previous] permite volver a la ronda anterior con lo que se había elegido entonces.
 */
data class EqualizerFinder(
    val output: OutputKind = OutputKind.Wired,
    val layout: BandLayout = BandLayout.Default,
    val valuesDb: Map<FinderDimension, Double> = output.startValuesDb(),
    val roundIndex: Int = 0,
    val previous: EqualizerFinder? = null,
) {
    val rounds: List<FinderRound> get() = output.rounds
    val isFinished: Boolean get() = roundIndex >= rounds.size
    val round: FinderRound get() = rounds[roundIndex.coerceAtMost(rounds.lastIndex)]
    val currentValueDb: Double get() = valuesDb.getValue(round.dimension)

    /**
     * Las [OPTIONS_PER_ROUND] opciones de la ronda, en dB y de menor a mayor, centradas en la
     * elección actual. Si quedarían fuera del rango de la zona, el grupo entero se recorre hacia
     * dentro, así siempre hay cinco opciones distintas.
     */
    fun options(): List<Double> {
        val range = output.rangeDb(round.dimension)
        val step = round.stepDb
        val half = (OPTIONS_PER_ROUND / 2) * step
        val center = currentValueDb.coerceIn(range.start + half, range.endInclusive - half)
        return (0 until OPTIONS_PER_ROUND).map { index -> center + (index - OPTIONS_PER_ROUND / 2) * step }
    }

    fun choose(valueDb: Double): EqualizerFinder {
        check(!isFinished) { "El test ya terminó" }
        val dimension = round.dimension
        return copy(
            valuesDb = valuesDb + (dimension to valueDb.coerceIn(output.rangeDb(dimension))),
            roundIndex = roundIndex + 1,
            previous = this,
        )
    }

    /** Curva completa con lo elegido hasta ahora: la suma de todas las zonas. */
    fun curveDb(): List<Double> = curveFor(valuesDb)

    /** Curva completa si en la ronda actual se eligiera [candidateDb]; es lo que suena al tocar un nodo. */
    fun curveWithCandidateDb(candidateDb: Double): List<Double> = curveFor(valuesDb + (round.dimension to candidateDb))

    /** Cuál de las [options] es la que hay ahora; no siempre es la del centro cerca de un límite. */
    val currentOptionIndex: Int
        get() = options().withIndex().minBy { (_, option) -> abs(option - currentValueDb) }.index

    /**
     * "No me gustó ninguno": la zona se queda como estaba y se pasa a la siguiente. Si es la pasada
     * gruesa también salta su afinado, porque no tiene sentido afinar algo que no gustó.
     */
    fun keepCurrent(): EqualizerFinder =
        copy(roundIndex = roundIndex + if (round.fine) 1 else 2, previous = this)

    /** El preset que se exportaría si en la ronda actual se eligiera [candidateDb]. */
    fun presetWithCandidate(candidateDb: Double): EqualizerSettings =
        presetFromCurve(curveWithCandidateDb(candidateDb), layout.frequenciesHz)

    /** El preset final, con redondeo, rango y preamp de Poweramp. */
    fun result(): EqualizerSettings = presetFromCurve(curveDb(), layout.frequenciesHz)

    private fun curveFor(values: Map<FinderDimension, Double>): List<Double> {
        val frequencies = layout.frequenciesHz
        val zero = List(frequencies.size) { 0.0 }
        return values.entries.fold(zero) { total, (dimension, value) ->
            sum(total, dimension.curve(value, frequencies))
        }
    }
}
