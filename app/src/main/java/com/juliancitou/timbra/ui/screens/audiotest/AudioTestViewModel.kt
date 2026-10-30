package com.juliancitou.timbra.ui.screens.audiotest

import android.app.Application
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.juliancitou.timbra.audio.AudioOutput
import com.juliancitou.timbra.audio.EqualizerSettings
import com.juliancitou.timbra.audio.EqualizerShapes.bell
import com.juliancitou.timbra.audio.EqualizerShapes.highShelf
import com.juliancitou.timbra.audio.EqualizerShapes.lowShelf
import com.juliancitou.timbra.audio.EqualizerShapes.sum
import com.juliancitou.timbra.audio.LoopSource
import com.juliancitou.timbra.audio.LoudnessMatcher
import com.juliancitou.timbra.audio.PinkNoise
import com.juliancitou.timbra.audio.PlaybackStatus
import com.juliancitou.timbra.audio.SignalSource
import com.juliancitou.timbra.audio.SineTone
import com.juliancitou.timbra.audio.presetFromCurve
import com.juliancitou.timbra.audio.music.Genre
import com.juliancitou.timbra.audio.music.MusicFocus
import com.juliancitou.timbra.audio.music.MusicLoop
import com.juliancitou.timbra.ui.screens.SampleData
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

enum class TestSignal { Music, PinkNoise, Tone }

/** Ecualizaciones para comparar de oído; las de curva siguen el formato de Poweramp (redondeo y preamp). */
enum class TestEqualization {
    Flat, SamplePreset, MoreBass, LessBass, LessMud, MoreVoice, LessSibilance,
    MoreBrightness, LessTreble, VShape, Warm, Bright, Rock, Concert,
}

data class AudioTestUiState(
    val signal: TestSignal = TestSignal.Music,
    val genre: Genre = Genre.Pop,
    val focus: MusicFocus = MusicFocus.Balanced,
    val equalization: TestEqualization = TestEqualization.Flat,
    val matchLoudness: Boolean = true,
    val preparingMusic: Boolean = true,
    val status: PlaybackStatus = PlaybackStatus.Stopped,
) {
    val canPlay: Boolean get() = signal != TestSignal.Music || !preparingMusic
}

private const val TONE_HZ = 1000.0
private const val TONE_AMPLITUDE = 0.1
private const val PINK_REFERENCE_SECONDS = 2
private const val STOP_SHARING_AFTER_MS = 5_000L

/**
 * Pantalla temporal para escuchar el motor de audio y la música del test en el teléfono: cambiar
 * de señal, zona y ecualización mientras suena permite comprobar que no hay clics ni saltos de
 * volumen. La música se genera en segundo plano al abrir la pantalla (tarda un par de segundos).
 */
class AudioTestViewModel(application: Application) : AndroidViewModel(application) {
    private val output = AudioOutput(application) { PinkNoise() }
    private val selection = MutableStateFlow(AudioTestUiState())

    /**
     * Hilo único y de baja prioridad para generar música e igualar volumen: el trabajo pesado no le
     * quita tiempo a la interfaz, y al ser un solo hilo no hace falta sincronizar las memorias.
     */
    private val synthesis = Executors.newSingleThreadExecutor { task ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            task.run()
        }, "TimbraSynthesis")
    }.asCoroutineDispatcher()

    private val musicLoops = mutableMapOf<Genre, MusicLoop>()
    private val mixes = mutableMapOf<Pair<Genre, MusicFocus>, DoubleArray>()
    private val matchers = mutableMapOf<Any, LoudnessMatcher>()
    private var currentLoop: LoopSource? = null
    private var currentGenre: Genre? = null

    val uiState: StateFlow<AudioTestUiState> = combine(selection, output.status) { selected, status ->
        selected.copy(status = status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_SHARING_AFTER_MS), AudioTestUiState())

    init {
        viewModelScope.launch(synthesis) { refreshMusic() }
    }

    fun togglePlayback() {
        if (output.status.value == PlaybackStatus.Playing) {
            output.stop()
        } else if (selection.value.canPlay) {
            output.play()
        }
    }

    fun stop() = output.stop()

    fun selectSignal(signal: TestSignal) {
        selection.update { it.copy(signal = signal) }
        viewModelScope.launch(synthesis) {
            when (signal) {
                TestSignal.Music -> refreshMusic()
                TestSignal.PinkNoise -> changeSource(PinkNoise())
                TestSignal.Tone -> changeSource(SineTone(TONE_HZ, TONE_AMPLITUDE, output.sampleRate))
            }
            applyEqualization()
        }
    }

    fun selectGenre(genre: Genre) {
        selection.update { it.copy(genre = genre) }
        viewModelScope.launch(synthesis) { refreshMusic() }
    }

    fun selectFocus(focus: MusicFocus) {
        selection.update { it.copy(focus = focus) }
        viewModelScope.launch(synthesis) { refreshMusic() }
    }

    fun selectEqualization(equalization: TestEqualization) {
        selection.update { it.copy(equalization = equalization) }
        viewModelScope.launch(synthesis) { applyEqualization() }
    }

    fun setLoudnessMatching(enabled: Boolean) {
        selection.update { it.copy(matchLoudness = enabled) }
        viewModelScope.launch(synthesis) { applyEqualization() }
    }

    override fun onCleared() {
        output.release()
        synthesis.close()
    }

    /**
     * Genera (o toma de la memoria) la mezcla del género y la zona elegidos y la pone a sonar. La
     * primera vez que se elige un género tarda un par de segundos; mientras, no se puede reproducir.
     */
    private fun refreshMusic() {
        val selected = selection.value
        if (selected.signal != TestSignal.Music) return
        selection.update { it.copy(preparingMusic = true) }
        val mix = mixFor(selected.genre, selected.focus)
        val latest = selection.value
        if (latest.signal == TestSignal.Music && latest.genre == selected.genre && latest.focus == selected.focus) {
            playLoop(mix, sameSong = currentGenre == selected.genre)
            currentGenre = selected.genre
            selection.update { it.copy(preparingMusic = false) }
        }
        applyEqualization()
    }

    /**
     * Cambia a [mix]. Dentro de la misma canción conserva el punto de la pieza, para que cambiar de
     * zona no reinicie el bucle; con otra canción empieza desde el inicio.
     */
    private fun playLoop(mix: DoubleArray, sameSong: Boolean) {
        val loop = LoopSource(mix, startIndex = if (sameSong) currentLoop?.position ?: 0 else 0)
        currentLoop = loop
        output.renderer.changeSource(loop)
    }

    private fun changeSource(source: SignalSource) {
        currentLoop = null
        currentGenre = null
        output.renderer.changeSource(source)
    }

    private fun mixFor(genre: Genre, focus: MusicFocus): DoubleArray {
        val loop = musicLoops.getOrPut(genre) { MusicLoop(output.sampleRate, genre.song) }
        return mixes.getOrPut(genre to focus) { loop.mix(focus) }
    }

    private fun applyEqualization() {
        val selected = selection.value
        val settings = settingsFor(selected.equalization)
        val final = if (selected.matchLoudness) matcherFor(selected).compensated(settings) else settings
        output.renderer.changeEqualization(final)
    }

    /** El volumen se iguala con la misma señal que está sonando; cada igualador se calcula una vez. */
    private fun matcherFor(selected: AudioTestUiState): LoudnessMatcher {
        val usesMusic = selected.signal == TestSignal.Music
        val key: Any = if (usesMusic) selected.genre to selected.focus else selected.signal
        matchers[key]?.let { return it }
        val reference = if (usesMusic) mixFor(selected.genre, selected.focus) else pinkReference()
        val matcher = LoudnessMatcher(output.sampleRate, reference)
        matchers[key] = matcher
        return matcher
    }

    private fun pinkReference(): DoubleArray {
        val noise = PinkNoise(seed = 11)
        return DoubleArray(PINK_REFERENCE_SECONDS * output.sampleRate) { noise.next() }
    }

    private fun settingsFor(equalization: TestEqualization): EqualizerSettings = when (equalization) {
        TestEqualization.Flat -> EqualizerSettings.Flat
        TestEqualization.SamplePreset -> EqualizerSettings(SampleData.curveDb, preampDb = -4.5f)
        TestEqualization.MoreBass -> presetFromCurve(lowShelf(120.0, 3.0))
        TestEqualization.LessBass -> presetFromCurve(lowShelf(120.0, -4.0))
        TestEqualization.LessMud -> presetFromCurve(bell(250.0, -3.0, widthOctaves = 1.5))
        TestEqualization.MoreVoice -> presetFromCurve(bell(2000.0, 3.0, widthOctaves = 1.5))
        TestEqualization.LessSibilance -> presetFromCurve(bell(7500.0, -5.0, widthOctaves = 1.0))
        TestEqualization.MoreBrightness -> presetFromCurve(highShelf(6000.0, 3.0))
        TestEqualization.LessTreble -> presetFromCurve(highShelf(5000.0, -5.0))
        TestEqualization.VShape -> presetFromCurve(
            sum(lowShelf(100.0, 3.0), bell(1000.0, -3.0, 2.0), highShelf(8000.0, 3.0)),
        )
        TestEqualization.Warm -> presetFromCurve(sum(lowShelf(200.0, 2.0), highShelf(4000.0, -3.0)))
        TestEqualization.Bright -> presetFromCurve(sum(lowShelf(200.0, -2.0), highShelf(4000.0, 3.0)))
        TestEqualization.Rock -> presetFromCurve(
            sum(lowShelf(90.0, 3.0), bell(500.0, -2.0, 1.5), bell(3000.0, 2.0, 1.5)),
        )
        TestEqualization.Concert -> presetFromCurve(
            sum(bell(250.0, -1.5, 1.5), bell(3500.0, 1.0, 2.0), highShelf(12_000.0, 1.5)),
        )
    }
}
