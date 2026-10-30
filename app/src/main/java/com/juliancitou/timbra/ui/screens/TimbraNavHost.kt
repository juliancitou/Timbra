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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.juliancitou.timbra.ui.screens.about.AboutScreen
import com.juliancitou.timbra.ui.screens.audiotest.AudioTestScreen
import com.juliancitou.timbra.ui.screens.comfort.ComfortLevelScreen
import com.juliancitou.timbra.ui.screens.export.ExportScreen
import com.juliancitou.timbra.ui.screens.history.HistoryScreen
import com.juliancitou.timbra.ui.screens.home.HomeScreen
import com.juliancitou.timbra.ui.screens.measurement.MeasurementScreen
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
@Serializable data object ComfortLevelRoute
@Serializable data object MeasurementRoute
@Serializable data object ResultsRoute
@Serializable data object TasteRoute
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
                        onOpenHistory = { navController.navigate(HistoryRoute) },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                    )
                }
                screen<PreparationRoute> {
                    PreparationScreen(onContinue = { navController.navigate(ComfortLevelRoute) }, onBack = back)
                }
                screen<ComfortLevelRoute> {
                    ComfortLevelScreen(onReady = { navController.navigate(MeasurementRoute) }, onBack = back)
                }
                screen<MeasurementRoute> {
                    MeasurementScreen(onFinished = { navController.navigate(ResultsRoute) }, onBack = back)
                }
                screen<ResultsRoute> {
                    ResultsScreen(onAdjustTaste = { navController.navigate(TasteRoute) }, onBack = back)
                }
                screen<TasteRoute> {
                    TasteScreen(onExport = { navController.navigate(ExportRoute) }, onBack = back)
                }
                screen<ExportRoute> {
                    ExportScreen(
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
