package com.juliancitou.timbra.data

import android.content.Context
import androidx.core.content.edit

private const val FILE_NAME = "chosen_preview_strength"
private const val KEY_STRENGTH = "strength"
private const val FULL_STRENGTH = 1f

/** Recuerda con qué fuerza eligió el usuario oír la vista previa. Por defecto, el preset completo. */
class PreviewStrengthPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): Double = preferences.getFloat(KEY_STRENGTH, FULL_STRENGTH).toDouble()

    fun save(strength: Double) = preferences.edit { putFloat(KEY_STRENGTH, strength.toFloat()) }
}
