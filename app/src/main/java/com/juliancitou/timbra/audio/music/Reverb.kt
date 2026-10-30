package com.juliancitou.timbra.audio.music

/**
 * Reverberación mono tipo Schroeder/Freeverb: cuatro filtros peine en paralelo (las reflexiones
 * de una sala) y dos pasa-todo en serie (difuminan los ecos para que no suenen como rebotes
 * separados). Los retardos están en muestras a 44.1 kHz y se escalan a la frecuencia real; son
 * primos entre sí para que los ecos no coincidan y suene natural.
 */
class Reverb(sampleRate: Int) {
    private val scale = sampleRate / 44_100.0
    private val combs = listOf(1116, 1188, 1277, 1356).map { Comb((it * scale).toInt()) }
    private val allPasses = listOf(556, 441).map { AllPass((it * scale).toInt()) }

    fun process(input: Double): Double {
        var output = combs.sumOf { it.process(input) } / combs.size
        for (allPass in allPasses) output = allPass.process(output)
        return output
    }

    private class Comb(length: Int) {
        private val buffer = DoubleArray(length)
        private var index = 0
        private var filtered = 0.0

        fun process(input: Double): Double {
            val output = buffer[index]
            filtered = output * (1 - DAMPING) + filtered * DAMPING
            buffer[index] = input + filtered * FEEDBACK
            index = (index + 1) % buffer.size
            return output
        }
    }

    private class AllPass(length: Int) {
        private val buffer = DoubleArray(length)
        private var index = 0

        fun process(input: Double): Double {
            val delayed = buffer[index]
            buffer[index] = input + delayed * ALL_PASS_GAIN
            index = (index + 1) % buffer.size
            return delayed - input * ALL_PASS_GAIN
        }
    }

    private companion object {
        /** Cola de ~1.2 s: sala mediana, suficiente para dar ambiente sin empastar. */
        const val FEEDBACK = 0.78
        /** Las paredes absorben agudos: cada reflexión sale un poco más opaca. */
        const val DAMPING = 0.3
        const val ALL_PASS_GAIN = 0.5
    }
}
