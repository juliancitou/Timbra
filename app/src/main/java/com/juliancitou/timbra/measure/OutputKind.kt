package com.juliancitou.timbra.measure

private val RECHECKED_ZONES = listOf(FinderDimension.Bass, FinderDimension.Voice)

/**
 * Salida para la que se busca la ecualización. Cambia de dónde parte el test y hasta dónde llega.
 *
 * - [Bluetooth] parte de la tendencia de los HUAWEI FreeBuds Pro 4: subgrave reforzado, algo de
 *   "lodo" y un pico de sibilancia, con la voz un poco abajo.
 * - [Wired] parte de plano porque puede ser cualquier audífono.
 * - [Speaker] recorta graves que la bocina no reproduce (subirlos solo la satura), por eso su ronda
 *   de graves nunca los sube, suaviza la zona chillona y omite la ronda de aire, que casi no se percibe en ella.
 *   Una bocina de teléfono casi no da sonido bajo ~300 Hz: sus graves y su claridad actúan más arriba,
 *   o esas rondas no se oirían.
 */
enum class OutputKind(
    private val startDb: Map<FinderDimension, Double> = emptyMap(),
    private val rangesDb: Map<FinderDimension, ClosedFloatingPointRange<Double>> = emptyMap(),
    private val skipped: Set<FinderDimension> = emptySet(),
    val bassCornerHz: Double = 120.0,
    val clarityCenterHz: Double = 250.0,
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
            FinderDimension.Bass to -8.0..0.0,
            FinderDimension.Clarity to -6.0..2.0,
            FinderDimension.Voice to -5.0..3.0,
            FinderDimension.Sibilance to -7.0..1.0,
        ),
        skipped = setOf(FinderDimension.Air),
        bassCornerHz = 300.0,
        clarityCenterHz = 500.0,
    );

    /**
     * Cada zona tiene una pasada gruesa seguida de una de afinado. Al final se comprueban graves y voz,
     * que son las zonas que más cambian lo que se oye.
     */
    val rounds: List<FinderRound> by lazy {
        val zones = FinderDimension.entries.filterNot { it in skipped }.flatMap { dimension ->
            listOf(FinderRound(dimension, fine = false), FinderRound(dimension, fine = true))
        }
        zones + RECHECKED_ZONES.map { FinderRound(it, fine = true, recheck = true) }
    }

    fun rangeDb(dimension: FinderDimension): ClosedFloatingPointRange<Double> =
        rangesDb[dimension] ?: dimension.rangeDb

    /** Con una curva medida de partida, ninguna zona empieza movida: la medición ya corrige los audífonos. */
    fun neutralValuesDb(): Map<FinderDimension, Double> = startValuesDb().mapValues { 0.0 }

    fun startValuesDb(): Map<FinderDimension, Double> =
        FinderDimension.entries.filterNot { it in skipped }.associateWith { startDb[it] ?: 0.0 }
}
