package com.juliancitou.timbra.ui.screens

import androidx.annotation.StringRes
import com.juliancitou.timbra.R
import com.juliancitou.timbra.audio.music.Genre

/** Nombre del género para mostrar en pantalla. */
@StringRes
fun Genre.label(): Int = when (this) {
    Genre.Pop -> R.string.genre_pop
    Genre.Rock -> R.string.genre_rock
    Genre.Classical -> R.string.genre_classical
    Genre.Electronic -> R.string.genre_electronic
    Genre.HipHop -> R.string.genre_hip_hop
    Genre.Jazz -> R.string.genre_jazz
}
