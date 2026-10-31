package com.juliancitou.timbra.export

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Bandas gráficas personalizadas de Poweramp Equalizer ("Habilitar bandas personalizadas"): [count]
 * bandas repartidas de forma logarítmica entre [firstHz] y [lastHz], cada una redondeada al entero.
 * Los límites son los que permite Poweramp. La configuración por defecto (25 bandas de 20 Hz a
 * 20 kHz) da exactamente las bandas del preset del usuario.
 */
data class BandLayout(val count: Int = 25, val firstHz: Int = 20, val lastHz: Int = 20_000) {
    init {
        require(count in COUNT_RANGE) { "Poweramp admite de ${COUNT_RANGE.first} a ${COUNT_RANGE.last} bandas" }
        require(firstHz in FIRST_HZ_RANGE) { "La primera frecuencia va de 5 a 200 Hz" }
        require(lastHz in LAST_HZ_RANGE) { "La última frecuencia va de 14 a 20 kHz" }
    }

    /** `f_k = primera · (última/primera)^(k/(N−1))`, redondeado al entero como lo muestra Poweramp. */
    val frequenciesHz: List<Int> by lazy {
        val ratio = lastHz.toDouble() / firstHz
        (0 until count).map { k -> (firstHz * ratio.pow(k.toDouble() / (count - 1))).roundToInt() }
    }

    /** Separación entre bandas vecinas, en octavas. */
    val spacingOctaves: Double get() = kotlin.math.log2(lastHz.toDouble() / firstHz) / (count - 1)

    companion object {
        val COUNT_RANGE = 5..32
        val FIRST_HZ_RANGE = 5..200
        val LAST_HZ_RANGE = 14_000..20_000
        val Default = BandLayout()
    }
}
