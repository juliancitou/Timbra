package com.juliancitou.timbra.ui.screens.audiotest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.juliancitou.timbra.audio.AudioOutput
import com.juliancitou.timbra.audio.EqualizerSettings
import com.juliancitou.timbra.audio.LoudnessMatcher
import com.juliancitou.timbra.audio.PinkNoise
import com.juliancitou.timbra.audio.PlaybackStatus
import com.juliancitou.timbra.audio.SignalSource
import com.juliancitou.timbra.audio.SineTone
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

enum class TestSignal { PinkNoise, Tone }

enum class TestEqualization { Flat, SamplePreset, MoreBass, LessTreble }

data class AudioTestUiState(
    val signal: TestSignal = TestSignal.PinkNoise,
    val equalization: TestEqualization = TestEqualization.Flat,
    val matchLoudness: Boolean = true,
    val status: PlaybackStatus = PlaybackStatus.Stopped,
)

private const val TONE_HZ = 1000.0
private const val TONE_AMPLITUDE = 0.1
private const val REFERENCE_SECONDS = 2
private const val STOP_SHARING_AFTER_MS = 5_000L

/**
 * Pantalla temporal de la Fase 2 para escuchar el motor de audio en el teléfono: cambiar de señal
 * y de ecualización mientras suena permite comprobar que no hay clics ni saltos de volumen.
 */
class AudioTestViewModel(application: Application) : AndroidViewModel(application) {
    private val output = AudioOutput(application) { PinkNoise() }
    private val selection = MutableStateFlow(AudioTestUiState())

    private val loudnessMatcher by lazy {
        val noise = PinkNoise(seed = 11)
        LoudnessMatcher(output.sampleRate, DoubleArray(REFERENCE_SECONDS * output.sampleRate) { noise.next() })
    }

    val uiState: StateFlow<AudioTestUiState> = combine(selection, output.status) { selected, status ->
        selected.copy(status = status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_SHARING_AFTER_MS), AudioTestUiState())

    fun togglePlayback() {
        if (output.status.value == PlaybackStatus.Playing) output.stop() else output.play()
    }

    fun stop() = output.stop()

    fun selectSignal(signal: TestSignal) {
        selection.update { it.copy(signal = signal) }
        output.renderer.changeSource(createSource(signal))
    }

    fun selectEqualization(equalization: TestEqualization) {
        selection.update { it.copy(equalization = equalization) }
        applyEqualization()
    }

    fun setLoudnessMatching(enabled: Boolean) {
        selection.update { it.copy(matchLoudness = enabled) }
        applyEqualization()
    }

    override fun onCleared() = output.release()

    private fun applyEqualization() {
        val selected = selection.value
        viewModelScope.launch(Dispatchers.Default) {
            val settings = settingsFor(selected.equalization)
            output.renderer.changeEqualization(
                if (selected.matchLoudness) loudnessMatcher.compensated(settings) else settings,
            )
        }
    }

    private fun createSource(signal: TestSignal): SignalSource = when (signal) {
        TestSignal.PinkNoise -> PinkNoise()
        TestSignal.Tone -> SineTone(TONE_HZ, TONE_AMPLITUDE, output.sampleRate)
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
