package com.trackit.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.trackit.app.ui.wedding.dashboard.WeddingDashboardScreen

data class WeddingBottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun WeddingNavHost(
    navController: NavHostController,
    weddingProfileId: String,
    onNavigateToMainProfile: () -> Unit
) {
    val bottomNavItems = listOf(
        WeddingBottomNavItem(Screen.WeddingDashboard.route, "Beranda", Icons.Default.Home),
        WeddingBottomNavItem(Screen.WeddingTasks.route, "Tugas", Icons.Default.CheckCircle),
        WeddingBottomNavItem(Screen.WeddingGuests.route, "Tamu", Icons.Default.People),
        WeddingBottomNavItem(Screen.WeddingBudget.route, "Anggaran", Icons.Default.AccountBalance),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(Screen.WeddingDashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
                // Settings
                NavigationBarItem(
                    selected = currentRoute == Screen.Settings.route,
                    onClick = { navController.navigate(Screen.Settings.route) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                    label = { Text("Pengaturan") }
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
                    onNavigateToGuests = { navController.navigate(Screen.WeddingGuests.route) }
                )
            }
            composable(Screen.WeddingTasks.route) {
                WeddingComingSoonScreen(title = "Timeline & Tugas")
            }
            composable(Screen.WeddingGuests.route) {
                WeddingComingSoonScreen(title = "Manajemen Tamu")
            }
            composable(Screen.WeddingBudget.route) {
                WeddingComingSoonScreen(title = "Anggaran & Split-Bill")
            }
            composable(Screen.WeddingDocuments.route) {
                WeddingComingSoonScreen(title = "Berkas KUA & Dokumen")
            }
            composable(Screen.WeddingVendors.route) {
                WeddingComingSoonScreen(title = "Vendor Hub")
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
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
