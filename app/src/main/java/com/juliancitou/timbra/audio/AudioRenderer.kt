package com.juliancitou.timbra.audio

import java.util.concurrent.atomic.AtomicReference

/** Tiempo máximo de sonido continuo antes del paro automático. No subirlo sin que el usuario lo pida. */
const val AUTO_STOP_SECONDS = 20 * 60

/**
 * Cadena completa de audio, en este orden fijo: fuente → ecualizador (con fundido al cambiar) →
 * entrada/salida de 50 ms → [SafetyLimiter]. Nada llega a la salida sin pasar por el limitador. Cada
 * canal tiene su ecualizador, porque los filtros guardan memoria de sus muestras anteriores.
 *
 * Los controles ([play], [stop], [changeEqualization], [changeSource]) se pueden llamar desde
 * cualquier hilo: solo dejan una indicación. [render] corre solo en el hilo de audio, es el único
 * que modifica el estado y aplica las indicaciones al inicio de cada buffer. Cambiar de fuente
 * baja el sonido, cambia y lo vuelve a subir.
 */
class AudioRenderer(private val sampleRate: Int, initialSource: SignalSource) {
    private val limiter = SafetyLimiter(sampleRate)
    private val ramp = GainRamp(sampleRate)
    private val leftEqualizer = EqualizerCrossfader(sampleRate)
    private val rightEqualizer = EqualizerCrossfader(sampleRate)
    private val autoStopSamples = AUTO_STOP_SECONDS.toLong() * sampleRate

    private var source = initialSource
    private val pendingSource = AtomicReference<SignalSource?>(null)
    private val pendingEqualization = AtomicReference<EqualizerSettings?>(null)

    @Volatile private var wantsSound = false
    @Volatile private var restartRequested = false
    private var audibleSamples = 0L

    /** `true` si se detuvo solo por llegar a [AUTO_STOP_SECONDS]. Se limpia al volver a [play]. */
    @Volatile var stoppedByTimeLimit = false
        private set

    /** `true` cuando ya terminó la salida suave: la salida de audio se puede pausar. */
    @Volatile var isSilent: Boolean = true
        private set

    /**
     * `true` si nadie pidió sonido y ya terminó la salida suave. A diferencia de [isSilent], cambia
     * en cuanto se llama a [play], aunque todavía no se haya procesado el siguiente buffer.
     */
    val isIdle: Boolean get() = !wantsSound && isSilent

    fun play() {
        stoppedByTimeLimit = false
        restartRequested = true
        wantsSound = true
    }

    fun stop() {
        wantsSound = false
    }

    fun changeEqualization(settings: EqualizerSettings) = pendingEqualization.set(settings)

    fun changeSource(newSource: SignalSource) = pendingSource.set(newSource)

    /** Llena [buffer] con [frames] cuadros estéreo intercalados (izquierdo, derecho). */
    fun render(buffer: FloatArray, frames: Int) {
        pendingEqualization.getAndSet(null)?.let {
            leftEqualizer.change(it)
            rightEqualizer.change(it)
        }
        if (restartRequested) {
            restartRequested = false
            audibleSamples = 0
        }
        for (frame in 0 until frames) {
            val swapping = pendingSource.get() != null
            if (swapping && ramp.isSilent) pendingSource.getAndSet(null)?.let { source = it }
            if (wantsSound && !swapping) ramp.fadeIn() else ramp.fadeOut()

            val leftInput = source.next()
            val rightInput = source.rightOf(leftInput)
            val gain = ramp.next()
            limiter.processStereo(
                leftEqualizer.process(leftInput) * gain,
                rightEqualizer.process(rightInput) * gain,
                buffer,
                2 * frame,
            )

            if (wantsSound && ++audibleSamples >= autoStopSamples) {
                wantsSound = false
                stoppedByTimeLimit = true
            }
        }
        isSilent = !wantsSound && ramp.isSilent
    }
}
