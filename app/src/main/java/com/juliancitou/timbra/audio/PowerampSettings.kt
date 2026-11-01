package com.juliancitou.timbra.audio

import com.juliancitou.timbra.export.BandLayout

private const val KEY_AUDIO_PROCESSING = "peq_enable"
private const val KEY_CUSTOM_BANDS = "equ_custom_bands"
private const val KEY_BAND_COUNT = "equ_custom_bands_num"
private const val KEY_FIRST_HZ = "equ_custom_bands_first_fr"
private const val KEY_LAST_HZ = "equ_custom_bands_last_fr"
private const val KEY_OVERLAP = "equ_overlap"
private const val KEY_SMOOTH_GAINS = "dsp_border_gain"
private const val KEY_NO_DVC_REDUCTION_MB = "no_dvc_gain_mb"
private const val CASCADE_OVERLAP = 1

enum class PowerampIssue { SmoothGains, NoDvcReduction }

/**
 * Lo que Timbra necesita de los ajustes de Poweramp Equalizer. Salen de la llamada "preference" de su API,
 * que los propios autores marcan como experimental: las claves pueden cambiar entre versiones. [layout] es
 * `null` si usa bandas fijas, porque sus frecuencias no están confirmadas.
 */
data class PowerampSettings(
    val audioProcessingOn: Boolean,
    val layout: BandLayout?,
    val overlap: BandOverlap,
    val issues: Set<PowerampIssue>,
)

/** `null` si faltan los datos básicos, por ejemplo porque una versión nueva cambió las claves. */
fun powerampSettingsFrom(values: Map<String, Any?>): PowerampSettings? {
    val audioProcessingOn = values[KEY_AUDIO_PROCESSING] as? Boolean ?: return null
    val overlap = values[KEY_OVERLAP] as? Int ?: return null
    return PowerampSettings(
        audioProcessingOn = audioProcessingOn,
        layout = customLayout(values),
        overlap = if (overlap == CASCADE_OVERLAP) BandOverlap.Cascade else BandOverlap.Envelope,
        issues = buildSet {
            if (values[KEY_SMOOTH_GAINS] == true) add(PowerampIssue.SmoothGains)
            if ((values[KEY_NO_DVC_REDUCTION_MB] as? Int ?: 0) != 0) add(PowerampIssue.NoDvcReduction)
        },
    )
}

private fun customLayout(values: Map<String, Any?>): BandLayout? {
    if (values[KEY_CUSTOM_BANDS] != true) return null
    val count = values[KEY_BAND_COUNT] as? Int ?: return null
    val firstHz = values[KEY_FIRST_HZ] as? Int ?: return null
    val lastHz = values[KEY_LAST_HZ] as? Int ?: return null
    return runCatching { BandLayout(count, firstHz, lastHz) }.getOrNull()
}
