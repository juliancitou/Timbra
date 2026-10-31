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
import com.juliancitou.timbra.audio.EqualizerSettings
import com.juliancitou.timbra.audio.LoopSource
import com.juliancitou.timbra.audio.LoudnessMatcher
import com.juliancitou.timbra.audio.PinkNoise
import com.juliancitou.timbra.audio.PlaybackStatus
import com.juliancitou.timbra.audio.PreparedSong
import com.juliancitou.timbra.audio.SPOTIFY_PACKAGE
import com.juliancitou.timbra.audio.SessionEqualizer
import com.juliancitou.timbra.audio.SessionEqualizerResult
import com.juliancitou.timbra.audio.SignalSource
import com.juliancitou.timbra.audio.SpotifySession
import com.juliancitou.timbra.audio.StereoLoopSource
import com.juliancitou.timbra.audio.decodeAudioFile
import com.juliancitou.timbra.audio.music.Genre
import com.juliancitou.timbra.audio.music.MusicFocus
import com.juliancitou.timbra.audio.music.MusicLoop
import com.juliancitou.timbra.audio.prepareSong
import com.juliancitou.timbra.audio.resample
import com.juliancitou.timbra.data.BandLayoutPreferences
import com.juliancitou.timbra.export.BandLayout
import com.juliancitou.timbra.export.PowerampExporter
import com.juliancitou.timbra.measure.EqualizerFinder
import com.juliancitou.timbra.measure.OutputKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/** Estado de la ecualización en vivo de Spotify. [Off]: suena música de Timbra, no Spotify. */
enum class SpotifyLink { Off, Waiting, Connected, Blocked, Failed }

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
    val status: PlaybackStatus = PlaybackStatus.Stopped,
) {
    val isPlaying: Boolean get() = status == PlaybackStatus.Playing
    val usesSpotify: Boolean get() = spotify != SpotifyLink.Off

    /** Nombre del preset: el que escribió el usuario o, si no escribió ninguno, el sugerido. */
    fun effectivePresetName(suggested: String): String = presetName.ifBlank { suggested }

    /** Otro estado del test, escuchando de entrada lo que ya hay y no lo que esté en el centro. */
    fun withFinder(next: EqualizerFinder): FinderUiState =
        copy(finder = next, auditionIndex = if (next.isFinished) CENTER_OPTION else next.currentOptionIndex)
}

private const val CENTER_OPTION = 2
private const val REFERENCE_SECONDS = 4

/** Con 2 minutos basta para el test, y una canción en estéreo no ocupa más de ~50 MB de memoria. */
private const val MAX_SONG_SECONDS = 120
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
    private val bandPreferences = BandLayoutPreferences(application)
    private val state = MutableStateFlow(FinderUiState(finder = EqualizerFinder(layout = bandPreferences.load())))
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

    val uiState: StateFlow<FinderUiState> = combine(state, playback) { current, status ->
        current.copy(status = status)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_SHARING_AFTER_MS), FinderUiState())

    init {
        viewModelScope.launch { SpotifySession.id.collect { syncSpotifyEqualizer() } }
    }

    fun setHeadphonesName(name: String) = state.update { it.copy(headphonesName = name) }

    fun setPresetName(name: String) = state.update { it.copy(presetName = name) }

    /** Cambiar la salida reinicia el test, porque cada una parte de una curva y unas rondas distintas. */
    fun setOutput(output: OutputKind) = state.update {
        if (it.finder.output == output) it else it.copy(finder = EqualizerFinder(output, it.finder.layout))
    }

    fun setBandLayout(layout: BandLayout) {
        bandPreferences.save(layout)
        state.update { it.copy(finder = EqualizerFinder(it.finder.output, layout)) }
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
        viewModelScope.launch(effectThread) { sessionEqualizer.release() }
    }

    /** Spotify suena por su cuenta y sus cambios de ecualización se aplican en vivo. */
    fun useSpotify() {
        if (state.value.loadingSong) return
        stopSound()
        state.update { it.copy(spotify = SpotifyLink.Waiting, songName = null, songFailed = false) }
        refresh()
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
            it.withFinder(EqualizerFinder(it.finder.output, it.finder.layout)).copy(bypass = false, presetName = "")
        }
        refresh()
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
            optionSettings = optionsFor(snapshot.finder, matcher)
            val latest = state.value
            val settings = when {
                latest.finder.isFinished && latest.bypass -> matcher.compensated(EqualizerSettings.Flat)
                latest.finder.isFinished -> matcher.compensated(latest.finder.result())
                else -> optionSettings.getOrNull(latest.auditionIndex) ?: EqualizerSettings.Flat
            }
            applyEqualization(settings)
            val unchanged = latest.finder == snapshot.finder && latest.genre == snapshot.genre &&
                latest.songName == snapshot.songName && latest.spotify.usesSameMusicAs(snapshot.spotify)
            if (unchanged && !latest.loadingSong) {
                state.update { it.copy(preparing = false) }
            }
        }
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
        val link = if (sessionId == null) {
            sessionEqualizer.release()
            SpotifyLink.Waiting
        } else {
            when (sessionEqualizer.apply(sessionId, spotifySettings)) {
                SessionEqualizerResult.Applied -> SpotifyLink.Connected
                SessionEqualizerResult.Blocked -> SpotifyLink.Blocked
                SessionEqualizerResult.Failed -> SpotifyLink.Failed
            }
        }
        state.update { if (it.usesSpotify) it.copy(spotify = link) else it }
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

    private fun optionsFor(finder: EqualizerFinder, matcher: LoudnessMatcher): List<EqualizerSettings> =
        if (finder.isFinished) {
            emptyList()
        } else {
            finder.options().map { option -> matcher.compensated(finder.presetWithCandidate(option)) }
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
