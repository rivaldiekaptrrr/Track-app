package com.trackit.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.trackit.app.ui.settings.SettingsScreen
import com.trackit.app.ui.wedding.budget.WeddingBudgetScreen
import com.trackit.app.ui.wedding.dashboard.WeddingDashboardScreen
import com.trackit.app.ui.wedding.documents.WeddingDocumentsScreen
import com.trackit.app.ui.wedding.guests.WeddingGuestsScreen
import com.trackit.app.ui.wedding.tasks.WeddingTasksScreen
import com.trackit.app.ui.wedding.vendor.WeddingVendorScreen
import com.trackit.app.ui.wedding.seserahan.WeddingSeserahanScreen
import com.trackit.app.ui.wedding.committee.WeddingCommitteeScreen
import com.trackit.app.ui.wedding.rundown.WeddingRundownScreen
import com.trackit.app.ui.wedding.settings.WeddingSettingsScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingNavHost(
    navController: NavHostController,
    weddingProfileId: String,
    onNavigateToMainProfile: () -> Unit,
    onExportPdf: (title: String, startDate: Long, endDate: Long, typeFilter: String) -> Unit,
    onExportCsv: (title: String, startDate: Long, endDate: Long, typeFilter: String) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showMenuSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Screen.WeddingDashboard.route,
                    onClick = {
                        navController.navigate(Screen.WeddingDashboard.route) {
                            popUpTo(Screen.WeddingDashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                    label = { Text("Beranda") }
                )
                NavigationBarItem(
                    selected = currentRoute == Screen.WeddingTasks.route,
                    onClick = {
                        navController.navigate(Screen.WeddingTasks.route) {
                            popUpTo(Screen.WeddingDashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Tugas") },
                    label = { Text("Tugas") }
                )
                NavigationBarItem(
                    selected = currentRoute == Screen.WeddingBudget.route,
                    onClick = {
                        navController.navigate(Screen.WeddingBudget.route) {
                            popUpTo(Screen.WeddingDashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Anggaran") },
                    label = { Text("Anggaran") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showMenuSheet = true },
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Menu") },
                    label = { Text("Menu") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.WeddingDashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.WeddingDashboard.route) {
                WeddingDashboardScreen(
                    weddingProfileId = weddingProfileId,
                    onNavigateToTasks = { navController.navigate(Screen.WeddingTasks.route) },
                    onNavigateToDocuments = { navController.navigate(Screen.WeddingDocuments.route) },
                    onNavigateToBudget = { navController.navigate(Screen.WeddingBudget.route) },
                    onNavigateToGuests = { navController.navigate(Screen.WeddingGuests.route) },
                    onNavigateToVendors = { navController.navigate(Screen.WeddingVendors.route) },
                    onNavigateToSeserahan = { navController.navigate(Screen.WeddingSeserahan.route) },
                    onNavigateToCommittee = { navController.navigate(Screen.WeddingCommittee.route) },
                    onNavigateToRundown = { navController.navigate(Screen.WeddingRundown.route) }
                )
            }
            composable(Screen.WeddingTasks.route) {
                WeddingTasksScreen(
                    weddingProfileId = weddingProfileId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WeddingGuests.route) {
                WeddingGuestsScreen(
                    weddingProfileId = weddingProfileId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WeddingBudget.route) {
                WeddingBudgetScreen(
                    weddingProfileId = weddingProfileId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WeddingDocuments.route) {
                WeddingDocumentsScreen(
                    weddingProfileId = weddingProfileId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WeddingVendors.route) {
                WeddingVendorScreen(
                    weddingProfileId = weddingProfileId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WeddingSeserahan.route) {
                WeddingSeserahanScreen(
                    weddingProfileId = weddingProfileId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WeddingCommittee.route) {
                WeddingCommitteeScreen(
                    weddingProfileId = weddingProfileId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WeddingRundown.route) {
                WeddingRundownScreen(
                    weddingProfileId = weddingProfileId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WeddingSettings.route) {
                WeddingSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMainProfile = onNavigateToMainProfile,
                    onExportPdf = onExportPdf,
                    onExportCsv = onExportCsv
                )
            }
            composable(Screen.CustomKeywords.route) {
                com.trackit.app.ui.settings.CustomKeywordScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.CategoryBudget.route) {
                com.trackit.app.ui.budget.CategoryBudgetScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ProfileManagement.route) {
                com.trackit.app.ui.profile.ProfileManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (showMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            WeddingMenuGrid(
                onNavigate = { route ->
                    showMenuSheet = false
                    navController.navigate(route) {
                        popUpTo(Screen.WeddingDashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun WeddingMenuGrid(onNavigate: (String) -> Unit) {
    val menuItems = listOf(
        Triple(Screen.WeddingGuests.route, "Tamu", Icons.Default.People),
        Triple(Screen.WeddingVendors.route, "Vendor", Icons.Default.Store),
        Triple(Screen.WeddingSeserahan.route, "Seserahan", Icons.Default.Redeem),
        Triple(Screen.WeddingCommittee.route, "Panitia", Icons.Default.Groups),
        Triple(Screen.WeddingRundown.route, "Rundown", Icons.Default.Event),
        Triple(Screen.WeddingDocuments.route, "Dokumen", Icons.Default.Folder),
        Triple(Screen.WeddingSettings.route, "Pengaturan", Icons.Default.Settings)
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Menu Lainnya",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            items(menuItems) { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onNavigate(item.first) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.third, contentDescription = item.second, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(item.second, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun WeddingComingSoonScreen(title: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Construction,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Fitur ini sedang dalam pengembangan dan akan hadir di Sprint berikutnya.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
