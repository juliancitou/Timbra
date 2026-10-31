package com.juliancitou.timbra.data

import android.content.Context
import androidx.core.content.edit
import com.juliancitou.timbra.audio.BandShape

private const val FILE_NAME = "band_shape"
private const val KEY_SHAPE = "shape"

/** Recuerda la forma elegida al calibrar contra Poweramp. */
class BandShapePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): BandShape =
        BandShape.entries.firstOrNull { it.name == preferences.getString(KEY_SHAPE, null) } ?: BandShape.Direct

    fun save(shape: BandShape) = preferences.edit { putString(KEY_SHAPE, shape.name) }
}
