package com.juliancitou.timbra.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.screens.about.AboutScreen
import com.juliancitou.timbra.ui.screens.audiotest.AudioTestScreen
import com.juliancitou.timbra.ui.screens.calibration.CalibrationScreen
import com.juliancitou.timbra.ui.screens.comfort.ComfortLevelScreen
import com.juliancitou.timbra.ui.screens.export.ExportScreen
import com.juliancitou.timbra.export.PowerampExporter
import com.juliancitou.timbra.ui.screens.finder.BlindTestScreen
import com.juliancitou.timbra.ui.screens.finder.EqualizerFinderViewModel
import com.juliancitou.timbra.ui.screens.finder.FinderResultScreen
import com.juliancitou.timbra.ui.screens.finder.FinderRoundScreen
import com.juliancitou.timbra.ui.screens.finder.suggestedPresetName
import com.juliancitou.timbra.ui.screens.history.HistoryScreen
import com.juliancitou.timbra.ui.screens.home.HomeScreen
import com.juliancitou.timbra.ui.screens.measurement.MeasurementScreen
import com.juliancitou.timbra.ui.screens.preparation.ChecklistScreen
import com.juliancitou.timbra.ui.screens.preparation.PreparationScreen
import com.juliancitou.timbra.ui.screens.results.ResultsScreen
import com.juliancitou.timbra.ui.screens.settings.SettingsScreen
import com.juliancitou.timbra.ui.screens.taste.TasteScreen
import com.juliancitou.timbra.ui.theme.EmphasizedAccelerate
import com.juliancitou.timbra.ui.theme.EmphasizedDecelerate
import com.juliancitou.timbra.ui.theme.LocalNavAnimatedVisibilityScope
import com.juliancitou.timbra.ui.theme.LocalSharedTransitionScope
import com.juliancitou.timbra.ui.theme.MotionDuration
import com.juliancitou.timbra.ui.theme.isReducedMotionEnabled
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object PreparationRoute
@Serializable data object ChecklistRoute
@Serializable data object CalibrationRoute
@Serializable data object ComfortLevelRoute
@Serializable data object MeasurementRoute
@Serializable data object ResultsRoute
@Serializable data object TasteRoute
@Serializable data object FinderRoundRoute
@Serializable data object FinderResultRoute
@Serializable data object BlindTestRoute
@Serializable data object ExportRoute
@Serializable data object HistoryRoute
@Serializable data object SettingsRoute
@Serializable data object AudioTestRoute
@Serializable data object AboutRoute

/**
 * Escalas del zoom entre pantallas: la que entra crece desde [SCALE_SMALL] y la que se va
 * hacia adelante se agranda hasta [SCALE_LARGE], como si pasaras a través de ella. Al
 * regresar ocurre lo contrario: la actual se encoge y se esconde.
 */
private const val SCALE_SMALL = 0.92f
private const val SCALE_LARGE = 1.06f
private const val FADE_DELAY_MS = 50

@Composable
fun TimbraNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val animated = !isReducedMotionEnabled()
    // El test se comparte entre varias pantallas: su ViewModel vive con la actividad.
    val finder: EqualizerFinderViewModel = viewModel()
    val finderState by finder.uiState.collectAsStateWithLifecycle()
    StopFinderSoundOutsideItsScreens(navController, onLeaveTest = finder::stopTestAudio, onStop = finder::stopSound, onTestDone = finder::restorePoweramp)
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                enterTransition = { if (animated) enterForward() else EnterTransition.None },
                exitTransition = { if (animated) exitForward() else ExitTransition.None },
                popEnterTransition = { if (animated) enterBackward() else EnterTransition.None },
                popExitTransition = { if (animated) exitBackward() else ExitTransition.None },
            ) {
                val back: () -> Unit = { navController.popBackStack() }
                screen<HomeRoute>(
                    exitTransition = {
                        when {
                            !animated -> ExitTransition.None
                            targetState.goesTo<SettingsRoute>() -> transformBackdropOut()
                            else -> exitForward()
                        }
                    },
                    popEnterTransition = {
                        when {
                            !animated -> EnterTransition.None
                            initialState.goesTo<SettingsRoute>() -> transformBackdropIn()
                            else -> enterBackward()
                        }
                    },
                ) {
                    HomeScreen(
                        onCreateEqualization = { navController.navigate(PreparationRoute) },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                    )
                }
                screen<PreparationRoute> {
                    LaunchedEffect(Unit) { finder.syncWithPoweramp() }
                    PreparationScreen(
                        output = finderState.finder.output,
                        onOutputChange = finder::setOutput,
                        bandLayout = finderState.finder.layout,
                        bandsFromPoweramp = finderState.powerampSettings?.layout == finderState.finder.layout,
                        onBandLayoutChange = finder::setBandLayout,
                        noiseMode = finderState.noiseMode,
                        onNoiseModeChange = finder::setNoiseMode,
                        autoEq = finderState.autoEq,
                        useAutoEq = finderState.useAutoEq,
                        onUseAutoEqChange = finder::setUseAutoEq,
                        onLookForAutoEq = finder::loadAutoEq,
                        headphonesName = finderState.headphonesName,
                        onHeadphonesNameChange = finder::setHeadphonesName,
                        onContinue = { navController.navigate(ChecklistRoute) },
                        onBack = back,
                    )
                }
                screen<ChecklistRoute> {
                    // También al volver de cambiar algo en Poweramp o en los ajustes del teléfono.
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { finder.checkPoweramp() }
                    ChecklistScreen(
                        output = finderState.finder.output,
                        poweramp = finderState.poweramp,
                        powerampIssues = finderState.powerampSettings?.issues.orEmpty(),
                        onTurnOffPoweramp = finder::turnOffPoweramp,
                        onContinue = { navController.navigate(ComfortLevelRoute) },
                        onBack = back,
                    )
                }
                screen<CalibrationRoute> {
                    LaunchedEffect(Unit) { finder.syncWithPoweramp() }
                    CalibrationScreen(
                        state = finderState,
                        onImportPreset = finder::importMeasurePreset,
                        onResetStrength = finder::resetPreviewStrength,
                        onUseStrength = finder::usePreviewStrength,
                        onStart = finder::startCalibration,
                        onSelectOverlap = finder::setBandOverlap,
                        onMeasurePoweramp = finder::measurePoweramp,
                        onOpenSpotify = finder::openSpotify,
                        onBack = back,
                    )
                }
                screen<ComfortLevelRoute> {
                    ComfortLevelScreen(
                        state = finderState,
                        onPrepareMusic = finder::prepareMusic,
                        onSelectGenre = finder::selectGenre,
                        onSelectSong = finder::selectSong,
                        onUseSpotify = finder::useSpotify,
                        onOpenSpotify = finder::openSpotify,
                        onTogglePlayback = finder::togglePlayback,
                        onReady = {
                            finder.startTest()
                            navController.navigate(FinderRoundRoute)
                        },
                        onBack = back,
                    )
                }
                screen<FinderRoundRoute> {
                    LaunchedEffect(finderState.finder.isFinished) {
                        if (finderState.finder.isFinished) navController.navigate(FinderResultRoute)
                    }
                    if (!finderState.finder.isFinished) {
                        FinderRoundScreen(
                            state = finderState,
                            onAudition = finder::audition,
                            onChoose = finder::chooseAudition,
                            onKeepCurrent = finder::keepCurrent,
                            onHoldCurrent = finder::holdCurrent,
                            onTogglePlayback = finder::togglePlayback,
                            onPreviousRound = finder::previousRound,
                            onExit = back,
                        )
                    }
                }
                screen<FinderResultRoute> {
                    // Al volver de Exportar, Spotify ya se había soltado y hay que unirse otra vez.
                    LaunchedEffect(Unit) { finder.prepareMusic() }
                    FinderResultScreen(
                        state = finderState,
                        onBypassChange = finder::setBypass,
                        onRealLevel = finder::listenAtRealLevel,
                        onTogglePlayback = finder::togglePlayback,
                        onPresetNameChange = finder::setPresetName,
                        onExport = { navController.navigate(ExportRoute) },
                        onBlindTest = {
                            finder.startBlindTest()
                            navController.navigate(BlindTestRoute)
                        },
                        onSelectGenre = finder::selectGenre,
                        onRestart = {
                            finder.startTest()
                            navController.popBackStack(FinderRoundRoute, inclusive = false)
                        },
                        onPreviousRound = {
                            finder.previousRound()
                            navController.popBackStack()
                        },
                    )
                }
                screen<BlindTestRoute> {
                    BlindTestScreen(
                        state = finderState,
                        onListen = finder::blindListen,
                        onChoose = finder::blindChoose,
                        onTogglePlayback = finder::togglePlayback,
                        onBack = {
                            finder.leaveBlindTest()
                            navController.popBackStack()
                        },
                    )
                }
                screen<MeasurementRoute> {
                    MeasurementScreen(onFinished = { navController.navigate(ResultsRoute) }, onBack = back)
                }
                screen<ResultsRoute> {
                    ResultsScreen(onAdjustTaste = { navController.navigate(TasteRoute) }, onBack = back)
                }
                screen<TasteRoute> {
                    TasteScreen(onFinish = { navController.popBackStack(HomeRoute, inclusive = false) }, onBack = back)
                }
                screen<ExportRoute> {
                    val suggestedName = suggestedPresetName(finderState)
                    val fullPresetName = PowerampExporter.NAME_PREFIX + finderState.effectivePresetName(suggestedName)
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                        finder.refreshPowerampSettings()
                        finder.checkPresetImported(fullPresetName)
                    }
                    ExportScreen(
                        presetName = finderState.effectivePresetName(suggestedName),
                        fileName = finder.presetFileName(suggestedName),
                        output = finderState.finder.output,
                        bandLayout = finderState.finder.layout,
                        powerampSettings = finderState.powerampSettings,
                        presetImported = finderState.presetImported,
                        onImportPreset = { json -> finder.importPreset(fullPresetName, json) },
                        presetJson = { finder.presetJson(suggestedName) },
                        onGoHome = { navController.popBackStack(HomeRoute, inclusive = false) },
                        onBack = back,
                    )
                }
                screen<HistoryRoute> {
                    HistoryScreen(onOpenMeasurement = { navController.navigate(ResultsRoute) }, onBack = back)
                }
                screen<SettingsRoute>(
                    enterTransition = { if (animated) transformBackdropIn() else EnterTransition.None },
                    popExitTransition = { if (animated) transformBackdropOut() else ExitTransition.None },
                ) {
                    SettingsScreen(
                        onOpenAudioTest = { navController.navigate(AudioTestRoute) },
                        onOpenCalibration = { navController.navigate(CalibrationRoute) },
                        onOpenAdvancedPreview = { navController.navigate(MeasurementRoute) },
                        onOpenAbout = { navController.navigate(AboutRoute) },
                        onBack = back,
                    )
                }
                screen<AudioTestRoute> {
                    AudioTestScreen(onBack = back)
                }
                screen<AboutRoute> {
                    AboutScreen(onBack = back)
                }
            }
        }
    }
}

/**
 * Destino con su escena de animación disponible para `containerTransform`. Las transiciones nulas
 * usan las generales del `NavHost`.
 */
/** Pantallas del test en las que puede sonar la música; en cualquier otra, el sonido se detiene. */
private val FinderSoundRoutes =
    listOf(
        ComfortLevelRoute::class,
        FinderRoundRoute::class,
        FinderResultRoute::class,
        BlindTestRoute::class,
        CalibrationRoute::class,
    )

/**
 * Detiene el sonido del test al salir de sus pantallas y cuando la app pasa a segundo plano o se
 * apaga la pantalla. Es la regla "nunca suena fuera de las pantallas del test".
 */
@Composable
private fun StopFinderSoundOutsideItsScreens(
    navController: NavHostController,
    onLeaveTest: () -> Unit,
    onStop: () -> Unit,
    onTestDone: () -> Unit,
) {
    val entry by navController.currentBackStackEntryAsState()
    LaunchedEffect(entry?.destination) {
        val destination = entry?.destination ?: return@LaunchedEffect
        if (FinderSoundRoutes.none { destination.hasRoute(it) }) onLeaveTest()
        if (destination.hasRoute(ExportRoute::class) || destination.hasRoute(HomeRoute::class)) onTestDone()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) onStop() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private inline fun <reified T : Any> NavGraphBuilder.screen(
    noinline enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    noinline exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    noinline popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    noinline popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    noinline content: @Composable () -> Unit,
) {
    composable<T>(
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
    ) {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) { content() }
    }
}

private inline fun <reified T : Any> NavBackStackEntry.goesTo(): Boolean = destination.hasRoute<T>()

/** Mientras el engrane crece hasta Ajustes, el resto solo se desvanece para no competir con él. */
private fun transformBackdropIn(): EnterTransition = fadeIn(tween(MotionDuration.CONTAINER_TRANSFORM_MS))
private fun transformBackdropOut(): ExitTransition = fadeOut(tween(MotionDuration.CONTAINER_TRANSFORM_MS / 2))

private fun <T> enterSpec() = tween<T>(MotionDuration.LONG_MS, easing = EmphasizedDecelerate)
private fun <T> exitSpec() = tween<T>(MotionDuration.SHORT_MS, easing = EmphasizedAccelerate)
private fun fadeInSpec() = tween<Float>(MotionDuration.MEDIUM_MS - FADE_DELAY_MS, delayMillis = FADE_DELAY_MS)

private fun enterForward(): EnterTransition =
    scaleIn(enterSpec(), initialScale = SCALE_SMALL) + fadeIn(fadeInSpec())

private fun exitForward(): ExitTransition =
    scaleOut(exitSpec(), targetScale = SCALE_LARGE) + fadeOut(exitSpec())

private fun enterBackward(): EnterTransition =
    scaleIn(enterSpec(), initialScale = SCALE_LARGE) + fadeIn(fadeInSpec())

private fun exitBackward(): ExitTransition =
    scaleOut(exitSpec(), targetScale = SCALE_SMALL) + fadeOut(exitSpec())
