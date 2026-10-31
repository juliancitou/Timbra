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
 * Duración del bloque de la FFT interna de `DynamicsProcessing`, en ms. Se redondea hacia arriba a
 * potencia de 2: unas 4096 muestras a 48 kHz, ~12 Hz por punto. Con los 10 ms por defecto (512
 * muestras, ~94 Hz por punto) casi todas las bandas graves caen en el mismo punto y dejan de oírse.
 * El costo es ~85 ms de retraso, que no importa en música.
 */
private const val PROCESSING_FRAME_MS = 85f

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

    /** Lee del efecto lo que de verdad tiene aplicado. `null` si no hay efecto o no se puede leer. */
    fun read(): EffectReading? = runCatching {
        val current = effect ?: return null
        EffectReading(
            enabled = current.enabled,
            hasControl = current.hasControl(),
            bands = appliedBands.indices.map { index ->
                val band = current.getPreEqBandByChannelIndex(0, index)
                PreviewBand(band.cutoffFrequency, band.gain)
            },
        )
    }.getOrNull()

    /**
     * Si otra app, como Poweramp, comparte el efecto, este sobrevive con los últimos ajustes. Se deja
     * plano para que el sonido de Timbra no se quede pegado después de salir del test.
     */
    fun release() {
        runCatching {
            effect?.let { current ->
                current.setInputGainAllChannelsTo(0f)
                appliedBands.forEachIndexed { index, band ->
                    current.setPreEqBandAllChannelsTo(index, DynamicsProcessing.EqBand(true, band.upperEdgeHz, 0f))
                }
            }
        }
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
        ).setPreferredFrameDuration(PROCESSING_FRAME_MS).build()
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
