package com.juliancitou.timbra.ui.screens.finder

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.measure.OutputKind

@get:StringRes
val OutputKind.labelRes: Int
    get() = when (this) {
        OutputKind.Bluetooth -> R.string.output_bluetooth
        OutputKind.Wired -> R.string.output_wired
        OutputKind.Speaker -> R.string.output_speaker
    }

@get:StringRes
val NoiseMode.labelRes: Int
    get() = when (this) {
        NoiseMode.Cancelling -> R.string.noise_mode_cancelling
        NoiseMode.Aware -> R.string.noise_mode_aware
        NoiseMode.Off -> R.string.noise_mode_off
    }

/**
 * Nombre sugerido del preset: "FreeBuds Pro 4 (Bluetooth)". Lleva la salida para distinguirlo en
 * Poweramp de otro hecho para otra salida; la bocina no tiene nombre de accesorio que sumarle.
 */
@Composable
fun suggestedPresetName(state: FinderUiState): String {
    val output = state.finder.output
    val label = stringResource(output.labelRes)
    if (output == OutputKind.Speaker) return label
    val name = state.headphonesName.trim().ifEmpty { stringResource(R.string.finder_default_preset_name) }
    if (output != OutputKind.Bluetooth || state.noiseMode == NoiseMode.Cancelling) return "$name ($label)"
    return "$name, ${stringResource(state.noiseMode.labelRes)} ($label)"
}

/** Con Spotify no se sabe si está sonando: un mismo botón pausa o reanuda. */
@StringRes
fun FinderUiState.playbackLabelRes(): Int = when {
    usesSpotify -> R.string.action_play_pause
    isPlaying -> R.string.action_pause
    else -> R.string.action_play
}
