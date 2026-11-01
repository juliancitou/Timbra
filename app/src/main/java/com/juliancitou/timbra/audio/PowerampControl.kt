package com.juliancitou.timbra.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.net.Uri

enum class PowerampState { NotInstalled, Off, On, Unknown }

const val POWERAMP_EQ_PACKAGE = "com.maxmpz.equalizer"
private const val API_RECEIVER = "com.maxmpz.equalizer.PeqAPIReceiver"
private const val API_ACTION = "com.maxmpz.equalizer.API_COMMAND"
private const val EXTRA_COMMAND = "cmd"
private const val EXTRA_PACKAGE = "pak"
private const val COMMAND_STOP_SERVICE = 100
private const val COMMAND_START_SERVICE = 101
private const val COMMAND_SET_PRESET = 50
private const val EXTRA_ID = "id"
private const val API_ACTIVITY = "com.maxmpz.equalizer.PeqAPIActivity"
private const val ACTION_IMPORT_PRESETS = "com.maxmpz.equalizer.IMPORT_PRESETS"
private const val EXTRA_NAMES = "names"
private const val EXTRA_PRESETS = "presets"
private const val COLUMN_ID = "_id"
private const val STEREO_CHANNELS = 2
private const val SETTINGS_URI = "content://com.maxmpz.equalizer.data"
private const val CALL_PREFERENCE = "preference"
private const val PRESETS_URI = "$SETTINGS_URI/eq_presets"
private const val COLUMN_NAME = "name"
private const val COLUMN_META = "meta"
private const val COLUMN_PARAMETRIC = "parametric"
private const val COLUMN_RESOLVED_BLOB = "COALESCE(data_blob, default_blob)"
private const val ANC_ON_MARK = "ANC on"
private const val ANC_OFF_MARK = "ANC off"

/** Menor que la de Poweramp (1337): si el efecto ya tiene dueño, esta prioridad no se lo quita. */
private const val PROBE_PRIORITY = 0

/**
 * Habla con Poweramp Equalizer por su API pública (PeqAPI). Esa API puede encenderlo y apagarlo, pero
 * no dice si está encendido. Para saberlo se intenta tomar `DynamicsProcessing` en la sesión de Spotify
 * con prioridad mínima: Android lo rechaza si otra app ya controla el efecto. El efecto de prueba no se
 * enciende, así que no cambia el sonido.
 */
class PowerampControl(private val context: Context) {
    val isInstalled: Boolean
        get() = context.packageManager.getLaunchIntentForPackage(POWERAMP_EQ_PACKAGE) != null

    /** Solo lee, y no pide permisos. `null` si no está instalado o no entregó lo necesario. */
    fun readSettings(): PowerampSettings? = runCatching {
        val bundle = context.contentResolver.call(Uri.parse(SETTINGS_URI), CALL_PREFERENCE, null, null) ?: return null
        @Suppress("DEPRECATION")
        powerampSettingsFrom(bundle.keySet().associateWith { bundle.get(it) })
    }.getOrNull()

    /** Si ya hay un preset con [name]; Poweramp agrega " (2)" al importar otro con el mismo nombre. */
    fun hasPreset(name: String): Boolean = runCatching {
        context.contentResolver.query(
            Uri.parse(PRESETS_URI),
            arrayOf(COLUMN_NAME),
            "$COLUMN_NAME = ? OR $COLUMN_NAME LIKE ?",
            arrayOf(name, "$name (%"),
            null,
        )?.use { it.count > 0 } ?: false
    }.getOrDefault(false)

    /**
     * La curva de AutoEq de [device], de las miles que trae Poweramp. Se prefiere la del modo de cancelación
     * de ruido en uso, porque esos audífonos suenan distinto en cada uno.
     */
    fun autoEqFor(device: String, noiseCancelling: Boolean): AutoEqProfile? = runCatching {
        val wanted = if (noiseCancelling) ANC_ON_MARK else ANC_OFF_MARK
        context.contentResolver.query(
            Uri.parse(PRESETS_URI),
            arrayOf(COLUMN_NAME, COLUMN_META, COLUMN_RESOLVED_BLOB),
            "$COLUMN_NAME LIKE ? AND $COLUMN_PARAMETRIC = 0 AND $COLUMN_META IS NOT NULL",
            arrayOf("$device%"),
            null,
        )?.use { cursor ->
            val profiles = buildList {
                while (cursor.moveToNext()) {
                    val bands = cursor.getBlob(2)?.let(::graphicBandsOf) ?: continue
                    add(AutoEqProfile(cursor.getString(0), cursor.getString(1), bands))
                }
            }
            profiles.firstOrNull { wanted in it.name } ?: profiles.firstOrNull()
        }
    }.getOrNull()

    /** Avisa a Poweramp de una sesión de audio, como hace cualquier reproductor, para que la ecualice. */
    fun announceSession(sessionId: Int, open: Boolean) {
        val action = if (open) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION
        } else {
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION
        }
        context.sendBroadcast(
            Intent(action)
                .setPackage(POWERAMP_EQ_PACKAGE)
                .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC),
        )
    }

    /** Abre el aviso de Poweramp para importar un preset. `false` si no se pudo abrir. */
    fun importPreset(name: String, json: String): Boolean = runCatching {
        context.startActivity(
            Intent(ACTION_IMPORT_PRESETS)
                .setComponent(ComponentName(POWERAMP_EQ_PACKAGE, API_ACTIVITY))
                .putExtra(EXTRA_NAMES, arrayOf(name))
                .putExtra(EXTRA_PRESETS, arrayOf(json))
                .putExtra(EXTRA_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    }.getOrDefault(false)

    /** Deja activo el preset con [name]. `false` si no existe. Poweramp no dice cuál estaba antes. */
    fun activatePreset(name: String): Boolean = runCatching {
        val id = context.contentResolver.query(
            Uri.parse(PRESETS_URI),
            arrayOf(COLUMN_ID),
            "$COLUMN_NAME = ?",
            arrayOf(name),
            null,
        )?.use { if (it.moveToFirst()) it.getLong(0) else null } ?: return false
        apiIntent(COMMAND_SET_PRESET).putExtra(EXTRA_ID, id).let(context::sendBroadcast)
        true
    }.getOrDefault(false)

    fun turnOff() = send(COMMAND_STOP_SERVICE)

    fun turnOn() = send(COMMAND_START_SERVICE)

    /** `true` si otra app controla el efecto de [sessionId]; `null` si no se pudo saber. */
    fun isControlling(sessionId: Int): Boolean? {
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            STEREO_CHANNELS,
            false,
            0,
            false,
            0,
            false,
            0,
            false,
        ).build()
        return try {
            val probe = DynamicsProcessing(PROBE_PRIORITY, sessionId, config)
            val hasControl = probe.hasControl()
            probe.release()
            !hasControl
        } catch (_: UnsupportedOperationException) {
            true
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun send(command: Int) = context.sendBroadcast(apiIntent(command))

    private fun apiIntent(command: Int) = Intent(API_ACTION)
        .setComponent(ComponentName(POWERAMP_EQ_PACKAGE, API_RECEIVER))
        .putExtra(EXTRA_COMMAND, command)
        .putExtra(EXTRA_PACKAGE, context.packageName)
}
