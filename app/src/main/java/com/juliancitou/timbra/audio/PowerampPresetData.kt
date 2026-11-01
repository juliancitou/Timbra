package com.juliancitou.timbra.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log2

private const val BLOB_HEADER_BYTES = 12
private const val BLOB_ENTRY_BYTES = 12
private const val BLOB_COUNT_OFFSET = 4
private const val GRAPHIC_BAND_TYPE = 2
private const val TYPE_MASK = 0xFF
private val BlobMagic = byteArrayOf(0x50, 0x61, 0x50, 0x01)

/** Una banda gráfica de un preset de Poweramp. */
data class PresetBand(val frequencyHz: Int, val gainDb: Float)

/**
 * Lee las bandas gráficas del dato binario de un preset de Poweramp. El formato no está documentado y se
 * dedujo de sus presets: cabecera de 12 bytes, con la cantidad de entradas en el byte 4, y entradas de 12
 * bytes (tipo, ganancia en dB y frecuencia en Hz, en little-endian). `null` si no coincide.
 */
fun graphicBandsOf(blob: ByteArray): List<PresetBand>? {
    if (blob.size < BLOB_HEADER_BYTES || !blob.copyOf(BlobMagic.size).contentEquals(BlobMagic)) return null
    val data = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN)
    val entries = data.getInt(BLOB_COUNT_OFFSET)
    if (entries <= 0 || blob.size != BLOB_HEADER_BYTES + entries * BLOB_ENTRY_BYTES) return null
    val bands = (0 until entries).mapNotNull { entry ->
        val offset = BLOB_HEADER_BYTES + entry * BLOB_ENTRY_BYTES
        if (data.getInt(offset) and TYPE_MASK != GRAPHIC_BAND_TYPE) return@mapNotNull null
        PresetBand(frequencyHz = data.getInt(offset + 8), gainDb = data.getFloat(offset + 4))
    }
    return bands.ifEmpty { null }
}

private const val SAME_GAIN_TOLERANCE_DB = 0.05f

/** Si [stored] es [name] o la copia que crea Poweramp al importar otro con el mismo nombre: "[name] (2)". */
fun isSamePresetName(stored: String, name: String): Boolean =
    stored == name || Regex("${Regex.escape(name)} \\(\\d+\\)").matches(stored)

/**
 * Si el preset guardado tiene otra curva que [gainsDb]: repetir el test da el mismo nombre con otro sonido. Un
 * dato que no se puede leer no lo contradice, porque el formato no está documentado y puede cambiar.
 */
fun blobDiffersFrom(blob: ByteArray?, gainsDb: List<Float>): Boolean {
    val stored = blob?.let(::graphicBandsOf) ?: return false
    return stored.size != gainsDb.size || stored.zip(gainsDb).any { (band, gain) -> abs(band.gainDb - gain) > SAME_GAIN_TOLERANCE_DB }
}

/** La curva de [bands] en [frequenciesHz], interpolada en octavas. Fuera de sus límites se queda en el extremo. */
fun gainsAt(bands: List<PresetBand>, frequenciesHz: List<Int>): List<Double> = frequenciesHz.map { hz ->
    val upperIndex = bands.indexOfFirst { it.frequencyHz >= hz }
    when {
        upperIndex == 0 -> bands.first().gainDb.toDouble()
        upperIndex < 0 -> bands.last().gainDb.toDouble()
        else -> {
            val lower = bands[upperIndex - 1]
            val upper = bands[upperIndex]
            val fraction = log2(hz.toDouble() / lower.frequencyHz) / log2(upper.frequencyHz.toDouble() / lower.frequencyHz)
            lower.gainDb + (upper.gainDb - lower.gainDb) * fraction
        }
    }
}

/** La curva de AutoEq para unos audífonos. [measuredBy] es quien los midió. */
data class AutoEqProfile(val name: String, val measuredBy: String?, val bands: List<PresetBand>)

/** Deja margen a las rondas para subir un poco cualquier zona sin chocar con el +3 de Poweramp. */
private const val AUTO_EQ_HIGHEST_DB = 1.5

/**
 * La curva de AutoEq como punto de partida del test. AutoEq sube hasta +6 dB, y los presets no pasan de +3:
 * recortarla aplastaría graves y medios juntos. Se baja entera, lo que conserva su forma; el nivel general
 * no importa porque el preamp y la igualación de volumen ya lo compensan.
 */
fun AutoEqProfile.startingCurveDb(frequenciesHz: List<Int>): List<Double> {
    val curve = gainsAt(bands, frequenciesHz)
    val shift = (curve.max() - AUTO_EQ_HIGHEST_DB).coerceAtLeast(0.0)
    return curve.map { it - shift }
}
