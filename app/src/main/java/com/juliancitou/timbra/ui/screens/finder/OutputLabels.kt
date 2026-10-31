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
    return "$name ($label)"
}
