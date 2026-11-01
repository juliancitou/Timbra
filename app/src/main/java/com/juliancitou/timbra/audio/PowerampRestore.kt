package com.juliancitou.timbra.audio

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.edit

private const val FILE_NAME = "poweramp_restore"
private const val KEY_OFF_BY_TIMBRA = "off_by_timbra"

/**
 * Recuerda que Timbra apagó Poweramp, para volver a encenderlo si el test se abandona. Se guarda en disco porque
 * la app puede morir sin aviso: si al abrirla sigue marcado, el test de entonces ya se perdió. Solo enciende lo que
 * Timbra apagó: si Poweramp ya estaba apagado por decisión del usuario, nunca se marca.
 */
class PowerampRestore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    val isOffByTimbra: Boolean get() = preferences.getBoolean(KEY_OFF_BY_TIMBRA, false)

    /** Llamar con la app en primer plano: Android no deja iniciar el servicio desde segundo plano. */
    fun markOff() {
        preferences.edit(commit = true) { putBoolean(KEY_OFF_BY_TIMBRA, true) }
        runCatching { appContext.startService(Intent(appContext, PowerampRestoreService::class.java)) }
    }

    fun forget() {
        preferences.edit(commit = true) { putBoolean(KEY_OFF_BY_TIMBRA, false) }
        appContext.stopService(Intent(appContext, PowerampRestoreService::class.java))
    }

    /** Vuelve a encender Poweramp si Timbra lo apagó. Devuelve si lo hizo. */
    fun restore(): Boolean {
        if (!isOffByTimbra) return false
        PowerampControl(appContext).turnOn()
        forget()
        return true
    }
}

/**
 * Solo existe para enterarse de que el usuario deslizó Timbra fuera de las recientes, lo único que mata la app sin
 * pasar por una pantalla. Android lo avisa con `onTaskRemoved` a un servicio iniciado. No hace nada más.
 */
class PowerampRestoreService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        PowerampRestore(this).restore()
        stopSelf()
    }
}
