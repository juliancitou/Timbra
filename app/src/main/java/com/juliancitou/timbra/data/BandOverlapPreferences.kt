package com.juliancitou.timbra.data

import android.content.Context
import androidx.core.content.edit
import com.juliancitou.timbra.audio.BandOverlap

private const val FILE_NAME = "band_overlap"
private const val KEY_OVERLAP = "overlap"

/** Recuerda la superposición de bandas elegida, que debe ser la misma que usa Poweramp. */
class BandOverlapPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): BandOverlap =
        BandOverlap.entries.firstOrNull { it.name == preferences.getString(KEY_OVERLAP, null) } ?: BandOverlap.Envelope

    fun save(overlap: BandOverlap) = preferences.edit { putString(KEY_OVERLAP, overlap.name) }
}
