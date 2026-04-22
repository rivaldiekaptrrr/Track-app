package com.trackit.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.trackit.app.ui.chart.ChartScreen
import com.trackit.app.ui.dashboard.DashboardScreen
import com.trackit.app.ui.dashboard.DashboardViewModel
import com.trackit.app.ui.profile.ProfileManagementScreen
import com.trackit.app.ui.settings.CustomKeywordScreen
import com.trackit.app.ui.settings.SettingsScreen
import com.trackit.app.ui.transaction.AddEditTransactionScreen

@Composable
fun TrackItNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    // Routes where bottom navbar should be hidden
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val hideNavBarRoutes = listOf(
        Screen.AddTransaction.route,
        "add_transaction?startVoice={startVoice}",
        Screen.EditTransaction.route,
        "edit_transaction/{transactionId}",
        Screen.CustomKeywords.route,
        Screen.ProfileManagement.route
    )
    val shouldShowNavBar = hideNavBarRoutes.none { currentRoute?.startsWith(it.substringBefore("{")) == true }

    // Shared DashboardViewModel for profile data in navbar
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (shouldShowNavBar) {
                TrackItBottomNavBar(
                    navController = navController,
                    onAddClick = {
                        navController.navigate(Screen.AddTransaction.createRoute(startVoice = false))
                    },
                    onMicLongClick = {
                        navController.navigate(Screen.AddTransaction.createRoute(startVoice = true))
                    },
                    allProfiles = dashboardUiState.allProfiles,
                    activeProfile = dashboardUiState.activeProfile,
                    onSwitchProfile = { profileId -> dashboardViewModel.switchProfile(profileId) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onAddTransaction = {
                        navController.navigate(Screen.AddTransaction.createRoute())
                    },
                    onEditTransaction = { id ->
                        navController.navigate(Screen.EditTransaction.createRoute(id))
                    },
                    onNavigateToProfiles = {
                        navController.navigate(Screen.ProfileManagement.route)
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
                    onExportCsv = onExportCsv,
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

            composable(Screen.ProfileManagement.route) {
                ProfileManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
