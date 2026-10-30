package com.juliancitou.timbra.ui.screens.audiotest

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juliancitou.timbra.R
import com.juliancitou.timbra.audio.PlaybackStatus
import com.juliancitou.timbra.audio.music.Genre
import com.juliancitou.timbra.audio.music.MusicFocus
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassChip
import com.juliancitou.timbra.ui.theme.GlassChipTone
import com.juliancitou.timbra.ui.theme.GlassScreen
import com.juliancitou.timbra.ui.theme.PlayingIndicator
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
                enabled = state.canPlay,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            GlassChip(
                label = stringResource(
                    if (state.canPlay) statusLabel(state.status) else R.string.audio_status_preparing,
                ),
                tone = if (state.status.isWarning()) GlassChipTone.Warning else GlassChipTone.Normal,
                selected = state.status == PlaybackStatus.Playing,
            )
            PlayingIndicator(playing = state.status == PlaybackStatus.Playing)
        }
        ChoiceSection(R.string.audio_test_section_signal) {
            TestSignal.entries.forEach { signal ->
                GlassChip(
                    label = stringResource(signalLabel(signal)),
                    selected = state.signal == signal,
                    onClick = { viewModel.selectSignal(signal) },
                )
            }
        }
        if (state.signal == TestSignal.Music) {
            ChoiceSection(R.string.audio_test_section_genre) {
                Genre.entries.forEach { genre ->
                    GlassChip(
                        label = stringResource(genreLabel(genre)),
                        selected = state.genre == genre,
                        onClick = { viewModel.selectGenre(genre) },
                    )
                }
            }
            ChoiceSection(R.string.audio_test_section_focus) {
                MusicFocus.entries.forEach { focus ->
                    GlassChip(
                        label = stringResource(focusLabel(focus)),
                        selected = state.focus == focus,
                        onClick = { viewModel.selectFocus(focus) },
                    )
                }
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
private fun genreLabel(genre: Genre): Int = when (genre) {
    Genre.Pop -> R.string.genre_pop
    Genre.Rock -> R.string.genre_rock
    Genre.Classical -> R.string.genre_classical
    Genre.Electronic -> R.string.genre_electronic
    Genre.HipHop -> R.string.genre_hip_hop
    Genre.Jazz -> R.string.genre_jazz
}

@StringRes
private fun focusLabel(focus: MusicFocus): Int = when (focus) {
    MusicFocus.Balanced -> R.string.audio_test_focus_balanced
    MusicFocus.Bass -> R.string.audio_test_focus_bass
    MusicFocus.Clarity -> R.string.audio_test_focus_clarity
    MusicFocus.Voice -> R.string.audio_test_focus_voice
    MusicFocus.Sibilance -> R.string.audio_test_focus_sibilance
    MusicFocus.Air -> R.string.audio_test_focus_air
}

@StringRes
private fun signalLabel(signal: TestSignal): Int = when (signal) {
    TestSignal.Music -> R.string.audio_test_signal_music
    TestSignal.PinkNoise -> R.string.audio_test_signal_pink
    TestSignal.Tone -> R.string.audio_test_signal_tone
}

@StringRes
private fun equalizationLabel(equalization: TestEqualization): Int = when (equalization) {
    TestEqualization.Flat -> R.string.audio_test_eq_flat
    TestEqualization.SamplePreset -> R.string.audio_test_eq_sample
    TestEqualization.MoreBass -> R.string.audio_test_eq_more_bass
    TestEqualization.LessBass -> R.string.audio_test_eq_less_bass
    TestEqualization.LessMud -> R.string.audio_test_eq_less_mud
    TestEqualization.MoreVoice -> R.string.audio_test_eq_more_voice
    TestEqualization.LessSibilance -> R.string.audio_test_eq_less_sibilance
    TestEqualization.MoreBrightness -> R.string.audio_test_eq_more_brightness
    TestEqualization.LessTreble -> R.string.audio_test_eq_less_treble
    TestEqualization.VShape -> R.string.audio_test_eq_v_shape
    TestEqualization.Warm -> R.string.audio_test_eq_warm
    TestEqualization.Bright -> R.string.audio_test_eq_bright
    TestEqualization.Rock -> R.string.audio_test_eq_rock
    TestEqualization.Concert -> R.string.audio_test_eq_concert
}
