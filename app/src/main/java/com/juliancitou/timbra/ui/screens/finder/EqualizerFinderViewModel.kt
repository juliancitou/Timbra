package com.juliancitou.timbra.ui.screens.finder

import android.app.Application
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.juliancitou.timbra.audio.AudioOutput
import com.juliancitou.timbra.audio.EqualizerSettings
import com.juliancitou.timbra.audio.LoopSource
import com.juliancitou.timbra.audio.LoudnessMatcher
import com.juliancitou.timbra.audio.PlaybackStatus
import com.juliancitou.timbra.audio.SignalSource
import com.juliancitou.timbra.audio.music.Genre
import com.juliancitou.timbra.audio.music.MusicFocus
import com.juliancitou.timbra.audio.music.MusicLoop
import com.juliancitou.timbra.audio.presetFromCurve
import com.juliancitou.timbra.export.PowerampExporter
import com.juliancitou.timbra.measure.EqualizerFinder
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

data class FinderUiState(
    val genre: Genre = Genre.Pop,
    val finder: EqualizerFinder = EqualizerFinder(),
    /** Índice del nodo que se está escuchando; empieza en el del centro (la elección actual). */
    val auditionIndex: Int = CENTER_OPTION,
    val preparing: Boolean = true,
    val bypass: Boolean = false,
    val headphonesName: String = "",
    val presetName: String = "",
    val status: PlaybackStatus = PlaybackStatus.Stopped,
) {
    val isPlaying: Boolean get() = status == PlaybackStatus.Playing

    /** Nombre del preset: el que escribió el usuario, o el de sus audífonos, o uno genérico. */
    fun effectivePresetName(fallback: String): String =
        presetName.ifBlank { headphonesName.ifBlank { fallback } }
}

private const val CENTER_OPTION = 2
private const val REFERENCE_SECONDS = 4
private const val STOP_SHARING_AFTER_MS = 5_000L

/**
 * Estado y sonido del test "Encuentra tu ecualización", compartido por todas sus pantallas. Al
 * empezar cada ronda prepara sus cinco opciones con el volumen igualado (para que se elija por el
 * tono y no por el volumen) y la música con la zona de esa ronda resaltada. Lo que se escucha es
 * exactamente el preset que se exportaría, más la compensación de volumen.
 */
class EqualizerFinderViewModel(application: Application) : AndroidViewModel(application) {
    private val synthesis = Executors.newSingleThreadExecutor { task ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            task.run()
        }, "TimbraFinder")
    }.asCoroutineDispatcher()

    /** La salida de audio se crea la primera vez que hace falta, no al abrir la app. */
    private val outputHolder = lazy {
        AudioOutput(application) { SignalSource { 0.0 } }.also { created ->
            viewModelScope.launch { created.status.collect { playback.value = it } }
        }
    }
    private val output by outputHolder
    private val state = MutableStateFlow(FinderUiState())
    private val playback = MutableStateFlow(PlaybackStatus.Stopped)

    private val musicLoops = mutableMapOf<Genre, MusicLoop>()
    private val mixes = mutableMapOf<Pair<Genre, MusicFocus>, DoubleArray>()
    private val matchers = mutableMapOf<Pair<Genre, MusicFocus>, LoudnessMatcher>()
    private var optionSettings: List<EqualizerSettings> = emptyList()
    private var currentLoop: LoopSource? = null
    private var currentGenre: Genre? = null

    val uiState: StateFlow<FinderUiState> = combine(state, playback) { current, status ->
        current.copy(status = status)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_SHARING_AFTER_MS), FinderUiState())

    fun setHeadphonesName(name: String) = state.update { it.copy(headphonesName = name) }

    fun setPresetName(name: String) = state.update { it.copy(presetName = name) }

    /** Prepara la música del género elegido para el nivel cómodo. */
    fun prepareMusic() = refresh()

    fun selectGenre(genre: Genre) {
        state.update { it.copy(genre = genre) }
        refresh()
    }

    fun togglePlayback() {
        if (playback.value == PlaybackStatus.Playing) {
            output.stop()
        } else if (!state.value.preparing) {
            output.play()
        }
    }

    fun stopSound() {
        if (outputHolder.isInitialized()) output.stop()
    }

    /** Empieza el test desde la primera ronda, conservando el género y el nombre de los audífonos. */
    fun startTest() {
        state.update {
            it.copy(finder = EqualizerFinder(), auditionIndex = CENTER_OPTION, bypass = false, presetName = "")
        }
        refresh()
    }

    /** Escucha la opción [index] de la ronda actual. */
    fun audition(index: Int) {
        state.update { it.copy(auditionIndex = index) }
        optionSettings.getOrNull(index)?.let { output.renderer.changeEqualization(it) }
    }

    /** Se queda con la opción que se está escuchando y pasa a la siguiente ronda. */
    fun chooseAudition() {
        val current = state.value
        if (current.preparing || current.finder.isFinished) return
        val chosen = current.finder.options()[current.auditionIndex]
        state.update { it.copy(finder = it.finder.choose(chosen), auditionIndex = CENTER_OPTION) }
        refresh()
    }

    /** Vuelve a la ronda anterior. Devuelve `false` si ya está en la primera. */
    fun previousRound(): Boolean {
        val previous = state.value.finder.previous ?: return false
        state.update { it.copy(finder = previous, auditionIndex = CENTER_OPTION) }
        refresh()
        return true
    }

    /** En el resultado: `true` para escuchar sin ecualizar y comparar. */
    fun setBypass(bypass: Boolean) {
        state.update { it.copy(bypass = bypass) }
        refresh()
    }

    fun presetJson(fallbackName: String): String {
        val current = state.value
        return PowerampExporter.toJson(current.effectivePresetName(fallbackName), current.finder.result())
    }

    fun presetFileName(fallbackName: String): String =
        PowerampExporter.fileName(state.value.effectivePresetName(fallbackName))

    override fun onCleared() {
        if (outputHolder.isInitialized()) output.release()
        synthesis.close()
    }

    /**
     * Pone a sonar la música y la ecualización que corresponden al estado actual. Las opciones se
     * calculan en el hilo de síntesis; mientras tanto, la ronda queda en "preparando".
     */
    private fun refresh() {
        state.update { it.copy(preparing = true) }
        viewModelScope.launch(synthesis) {
            val snapshot = state.value
            val focus = if (snapshot.finder.isFinished) MusicFocus.Balanced else snapshot.finder.round.dimension.focus
            val mix = mixFor(snapshot.genre, focus)
            val matcher = matcherFor(snapshot.genre, focus, mix)
            optionSettings = optionsFor(snapshot.finder, matcher)
            playLoop(mix, sameSong = currentGenre == snapshot.genre)
            currentGenre = snapshot.genre
            val latest = state.value
            val settings = when {
                latest.finder.isFinished && latest.bypass -> matcher.compensated(EqualizerSettings.Flat)
                latest.finder.isFinished -> matcher.compensated(latest.finder.result())
                else -> optionSettings.getOrNull(latest.auditionIndex) ?: EqualizerSettings.Flat
            }
            output.renderer.changeEqualization(settings)
            if (latest.finder == snapshot.finder && latest.genre == snapshot.genre) {
                state.update { it.copy(preparing = false) }
            }
        }
    }

    private fun optionsFor(finder: EqualizerFinder, matcher: LoudnessMatcher): List<EqualizerSettings> =
        if (finder.isFinished) {
            emptyList()
        } else {
            finder.options().map { option -> matcher.compensated(presetFromCurve(finder.curveWithCandidateDb(option))) }
        }

    private fun playLoop(mix: DoubleArray, sameSong: Boolean) {
        val loop = LoopSource(mix, startIndex = if (sameSong) currentLoop?.position ?: 0 else 0)
        currentLoop = loop
        output.renderer.changeSource(loop)
    }

    private fun mixFor(genre: Genre, focus: MusicFocus): DoubleArray {
        val loop = musicLoops.getOrPut(genre) { MusicLoop(output.sampleRate, genre.song) }
        return mixes.getOrPut(genre to focus) { loop.mix(focus) }
    }

    /** El volumen se iguala con los primeros segundos de la misma música que suena. */
    private fun matcherFor(genre: Genre, focus: MusicFocus, mix: DoubleArray): LoudnessMatcher =
        matchers.getOrPut(genre to focus) {
            val length = minOf(mix.size, REFERENCE_SECONDS * output.sampleRate)
            LoudnessMatcher(output.sampleRate, mix.copyOfRange(0, length))
        }
}
