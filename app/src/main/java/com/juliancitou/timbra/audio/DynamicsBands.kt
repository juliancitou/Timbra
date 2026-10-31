package com.juliancitou.timbra.audio

import kotlin.math.sqrt

/**
 * `DynamicsProcessing` define cada banda por dónde termina y aplica su ganancia hasta ahí, no por su
 * centro. El límite entre dos bandas va en la media geométrica de sus centros, que es el punto medio
 * al oído. La última llega hasta [lastEdgeHz] para cubrir todo lo que quede arriba de ella.
 */
fun bandUpperEdgesHz(centersHz: List<Int>, lastEdgeHz: Float = NO_UPPER_LIMIT_HZ): List<Float> =
    centersHz.zipWithNext { lower, upper -> sqrt(lower.toFloat() * upper) } +
        maxOf(lastEdgeHz, centersHz.last().toFloat())

/** Más allá de lo que cualquier teléfono reproduce. */
private const val NO_UPPER_LIMIT_HZ = 24_000f
