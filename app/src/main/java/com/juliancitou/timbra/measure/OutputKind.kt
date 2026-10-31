package com.juliancitou.timbra.measure

/**
 * Salida para la que se busca la ecualización. Cambia de dónde parte el test y hasta dónde llega.
 *
 * - [Bluetooth] parte de la tendencia de los HUAWEI FreeBuds Pro 4: subgrave reforzado, algo de
 *   "lodo" y un pico de sibilancia, con la voz un poco abajo.
 * - [Wired] parte de plano porque puede ser cualquier audífono.
 * - [Speaker] recorta graves que la bocina no reproduce (subirlos solo la satura), por eso nunca
 *   los sube, suaviza la zona chillona y omite la ronda de aire, que casi no se percibe en ella.
 */
enum class OutputKind(
    private val startDb: Map<FinderDimension, Double> = emptyMap(),
    private val rangesDb: Map<FinderDimension, ClosedFloatingPointRange<Double>> = emptyMap(),
    private val skipped: Set<FinderDimension> = emptySet(),
) {
    Bluetooth(
        startDb = mapOf(
            FinderDimension.Bass to -1.0,
            FinderDimension.Clarity to -2.0,
            FinderDimension.Voice to 1.0,
            FinderDimension.Sibilance to -3.0,
            FinderDimension.Air to -1.0,
        ),
    ),
    Wired,
    Speaker(
        startDb = mapOf(FinderDimension.Bass to -4.0, FinderDimension.Voice to -1.0),
        rangesDb = mapOf(
            FinderDimension.Balance to -4.5..3.0,
            FinderDimension.Bass to -8.0..0.0,
            FinderDimension.Clarity to -4.0..2.0,
            FinderDimension.Voice to -4.0..2.0,
            FinderDimension.Sibilance to -6.0..1.0,
        ),
        skipped = setOf(FinderDimension.Air),
    );

    /** Cada zona tiene una pasada gruesa seguida de una de afinado. */
    val rounds: List<FinderRound> by lazy {
        FinderDimension.entries.filterNot { it in skipped }.flatMap { dimension ->
            listOf(FinderRound(dimension, fine = false), FinderRound(dimension, fine = true))
        }
    }

    fun rangeDb(dimension: FinderDimension): ClosedFloatingPointRange<Double> =
        rangesDb[dimension] ?: dimension.rangeDb

    fun startValuesDb(): Map<FinderDimension, Double> =
        FinderDimension.entries.filterNot { it in skipped }.associateWith { startDb[it] ?: 0.0 }
}
