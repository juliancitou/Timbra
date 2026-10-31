package com.juliancitou.timbra.measure

import com.juliancitou.timbra.audio.EqualizerSettings
import com.juliancitou.timbra.audio.POWERAMP_MIN_GAIN_DB
import com.juliancitou.timbra.audio.powerampPreampDb
import com.juliancitou.timbra.export.BandLayout

private const val BANDS_BETWEEN_DIPS = 4

/**
 * Preset para comparar de oído contra Poweramp: una banda de cada [BANDS_BETWEEN_DIPS] al mínimo y las
 * demás en 0 dB. Con caídas aisladas se nota el ancho de cada banda; con una curva suave casi no.
 */
fun calibrationPreset(layout: BandLayout): EqualizerSettings {
    val gains = List(layout.count) { band ->
        if (band % BANDS_BETWEEN_DIPS == BANDS_BETWEEN_DIPS / 2) POWERAMP_MIN_GAIN_DB else 0f
    }
    return EqualizerSettings(gains, preampDb = powerampPreampDb(gains), bandFrequenciesHz = layout.frequenciesHz)
}
