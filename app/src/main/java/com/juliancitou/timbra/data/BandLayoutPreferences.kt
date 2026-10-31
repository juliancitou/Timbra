package com.juliancitou.timbra.data

import android.content.Context
import androidx.core.content.edit
import com.juliancitou.timbra.export.BandLayout

private const val FILE_NAME = "band_layout"
private const val KEY_COUNT = "count"
private const val KEY_FIRST_HZ = "first_hz"
private const val KEY_LAST_HZ = "last_hz"

/** Recuerda las bandas elegidas: normalmente son las mismas de Poweramp en todas las sesiones. */
class BandLayoutPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): BandLayout = runCatching {
        val default = BandLayout.Default
        BandLayout(
            count = preferences.getInt(KEY_COUNT, default.count),
            firstHz = preferences.getInt(KEY_FIRST_HZ, default.firstHz),
            lastHz = preferences.getInt(KEY_LAST_HZ, default.lastHz),
        )
    }.getOrDefault(BandLayout.Default)

    fun save(layout: BandLayout) {
        preferences.edit {
            putInt(KEY_COUNT, layout.count)
            putInt(KEY_FIRST_HZ, layout.firstHz)
            putInt(KEY_LAST_HZ, layout.lastHz)
        }
    }
}
