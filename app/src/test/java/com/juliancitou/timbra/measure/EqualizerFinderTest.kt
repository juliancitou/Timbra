package com.juliancitou.timbra.measure

import com.juliancitou.timbra.audio.POWERAMP_MAX_GAIN_DB
import com.juliancitou.timbra.audio.POWERAMP_MIN_GAIN_DB
import com.juliancitou.timbra.export.BandLayout
import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

private const val MIN_CHANGED_BANDS = 3

class EqualizerFinderTest {

    private fun gainAt(curve: List<Double>, hz: Int) = curve[POWERAMP_BAND_FREQUENCIES_HZ.indexOf(hz)]

    @Test
    fun roundsAreCoarseThenFineAndEndWithChecks() {
        assertEquals(14, OutputKind.Wired.rounds.size)
        assertEquals(FinderRound(FinderDimension.Balance, fine = false), OutputKind.Wired.rounds[0])
        assertEquals(FinderRound(FinderDimension.Balance, fine = true), OutputKind.Wired.rounds[1])
        assertEquals(FinderRound(FinderDimension.Air, fine = true), OutputKind.Wired.rounds[11])
        assertTrue(OutputKind.Wired.rounds.takeLast(2).all { it.recheck })
    }

    @Test
    fun optionsAreCenteredOnTheCurrentChoice() {
        val options = EqualizerFinder().options()
        assertEquals(OPTIONS_PER_ROUND, options.size)
        assertEquals(listOf(-6.0, -3.0, 0.0, 3.0, 6.0), options)
    }

    @Test
    fun fineRoundUsesHalfSteps() {
        val afterCoarse = EqualizerFinder().choose(3.0)
        assertTrue(afterCoarse.round.fine)
        assertEquals(listOf(0.0, 1.5, 3.0, 4.5, 6.0), afterCoarse.options())
    }

    @Test
    fun optionsShiftInsideTheRangeNearItsEdge() {
        val bassRound = OutputKind.Wired.rounds.indexOf(FinderRound(FinderDimension.Bass, fine = false))
        val finder = EqualizerFinder(roundIndex = bassRound).let {
            it.copy(valuesDb = it.valuesDb + (FinderDimension.Bass to 3.0))
        }
        val options = finder.options()
        assertEquals(3.0, options.last(), 1e-9)
        assertEquals(OPTIONS_PER_ROUND, options.distinct().size)
        assertTrue(options.all { it in OutputKind.Wired.rangeDb(FinderDimension.Bass) })
    }

    @Test
    fun choosingAdvancesAndBackRestoresTheEarlierState() {
        val start = EqualizerFinder()
        val next = start.choose(-1.5)
        assertEquals(1, next.roundIndex)
        assertEquals(-1.5, next.valuesDb.getValue(FinderDimension.Balance), 0.0)
        assertEquals(start, next.previous)
        assertNull(start.previous)
    }

    @Test
    fun finishesAfterTheLastRound() {
        var finder = EqualizerFinder()
        repeat(OutputKind.Wired.rounds.size) {
            assertFalse(finder.isFinished)
            finder = finder.choose(finder.currentValueDb)
        }
        assertTrue(finder.isFinished)
    }

    @Test
    fun brighterBalanceTiltsTheCurveUpward() {
        val curve = FinderDimension.Balance.curve(4.0)
        assertTrue(gainAt(curve, 20) < 0)
        assertTrue(gainAt(curve, 20000) > 0)
    }

    @Test
    fun eachZoneActsWhereItShould() {
        assertEquals(-4.0, gainAt(FinderDimension.Bass.curve(-4.0), 20), 0.1)
        assertEquals(-4.0, gainAt(FinderDimension.Clarity.curve(-4.0), 267), 0.2)
        assertEquals(2.0, gainAt(FinderDimension.Voice.curve(2.0), 2000), 0.01)
        assertTrue(gainAt(FinderDimension.Sibilance.curve(-6.0), 6325) < -5)
        assertEquals(0.0, gainAt(FinderDimension.Sibilance.curve(-6.0), 200), 0.01)
        assertEquals(3.0, gainAt(FinderDimension.Air.curve(3.0), 20000), 0.2)
    }

    @Test
    fun candidateChangesOnlyTheCurrentZone() {
        val finder = EqualizerFinder().choose(0.0).choose(0.0)
        assertEquals(FinderDimension.Bass, finder.round.dimension)
        val withCandidate = finder.curveWithCandidateDb(3.0)
        assertTrue(gainAt(withCandidate, 20) > 2.5)
        assertEquals(0.0, gainAt(withCandidate, 4743), 0.05)
    }

    @Test
    fun resultFollowsThePowerampContractEvenWithExtremeChoices() {
        var finder = EqualizerFinder()
        repeat(OutputKind.Wired.rounds.size) { finder = finder.choose(finder.options().last()) }
        val preset = finder.result()
        assertTrue(preset.bandGainsDb.all { it in POWERAMP_MIN_GAIN_DB..POWERAMP_MAX_GAIN_DB })
        assertTrue(preset.preampDb <= -4.5f)
    }

    @Test
    fun bluetoothStartsFromTheFreeBudsTendency() {
        val finder = EqualizerFinder(output = OutputKind.Bluetooth)
        assertEquals(-3.0, finder.valuesDb.getValue(FinderDimension.Sibilance), 0.0)
        assertEquals(-2.0, finder.valuesDb.getValue(FinderDimension.Clarity), 0.0)
    }

    @Test
    fun wiredStartsFlat() {
        assertTrue(EqualizerFinder(output = OutputKind.Wired).curveDb().all { it == 0.0 })
    }

    @Test
    fun speakerNeverBoostsBassAndHasNoAirRound() {
        val speaker = OutputKind.Speaker
        assertEquals(12, speaker.rounds.size)
        assertTrue(speaker.rounds.none { it.dimension == FinderDimension.Air })
        var finder = EqualizerFinder(output = speaker)
        repeat(speaker.rounds.size) {
            if (finder.round.dimension == FinderDimension.Bass) assertTrue(finder.options().all { it <= 0.0 })
            finder = finder.choose(finder.options().last())
        }
        assertTrue(finder.isFinished)
        assertTrue(finder.valuesDb.getValue(FinderDimension.Bass) <= 0.0)
    }

    @Test
    fun everyOutputOffersFiveDistinctOptionsInsideItsRanges() {
        OutputKind.entries.forEach { output ->
            var finder = EqualizerFinder(output = output)
            repeat(output.rounds.size) {
                val options = finder.options()
                assertEquals(output.name, OPTIONS_PER_ROUND, options.distinct().size)
                assertTrue(output.name, options.all { it in output.rangeDb(finder.round.dimension) })
                finder = finder.choose(options.first())
            }
        }
    }

    @Test
    fun resultUsesTheChosenBands() {
        val layout = BandLayout(count = 10, firstHz = 31, lastHz = 16_000)
        var finder = EqualizerFinder(layout = layout)
        repeat(finder.rounds.size) { finder = finder.choose(finder.options()[1]) }
        val preset = finder.result()
        assertEquals(layout.frequenciesHz, preset.bandFrequenciesHz)
        assertEquals(10, preset.bandGainsDb.size)
    }

    @Test
    fun noneInTheCoarsePassKeepsTheZoneAndSkipsItsFinePass() {
        val start = EqualizerFinder()
        val next = start.keepCurrent()
        assertEquals(start.valuesDb, next.valuesDb)
        assertEquals(FinderRound(FinderDimension.Bass, fine = false), next.round)
        assertEquals(start, next.previous)
    }

    @Test
    fun noneInTheFinePassKeepsWhatWasChosenInTheCoarsePass() {
        val next = EqualizerFinder().choose(3.0).keepCurrent()
        assertEquals(3.0, next.valuesDb.getValue(FinderDimension.Balance), 0.0)
        assertEquals(FinderRound(FinderDimension.Bass, fine = false), next.round)
    }

    @Test
    fun rejectingEveryRoundFinishesWithTheStartingCurve() {
        OutputKind.entries.forEach { output ->
            val start = EqualizerFinder(output = output)
            var finder = start
            while (!finder.isFinished) finder = finder.keepCurrent()
            assertEquals(start.curveDb(), finder.curveDb())
        }
    }

    @Test
    fun currentOptionIsFoundEvenNearARangeEdge() {
        val bassRound = OutputKind.Wired.rounds.indexOf(FinderRound(FinderDimension.Bass, fine = false))
        val finder = EqualizerFinder(roundIndex = bassRound).let {
            it.copy(valuesDb = it.valuesDb + (FinderDimension.Bass to 3.0))
        }
        assertEquals(3.0, finder.options()[finder.currentOptionIndex], 1e-9)
    }

    private fun atFirstRecheck(): EqualizerFinder {
        var finder = EqualizerFinder()
        while (!finder.round.recheck) finder = finder.choose(finder.currentValueDb)
        return finder
    }

    @Test
    fun recheckDoesNotPutTheCurrentValueInTheCenter() {
        val finder = atFirstRecheck()
        assertTrue(finder.options().any { abs(it - finder.currentValueDb) < 1e-9 })
        assertTrue(abs(finder.options()[OPTIONS_PER_ROUND / 2] - finder.currentValueDb) > 0.1)
    }

    @Test
    fun recheckAveragesAndNotesADoubtWhenItDisagrees() {
        val finder = atFirstRecheck()
        val current = finder.currentValueDb
        val far = finder.options().maxBy { abs(it - current) }
        val next = finder.choose(far)
        assertEquals((current + far) / 2, next.valuesDb.getValue(finder.round.dimension), 1e-9)
        assertTrue(finder.round.dimension in next.doubts)
    }

    @Test
    fun recheckWithTheSameChoiceChangesNothing() {
        val finder = atFirstRecheck()
        val next = finder.choose(finder.currentValueDb)
        assertEquals(finder.valuesDb, next.valuesDb)
        assertTrue(next.doubts.isEmpty())
    }

    /** Lo que de verdad se exporta, ya redondeado: dos opciones vecinas no pueden sonar igual. */
    @Test
    fun neighbouringOptionsChangeEnoughToBeHeard() {
        OutputKind.entries.forEach { output ->
            var finder = EqualizerFinder(output = output)
            repeat(output.rounds.size) {
                val round = finder.round
                val presets = finder.options().map { finder.presetWithCandidate(it).bandGainsDb }
                presets.zipWithNext { lower, upper ->
                    val changes = lower.zip(upper) { a, b -> abs(a - b) }
                    val label = "$output $round"
                    assertTrue(label, changes.max() >= if (round.fine && !round.recheck) 0.5f else 1f)
                    assertTrue(label, changes.count { it > 0f } >= MIN_CHANGED_BANDS)
                }
                finder = finder.choose(finder.currentValueDb)
            }
        }
    }

    @Test
    fun speakerBassActsWhereTheSpeakerCanPlayIt() {
        val headphones = FinderDimension.Bass.curve(-4.0)
        val speaker = FinderDimension.Bass.curve(-4.0, output = OutputKind.Speaker)
        assertTrue(gainAt(speaker, 267) < gainAt(headphones, 267) - 1.5)
    }

    @Test
    fun calibrationPresetFollowsTheContract() {
        val preset = calibrationPreset(BandLayout.Default)
        assertTrue(preset.bandGainsDb.any { it == -6f })
        assertEquals(0f, gainAt(preset.bandGainsDb.map { it.toDouble() }, 2000).toFloat(), 0f)
        assertEquals(-4.5f, preset.preampDb, 0f)
    }
}
