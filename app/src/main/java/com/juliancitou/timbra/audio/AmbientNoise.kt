package com.juliancitou.timbra.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

private const val RECORD_RATE_HZ = 16_000
private const val SKIPPED_SAMPLES = RECORD_RATE_HZ / 4
private const val MEASURED_SAMPLES = 2 * RECORD_RATE_HZ
private const val SILENCE_DBFS = -120f

/**
 * Más que esto ya distrae del test. Es orientativo: el micrófono de cada teléfono tiene otra
 * sensibilidad y no está calibrado, así que no equivale a decibeles reales del cuarto.
 */
const val QUIET_ROOM_MAX_DBFS = -45f

/** Nivel RMS de [samples], en dB respecto a la escala completa. */
fun rmsDbfs(samples: ShortArray): Float {
    if (samples.isEmpty()) return SILENCE_DBFS
    val sumOfSquares = samples.sumOf { it.toDouble() * it.toDouble() }
    val rms = sqrt(sumOfSquares / samples.size) / Short.MAX_VALUE
    return max(SILENCE_DBFS.toDouble(), 20 * log10(max(rms, 1e-9))).toFloat()
}

/**
 * Oye el micrófono unos 2 segundos y devuelve solo su nivel. El audio no se guarda ni sale del teléfono.
 * Es bloqueante. Quien llama debe haber comprobado el permiso; `null` si no se pudo grabar.
 */
@SuppressLint("MissingPermission")
fun measureAmbientNoiseDbfs(): Float? = runCatching {
    val bufferBytes = AudioRecord.getMinBufferSize(
        RECORD_RATE_HZ,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    )
    // Esta fuente evita el control automático de ganancia, que subiría el ruido de un cuarto callado.
    val record = AudioRecord(
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        RECORD_RATE_HZ,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferBytes,
    )
    try {
        record.startRecording()
        val skipped = ShortArray(SKIPPED_SAMPLES)
        record.read(skipped, 0, skipped.size)
        val samples = ShortArray(MEASURED_SAMPLES)
        var filled = 0
        while (filled < samples.size) {
            val read = record.read(samples, filled, samples.size - filled)
            if (read <= 0) return@runCatching null
            filled += read
        }
        rmsDbfs(samples)
    } finally {
        record.release()
    }
}.getOrNull()
