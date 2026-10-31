package com.juliancitou.timbra.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.audiofx.DynamicsProcessing

enum class PowerampState { NotInstalled, Off, On, Unknown }

private const val POWERAMP_EQ_PACKAGE = "com.maxmpz.equalizer"
private const val API_RECEIVER = "com.maxmpz.equalizer.PeqAPIReceiver"
private const val API_ACTION = "com.maxmpz.equalizer.API_COMMAND"
private const val EXTRA_COMMAND = "cmd"
private const val EXTRA_PACKAGE = "pak"
private const val COMMAND_STOP_SERVICE = 100
private const val COMMAND_START_SERVICE = 101
private const val STEREO_CHANNELS = 2

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

    private fun send(command: Int) {
        context.sendBroadcast(
            Intent(API_ACTION)
                .setComponent(ComponentName(POWERAMP_EQ_PACKAGE, API_RECEIVER))
                .putExtra(EXTRA_COMMAND, command)
                .putExtra(EXTRA_PACKAGE, context.packageName),
        )
    }
}
