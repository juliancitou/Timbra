package com.juliancitou.timbra.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackStatus {
    Stopped,
    Playing,
    PausedByOtherApp,
    PausedHeadphonesDisconnected,
    StoppedByTimeLimit,
}

private const val FALLBACK_SAMPLE_RATE = 48_000
private const val CHANNELS = 2
private const val BYTES_PER_SAMPLE = 4

/** Cuadros por bloque de render: ~10 ms a 48 kHz, para que los cambios se oigan casi al instante. */
private const val FRAMES_PER_BLOCK = 480
private const val RELEASE_TIMEOUT_MS = 500L

/**
 * Salida de audio real. Envía lo que produce [AudioRenderer] a un `AudioTrack` (PCM float, estéreo,
 * frecuencia nativa del teléfono) desde un hilo con prioridad de audio que solo vive mientras hay
 * sonido. Pide el foco de audio y respeta su pérdida (otra app, una llamada) sin reanudar sola, y
 * pausa en cuanto se desconectan los audífonos. Nunca cambia el volumen del teléfono.
 */
class AudioOutput(context: Context, sourceFactory: (sampleRate: Int) -> SignalSource) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)

    val sampleRate: Int = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
        ?: FALLBACK_SAMPLE_RATE

    val renderer = AudioRenderer(sampleRate, sourceFactory(sampleRate))

    private val statusFlow = MutableStateFlow(PlaybackStatus.Stopped)
    val status: StateFlow<PlaybackStatus> = statusFlow.asStateFlow()

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { change ->
            if (change < 0) pauseWith(PlaybackStatus.PausedByOtherApp)
        }
        .build()

    private val track: AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(attributes)
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setSampleRate(sampleRate)
                .build(),
        )
        .setBufferSizeInBytes(bufferSizeInBytes())
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            pauseWith(PlaybackStatus.PausedHeadphonesDisconnected)
        }
    }

    val sessionId: Int get() = track.audioSessionId

    private val lock = Any()
    private var renderThread: Thread? = null
    private var receiverRegistered = false
    private var released = false

    /** Empieza a sonar con entrada suave. Devuelve `false` si otra app no cede el audio. */
    fun play(): Boolean {
        synchronized(lock) {
            if (released) return false
            val focus = audioManager.requestAudioFocus(focusRequest)
            if (focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false
            registerNoisyReceiver()
            renderer.play()
            statusFlow.value = PlaybackStatus.Playing
            if (renderThread == null) startRenderThread()
            return true
        }
    }

    /** Detiene con salida suave. */
    fun stop() = pauseWith(PlaybackStatus.Stopped)

    /** Detiene, espera la salida suave y libera el `AudioTrack`. La instancia ya no se puede usar. */
    fun release() {
        stop()
        val thread = synchronized(lock) {
            released = true
            renderThread
        }
        thread?.join(RELEASE_TIMEOUT_MS)
        track.release()
    }

    private fun pauseWith(newStatus: PlaybackStatus) {
        renderer.stop()
        statusFlow.value = newStatus
        audioManager.abandonAudioFocusRequest(focusRequest)
        unregisterNoisyReceiver()
    }

    private fun startRenderThread() {
        renderThread = Thread(::renderLoop, "TimbraAudio").also { it.start() }
    }

    private fun renderLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val buffer = FloatArray(FRAMES_PER_BLOCK * CHANNELS)
        track.play()
        while (true) {
            renderer.render(buffer, FRAMES_PER_BLOCK)
            track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
            val finished = synchronized(lock) {
                if (renderer.isIdle) {
                    track.pause()
                    track.flush()
                    renderThread = null
                    true
                } else {
                    false
                }
            }
            if (finished) break
        }
        if (renderer.stoppedByTimeLimit) pauseWith(PlaybackStatus.StoppedByTimeLimit)
    }

    private fun registerNoisyReceiver() {
        if (receiverRegistered) return
        ContextCompat.registerReceiver(
            appContext,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun unregisterNoisyReceiver() {
        synchronized(lock) {
            if (!receiverRegistered) return
            appContext.unregisterReceiver(noisyReceiver)
            receiverRegistered = false
        }
    }

    private fun bufferSizeInBytes(): Int {
        val minimum = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        return maxOf(minimum, FRAMES_PER_BLOCK * CHANNELS * BYTES_PER_SAMPLE * 4)
    }
}
