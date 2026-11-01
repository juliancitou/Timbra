package com.juliancitou.timbra.measure

const val BLIND_TRIALS = 5
const val BLIND_OPTIONS = 2

/** Con 5 intentos, acertar 4 por azar pasa cerca de 1 de cada 5 veces: es una señal, no una prueba estadística. */
private const val CLEAR_PREFERENCE_TRIALS = 4

enum class BlindVerdict { PrefersEqualization, PrefersOriginal, NoClearPreference }

/**
 * Prueba a ciegas: en cada intento, una de dos opciones sin nombre es la ecualización y la otra el sonido
 * original, y se elige la que gusta más. Evita preferir el resultado solo por saber cuál es.
 */
data class BlindTest(
    /** Cuál de las dos opciones es la ecualización en este intento. */
    val equalizedOption: Int,
    val trial: Int = 0,
    val choseEqualization: Int = 0,
) {
    val isFinished: Boolean get() = trial >= BLIND_TRIALS

    val verdict: BlindVerdict
        get() = when {
            choseEqualization >= CLEAR_PREFERENCE_TRIALS -> BlindVerdict.PrefersEqualization
            BLIND_TRIALS - choseEqualization >= CLEAR_PREFERENCE_TRIALS -> BlindVerdict.PrefersOriginal
            else -> BlindVerdict.NoClearPreference
        }

    fun choose(option: Int, nextEqualizedOption: Int): BlindTest = copy(
        equalizedOption = nextEqualizedOption,
        trial = trial + 1,
        choseEqualization = choseEqualization + if (option == equalizedOption) 1 else 0,
    )
}
