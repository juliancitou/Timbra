package com.juliancitou.timbra.audio

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val SPOTIFY_PACKAGE = "com.spotify.music"

/**
 * La sesión de audio que Spotify anunció por última vez. Un anuncio puede llegar con Timbra en
 * segundo plano, así que vive fuera de las pantallas. Solo guarda el número de sesión.
 */
object SpotifySession {
    private val current = MutableStateFlow<Int?>(null)
    val id: StateFlow<Int?> = current.asStateFlow()

    fun opened(intent: Intent) {
        sessionOfSpotify(intent)?.let { current.value = it }
    }

    fun closed(intent: Intent) {
        val session = sessionOfSpotify(intent) ?: return
        current.compareAndSet(session, null)
    }

    private fun sessionOfSpotify(intent: Intent): Int? {
        if (intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) != SPOTIFY_PACKAGE) return null
        return intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR_BAD_VALUE).takeIf { it > 0 }
    }
}

/** Recibe los anuncios de Spotify al reproducir y pausar; se declara en el manifiesto. */
class AudioSessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> SpotifySession.opened(intent)
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> SpotifySession.closed(intent)
        }
    }
}

/**
 * Hace que Timbra aparezca como ecualizador para las apps que buscan uno, y da otra vía de obtener la
 * sesión: elegir Timbra en el ajuste "Ecualizador" de Spotify. No muestra nada: anota la sesión y vuelve.
 */
class AudioEffectPanelActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpotifySession.opened(intent)
        finish()
    }
}
