package com.juliancitou.timbra.measure

import kotlin.math.abs

/** Menos que esto, en dB, casi no se distingue de oído. */
private const val NOTICEABLE_CHANGE_DB = 0.5

data class ZoneChange(val dimension: FinderDimension, val valueDb: Double)

/** Lo que cambió respecto al sonido original, con lo más grande primero en cada grupo. */
data class FinderChanges(
    val added: List<ZoneChange>,
    val removed: List<ZoneChange>,
    val unchanged: List<FinderDimension>,
)

fun EqualizerFinder.changes(): FinderChanges {
    val zones = valuesDb.entries.map { (dimension, valueDb) -> ZoneChange(dimension, valueDb) }
        .sortedByDescending { abs(it.valueDb) }
    return FinderChanges(
        added = zones.filter { it.valueDb >= NOTICEABLE_CHANGE_DB },
        removed = zones.filter { it.valueDb <= -NOTICEABLE_CHANGE_DB },
        unchanged = zones.filter { abs(it.valueDb) < NOTICEABLE_CHANGE_DB }.map { it.dimension }.sortedBy { it.ordinal },
    )
}
