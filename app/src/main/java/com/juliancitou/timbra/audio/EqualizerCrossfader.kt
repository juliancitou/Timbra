package com.juliancitou.timbra.audio

/**
 * Cambia de ecualización sin clics. Cambiar los coeficientes de golpe produce un salto en la
 * señal; en su lugar, durante [FADE_SECONDS] se procesan la ecualización anterior y la nueva a
 * la vez y se pasa de una a otra con [raisedCosine]. Las dos salen de la misma señal (están
 * correlacionadas), así que los pesos suman 1 en lugar de usar potencia constante, que daría
 * un abultamiento de +3 dB a la mitad.
 *
 * No es seguro entre hilos: [change] y [process] se llaman desde el hilo de audio.
 */
class EqualizerCrossfader(private val sampleRate: Int, initial: EqualizerSettings = EqualizerSettings.Flat) {
    private val step = 1.0 / (FADE_SECONDS * sampleRate)
    private var current = GraphicEqualizer(sampleRate, initial)
    private var previous: GraphicEqualizer? = null
    private var progress = 1.0

    val settings: EqualizerSettings get() = current.settings

    fun change(settings: EqualizerSettings) {
        if (settings == current.settings) return
        previous = current
        current = GraphicEqualizer(sampleRate, settings)
        progress = 0.0
    }

    fun process(sample: Double): Double {
        val outgoing = previous ?: return current.process(sample)
        progress = minOf(1.0, progress + step)
        val incomingWeight = raisedCosine(progress)
        val mixed = outgoing.process(sample) * (1 - incomingWeight) + current.process(sample) * incomingWeight
        if (progress >= 1.0) previous = null
        return mixed
    }
}
