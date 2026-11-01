package com.juliancitou.timbra.ui.screens.finder

import android.app.Application
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Process
import android.provider.OpenableColumns
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.juliancitou.timbra.R
import com.juliancitou.timbra.audio.AudioOutput
import com.juliancitou.timbra.audio.AutoEqProfile
import com.juliancitou.timbra.audio.BandOverlap
import com.juliancitou.timbra.audio.EqualizerSettings
import com.juliancitou.timbra.audio.LoopSource
import com.juliancitou.timbra.audio.LoudnessMatcher
import com.juliancitou.timbra.audio.alignedToItsMedian
import com.juliancitou.timbra.audio.bandDifferenceDb
import com.juliancitou.timbra.audio.cutDepthDb
import com.juliancitou.timbra.audio.PinkNoise
import com.juliancitou.timbra.audio.PowerampControl
import com.juliancitou.timbra.audio.PowerampSettings
import com.juliancitou.timbra.audio.PowerampState
import com.juliancitou.timbra.audio.PlaybackStatus
import com.juliancitou.timbra.audio.PreparedSong
import com.juliancitou.timbra.audio.PreviewBand
import com.juliancitou.timbra.audio.SPOTIFY_PACKAGE
import com.juliancitou.timbra.audio.SessionEqualizer
import com.juliancitou.timbra.audio.SessionEqualizerResult
import com.juliancitou.timbra.audio.SignalSource
import com.juliancitou.timbra.audio.SpotifySession
import com.juliancitou.timbra.audio.StereoLoopSource
import com.juliancitou.timbra.audio.confirmedBandCount
import com.juliancitou.timbra.audio.decodeAudioFile
import com.juliancitou.timbra.audio.gainsAtFrequencies
import com.juliancitou.timbra.audio.measureEqualizerDifference
import com.juliancitou.timbra.audio.music.Genre
import com.juliancitou.timbra.audio.music.MusicFocus
import com.juliancitou.timbra.audio.music.MusicLoop
import com.juliancitou.timbra.audio.prepareSong
import com.juliancitou.timbra.audio.previewBands
import com.juliancitou.timbra.audio.recordMicrophone
import com.juliancitou.timbra.audio.resample
import com.juliancitou.timbra.audio.startingCurveDb
import com.juliancitou.timbra.data.BandLayoutPreferences
import com.juliancitou.timbra.data.BandOverlapPreferences
import com.juliancitou.timbra.data.PreviewStrengthPreferences
import com.juliancitou.timbra.export.BandLayout
import com.juliancitou.timbra.export.PowerampExporter
import com.juliancitou.timbra.measure.BLIND_OPTIONS
import com.juliancitou.timbra.measure.BlindTest
import com.juliancitou.timbra.measure.EqualizerFinder
import com.juliancitou.timbra.measure.OutputKind
import com.juliancitou.timbra.measure.calibrationPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.random.Random

private const val MIN_PREVIEW_STRENGTH = 0.2
private const val FULL_PREVIEW_STRENGTH = 1.0

/** Estado de la ecualización en vivo de Spotify. [Off]: suena música de Timbra, no Spotify. */
enum class SpotifyLink { Off, Waiting, Connected, Blocked, Failed }

/** Cada modo cambia cómo suenan los audífonos, así que el preset es válido solo para el modo en que se hizo. */
enum class NoiseMode { Cancelling, Aware, Off }

/** Lo que hacen Poweramp y Timbra con el mismo preset de prueba, medido en la salida del teléfono. */
sealed interface PowerampMeasure {
    data object Idle : PowerampMeasure
    data object Running : PowerampMeasure

    /** No se pudo grabar, o no se notó ni el ecualizador de Timbra: la medición no es válida. */
    data object CouldNotCapture : PowerampMeasure

    /** Falta importar el preset de medición. */
    data object PresetMissing : PowerampMeasure

    /** La señal apenas le gana al ruido del cuarto. */
    data object TooQuiet : PowerampMeasure

    /** La medición funciona, pero Poweramp no aplicó el preset de prueba. */
    data object NoEffect : PowerampMeasure

    data class Done(
        val frequenciesHz: List<Int>,
        val measuredDb: List<Float>,
        val expectedDb: List<Float>,
        val timbraCutDb: Float,
        val powerampCutDb: Float,
    ) : PowerampMeasure {
        /** Qué fracción del corte de Timbra entregó Poweramp. */
        val strength: Double
            get() = (powerampCutDb / timbraCutDb).toDouble().coerceIn(MIN_PREVIEW_STRENGTH, FULL_PREVIEW_STRENGTH)
    }
}

/** De dónde salió lo que se muestra como "lo que suena ahora". */
sealed interface SoundingProof {
    /** Comparando el audio de la app con y sin ecualizar. */
    data object MeasuredInAudio : SoundingProof

    /** Leído del efecto puesto sobre Spotify. */
    data class ReadFromEffect(val confirmedBands: Int, val totalBands: Int, val active: Boolean) : SoundingProof {
        val isConfirmed: Boolean get() = active && confirmedBands == totalBands
    }
}

/** Cuánto cambia ahora cada banda del preset, en dB; `null` donde no se pudo medir. */
data class SoundingCheck(val gainsDb: List<Float?>, val proof: SoundingProof)

data class FinderUiState(
    val genre: Genre = Genre.Pop,
    val finder: EqualizerFinder = EqualizerFinder(),
    /** Índice del nodo que se está escuchando; empieza en el del centro (la elección actual). */
    val auditionIndex: Int = CENTER_OPTION,
    val preparing: Boolean = true,
    val bypass: Boolean = false,
    val headphonesName: String = "",
    val presetName: String = "",
    /** Nombre de la canción del usuario que suena; `null` si suena la música de la app. */
    val songName: String? = null,
    val loadingSong: Boolean = false,
    val songFailed: Boolean = false,
    val spotify: SpotifyLink = SpotifyLink.Off,
    val bandOverlap: BandOverlap = BandOverlap.Envelope,
    val sounding: SoundingCheck? = null,
    val poweramp: PowerampState = PowerampState.Unknown,
    val powerampSettings: PowerampSettings? = null,
    val noiseMode: NoiseMode = NoiseMode.Cancelling,
    val autoEq: AutoEqProfile? = null,
    val useAutoEq: Boolean = true,
    /** Si el preset con este nombre ya está en Poweramp. */
    val presetImported: Boolean = false,
    val blindTest: BlindTest? = null,
    /** La opción que se está escuchando en la prueba a ciegas; ninguna hasta tocar una. */
    val blindOption: Int? = null,
    val powerampMeasure: PowerampMeasure = PowerampMeasure.Idle,
    /** Fracción del preset con la que suena la vista previa; la elige el usuario tras medir. */
    val previewStrength: Double = 1.0,
    val status: PlaybackStatus = PlaybackStatus.Stopped,
) {
    val isPlaying: Boolean get() = status == PlaybackStatus.Playing
    val usesSpotify: Boolean get() = spotify != SpotifyLink.Off

    /** Nombre del preset: el que escribió el usuario o, si no escribió ninguno, el sugerido. */
    fun effectivePresetName(suggested: String): String = presetName.ifBlank { suggested }

    /** Otro estado del test, escuchando de entrada lo que ya hay y no lo que esté en el centro. */
    fun withFinder(next: EqualizerFinder): FinderUiState {
        val startsOnCurrent = !next.isFinished && !next.round.recheck
        return copy(finder = next, auditionIndex = if (startsOnCurrent) next.currentOptionIndex else CENTER_OPTION)
    }
}

private const val CENTER_OPTION = 2
private const val REFERENCE_SECONDS = 4
private const val MEASURE_PRESET_NAME = "Medicion"

/** Con 2 minutos basta para el test, y una canción en estéreo no ocupa más de ~50 MB de memoria. */
private const val MAX_SONG_SECONDS = 120
private const val STOP_SHARING_AFTER_MS = 5_000L
private const val POWERAMP_STOP_WAIT_MS = 1_000L

/**
 * Nivel de las opciones respecto al sonido original. Igualar el volumen hacia arriba las dejaría sin margen: con música
 * masterizada actuaría el limitador, que aplasta los golpes del bajo y endurece los agudos, y sonarían más fuerte que en
 * Poweramp. Es lo que baja el preamp de un preset típico.
 */
private const val PREVIEW_LEVEL_DB = -6f
private const val MEASURE_NOISE_SECONDS = 8
private const val MEASURE_SETTLE_MS = 3_000L
private const val MEASURE_CAPTURE_SECONDS = 6

/** Con menos, lo grabado en esa banda es en buena parte ruido del cuarto y no se puede comparar. */
private const val MEASURE_MIN_OVER_ROOM_DB = 12f
private const val MEASURE_MIN_CUT_BANDS = 2

/** El preset de prueba corta −8 dB: si esas bandas casi no bajan con Timbra, la medición no es válida. */
private const val MEASURE_MIN_TIMBRA_CUT_DB = 3f

/** Poweramp puede cortar mucho menos de lo que muestra: en el teléfono del usuario, cerca de −2 dB. Eso es un resultado. */
private const val MEASURE_MIN_POWERAMP_CUT_DB = 0.5f

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
    private val bandPreferences = BandLayoutPreferences(application)
    private val overlapPreferences = BandOverlapPreferences(application)
    private val strengthPreferences = PreviewStrengthPreferences(application)
    private val state = MutableStateFlow(
        FinderUiState(finder = EqualizerFinder(layout = bandPreferences.load()), bandOverlap = overlapPreferences.load(), previewStrength = strengthPreferences.load()),
    )
    private val playback = MutableStateFlow(PlaybackStatus.Stopped)

    private val musicLoops = mutableMapOf<Genre, MusicLoop>()
    private val mixes = mutableMapOf<Pair<Genre, MusicFocus>, DoubleArray>()
    private val matchers = mutableMapOf<Pair<Genre, MusicFocus>, LoudnessMatcher>()
    private var optionSettings: List<EqualizerSettings> = emptyList()
    private var currentLoop: LoopSource? = null
    private var currentGenre: Genre? = null

    /** Solo se tocan desde el hilo de síntesis. */
    private var userSong: PreparedSong? = null
    private var userSongMatcher: LoudnessMatcher? = null
    private var playingSong: PreparedSong? = null
    private var pinkNoiseMatcher: LoudnessMatcher? = null

    /** Las llamadas al efecto van a otro proceso: una cola propia evita trabar pantalla y síntesis. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val effectThread = Dispatchers.Default.limitedParallelism(1)
    private val sessionEqualizer = SessionEqualizer()
    private var spotifySettings: EqualizerSettings = EqualizerSettings.Flat
    private var calibrating = false
    private val powerampControl = PowerampControl(application)
    private var powerampOffByTimbra = false

    /** Solo se tocan desde el hilo de síntesis. Medir toma cerca de un segundo: se guarda el resultado. */
    private var measuredFor: Any? = null
    private var measuredGains: List<Float?> = emptyList()

    val uiState: StateFlow<FinderUiState> = combine(state, playback) { current, status ->
        current.copy(status = status)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_SHARING_AFTER_MS), FinderUiState())

    init {
        viewModelScope.launch { SpotifySession.id.collect { syncSpotifyEqualizer() } }
    }

    fun setHeadphonesName(name: String) = state.update { it.copy(headphonesName = name) }

    fun setNoiseMode(mode: NoiseMode) = state.update { it.copy(noiseMode = mode) }

    fun setUseAutoEq(use: Boolean) = state.update { it.copy(useAutoEq = use) }

    /** Busca la medición de los audífonos conectados entre los presets de Poweramp. */
    fun loadAutoEq() {
        val current = state.value
        val device = current.headphonesName.trim()
        viewModelScope.launch(effectThread) {
            val profile = if (device.isEmpty()) {
                null
            } else {
                powerampControl.autoEqFor(device, current.noiseMode == NoiseMode.Cancelling)
            }
            state.update { it.copy(autoEq = profile) }
        }
    }

    /** Actualiza si el preset [fullName] ya está en Poweramp, por ejemplo al volver de importarlo. */
    fun checkPresetImported(fullName: String) {
        viewModelScope.launch(effectThread) {
            val imported = powerampControl.hasPreset(fullName)
            state.update { it.copy(presetImported = imported) }
        }
    }

    fun setPresetName(name: String) = state.update { it.copy(presetName = name) }

    /** Cambiar la salida reinicia el test, porque cada una parte de una curva y unas rondas distintas. */
    fun setOutput(output: OutputKind) = state.update {
        if (it.finder.output == output) it else it.copy(finder = EqualizerFinder(output, it.finder.layout))
    }

    fun setBandLayout(layout: BandLayout) {
        bandPreferences.save(layout)
        state.update { it.copy(finder = EqualizerFinder(it.finder.output, layout)) }
    }

    /**
     * Copia de Poweramp las bandas y la superposición, para que el test suene como el preset. Reinicia el
     * test si cambian las bandas: se llama solo antes de empezar, no a mitad de las rondas.
     */
    fun syncWithPoweramp() {
        viewModelScope.launch(effectThread) {
            val settings = powerampControl.readSettings()
            state.update { it.copy(powerampSettings = settings) }
            if (settings == null) return@launch
            overlapPreferences.save(settings.overlap)
            state.update { it.copy(bandOverlap = settings.overlap) }
            settings.layout?.takeIf { it != state.value.finder.layout }?.let(::setBandLayout)
        }
    }

    /** Vuelve a leer los ajustes sin tocar el test, por si se cambiaron en Poweramp. */
    fun refreshPowerampSettings() {
        viewModelScope.launch(effectThread) {
            val settings = powerampControl.readSettings()
            state.update { it.copy(powerampSettings = settings) }
        }
    }

    /** Sin Spotify abierto no hay sesión que revisar y el estado queda como desconocido. */
    fun checkPoweramp() {
        viewModelScope.launch(effectThread) {
            // El efecto propio contaría como "otra app" controlando la sesión.
            sessionEqualizer.release()
            val found = readPowerampState()
            val settings = powerampControl.readSettings()
            state.update { it.copy(poweramp = found, powerampSettings = settings) }
        }
    }

    /** Solo cuando el usuario lo pide. Al terminar el test se vuelve a encender con [restorePoweramp]. */
    fun turnOffPoweramp() {
        powerampControl.turnOff()
        powerampOffByTimbra = true
        state.update { it.copy(poweramp = PowerampState.Off) }
        viewModelScope.launch(effectThread) {
            delay(POWERAMP_STOP_WAIT_MS)
            val found = readPowerampState()
            if (found == PowerampState.On) powerampOffByTimbra = false
            state.update { it.copy(poweramp = found) }
        }
    }

    /** Deja Poweramp como estaba, y solo si fue Timbra quien lo apagó. */
    fun restorePoweramp() {
        if (!powerampOffByTimbra) return
        powerampOffByTimbra = false
        powerampControl.turnOn()
        state.update { it.copy(poweramp = PowerampState.Unknown) }
    }

    private fun readPowerampState(): PowerampState {
        if (!powerampControl.isInstalled) return PowerampState.NotInstalled
        if (powerampControl.readSettings()?.audioProcessingOn == false) return PowerampState.Off
        val sessionId = SpotifySession.id.value
        val controlling = sessionId?.let(powerampControl::isControlling)
        return when {
            controlling == true -> PowerampState.On
            controlling == false || powerampOffByTimbra -> PowerampState.Off
            else -> PowerampState.Unknown
        }
    }

    /** Prepara la música del género elegido para el nivel cómodo. */
    fun prepareMusic() = refresh()

    fun selectGenre(genre: Genre) {
        if (state.value.loadingSong) return
        leaveSpotify()
        state.update { it.copy(genre = genre, songName = null, songFailed = false) }
        refresh()
    }

    /** Lee la canción elegida con el selector del sistema. No empieza a sonar sola. */
    fun selectSong(uri: Uri) {
        stopSound()
        leaveSpotify()
        state.update { it.copy(loadingSong = true, songFailed = false, preparing = true) }
        viewModelScope.launch(synthesis) {
            // También cubre quedarse sin memoria: el test puede seguir con la música de la app.
            val loaded = runCatching { loadSong(uri) }.getOrNull()
            if (loaded != null) {
                userSong = loaded
                userSongMatcher = LoudnessMatcher(output.sampleRate, loaded.reference)
            }
            state.update {
                it.copy(
                    loadingSong = false,
                    songFailed = loaded == null,
                    songName = if (loaded != null) songDisplayName(uri) else it.songName,
                )
            }
            refresh()
        }
    }

    fun togglePlayback() {
        if (state.value.usesSpotify) {
            pressMediaPlayPause()
        } else if (playback.value == PlaybackStatus.Playing) {
            output.stop()
        } else if (!state.value.preparing) {
            output.play()
        }
    }

    fun stopSound() {
        if (outputHolder.isInitialized()) output.stop()
    }

    /** Al salir de las pantallas del test, Spotify vuelve a sonar sin ecualizar. */
    fun stopTestAudio() {
        stopSound()
        if (calibrating) {
            calibrating = false
            leaveSpotify()
        }
        viewModelScope.launch(effectThread) { sessionEqualizer.release() }
    }

    /** Spotify suena por su cuenta y sus cambios de ecualización se aplican en vivo. */
    fun useSpotify() {
        if (state.value.loadingSong) return
        stopSound()
        state.update { it.copy(spotify = SpotifyLink.Waiting, songName = null, songFailed = false) }
        refresh()
    }

    /** Spotify suena con el preset de calibración, para compararlo con el mismo preset en Poweramp. */
    fun startCalibration() {
        stopSound()
        calibrating = true
        state.update { it.copy(spotify = SpotifyLink.Waiting, songName = null, songFailed = false) }
        applyEqualization(calibrationSettings())
    }

    fun setBandOverlap(shape: BandOverlap) {
        overlapPreferences.save(shape)
        state.update { it.copy(bandOverlap = shape) }
        if (calibrating) applyEqualization(calibrationSettings()) else refresh()
    }

    fun importMeasurePreset(): Boolean = powerampControl.importPreset(
        PowerampExporter.NAME_PREFIX + MEASURE_PRESET_NAME,
        PowerampExporter.toJson(MEASURE_PRESET_NAME, calibrationPreset(state.value.finder.layout)),
    )

    fun importPreset(fullName: String, json: String): Boolean = powerampControl.importPreset(fullName, json)

    /** Vuelve a escuchar el preset completo, como si Poweramp lo aplicara entero. */
    fun resetPreviewStrength() = usePreviewStrength(FULL_PREVIEW_STRENGTH)

    /**
     * No se aplica sola al medir: la medición usa cortes que terminan en 10 kHz, y con un corte hasta 20 kHz el
     * usuario oyó en Timbra más agudos que en Poweramp. Es una opción que se compara de oído.
     */
    fun usePreviewStrength(strength: Double) {
        strengthPreferences.save(strength)
        state.update { it.copy(previewStrength = strength) }
    }

    fun startBlindTest() {
        state.update { it.copy(blindTest = BlindTest(equalizedOption = Random.nextInt(BLIND_OPTIONS)), blindOption = null) }
    }

    fun blindListen(option: Int) {
        val test = state.value.blindTest ?: return
        state.update { it.copy(blindOption = option) }
        setBypass(option != test.equalizedOption)
    }

    fun blindChoose() {
        state.update { current ->
            val test = current.blindTest
            val option = current.blindOption
            if (test == null || option == null) {
                current
            } else {
                current.copy(blindTest = test.choose(option, Random.nextInt(BLIND_OPTIONS)), blindOption = null)
            }
        }
    }

    /** Deja de nuevo la ecualización, que es lo que muestra la pantalla de resultado. */
    fun leaveBlindTest() {
        state.update { it.copy(blindTest = null, blindOption = null) }
        setBypass(false)
    }

    /**
     * Compara a Poweramp y a Timbra con el micrófono y el mismo preset de prueba: silencio, ruido rosa solo, con el
     * ecualizador de Timbra y con el de Poweramp. Solo importa la diferencia entre grabaciones, así que el micrófono y
     * la bocina, que distan de ser planos, se cancelan. El ruido del cuarto no: las bandas donde la señal no le gana
     * se descartan. Medir primero a Timbra, cuya curva se conoce, dice si la medición es válida.
     */
    fun measurePoweramp() {
        if (state.value.powerampMeasure == PowerampMeasure.Running) return
        calibrating = false
        leaveSpotify()
        state.update { it.copy(powerampMeasure = PowerampMeasure.Running) }
        viewModelScope.launch(synthesis) {
            val noise = PinkNoise()
            val rate = output.sampleRate
            output.renderer.changeEqualization(EqualizerSettings.Flat)
            output.renderer.changeSource(LoopSource(DoubleArray(MEASURE_NOISE_SECONDS * rate) { noise.next() }))
            val session = output.sessionId
            val preset = calibrationSettings().copy(previewStrength = FULL_PREVIEW_STRENGTH)
            if (!powerampControl.activatePreset(PowerampExporter.NAME_PREFIX + MEASURE_PRESET_NAME)) {
                state.update { it.copy(powerampMeasure = PowerampMeasure.PresetMissing) }
                return@launch
            }
            val result = try {
                val room = recordForMeasure()
                output.play()
                delay(MEASURE_SETTLE_MS)
                val alone = recordForMeasure()
                withContext(effectThread) {
                    sessionEqualizer.apply(session, 0f, previewBands(preset, state.value.bandOverlap, rate))
                }
                delay(MEASURE_SETTLE_MS)
                val withTimbra = recordForMeasure()
                withContext(effectThread) { sessionEqualizer.release() }
                powerampControl.turnOn()
                powerampControl.announceSession(session, open = true)
                delay(MEASURE_SETTLE_MS)
                val withPoweramp = recordForMeasure()
                if (room == null || alone == null || withTimbra == null || withPoweramp == null) {
                    PowerampMeasure.CouldNotCapture
                } else {
                    compareMeasurements(preset, room, alone, withTimbra, withPoweramp)
                }
            } finally {
                output.stop()
                powerampControl.announceSession(session, open = false)
                withContext(NonCancellable + effectThread) { sessionEqualizer.release() }
                currentLoop = null
                currentGenre = null
                playingSong = null
            }
            state.update { it.copy(powerampMeasure = result) }
        }
    }

    private suspend fun recordForMeasure(): DoubleArray? = withContext(Dispatchers.IO) {
        recordMicrophone(MEASURE_CAPTURE_SECONDS * output.sampleRate, output.sampleRate)
            ?.let { samples -> DoubleArray(samples.size) { samples[it].toDouble() / Short.MAX_VALUE } }
    }

    private fun compareMeasurements(
        preset: EqualizerSettings,
        room: DoubleArray,
        alone: DoubleArray,
        withTimbra: DoubleArray,
        withPoweramp: DoubleArray,
    ): PowerampMeasure {
        val rate = output.sampleRate
        val frequencies = preset.bandFrequenciesHz
        val overRoom = bandDifferenceDb(room, alone, rate, frequencies)
        val timbra = bandDifferenceDb(alone, withTimbra, rate, frequencies)
        val poweramp = bandDifferenceDb(alone, withPoweramp, rate, frequencies)
        val bands = frequencies.indices.filter {
            (overRoom[it] ?: 0f) >= MEASURE_MIN_OVER_ROOM_DB && timbra[it] != null && poweramp[it] != null
        }
        val cuts = bands.map { preset.bandGainsDb[it] < 0f }
        if (cuts.count { it } < MEASURE_MIN_CUT_BANDS) return PowerampMeasure.TooQuiet
        val timbraDb = bands.map { requireNotNull(timbra[it]) }.alignedToItsMedian()
        val powerampDb = bands.map { requireNotNull(poweramp[it]) }.alignedToItsMedian()
        val timbraCut = cutDepthDb(timbraDb, cuts)
        val powerampCut = cutDepthDb(powerampDb, cuts)
        if (timbraCut < MEASURE_MIN_TIMBRA_CUT_DB) return PowerampMeasure.CouldNotCapture
        if (powerampCut < MEASURE_MIN_POWERAMP_CUT_DB) return PowerampMeasure.NoEffect
        return PowerampMeasure.Done(
            frequenciesHz = bands.map { frequencies[it] },
            measuredDb = powerampDb,
            expectedDb = timbraDb,
            timbraCutDb = -timbraCut,
            powerampCutDb = -powerampCut,
        )
    }

    /** Mientras se mantiene presionado, se escucha cómo estaba esta zona antes de la ronda. */
    fun holdCurrent(holding: Boolean) {
        val current = state.value
        if (current.finder.isFinished) return
        val index = if (holding) current.finder.currentOptionIndex else current.auditionIndex
        optionSettings.getOrNull(index)?.let(::applyEqualization)
    }

    fun openSpotify() {
        val context = getApplication<Application>()
        context.packageManager.getLaunchIntentForPackage(SPOTIFY_PACKAGE)?.let { intent ->
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Empieza el test desde la primera ronda, conservando el género y el nombre de los audífonos. */
    fun startTest() {
        state.update {
            it.withFinder(startingFinder(it)).copy(bypass = false, presetName = "")
        }
        refresh()
    }

    private fun startingFinder(current: FinderUiState): EqualizerFinder {
        val output = current.finder.output
        val layout = current.finder.layout
        val profile = current.autoEq?.takeIf { current.useAutoEq && output == OutputKind.Bluetooth }
            ?: return EqualizerFinder(output, layout)
        return EqualizerFinder(
            output = output,
            layout = layout,
            valuesDb = output.neutralValuesDb(),
            baseCurveDb = profile.startingCurveDb(layout.frequenciesHz),
        )
    }

    /** Escucha la opción [index] de la ronda actual. */
    fun audition(index: Int) {
        state.update { it.copy(auditionIndex = index) }
        optionSettings.getOrNull(index)?.let(::applyEqualization)
    }

    /** Se queda con la opción que se está escuchando y pasa a la siguiente ronda. */
    fun chooseAudition() {
        val current = state.value
        if (current.preparing || current.finder.isFinished) return
        val chosen = current.finder.options()[current.auditionIndex]
        state.update { it.withFinder(it.finder.choose(chosen)) }
        refresh()
    }

    /** "No me gustó ninguno": deja la zona como estaba y pasa a la siguiente. */
    fun keepCurrent() {
        val current = state.value
        if (current.preparing || current.finder.isFinished) return
        state.update { it.withFinder(it.finder.keepCurrent()) }
        refresh()
    }

    /** Vuelve a la ronda anterior. Devuelve `false` si ya está en la primera. */
    fun previousRound(): Boolean {
        val previous = state.value.finder.previous ?: return false
        state.update { it.withFinder(previous) }
        refresh()
        return true
    }

    /** En el resultado: `true` para escuchar sin ecualizar y comparar. */
    fun setBypass(bypass: Boolean) {
        state.update { it.copy(bypass = bypass) }
        refresh()
    }

    fun presetJson(suggestedName: String): String {
        val current = state.value
        return PowerampExporter.toJson(current.effectivePresetName(suggestedName), current.finder.result())
    }

    fun presetFileName(suggestedName: String): String =
        PowerampExporter.fileName(state.value.effectivePresetName(suggestedName))

    override fun onCleared() {
        restorePoweramp()
        if (outputHolder.isInitialized()) output.release()
        sessionEqualizer.release()
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
            val song = userSong.takeIf { snapshot.songName != null }
            if (song == null) userSong = null
            val matcher = when {
                snapshot.usesSpotify -> spotifyMatcher()
                song != null -> playUserSong(song)
                else -> playAppMusic(snapshot)
            }
            optionSettings = optionsFor(snapshot, matcher)
            val latest = state.value
            val settings = when {
                latest.finder.isFinished && latest.bypass ->
                    matcher.flatMatching(latest.finder.result().overlappingAs(latest.bandOverlap))
                latest.finder.isFinished -> latest.finder.result().overlappingAs(latest.bandOverlap)
                else -> optionSettings.getOrNull(latest.auditionIndex) ?: EqualizerSettings.Flat
            }
            applyEqualization(settings)
            val unchanged = latest.finder == snapshot.finder && latest.genre == snapshot.genre &&
                latest.songName == snapshot.songName && latest.spotify.usesSameMusicAs(snapshot.spotify)
            if (unchanged && !latest.loadingSong) {
                state.update { it.copy(preparing = false) }
            }
            updateMeasuredSounding(song)
        }
    }

    private fun flatFor(finder: EqualizerFinder): EqualizerSettings {
        val preset = finder.result()
        return preset.copy(bandGainsDb = preset.bandGainsDb.map { 0f }, preampDb = 0f)
    }

    /** Con audio propio se puede comparar de verdad: se procesa con y sin ecualizar y se mide la diferencia. */
    private fun updateMeasuredSounding(song: PreparedSong?) {
        val current = state.value
        if (!current.finder.isFinished) {
            state.update { it.copy(sounding = null) }
            return
        }
        if (current.usesSpotify) return
        val preset = current.finder.result().overlappingAs(current.bandOverlap)
        val key = listOf(preset, song ?: current.genre)
        if (measuredFor != key) {
            val samples = song?.reference ?: mixFor(current.genre, MusicFocus.Balanced)
            val matcher = if (song != null) requireNotNull(userSongMatcher) else
                matcherFor(current.genre, MusicFocus.Balanced, samples)
            measuredGains = measureEqualizerDifference(
                samples,
                output.sampleRate,
                matcher.compensated(flatFor(current.finder)),
                matcher.compensated(preset),
            )
            measuredFor = key
        }
        state.update {
            val gains = if (it.bypass) measuredGains.map { gain -> gain?.let { 0f } } else measuredGains
            it.copy(sounding = SoundingCheck(gains, SoundingProof.MeasuredInAudio))
        }
    }

    private fun readSpotifySounding(wanted: List<PreviewBand>): SoundingCheck? {
        val current = state.value
        if (!current.finder.isFinished) return null
        val reading = sessionEqualizer.read() ?: return null
        val frequencies = current.finder.result().bandFrequenciesHz
        return SoundingCheck(
            gainsDb = gainsAtFrequencies(reading.bands, frequencies),
            proof = SoundingProof.ReadFromEffect(
                confirmedBands = confirmedBandCount(reading, wanted),
                totalBands = wanted.size,
                active = reading.enabled && reading.hasControl,
            ),
        )
    }

    private fun SpotifyLink.usesSameMusicAs(other: SpotifyLink) =
        (this == SpotifyLink.Off) == (other == SpotifyLink.Off)

    private fun applyEqualization(settings: EqualizerSettings) {
        if (state.value.usesSpotify) {
            viewModelScope.launch(effectThread) {
                spotifySettings = settings
                applySpotifyEqualization()
            }
        } else {
            output.renderer.changeEqualization(settings)
        }
    }

    private fun syncSpotifyEqualizer() {
        if (state.value.usesSpotify) viewModelScope.launch(effectThread) { applySpotifyEqualization() }
    }

    /** Se reaplica cada vez que Spotify abre una sesión: al pausar y reanudar puede estrenar otra. */
    private fun applySpotifyEqualization() {
        if (!state.value.usesSpotify) return
        val sessionId = SpotifySession.id.value
        var sounding: SoundingCheck? = null
        val link = if (sessionId == null) {
            sessionEqualizer.release()
            SpotifyLink.Waiting
        } else {
            val shape = state.value.bandOverlap
            val bands = previewBands(spotifySettings, shape, output.sampleRate)
            when (sessionEqualizer.apply(sessionId, spotifySettings.preampDb, bands)) {
                SessionEqualizerResult.Applied -> {
                    sounding = readSpotifySounding(bands)
                    SpotifyLink.Connected
                }
                SessionEqualizerResult.Blocked -> SpotifyLink.Blocked
                SessionEqualizerResult.Failed -> SpotifyLink.Failed
            }
        }
        state.update { if (it.usesSpotify) it.copy(spotify = link, sounding = sounding) else it }
    }

    private fun leaveSpotify() {
        state.update { it.copy(spotify = SpotifyLink.Off) }
        viewModelScope.launch(effectThread) { sessionEqualizer.release() }
    }

    /** No se puede medir la música de otra app: el volumen entre opciones se iguala con ruido rosa. */
    private fun spotifyMatcher(): LoudnessMatcher = pinkNoiseMatcher ?: run {
        val noise = PinkNoise()
        val rate = output.sampleRate
        LoudnessMatcher(rate, DoubleArray(REFERENCE_SECONDS * rate) { noise.next() }).also { pinkNoiseMatcher = it }
    }

    /** Pausa o reanuda lo que esté sonando, como el botón de unos audífonos. */
    private fun pressMediaPlayPause() {
        val audioManager = getApplication<Application>().getSystemService(AudioManager::class.java)
        listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
            audioManager.dispatchMediaKeyEvent(KeyEvent(action, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        }
    }

    /** Una canción real no se reinicia entre rondas: solo cambia la ecualización. */
    private fun playUserSong(song: PreparedSong): LoudnessMatcher {
        if (playingSong !== song) {
            output.renderer.changeSource(StereoLoopSource(song.left, song.right))
            playingSong = song
            currentLoop = null
            currentGenre = null
        }
        return requireNotNull(userSongMatcher)
    }

    private fun playAppMusic(snapshot: FinderUiState): LoudnessMatcher {
        val focus = if (snapshot.finder.isFinished) MusicFocus.Balanced else snapshot.finder.round.dimension.focus
        val mix = mixFor(snapshot.genre, focus)
        val matcher = matcherFor(snapshot.genre, focus, mix)
        playLoop(mix, sameSong = currentGenre == snapshot.genre)
        currentGenre = snapshot.genre
        playingSong = null
        return matcher
    }

    private fun loadSong(uri: Uri): PreparedSong {
        val decoded = decodeAudioFile(getApplication(), uri, MAX_SONG_SECONDS)
        val rate = output.sampleRate
        return prepareSong(
            resample(decoded.left, decoded.sampleRate, rate),
            resample(decoded.right, decoded.sampleRate, rate),
            rate,
        )
    }

    /** El nombre del archivo sin su extensión; si el sistema no lo da, se usa uno genérico. */
    private fun songDisplayName(uri: Uri): String {
        val resolver = getApplication<Application>().contentResolver
        val name = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return name?.substringBeforeLast('.').orEmpty().ifBlank {
            getApplication<Application>().getString(R.string.comfort_song_fallback)
        }
    }

    private fun optionsFor(snapshot: FinderUiState, matcher: LoudnessMatcher): List<EqualizerSettings> {
        val finder = snapshot.finder
        if (finder.isFinished) return emptyList()
        return finder.options().map { option ->
            val compensated = matcher.compensated(finder.presetWithCandidate(option).overlappingAs(snapshot.bandOverlap))
            compensated.copy(preampDb = compensated.preampDb + PREVIEW_LEVEL_DB)
        }
    }

    private fun EqualizerSettings.overlappingAs(shape: BandOverlap) =
        copy(overlap = shape, previewStrength = state.value.previewStrength)

    private fun calibrationSettings(): EqualizerSettings {
        val current = state.value
        return calibrationPreset(current.finder.layout).overlappingAs(current.bandOverlap)
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
