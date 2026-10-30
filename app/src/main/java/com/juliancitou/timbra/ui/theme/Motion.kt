package com.juliancitou.timbra.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object MotionDuration {
    const val SHORT_MS = 200
    const val MEDIUM_MS = 300
    const val LONG_MS = 350
}

/** Curvas "enfatizadas" de Material 3: arranque rápido y frenado suave al entrar; al revés al salir. */
val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

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
