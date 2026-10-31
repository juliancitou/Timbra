package com.juliancitou.timbra.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.juliancitou.timbra.measure.OutputKind
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Por dónde sonaría la música ahora. [name] es el nombre del accesorio, si Android lo da. */
data class DetectedOutput(val kind: OutputKind, val name: String? = null)

private val BluetoothTypes = setOf(
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_BLE_SPEAKER,
)
private val WiredTypes = setOf(
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_USB_DEVICE,
)

/** Solo lee el estado del sistema; no cambia la ruta de audio ni necesita permisos. */
class OutputDetector(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val mediaAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    /** La salida actual y cada cambio al conectar o desconectar un accesorio. */
    fun changes(): Flow<DetectedOutput> = callbackFlow {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                trySend(current())
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                trySend(current())
            }
        }
        trySend(current())
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { audioManager.unregisterAudioDeviceCallback(callback) }
    }.distinctUntilChanged()

    /**
     * Pregunta primero a qué dispositivo iría la música, porque puede haber varios conectados. Si el
     * sistema no lo dice, prefiere Bluetooth sobre cable y cable sobre la bocina.
     */
    private fun current(): DetectedOutput {
        val routed = runCatching { audioManager.getAudioDevicesForAttributes(mediaAttributes) }
            .getOrDefault(emptyList())
        val devices = routed.ifEmpty { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList() }
        val bluetooth = devices.firstOrNull { it.type in BluetoothTypes }
        val wired = devices.firstOrNull { it.type in WiredTypes }
        return when {
            bluetooth != null -> DetectedOutput(OutputKind.Bluetooth, accessoryName(bluetooth))
            wired != null -> DetectedOutput(OutputKind.Wired, accessoryName(wired))
            else -> DetectedOutput(OutputKind.Speaker)
        }
    }

    /** Algunos sistemas devuelven el modelo del teléfono en vez del nombre del accesorio. */
    private fun accessoryName(device: AudioDeviceInfo): String? =
        device.productName?.toString()?.trim()?.takeUnless { it.isEmpty() || it == Build.MODEL }
}
