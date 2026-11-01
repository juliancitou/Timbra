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
 * pasada gruesa (dB) y el rango permitido. Subir más de +3 dB en graves hace que el limitador de
 * Poweramp "bombee" el volumen. Los pasos son lo bastante grandes para oírse con música: 1 dB en la
 * pasada de afinado es casi el mínimo que se distingue, y con 0.5 dB dos opciones sonaban igual. Cada
 * rango debe caber en cuatro pasos gruesos, para que siempre haya cinco opciones distintas.
 */
enum class FinderDimension(
    val focus: MusicFocus,
    val coarseStepDb: Double,
    val rangeDb: ClosedFloatingPointRange<Double>,
) {
    /** Inclinación de toda la curva: negativo es más cálido, positivo más brillante. */
    Balance(MusicFocus.Balanced, coarseStepDb = 3.0, rangeDb = -6.0..6.0),
    Bass(MusicFocus.Bass, coarseStepDb = 2.0, rangeDb = -6.0..3.0),

    /** El "lodo" entre 150 y 400 Hz: bajarlo limpia voces y piano. */
    Clarity(MusicFocus.Clarity, coarseStepDb = 2.0, rangeDb = -6.0..2.0),
    Voice(MusicFocus.Voice, coarseStepDb = 2.0, rangeDb = -5.0..3.0),

    /** La "S" y el "tss" de los platillos, entre 5 y 10 kHz. */
    Sibilance(MusicFocus.Sibilance, coarseStepDb = 2.0, rangeDb = -8.0..2.0),

    /** Casi no se oye arriba de 14 kHz, así que necesita más paso y empezar antes que el resto. */
    Air(MusicFocus.Air, coarseStepDb = 2.5, rangeDb = -7.0..3.0);

    /** Curva de esta zona sobre [frequenciesHz] cuando vale [valueDb], en la salida [output]. */
    fun curve(
        valueDb: Double,
        frequenciesHz: List<Int> = POWERAMP_BAND_FREQUENCIES_HZ,
        output: OutputKind = OutputKind.Wired,
    ): List<Double> = when (this) {
        Balance -> sum(
            lowShelf(400.0, -valueDb / 2, transitionOctaves = 2.0, frequenciesHz = frequenciesHz),
            highShelf(2500.0, valueDb / 2, transitionOctaves = 2.0, frequenciesHz = frequenciesHz),
        )
        Bass -> lowShelf(output.bassCornerHz, valueDb, frequenciesHz = frequenciesHz)
        Clarity -> bell(output.clarityCenterHz, valueDb, widthOctaves = 1.5, frequenciesHz = frequenciesHz)
        Voice -> bell(2000.0, valueDb, widthOctaves = 1.5, frequenciesHz = frequenciesHz)
        Sibilance -> bell(7000.0, valueDb, widthOctaves = 1.2, frequenciesHz = frequenciesHz)
        Air -> highShelf(9_500.0, valueDb, frequenciesHz = frequenciesHz)
    }
}

/**
 * Una ronda: qué zona se ajusta y si es la pasada de afinado (pasos de la mitad de tamaño). [recheck]
 * es la comprobación final: la misma zona otra vez, sin marcar cuál es la actual y con los pasos
 * gruesos, para ver si se vuelve a elegir lo mismo.
 */
data class FinderRound(val dimension: FinderDimension, val fine: Boolean, val recheck: Boolean = false) {
    val stepDb: Double get() = if (fine && !recheck) dimension.coarseStepDb / 2 else dimension.coarseStepDb
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
    /** Zonas donde la comprobación final no coincidió con lo elegido antes. */
    val doubts: Set<FinderDimension> = emptySet(),
    /** Curva de la que parte el test, como la medición de los audífonos; las zonas se suman encima. */
    val baseCurveDb: List<Double> = emptyList(),
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
        val center = (currentValueDb + recheckOffsetDb()).coerceIn(range.start + half, range.endInclusive - half)
        return (0 until OPTIONS_PER_ROUND).map { index -> center + (index - OPTIONS_PER_ROUND / 2) * step }
    }

    /**
     * Guarda la elección y avanza. En la comprobación final no reemplaza lo anterior: una sola escucha
     * puede engañar, así que queda el punto medio de las dos, y si difieren más de un paso se anota
     * la duda.
     */
    fun choose(valueDb: Double): EqualizerFinder {
        check(!isFinished) { "El test ya terminó" }
        val dimension = round.dimension
        val value = if (round.recheck) (currentValueDb + valueDb) / 2 else valueDb
        val disagrees = round.recheck && abs(valueDb - currentValueDb) > round.stepDb
        return copy(
            valuesDb = valuesDb + (dimension to value.coerceIn(output.rangeDb(dimension))),
            roundIndex = roundIndex + 1,
            previous = this,
            doubts = if (disagrees) doubts + dimension else doubts,
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

    /** En la comprobación, lo actual no queda en el centro: así no se elige por la posición. */
    private fun recheckOffsetDb(): Double {
        if (!round.recheck) return 0.0
        return round.stepDb * if (round.dimension.ordinal % 2 == 0) 1 else -1
    }

    private fun curveFor(values: Map<FinderDimension, Double>): List<Double> {
        val frequencies = layout.frequenciesHz
        val start = baseCurveDb.ifEmpty { List(frequencies.size) { 0.0 } }
        return values.entries.fold(start) { total, (dimension, value) ->
            sum(total, dimension.curve(value, frequencies, output))
        }
    }
}
