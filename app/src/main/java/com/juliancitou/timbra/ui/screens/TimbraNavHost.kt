package com.juliancitou.timbra.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.juliancitou.timbra.ui.screens.comfort.ComfortLevelScreen
import com.juliancitou.timbra.ui.screens.export.ExportScreen
import com.juliancitou.timbra.ui.screens.history.HistoryScreen
import com.juliancitou.timbra.ui.screens.home.HomeScreen
import com.juliancitou.timbra.ui.screens.measurement.MeasurementScreen
import com.juliancitou.timbra.ui.screens.preparation.PreparationScreen
import com.juliancitou.timbra.ui.screens.results.ResultsScreen
import com.juliancitou.timbra.ui.screens.settings.SettingsScreen
import com.juliancitou.timbra.ui.screens.taste.TasteScreen
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

/** Desplazamiento lateral de las transiciones, como fracción del ancho de pantalla. */
private const val SLIDE_FRACTION = 0.12f

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
            SettingsScreen(blurEnabled = blurEnabled, onBlurEnabledChange = onBlurEnabledChange, onBack = back)
        }
    }
}

private fun <T> transitionSpec() = tween<T>(MotionDuration.MEDIUM_MS)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enterForward(): EnterTransition =
    fadeIn(transitionSpec()) + slideInHorizontally(transitionSpec()) { (it * SLIDE_FRACTION).toInt() }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exitForward(): ExitTransition =
    fadeOut(transitionSpec()) + slideOutHorizontally(transitionSpec()) { -(it * SLIDE_FRACTION).toInt() }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enterBackward(): EnterTransition =
    fadeIn(transitionSpec()) + slideInHorizontally(transitionSpec()) { -(it * SLIDE_FRACTION).toInt() }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exitBackward(): ExitTransition =
    fadeOut(transitionSpec()) + slideOutHorizontally(transitionSpec()) { (it * SLIDE_FRACTION).toInt() }
