package com.juliancitou.timbra.ui.screens.audiotest

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juliancitou.timbra.R
import com.juliancitou.timbra.audio.PlaybackStatus
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassChip
import com.juliancitou.timbra.ui.theme.GlassChipTone
import com.juliancitou.timbra.ui.theme.GlassScreen
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.Spacing

@Composable
fun AudioTestScreen(onBack: () -> Unit, viewModel: AudioTestViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StopWhenLeavingForeground(onStop = viewModel::stop)

    GlassScreen(
        title = stringResource(R.string.audio_test_title),
        lead = stringResource(R.string.audio_test_lead),
        onBack = onBack,
        actions = {
            GlassButton(
                text = stringResource(
                    if (state.status == PlaybackStatus.Playing) R.string.audio_test_stop else R.string.action_play,
                ),
                onClick = viewModel::togglePlayback,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        GlassChip(
            label = stringResource(statusLabel(state.status)),
            tone = if (state.status.isWarning()) GlassChipTone.Warning else GlassChipTone.Normal,
            selected = state.status == PlaybackStatus.Playing,
        )
        ChoiceSection(R.string.audio_test_section_signal) {
            TestSignal.entries.forEach { signal ->
                GlassChip(
                    label = stringResource(signalLabel(signal)),
                    selected = state.signal == signal,
                    onClick = { viewModel.selectSignal(signal) },
                )
            }
        }
        ChoiceSection(R.string.audio_test_section_equalization) {
            TestEqualization.entries.forEach { equalization ->
                GlassChip(
                    label = stringResource(equalizationLabel(equalization)),
                    selected = state.equalization == equalization,
                    onClick = { viewModel.selectEqualization(equalization) },
                )
            }
        }
        ChoiceSection(R.string.audio_test_section_loudness) {
            GlassChip(
                label = stringResource(
                    if (state.matchLoudness) R.string.audio_test_match_on else R.string.audio_test_match_off,
                ),
                selected = state.matchLoudness,
                onClick = { viewModel.setLoudnessMatching(!state.matchLoudness) },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            SectionLabel(stringResource(R.string.audio_test_section_check))
            DetailText(stringResource(R.string.audio_test_check))
        }
    }
}

@Composable
private fun ChoiceSection(@StringRes titleRes: Int, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        SectionLabel(stringResource(titleRes))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) { chips() }
    }
}

/** Detiene el sonido si la app pasa a segundo plano o se apaga la pantalla. */
@Composable
private fun StopWhenLeavingForeground(onStop: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) onStop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private fun PlaybackStatus.isWarning() =
    this == PlaybackStatus.PausedByOtherApp ||
        this == PlaybackStatus.PausedHeadphonesDisconnected ||
        this == PlaybackStatus.StoppedByTimeLimit

@StringRes
private fun statusLabel(status: PlaybackStatus): Int = when (status) {
    PlaybackStatus.Stopped -> R.string.audio_status_stopped
    PlaybackStatus.Playing -> R.string.audio_status_playing
    PlaybackStatus.PausedByOtherApp -> R.string.audio_status_other_app
    PlaybackStatus.PausedHeadphonesDisconnected -> R.string.audio_status_headphones
    PlaybackStatus.StoppedByTimeLimit -> R.string.audio_status_time_limit
}

@StringRes
private fun signalLabel(signal: TestSignal): Int = when (signal) {
    TestSignal.PinkNoise -> R.string.audio_test_signal_pink
    TestSignal.Tone -> R.string.audio_test_signal_tone
}

@StringRes
private fun equalizationLabel(equalization: TestEqualization): Int = when (equalization) {
    TestEqualization.Flat -> R.string.audio_test_eq_flat
    TestEqualization.SamplePreset -> R.string.audio_test_eq_sample
    TestEqualization.MoreBass -> R.string.audio_test_eq_bass
    TestEqualization.LessTreble -> R.string.audio_test_eq_treble
}
