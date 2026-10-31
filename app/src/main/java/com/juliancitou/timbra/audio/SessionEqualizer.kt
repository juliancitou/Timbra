package com.juliancitou.timbra.audio

import android.media.audiofx.DynamicsProcessing

enum class SessionEqualizerResult { Applied, Blocked, Failed }

private const val STEREO_CHANNELS = 2

/** Por encima de cualquier otra app, para poder comparar sin que otro ecualizador mande. */
private const val EFFECT_PRIORITY = Int.MAX_VALUE
private const val LIMITER_LINK_GROUP = 0
private const val LIMITER_ATTACK_MS = 1f
private const val LIMITER_RELEASE_MS = 60f
private const val LIMITER_RATIO = 10f
private const val LIMITER_THRESHOLD_DB = -1f
private const val LIMITER_POST_GAIN_DB = 0f

/**
 * Ecualiza la sesión de audio de otra app con `DynamicsProcessing`, en vivo y sin copiar su audio.
 * El volumen lo decide esa app y el teléfono, así que aquí no aplica el nivel máximo de
 * [SafetyLimiter]. La regla es otra: nunca sonar más fuerte que sin ecualizar. Por eso la ganancia
 * de entrada no pasa de 0 dB y un limitador al final contiene los picos de las bandas que suben.
 *
 * No es seguro entre hilos: se usa siempre desde el mismo hilo.
 */
class SessionEqualizer {
    private var effect: DynamicsProcessing? = null
    private var attachedSession = 0
    private var appliedBands: List<PreviewBand> = emptyList()

    fun apply(sessionId: Int, preampDb: Float, bands: List<PreviewBand>): SessionEqualizerResult = runCatching {
        val current = effectFor(sessionId, bands.size)
        if (!current.hasControl()) return SessionEqualizerResult.Blocked
        current.setInputGainAllChannelsTo(preampDb.coerceAtMost(0f))
        // Cada banda es una llamada a otro proceso: solo se envían las que cambiaron.
        bands.forEachIndexed { index, band ->
            if (band != appliedBands.getOrNull(index)) {
                current.setPreEqBandAllChannelsTo(index, DynamicsProcessing.EqBand(true, band.upperEdgeHz, band.gainDb))
            }
        }
        appliedBands = bands
        current.enabled = true
        SessionEqualizerResult.Applied
    }.getOrElse {
        release()
        SessionEqualizerResult.Failed
    }

    fun release() {
        runCatching { effect?.release() }
        effect = null
        attachedSession = 0
        appliedBands = emptyList()
    }

    private fun effectFor(sessionId: Int, bandCount: Int): DynamicsProcessing {
        effect?.takeIf { attachedSession == sessionId && appliedBands.size == bandCount }?.let { return it }
        release()
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            STEREO_CHANNELS,
            true,
            bandCount,
            false,
            0,
            false,
            0,
            true,
        ).build()
        val created = DynamicsProcessing(EFFECT_PRIORITY, sessionId, config)
        created.setLimiterAllChannelsTo(
            DynamicsProcessing.Limiter(
                true,
                true,
                LIMITER_LINK_GROUP,
                LIMITER_ATTACK_MS,
                LIMITER_RELEASE_MS,
                LIMITER_RATIO,
                LIMITER_THRESHOLD_DB,
                LIMITER_POST_GAIN_DB,
            ),
        )
        effect = created
        attachedSession = sessionId
        return created
    }
}
