package com.juliancitou.timbra.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
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
    blurEnabled: Boolean,
    onBlurEnabledChange: (Boolean) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val animated = !isReducedMotionEnabled()
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { if (animated) enterForward() else EnterTransition.None },
        exitTransition = { if (animated) exitForward() else ExitTransition.None },
        popEnterTransition = { if (animated) enterBackward() else EnterTransition.None },
        popExitTransition = { if (animated) exitBackward() else ExitTransition.None },
    ) {
        val back: () -> Unit = { navController.popBackStack() }
        composable<HomeRoute> {
            HomeScreen(
                onCreateEqualization = { navController.navigate(PreparationRoute) },
                onOpenHistory = { navController.navigate(HistoryRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<PreparationRoute> {
            PreparationScreen(onContinue = { navController.navigate(ComfortLevelRoute) }, onBack = back)
        }
        composable<ComfortLevelRoute> {
            ComfortLevelScreen(onReady = { navController.navigate(MeasurementRoute) }, onBack = back)
        }
        composable<MeasurementRoute> {
            MeasurementScreen(onFinished = { navController.navigate(ResultsRoute) }, onBack = back)
        }
        composable<ResultsRoute> {
            ResultsScreen(onAdjustTaste = { navController.navigate(TasteRoute) }, onBack = back)
        }
        composable<TasteRoute> {
            TasteScreen(onExport = { navController.navigate(ExportRoute) }, onBack = back)
        }
        composable<ExportRoute> {
            ExportScreen(
                onGoHome = { navController.popBackStack(HomeRoute, inclusive = false) },
                onBack = back,
            )
        }
        composable<HistoryRoute> {
            HistoryScreen(onOpenMeasurement = { navController.navigate(ResultsRoute) }, onBack = back)
        }
        composable<SettingsRoute> {
            SettingsScreen(
                blurEnabled = blurEnabled,
                onBlurEnabledChange = onBlurEnabledChange,
                onOpenAudioTest = { navController.navigate(AudioTestRoute) },
                onOpenAbout = { navController.navigate(AboutRoute) },
                onBack = back,
            )
        }
        composable<AudioTestRoute> {
            AudioTestScreen(onBack = back)
        }
        composable<AboutRoute> {
            AboutScreen(onBack = back)
        }
    }
}

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
