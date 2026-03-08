package com.trackit.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.trackit.app.ui.chart.ChartScreen
import com.trackit.app.ui.dashboard.DashboardScreen
import com.trackit.app.ui.scan.ScanReceiptScreen
import com.trackit.app.ui.settings.SettingsScreen
import com.trackit.app.ui.transaction.AddEditTransactionScreen

@Composable
fun TrackItNavHost(
    navController: NavHostController,
    onExportPdf: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
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
                navArgument("ocrAmount") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val ocrAmountArg = backStackEntry.arguments?.getString("ocrAmount")?.toDoubleOrNull()
            // Also check savedStateHandle (set when returning from ScanReceipt)
            val savedOcrAmount = backStackEntry.savedStateHandle.get<Double>("ocrAmount")
            val finalOcrAmount = savedOcrAmount ?: ocrAmountArg
            AddEditTransactionScreen(
                ocrAmount = finalOcrAmount,
                onNavigateBack = { navController.popBackStack() },
                onOpenCamera = {
                    navController.navigate(Screen.ScanReceipt.route)
                }
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.LongType }
            )
        ) {
            AddEditTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenCamera = {
                    navController.navigate(Screen.ScanReceipt.route)
                }
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
                onExportPdf = onExportPdf
            )
        }

        composable(Screen.ScanReceipt.route) {
            ScanReceiptScreen(
                onNavigateBack = { navController.popBackStack() },
                onAmountDetected = { amount ->
                    // Navigate back to add transaction with detected amount
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("ocrAmount", amount)
                    navController.popBackStack()
                }
            )
        }
    }
}
