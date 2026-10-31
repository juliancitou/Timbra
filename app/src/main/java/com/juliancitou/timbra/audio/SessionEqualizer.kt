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
    private var attachedBands = 0

    fun apply(sessionId: Int, settings: EqualizerSettings): SessionEqualizerResult = runCatching {
        val current = effectFor(sessionId, settings.bandGainsDb.size)
        if (!current.hasControl()) return SessionEqualizerResult.Blocked
        current.setInputGainAllChannelsTo(settings.preampDb.coerceAtMost(0f))
        val edges = bandUpperEdgesHz(settings.bandFrequenciesHz)
        settings.bandGainsDb.forEachIndexed { band, gainDb ->
            current.setPreEqBandAllChannelsTo(band, DynamicsProcessing.EqBand(true, edges[band], gainDb))
        }
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
        attachedBands = 0
    }

    private fun effectFor(sessionId: Int, bandCount: Int): DynamicsProcessing {
        effect?.takeIf { attachedSession == sessionId && attachedBands == bandCount }?.let { return it }
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
        attachedBands = bandCount
        return created
    }
}
