package com.juliancitou.timbra.ui.screens.calibration

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.audio.BandOverlap
import com.juliancitou.timbra.ui.screens.export.writeTo
import com.juliancitou.timbra.ui.screens.finder.FinderUiState
import com.juliancitou.timbra.ui.screens.finder.SpotifyLink
import com.juliancitou.timbra.ui.theme.CodeLines
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalButtonStyle
import com.juliancitou.timbra.ui.theme.TerminalChip
import com.juliancitou.timbra.ui.theme.TerminalScreen
import com.juliancitou.timbra.ui.theme.TerminalSection

private const val PRESET_MIME_TYPE = "application/json"

private fun BandOverlap.labelRes() = when (this) {
    BandOverlap.Envelope -> R.string.calibration_overlap_envelope
    BandOverlap.Cascade -> R.string.calibration_overlap_cascade
}

/**
 * Ajusta la vista previa en vivo para que suene igual que este Poweramp. Una app no puede leer sus
 * ajustes internos, así que se compara de oído el mismo preset en Poweramp y en Timbra.
 */
@Composable
fun CalibrationScreen(
    state: FinderUiState,
    presetJson: () -> String,
    fileName: String,
    onStart: () -> Unit,
    onSelectOverlap: (BandOverlap) -> Unit,
    onOpenSpotify: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { onStart() }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PRESET_MIME_TYPE)) { uri ->
        if (uri != null) writeTo(context, uri, presetJson())
    }

    TerminalScreen(
        title = stringResource(R.string.calibration_title),
        lead = stringResource(R.string.calibration_lead),
        onBack = onBack,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.inSection)) {
                TerminalButton(
                    text = stringResource(R.string.calibration_download),
                    onClick = { save.launch(fileName) },
                    style = TerminalButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                TerminalButton(
                    text = stringResource(R.string.action_open_spotify),
                    onClick = onOpenSpotify,
                    style = TerminalButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
            TerminalButton(
                text = stringResource(R.string.action_ready),
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        TerminalSection(stringResource(R.string.calibration_section_steps)) {
            CodeLines(
                listOf(
                    stringResource(R.string.calibration_step_import),
                    stringResource(R.string.calibration_step_listen),
                    stringResource(R.string.calibration_step_return),
                    stringResource(R.string.calibration_step_choose),
                ),
            )
        }
        TerminalSection(stringResource(R.string.calibration_section_shape)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                BandOverlap.entries.forEach { shape ->
                    TerminalChip(
                        label = stringResource(shape.labelRes()),
                        selected = state.bandOverlap == shape,
                        onClick = { onSelectOverlap(shape) },
                    )
                }
            }
            DetailText(
                stringResource(
                    if (state.spotify == SpotifyLink.Connected) {
                        R.string.comfort_spotify_connected
                    } else {
                        R.string.calibration_waiting
                    },
                ),
            )
        }
    }
}
