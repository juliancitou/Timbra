package com.juliancitou.timbra.measure

import com.juliancitou.timbra.audio.EqualizerSettings
import com.juliancitou.timbra.audio.powerampPreampDb
import com.juliancitou.timbra.export.BandLayout

private const val MEASURE_CUT_DB = -6f
private val MeasuredCutsHz = listOf(300..800, 3500..10_000)

/**
 * Preset para medir a Poweramp: dos mesetas de −6 dB con una zona en 0 dB entre ellas, que sirve de
 * referencia. Son anchas porque es lo que suele traer un preset real: los cortes angostos salen aún más
 * suaves en Poweramp y darían una fuerza menor a la que se oye con música.
 */
fun calibrationPreset(layout: BandLayout): EqualizerSettings {
    val gains = layout.frequenciesHz.map { hz -> if (MeasuredCutsHz.any { hz in it }) MEASURE_CUT_DB else 0f }
    return EqualizerSettings(gains, preampDb = powerampPreampDb(gains), bandFrequenciesHz = layout.frequenciesHz)
}
