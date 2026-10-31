package com.juliancitou.timbra.export

import com.juliancitou.timbra.audio.EqualizerSettings
import java.text.Normalizer
import java.util.Locale

/**
 * Preset para Poweramp Equalizer en el formato que importa su modo gráfico: un arreglo con un
 * objeto, dos bandas de tono (grave tipo 0 en 90 Hz y aguda tipo 1 en 10 kHz) siempre en 0 dB y
 * luego las bandas gráficas (tipo 2) de la configuración elegida. El texto sale igual al de un
 * preset que ya se sabe que Poweramp importa bien (ver la prueba golden), así que no conviene cambiar
 * espacios ni orden.
 */
object PowerampExporter {
    const val NAME_PREFIX = "Timbra - "

    private data class ToneBand(val type: Int, val frequencyHz: Int, val q: Float)

    private val toneBands = listOf(
        ToneBand(type = 0, frequencyHz = 90, q = 0.8f),
        ToneBand(type = 1, frequencyHz = 10_000, q = 0.6f),
    )
    private const val GRAPHIC_BAND_TYPE = 2

    fun toJson(presetName: String, settings: EqualizerSettings): String {
        val bands = toneBands.map { band(it.type, it.frequencyHz, it.q, 0f) } +
            settings.bandFrequenciesHz.zip(settings.bandGainsDb) { hz, gain -> band(GRAPHIC_BAND_TYPE, hz, 0f, gain) }
        return buildString {
            append("[\n  {\n")
            append("    \"name\": ${jsonString(NAME_PREFIX + presetName.trim())},\n")
            append("    \"preamp\": ${decimal(settings.preampDb)},\n")
            append("    \"parametric\": false,\n")
            append("    \"bands\": [\n")
            append(bands.joinToString(",\n") { "      $it" })
            append("\n    ]\n  }\n]\n")
        }
    }

    /** Nombre de archivo sugerido: `timbra-mi-ecualizacion.json`, sin acentos ni espacios. */
    fun fileName(presetName: String): String {
        val slug = Normalizer.normalize(presetName.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifEmpty { "preset" }
        return "timbra-$slug.json"
    }

    private fun band(type: Int, frequencyHz: Int, q: Float, gainDb: Float) =
        "{\"type\": $type, \"channels\": 0, \"frequency\": $frequencyHz, \"q\": ${decimal(q)}, " +
            "\"gain\": ${decimal(gainDb)}, \"color\": 0}"

    /** Un decimal, con punto y sin "-0.0" (Poweramp espera ganancias en pasos de 0.5 dB). */
    private fun decimal(value: Float): String = String.format(Locale.ROOT, "%.1f", if (value == 0f) 0f else value)

    private fun jsonString(text: String): String = buildString {
        append('"')
        text.forEach { character ->
            when {
                character == '"' -> append("\\\"")
                character == '\\' -> append("\\\\")
                character < ' ' -> append(String.format(Locale.ROOT, "\\u%04x", character.code))
                else -> append(character)
            }
        }
        append('"')
    }
}
