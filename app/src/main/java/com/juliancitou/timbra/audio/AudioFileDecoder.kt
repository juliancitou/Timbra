package com.juliancitou.timbra.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Audio decodificado, siempre a dos canales. */
class DecodedAudio(val left: FloatArray, val right: FloatArray, val sampleRate: Int)

private const val DEQUEUE_TIMEOUT_US = 10_000L
private const val MICROSECONDS_PER_SECOND = 1_000_000L
private const val MAX_IDLE_ATTEMPTS = 500
private const val PCM_16_SCALE = 32_768f

/**
 * Lee hasta [maxSeconds] de un archivo de audio elegido por el usuario, con los códecs del teléfono.
 * Todo queda en memoria: el archivo no se copia ni se modifica. Lanza una excepción si el formato no
 * se puede leer, por ejemplo si el archivo está protegido.
 */
fun decodeAudioFile(context: Context, uri: Uri, maxSeconds: Int): DecodedAudio {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(context, uri, null)
        val track = (0 until extractor.trackCount).first { index ->
            extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")
        }
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME).orEmpty())
        try {
            format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_FLOAT)
            codec.configure(format, null, null, 0)
            codec.start()
            return drain(extractor, codec, format, maxSeconds)
        } finally {
            codec.release()
        }
    } finally {
        extractor.release()
    }
}

private fun drain(extractor: MediaExtractor, codec: MediaCodec, format: MediaFormat, maxSeconds: Int): DecodedAudio {
    val info = MediaCodec.BufferInfo()
    var collector: FrameCollector? = null
    var inputDone = false
    var idleAttempts = 0
    while (collector?.isFull != true && idleAttempts < MAX_IDLE_ATTEMPTS) {
        if (!inputDone) inputDone = feed(extractor, codec)
        val index = codec.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US)
        if (index < 0) {
            idleAttempts++
            continue
        }
        idleAttempts = 0
        val target = collector ?: FrameCollector.forOutput(codec.outputFormat, format, maxSeconds).also { collector = it }
        if (info.size > 0) target.append(requireNotNull(codec.getOutputBuffer(index)), info)
        codec.releaseOutputBuffer(index, false)
        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
    }
    return requireNotNull(collector) { "El archivo no tiene audio" }.finish()
}

/** Entrega un bloque comprimido al códec. Devuelve `true` al llegar al final del archivo. */
private fun feed(extractor: MediaExtractor, codec: MediaCodec): Boolean {
    val index = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
    if (index < 0) return false
    val size = extractor.readSampleData(requireNotNull(codec.getInputBuffer(index)), 0)
    if (size < 0) {
        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        return true
    }
    codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
    extractor.advance()
    return false
}

/** Junta los cuadros. Un archivo mono se duplica y de uno multicanal se usan los dos primeros. */
private class FrameCollector(
    private val channels: Int,
    private val encoding: Int,
    private val sampleRate: Int,
    capacity: Int,
) {
    private val left = FloatArray(capacity)
    private val right = FloatArray(capacity)
    private var frames = 0

    val isFull: Boolean get() = frames == left.size

    fun append(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        buffer.position(info.offset)
        buffer.limit(info.offset + info.size)
        val ordered = buffer.slice().order(ByteOrder.nativeOrder())
        when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> {
                val samples = ordered.asFloatBuffer()
                copy(samples.remaining()) { samples.get(it) }
            }
            AudioFormat.ENCODING_PCM_16BIT -> {
                val samples = ordered.asShortBuffer()
                copy(samples.remaining()) { samples.get(it) / PCM_16_SCALE }
            }
            else -> error("Codificación no admitida: $encoding")
        }
    }

    fun finish() = DecodedAudio(left.copyOf(frames), right.copyOf(frames), sampleRate)

    private inline fun copy(sampleCount: Int, sampleAt: (Int) -> Float) {
        val available = minOf(sampleCount / channels, left.size - frames)
        for (frame in 0 until available) {
            val first = frame * channels
            left[frames + frame] = sampleAt(first)
            right[frames + frame] = if (channels > 1) sampleAt(first + 1) else left[frames + frame]
        }
        frames += available
    }

    companion object {
        fun forOutput(output: MediaFormat, input: MediaFormat, maxSeconds: Int): FrameCollector {
            val sampleRate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val encoding = if (output.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                output.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                AudioFormat.ENCODING_PCM_16BIT
            }
            val limit = maxSeconds.toLong() * MICROSECONDS_PER_SECOND
            val duration = if (input.containsKey(MediaFormat.KEY_DURATION)) input.getLong(MediaFormat.KEY_DURATION) else limit
            val capacity = minOf(duration, limit) * sampleRate / MICROSECONDS_PER_SECOND + sampleRate
            return FrameCollector(
                output.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                encoding,
                sampleRate,
                minOf(capacity, maxSeconds.toLong() * sampleRate).toInt(),
            )
        }
    }
}
