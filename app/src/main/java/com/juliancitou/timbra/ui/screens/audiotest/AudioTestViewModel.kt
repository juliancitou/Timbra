package com.juliancitou.timbra.ui.screens.audiotest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.juliancitou.timbra.audio.AudioOutput
import com.juliancitou.timbra.audio.EqualizerSettings
import com.juliancitou.timbra.audio.LoopSource
import com.juliancitou.timbra.audio.LoudnessMatcher
import com.juliancitou.timbra.audio.PinkNoise
import com.juliancitou.timbra.audio.PlaybackStatus
import com.juliancitou.timbra.audio.SignalSource
import com.juliancitou.timbra.audio.SineTone
import com.juliancitou.timbra.audio.music.MusicFocus
import com.juliancitou.timbra.audio.music.MusicLoop
import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import com.juliancitou.timbra.ui.screens.SampleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class TestSignal { Music, PinkNoise, Tone }

enum class TestEqualization { Flat, SamplePreset, MoreBass, LessTreble }

data class AudioTestUiState(
    val signal: TestSignal = TestSignal.Music,
    val focus: MusicFocus = MusicFocus.Balanced,
    val equalization: TestEqualization = TestEqualization.Flat,
    val matchLoudness: Boolean = true,
    val musicReady: Boolean = false,
    val status: PlaybackStatus = PlaybackStatus.Stopped,
) {
    val canPlay: Boolean get() = signal != TestSignal.Music || musicReady
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

    private val musicMutex = Mutex()
    private var musicLoop: MusicLoop? = null
    private val mixes = mutableMapOf<MusicFocus, DoubleArray>()
    private val matchers = mutableMapOf<Any, LoudnessMatcher>()
    private var currentLoop: LoopSource? = null

    val uiState: StateFlow<AudioTestUiState> = combine(selection, output.status) { selected, status ->
        selected.copy(status = status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_SHARING_AFTER_MS), AudioTestUiState())

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val mix = mixFor(MusicFocus.Balanced)
            selection.update { it.copy(musicReady = true) }
            if (selection.value.signal == TestSignal.Music) playLoop(mix)
            applyEqualization()
        }
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
        viewModelScope.launch(Dispatchers.Default) {
            when (signal) {
                TestSignal.Music -> if (selection.value.musicReady) playLoop(mixFor(selection.value.focus))
                TestSignal.PinkNoise -> changeSource(PinkNoise())
                TestSignal.Tone -> changeSource(SineTone(TONE_HZ, TONE_AMPLITUDE, output.sampleRate))
            }
            applyEqualization()
        }
    }

    fun selectFocus(focus: MusicFocus) {
        selection.update { it.copy(focus = focus) }
        viewModelScope.launch(Dispatchers.Default) {
            if (selection.value.signal == TestSignal.Music) playLoop(mixFor(focus))
            applyEqualization()
        }
    }

    fun selectEqualization(equalization: TestEqualization) {
        selection.update { it.copy(equalization = equalization) }
        viewModelScope.launch(Dispatchers.Default) { applyEqualization() }
    }

    fun setLoudnessMatching(enabled: Boolean) {
        selection.update { it.copy(matchLoudness = enabled) }
        viewModelScope.launch(Dispatchers.Default) { applyEqualization() }
    }

    override fun onCleared() = output.release()

    /** Cambia a [mix] en el mismo punto de la pieza, para que cambiar de zona no reinicie el bucle. */
    private fun playLoop(mix: DoubleArray) {
        val loop = LoopSource(mix, startIndex = currentLoop?.position ?: 0)
        currentLoop = loop
        output.renderer.changeSource(loop)
    }

    private fun changeSource(source: SignalSource) {
        currentLoop = null
        output.renderer.changeSource(source)
    }

    private suspend fun mixFor(focus: MusicFocus): DoubleArray = musicMutex.withLock {
        val loop = musicLoop ?: MusicLoop(output.sampleRate).also { musicLoop = it }
        mixes.getOrPut(focus) { loop.mix(focus) }
    }

    private suspend fun applyEqualization() {
        val selected = selection.value
        val settings = settingsFor(selected.equalization)
        val final = if (selected.matchLoudness) matcherFor(selected).compensated(settings) else settings
        output.renderer.changeEqualization(final)
    }

    /** El volumen se iguala con la misma señal que está sonando; cada igualador se calcula una vez. */
    private suspend fun matcherFor(selected: AudioTestUiState): LoudnessMatcher {
        val usesMusic = selected.signal == TestSignal.Music && selected.musicReady
        val key: Any = if (usesMusic) selected.focus else selected.signal
        musicMutex.withLock { matchers[key] }?.let { return it }
        val reference = if (usesMusic) mixFor(selected.focus) else pinkReference()
        val matcher = LoudnessMatcher(output.sampleRate, reference)
        musicMutex.withLock { matchers[key] = matcher }
        return matcher
    }

    private fun pinkReference(): DoubleArray {
        val noise = PinkNoise(seed = 11)
        return DoubleArray(PINK_REFERENCE_SECONDS * output.sampleRate) { noise.next() }
    }

    private fun settingsFor(equalization: TestEqualization): EqualizerSettings = when (equalization) {
        TestEqualization.Flat -> EqualizerSettings.Flat
        TestEqualization.SamplePreset -> EqualizerSettings(SampleData.curveDb, preampDb = -4.5f)
        TestEqualization.MoreBass -> bandsWhere(gainDb = 3f) { it <= 112 }
        TestEqualization.LessTreble -> bandsWhere(gainDb = -6f) { it >= 4743 }
    }

    private fun bandsWhere(gainDb: Float, includes: (Int) -> Boolean) =
        EqualizerSettings(POWERAMP_BAND_FREQUENCIES_HZ.map { if (includes(it)) gainDb else 0f })
}
