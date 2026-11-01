package com.juliancitou.timbra.ui.screens.calibration

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.pm.PackageManager
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.screens.finder.PowerampMeasure
import com.juliancitou.timbra.ui.theme.FrequencyChart
import com.juliancitou.timbra.ui.theme.formatDecibels
import com.juliancitou.timbra.audio.BandOverlap
import com.juliancitou.timbra.ui.screens.finder.FinderUiState
import com.juliancitou.timbra.ui.screens.finder.SpotifyLink
import com.juliancitou.timbra.ui.theme.CodeLines
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalButtonRow
import com.juliancitou.timbra.ui.theme.TerminalButtonStyle
import com.juliancitou.timbra.ui.theme.TerminalChip
import com.juliancitou.timbra.ui.theme.TerminalScreen
import com.juliancitou.timbra.ui.theme.TerminalSection

/** La gráfica muestra lo medido en ámbar y lo que Timbra espera en naranja. */
@Composable
private fun MeasureSection(
    measure: PowerampMeasure,
    previewStrength: Double,
    onResetStrength: () -> Unit,
    onUseStrength: (Double) -> Unit,
    onMeasure: () -> Unit,
) {
    TerminalSection(stringResource(R.string.calibration_section_measure)) {
        DetailText(stringResource(R.string.calibration_measure_hint))
        when (measure) {
            PowerampMeasure.Idle -> Unit
            PowerampMeasure.Running -> DetailText(stringResource(R.string.calibration_measure_running))
            PowerampMeasure.CouldNotCapture -> DetailText(stringResource(R.string.calibration_measure_no_capture))
            PowerampMeasure.TooQuiet -> DetailText(stringResource(R.string.calibration_measure_too_quiet))
            PowerampMeasure.PresetMissing -> DetailText(stringResource(R.string.calibration_measure_no_preset))
            PowerampMeasure.NoEffect -> DetailText(stringResource(R.string.calibration_measure_no_effect))
            is PowerampMeasure.Done -> {
                FrequencyChart(
                    gainsDb = measure.measuredDb,
                    contentDescription = stringResource(R.string.calibration_measure_chart),
                    frequenciesHz = measure.frequenciesHz,
                    overlayDb = measure.expectedDb,
                )
                DetailText(stringResource(R.string.calibration_measure_legend))
                DetailText(
                    stringResource(
                        R.string.calibration_measure_result,
                        formatDecibels(measure.timbraCutDb),
                        formatDecibels(measure.powerampCutDb),
                    ),
                )
                DetailText(stringResource(R.string.calibration_measure_caveat))
                TerminalChip(
                    label = stringResource(R.string.calibration_strength_use, (measure.strength * 100).roundToInt()),
                    selected = previewStrength == measure.strength,
                    onClick = { onUseStrength(measure.strength) },
                )
            }
        }
        if (previewStrength < 1.0) {
            DetailText(stringResource(R.string.calibration_strength, (previewStrength * 100).roundToInt()))
        }
        if (measure != PowerampMeasure.Running) {
            TerminalChip(
                label = stringResource(R.string.calibration_measure_start),
                selected = false,
                onClick = onMeasure,
            )
            if (previewStrength < 1.0) {
                TerminalChip(
                    label = stringResource(R.string.calibration_strength_reset),
                    selected = false,
                    onClick = onResetStrength,
                )
            }
        }
    }
}

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
    onImportPreset: () -> Boolean,
    onResetStrength: () -> Unit,
    onUseStrength: (Double) -> Unit,
    onStart: () -> Unit,
    onSelectOverlap: (BandOverlap) -> Unit,
    onMeasurePoweramp: () -> Unit,
    onOpenSpotify: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { onStart() }
    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onMeasurePoweramp()
    }
    TerminalScreen(
        title = stringResource(R.string.calibration_title),
        lead = stringResource(R.string.calibration_lead),
        onBack = onBack,
        actions = {
            TerminalButtonRow {
                TerminalButton(
                    text = stringResource(R.string.calibration_import),
                    onClick = { onImportPreset() },
                    style = TerminalButtonStyle.Secondary,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                TerminalButton(
                    text = stringResource(R.string.action_open_spotify),
                    onClick = onOpenSpotify,
                    style = TerminalButtonStyle.Secondary,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
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
        MeasureSection(state.powerampMeasure, state.previewStrength, onResetStrength, onUseStrength) {
            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                onMeasurePoweramp()
            } else {
                microphone.launch(Manifest.permission.RECORD_AUDIO)
            }
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
            if (state.spotify != SpotifyLink.Off) {
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
}
