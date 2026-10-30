package com.juliancitou.timbra.measure

import com.juliancitou.timbra.audio.POWERAMP_MAX_GAIN_DB
import com.juliancitou.timbra.audio.POWERAMP_MIN_GAIN_DB
import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerFinderTest {

    private fun gainAt(curve: List<Double>, hz: Int) = curve[POWERAMP_BAND_FREQUENCIES_HZ.indexOf(hz)]

    @Test
    fun thereAreTwelveRoundsCoarseThenFine() {
        assertEquals(12, FINDER_ROUNDS.size)
        assertEquals(FinderRound(FinderDimension.Balance, fine = false), FINDER_ROUNDS[0])
        assertEquals(FinderRound(FinderDimension.Balance, fine = true), FINDER_ROUNDS[1])
        assertEquals(FinderDimension.Air, FINDER_ROUNDS.last().dimension)
    }

    @Test
    fun optionsAreCenteredOnTheCurrentChoice() {
        val options = EqualizerFinder().options()
        assertEquals(OPTIONS_PER_ROUND, options.size)
        assertEquals(listOf(-3.0, -1.5, 0.0, 1.5, 3.0), options)
    }

    @Test
    fun fineRoundUsesHalfSteps() {
        val afterCoarse = EqualizerFinder().choose(1.5)
        assertTrue(afterCoarse.round.fine)
        assertEquals(listOf(0.0, 0.75, 1.5, 2.25, 3.0), afterCoarse.options())
    }

    @Test
    fun optionsShiftInsideTheRangeNearItsEdge() {
        val bassRound = FINDER_ROUNDS.indexOf(FinderRound(FinderDimension.Bass, fine = false))
        val finder = EqualizerFinder(roundIndex = bassRound).let {
            it.copy(valuesDb = it.valuesDb + (FinderDimension.Bass to 3.0))
        }
        val options = finder.options()
        assertEquals(3.0, options.last(), 1e-9)
        assertEquals(OPTIONS_PER_ROUND, options.distinct().size)
        assertTrue(options.all { it in FinderDimension.Bass.rangeDb })
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
        repeat(FINDER_ROUNDS.size) {
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
        repeat(FINDER_ROUNDS.size) { finder = finder.choose(finder.options().last()) }
        val preset = finder.result()
        assertTrue(preset.bandGainsDb.all { it in POWERAMP_MIN_GAIN_DB..POWERAMP_MAX_GAIN_DB })
        assertTrue(preset.preampDb <= -4.5f)
    }
}
