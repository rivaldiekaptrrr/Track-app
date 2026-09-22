package com.trackit.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trackit.app.data.repository.AccessLevel
import com.trackit.app.data.repository.AuthRepository
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.ui.admin.AdminDashboardScreen
import com.trackit.app.ui.auth.ModuleSelectionScreen
import com.trackit.app.ui.auth.PendingVerificationScreen
import com.trackit.app.ui.budget.CategoryBudgetScreen
import com.trackit.app.ui.chart.ChartScreen
import com.trackit.app.ui.dashboard.DashboardScreen
import com.trackit.app.ui.dashboard.DashboardViewModel
import com.trackit.app.ui.profile.ProfileManagementScreen
import com.trackit.app.ui.settings.CustomKeywordScreen
import com.trackit.app.ui.settings.SettingsScreen
import com.trackit.app.ui.transaction.AddEditTransactionScreen
import com.trackit.app.ui.auth.LoginScreen
import com.trackit.app.ui.auth.WelcomeScreen
import com.trackit.app.ui.auth.AuthViewModel


@Composable
fun TrackItNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route,
    authRepository: AuthRepository,
    preferencesManager: PreferencesManager,
    onExportPdf: (title: String, startDate: Long, endDate: Long, typeFilter: String) -> Unit,
    onExportCsv: (title: String, startDate: Long, endDate: Long, typeFilter: String) -> Unit,
    onExportWeddingPdf: (profileId: String, profileName: String) -> Unit = { _, _ -> },
    onExportWeddingCsv: (profileId: String, profileName: String) -> Unit = { _, _ -> }
) {
    // Shared DashboardViewModel for profile data
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

    val authViewModel: AuthViewModel = hiltViewModel()
    val accessLevel by preferencesManager.accessLevel.collectAsState(initial = AccessLevel.NONE)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Auto-switch profile / route based on tier.
    // "Safe to decide" = Room has finished loading AND Firestore sync is complete.
    //   - isLoading=true  → Room hasn't emitted profiles yet → wait
    //   - isSyncing=true  → Firestore pull in progress → wait
    //   - both false      → data is settled; act on what's actually there
    val isSyncing = dashboardUiState.isSyncing
    val isLoading = dashboardUiState.isLoading
    LaunchedEffect(accessLevel, dashboardUiState.allProfiles, dashboardUiState.activeProfile, isSyncing, isLoading, currentRoute) {
        val safeRoutes = setOf(
            Screen.ProfileManagement.route,
            Screen.Login.route,
            Screen.PendingVerification.route,
            Screen.Welcome.route
        )
        // Only act when data has fully settled
        val dataReady = !isLoading && !isSyncing
        if (accessLevel == AccessLevel.WEDDING) {
            val currentActive = dashboardUiState.activeProfile
            val allProfiles = dashboardUiState.allProfiles
            if (currentActive == null || currentActive.mode != "WEDDING") {
                val weddingProfile = allProfiles.find { it.mode == "WEDDING" }
                when {
                    weddingProfile != null -> {
                        // A wedding profile exists — auto-select it immediately
                        dashboardViewModel.switchProfile(weddingProfile.id)
                    }
                    dataReady && currentRoute !in safeRoutes -> {
                        // Data settled, no wedding profile found → user must create one
                        navController.navigate(Screen.ProfileManagement.route)
                    }
                    // Still loading or syncing → wait
                }
            }
        } else if (accessLevel == AccessLevel.EXPENSE) {
            val currentActive = dashboardUiState.activeProfile
            val allProfiles = dashboardUiState.allProfiles
            if (currentActive == null || currentActive.mode == "WEDDING") {
                val expenseProfile = allProfiles.find { it.mode != "WEDDING" }
                when {
                    expenseProfile != null -> {
                        dashboardViewModel.switchProfile(expenseProfile.id)
                    }
                    dataReady && currentRoute !in safeRoutes -> {
                        navController.navigate(Screen.ProfileManagement.route)
                    }
                    // Still loading or syncing → wait
                }
            }
        } else if (accessLevel == AccessLevel.BOTH || accessLevel == AccessLevel.ADMIN) {
            val currentActive = dashboardUiState.activeProfile
            val allProfiles = dashboardUiState.allProfiles
            if (currentActive == null && allProfiles.isNotEmpty()) {
                // Auto-select first profile if none is active
                dashboardViewModel.switchProfile(allProfiles.first().id)
            }
        }
    }

    // === SMART ROUTER: Wedding mode check ===
    val nonWeddingRoutes = setOf(
        Screen.ProfileManagement.route,
        Screen.ModuleSelection.route,
        Screen.Login.route,
        Screen.PendingVerification.route,
        Screen.Welcome.route,
        Screen.AdminDashboard.route
    )
    val activeProfile = dashboardUiState.activeProfile
    if (activeProfile?.mode == "WEDDING" && activeProfile.weddingProfileId != null &&
        currentRoute !in nonWeddingRoutes) {
        val weddingNavController = rememberNavController()
        WeddingNavHost(
            navController = weddingNavController,
            weddingProfileId = activeProfile.weddingProfileId,
            onNavigateToMainProfile = {
                navController.navigate(Screen.ProfileManagement.route)
            },
            onExportPdf = onExportPdf,
            onExportCsv = onExportCsv,
            onExportWeddingPdf = onExportWeddingPdf,
            onExportWeddingCsv = onExportWeddingCsv,
            onNavigateToLogin = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onNavigateToModuleSelection = {
                navController.navigate(Screen.ModuleSelection.route)
            }
        )
        return
    }

    // === EXPENSE TRACKER (default) ===

    val hideNavBarRoutes = listOf(
        Screen.AddTransaction.route,
        "add_transaction?startVoice={startVoice}",
        Screen.EditTransaction.route,
        "edit_transaction/{transactionId}",
        Screen.CustomKeywords.route,
        Screen.ProfileManagement.route,
        Screen.CategoryBudget.route,
        Screen.Login.route,
        Screen.Welcome.route,
        Screen.PendingVerification.route,
        Screen.ModuleSelection.route,
        Screen.AdminDashboard.route
    )
    val shouldShowNavBar = hideNavBarRoutes.none { currentRoute?.startsWith(it.substringBefore("{")) == true }

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
                    accessLevel = accessLevel,
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
                    },
                    onAddTransactionWithVoice = {
                        navController.navigate(Screen.AddTransaction.createRoute(startVoice = true))
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
                    navArgument("transactionId") { type = NavType.StringType }
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
                    onExportPdf = { title, startDate, endDate, typeFilter ->
                        onExportPdf(title, startDate, endDate, typeFilter)
                    },
                    onExportCsv = { title, startDate, endDate, typeFilter ->
                        onExportCsv(title, startDate, endDate, typeFilter)
                    },
                    onNavigateToCustomKeywords = {
                        navController.navigate(Screen.CustomKeywords.route)
                    },
                    onNavigateToCategoryBudget = {
                        navController.navigate(Screen.CategoryBudget.route)
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route)
                    },
                    onNavigateToModuleSelection = {
                        navController.navigate(Screen.ModuleSelection.route)
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
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.ProfileManagement.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.CategoryBudget.route) {
                CategoryBudgetScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { level ->
                        // Always navigate to Dashboard; the smart router LaunchedEffect above
                        // will redirect to ProfileManagement AFTER sync completes if needed.
                        // Never route to ProfileManagement directly from login because
                        // profiles may not have loaded yet (sync is async).
                        val dest = when (level) {
                            AccessLevel.ADMIN -> Screen.AdminDashboard.route
                            AccessLevel.NONE -> Screen.PendingVerification.route
                            AccessLevel.BOTH -> Screen.ModuleSelection.route
                            else -> Screen.Dashboard.route
                        }
                        navController.navigate(dest) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        // onSkip is kept for compatibility but won't be shown in UI
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Welcome.route) {
                val vm: AuthViewModel = hiltViewModel()
                WelcomeScreen(
                    onContinue = {
                        vm.setSeenWelcome()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.PendingVerification.route) {
                PendingVerificationScreen(
                    authRepository = authRepository,
                    preferencesManager = preferencesManager,
                    onAccessGranted = { level ->
                        // Same as login: always go to Dashboard so smart router can
                        // redirect after sync completes, not before.
                        val dest = when (level) {
                            AccessLevel.BOTH -> Screen.ModuleSelection.route
                            AccessLevel.ADMIN -> Screen.AdminDashboard.route
                            AccessLevel.NONE -> Screen.PendingVerification.route
                            else -> Screen.Dashboard.route
                        }
                        navController.navigate(dest) {
                            popUpTo(Screen.PendingVerification.route) { inclusive = true }
                        }
                    },
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.ModuleSelection.route) {
                ModuleSelectionScreen(
                    onSelectExpense = {
                        val expenseProfile = dashboardUiState.allProfiles.firstOrNull { it.mode != "WEDDING" }
                        if (expenseProfile != null) {
                            dashboardViewModel.switchProfile(expenseProfile.id)
                        }
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.ModuleSelection.route) { inclusive = true }
                        }
                    },
                    onSelectWedding = {
                        // Switch to wedding profile then navigate
                        val weddingProfile = dashboardUiState.allProfiles.firstOrNull { it.mode == "WEDDING" }
                        if (weddingProfile != null) {
                            dashboardViewModel.switchProfile(weddingProfile.id)
                        }
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.ModuleSelection.route) { inclusive = true }
                        }
                    },
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
