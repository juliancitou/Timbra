package com.juliancitou.timbra.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object MotionDuration {
    const val SHORT_MS = 200
    const val MEDIUM_MS = 300
    const val LONG_MS = 350
}

/**
 * `true` si el usuario desactivó las animaciones del sistema (Opciones de accesibilidad o de
 * desarrollador ponen la escala de animación en 0). En ese caso la aurora queda fija.
 */
@Composable
fun isReducedMotionEnabled(): Boolean {
    val contentResolver = LocalContext.current.contentResolver
    return remember(contentResolver) {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
