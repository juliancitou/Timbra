package com.juliancitou.timbra.ui.screens.comfort

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.audio.music.Genre
import com.juliancitou.timbra.ui.screens.finder.FinderUiState
import com.juliancitou.timbra.ui.screens.finder.SpotifyLink
import com.juliancitou.timbra.ui.screens.finder.playbackLabelRes
import com.juliancitou.timbra.ui.screens.label
import com.juliancitou.timbra.ui.theme.CodeLines
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.PlayingIndicator
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalButtonRow
import com.juliancitou.timbra.ui.theme.TerminalButtonStyle
import com.juliancitou.timbra.ui.theme.TerminalChip
import com.juliancitou.timbra.ui.theme.TerminalScreen
import com.juliancitou.timbra.ui.theme.TerminalSection

private const val AUDIO_MIME_TYPES = "audio/*"

/** De dónde sale la música del test. Se muestra solo lo que corresponde a la elegida. */
private enum class MusicSource { App, Spotify, Song }

private fun FinderUiState.musicSource() = when {
    usesSpotify -> MusicSource.Spotify
    songName != null -> MusicSource.Song
    else -> MusicSource.App
}

/** Elegir qué se escucha en el test y llegar a un volumen cómodo antes de empezar las rondas. */
@Composable
fun ComfortLevelScreen(
    state: FinderUiState,
    onPrepareMusic: () -> Unit,
    onSelectGenre: (Genre) -> Unit,
    onSelectSong: (Uri) -> Unit,
    onUseSpotify: () -> Unit,
    onOpenSpotify: () -> Unit,
    onTogglePlayback: () -> Unit,
    onReady: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { onPrepareMusic() }
    var source by rememberSaveable { mutableStateOf(state.musicSource()) }

    TerminalScreen(
        title = stringResource(R.string.comfort_title),
        lead = stringResource(R.string.comfort_lead),
        onBack = onBack,
        actions = {
            TerminalButtonRow {
                TerminalButton(
                    text = stringResource(state.playbackLabelRes()),
                    onClick = onTogglePlayback,
                    style = TerminalButtonStyle.Secondary,
                    enabled = !state.preparing,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                TerminalButton(
                    text = stringResource(R.string.action_ready),
                    onClick = onReady,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        },
    ) {
        TerminalSection(stringResource(R.string.comfort_section_source)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                TerminalChip(
                    label = stringResource(R.string.comfort_source_app),
                    selected = source == MusicSource.App,
                    onClick = {
                        source = MusicSource.App
                        onSelectGenre(state.genre)
                    },
                )
                TerminalChip(
                    label = stringResource(R.string.comfort_source_spotify),
                    selected = source == MusicSource.Spotify,
                    onClick = {
                        source = MusicSource.Spotify
                        onUseSpotify()
                    },
                )
                TerminalChip(
                    label = stringResource(R.string.comfort_source_song),
                    selected = source == MusicSource.Song,
                    onClick = { source = MusicSource.Song },
                )
            }
        }
        when (source) {
            MusicSource.App -> GenreSection(state, onSelectGenre)
            MusicSource.Spotify -> SpotifySection(state, onOpenSpotify)
            MusicSource.Song -> SongSection(state, onSelectSong, onTogglePlayback)
        }
        TerminalSection(stringResource(R.string.comfort_section_safety)) {
            DetailText(
                stringResource(
                    if (source == MusicSource.Spotify) R.string.comfort_safety_spotify else R.string.comfort_safety,
                ),
            )
        }
    }
}

@Composable
private fun GenreSection(state: FinderUiState, onSelectGenre: (Genre) -> Unit) {
    TerminalSection(stringResource(R.string.comfort_section_genre)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Genre.entries.forEach { genre ->
                TerminalChip(
                    label = stringResource(genre.label()),
                    selected = !state.usesSpotify && state.songName == null && state.genre == genre,
                    onClick = { onSelectGenre(genre) },
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            PlayingIndicator(playing = state.isPlaying)
            if (state.preparing) DetailText(stringResource(R.string.audio_status_preparing))
        }
    }
}

@Composable
private fun SongSection(state: FinderUiState, onSelectSong: (Uri) -> Unit, onTogglePlayback: () -> Unit) {
    val pickSong = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onSelectSong(uri)
    }
    TerminalSection(stringResource(R.string.comfort_section_song)) {
        when {
            state.loadingSong -> DetailText(stringResource(R.string.comfort_song_loading))
            state.songFailed -> DetailText(
                stringResource(R.string.comfort_song_failed),
                color = MaterialTheme.colorScheme.secondary,
            )
            state.songName != null -> DetailText(
                stringResource(R.string.comfort_song_selected, state.songName),
                color = MaterialTheme.colorScheme.primary,
            )
            else -> DetailText(stringResource(R.string.comfort_song_hint))
        }
        TerminalButton(
            text = stringResource(R.string.action_pick_song),
            onClick = {
                if (state.isPlaying) onTogglePlayback()
                pickSong.launch(arrayOf(AUDIO_MIME_TYPES))
            },
            style = TerminalButtonStyle.Secondary,
            enabled = !state.loadingSong,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SpotifySection(state: FinderUiState, onOpenSpotify: () -> Unit) {
    TerminalSection(stringResource(R.string.comfort_section_spotify)) {
        when (state.spotify) {
            SpotifyLink.Connected -> DetailText(
                stringResource(R.string.comfort_spotify_connected),
                color = MaterialTheme.colorScheme.primary,
            )
            SpotifyLink.Blocked -> DetailText(
                stringResource(R.string.comfort_spotify_blocked),
                color = MaterialTheme.colorScheme.secondary,
            )
            SpotifyLink.Failed -> DetailText(
                stringResource(R.string.comfort_spotify_failed),
                color = MaterialTheme.colorScheme.secondary,
            )
            else -> CodeLines(
                listOf(
                    stringResource(R.string.comfort_spotify_step_play),
                    stringResource(R.string.comfort_spotify_step_return),
                    stringResource(R.string.comfort_spotify_step_retry),
                ),
            )
        }
        TerminalButton(
            text = stringResource(R.string.action_open_spotify),
            onClick = onOpenSpotify,
            style = TerminalButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
