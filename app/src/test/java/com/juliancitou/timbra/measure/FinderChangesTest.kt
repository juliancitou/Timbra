package com.juliancitou.timbra.measure

import org.junit.Assert.assertEquals
import org.junit.Test

class FinderChangesTest {
    @Test
    fun groupsWhatWasAddedRemovedAndLeftAlone() {
        val finder = EqualizerFinder(
            valuesDb = mapOf(
                FinderDimension.Bass to -2.0,
                FinderDimension.Sibilance to -4.0,
                FinderDimension.Voice to 1.5,
                FinderDimension.Air to 0.25,
            ),
        )
        val changes = finder.changes()
        assertEquals(listOf(FinderDimension.Voice), changes.added.map { it.dimension })
        assertEquals(listOf(FinderDimension.Sibilance, FinderDimension.Bass), changes.removed.map { it.dimension })
        assertEquals(listOf(FinderDimension.Air), changes.unchanged)
    }

    @Test
    fun aFlatResultChangesNothing() {
        val changes = EqualizerFinder().changes()
        assertEquals(emptyList<ZoneChange>(), changes.added + changes.removed)
        assertEquals(FinderDimension.entries, changes.unchanged)
    }
}
