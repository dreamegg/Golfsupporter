package com.golfsupporter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.golfsupporter.ui.history.HistoryScreen
import com.golfsupporter.ui.home.HomeScreen
import com.golfsupporter.ui.result.ResultScreen
import com.golfsupporter.ui.round.InterstitialScreen
import com.golfsupporter.ui.round.RoundScreen
import com.golfsupporter.ui.setup.SetupScreen

@Composable
fun GolfNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onNewGame = { navController.navigate(Routes.SETUP) },
                onContinue = { sessionId -> navController.navigate(Routes.round(sessionId)) },
                onStartBack = { sessionId -> navController.navigate(Routes.round(sessionId)) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpen = { sessionId -> navController.navigate(Routes.result(sessionId)) },
            )
        }

        composable(Routes.SETUP) {
            SetupScreen(
                onBack = { navController.popBackStack() },
                onGameCreated = { sessionId ->
                    navController.navigate(Routes.round(sessionId)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(
            route = Routes.ROUND,
            arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.StringType })
        ) {
            RoundScreen(
                onFrontNineComplete = { sessionId ->
                    navController.navigate(Routes.interstitial(sessionId))
                },
                onRoundComplete = { sessionId ->
                    navController.navigate(Routes.result(sessionId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onExit = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.INTERSTITIAL,
            arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.StringType })
        ) {
            InterstitialScreen(
                onStartBackNine = { sessionId ->
                    navController.navigate(Routes.round(sessionId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onLater = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.StringType })
        ) {
            ResultScreen(
                onHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
