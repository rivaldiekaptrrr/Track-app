package com.trackit.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.trackit.app.ui.chart.ChartScreen
import com.trackit.app.ui.dashboard.DashboardScreen
import com.trackit.app.ui.settings.CustomKeywordScreen
import com.trackit.app.ui.settings.SettingsScreen
import com.trackit.app.ui.transaction.AddEditTransactionScreen

@Composable
fun TrackItNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route,
    onExportPdf: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute())
                },
                onEditTransaction = { id ->
                    navController.navigate(Screen.EditTransaction.createRoute(id))
                },
                onNavigateToChart = {
                    navController.navigate(Screen.Chart.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("startVoice") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val startVoice = backStackEntry.arguments?.getBoolean("startVoice") ?: false
            AddEditTransactionScreen(
                startVoice = startVoice,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.LongType }
            )
        ) {
            AddEditTransactionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Chart.route) {
            ChartScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onExportPdf = onExportPdf,
                onNavigateToCustomKeywords = {
                    navController.navigate(Screen.CustomKeywords.route)
                }
            )
        }

        composable(Screen.CustomKeywords.route) {
            CustomKeywordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
