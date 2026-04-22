package com.trackit.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.trackit.app.data.local.entity.ProfileEntity
import com.trackit.app.util.CategoryIconMapper

data class BottomNavDestination(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val showBadge: Boolean = false
)

val bottomNavDestinations = listOf(
    BottomNavDestination(Screen.Dashboard.route, "Beranda", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavDestination(Screen.Chart.route, "Statistik", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    BottomNavDestination(Screen.Settings.route, "Pengaturan", Icons.Filled.Settings, Icons.Outlined.Settings),
    BottomNavDestination(Screen.ProfileManagement.route, "Profil", Icons.Filled.Person, Icons.Outlined.Person)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackItBottomNavBar(
    navController: NavController,
    onAddClick: () -> Unit,
    onMicLongClick: () -> Unit,
    allProfiles: List<ProfileEntity> = emptyList(),
    activeProfile: ProfileEntity? = null,
    onSwitchProfile: (Long) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showProfileSwitcher by remember { mutableStateOf(false) }

    // Helper to robustly match routes including parameterized ones
    fun isRouteActive(route: String): Boolean {
        val current = currentRoute ?: return false
        return current == route || current.startsWith("${route}?")
    }

    // Derive badge condition from profile data (budget warning)
    val budgetWarning = false // Can be wired to ViewModel later

    val navBackground = Color(0xFF1C1C1E)
    val accentPurple = Color(0xFF6342E8)
    val ringLightBlue = Color(0xFFB0C4FF)
    val activeTextBlue = Color(0xFF4285F4)
    val inactiveGray = Color(0xFF8E8E93)

    // Profile switcher Instagram-style dialog
    if (showProfileSwitcher && allProfiles.isNotEmpty()) {
        Dialog(onDismissRequest = { showProfileSwitcher = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Ganti Profil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    allProfiles.forEach { profile ->
                        val isActive = profile.id == activeProfile?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isActive) Color(0xFF3A3A3C) else Color.Transparent
                                )
                                .clickable {
                                    if (!isActive) onSwitchProfile(profile.id)
                                    showProfileSwitcher = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CategoryIconMapper.parseColor(profile.colorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconMapper.getIcon(profile.iconName),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    profile.name,
                                    color = Color.White,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (isActive) {
                                    Text(
                                        "Aktif sekarang",
                                        color = activeTextBlue,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            if (isActive) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = activeTextBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (profile != allProfiles.last()) {
                            HorizontalDivider(
                                color = Color(0xFF48484A),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            showProfileSwitcher = false
                            navController.navigate(Screen.ProfileManagement.route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = inactiveGray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kelola Profil", color = inactiveGray)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        // Background Navbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(16.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(navBackground)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Beranda, Statistik
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bottomNavDestinations.take(2).forEach { dest ->
                    AnimatedNavItem(
                        label = dest.label,
                        activeIcon = dest.activeIcon,
                        inactiveIcon = dest.inactiveIcon,
                        isActive = isRouteActive(dest.route),
                        activeColor = activeTextBlue,
                        inactiveColor = inactiveGray,
                        showBadge = if (dest.route == Screen.Dashboard.route) budgetWarning else dest.showBadge
                    ) {
                        if (!isRouteActive(dest.route)) {
                            navController.navigate(dest.route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            }

            // Center space for Mic FAB
            Spacer(modifier = Modifier.width(72.dp))

            // Right: Pengaturan, Profil
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Pengaturan (index 2)
                val dest = bottomNavDestinations[2]
                AnimatedNavItem(
                    label = dest.label,
                    activeIcon = dest.activeIcon,
                    inactiveIcon = dest.inactiveIcon,
                    isActive = isRouteActive(dest.route),
                    activeColor = activeTextBlue,
                    inactiveColor = inactiveGray,
                    showBadge = dest.showBadge
                ) {
                    if (!isRouteActive(dest.route)) {
                        navController.navigate(dest.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

                // Profil — with long press to switch
                val profileDest = bottomNavDestinations.last()
                val isProfileActive = currentRoute == profileDest.route
                val profileColor by animateColorAsState(
                    targetValue = if (isProfileActive) activeTextBlue else inactiveGray,
                    animationSpec = tween(300),
                    label = "ProfileColorAnim"
                )

                if (activeProfile != null) {
                    // Show actual profile avatar on long press
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (currentRoute != profileDest.route) {
                                        navController.navigate(profileDest.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showProfileSwitcher = true
                                }
                            )
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isProfileActive)
                                        CategoryIconMapper.parseColor(activeProfile.colorHex)
                                    else
                                        CategoryIconMapper.parseColor(activeProfile.colorHex).copy(alpha = 0.5f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconMapper.getIcon(activeProfile.iconName),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = activeProfile.name.take(8),
                            color = profileColor,
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                } else {
                    AnimatedNavItem(
                        label = profileDest.label,
                        activeIcon = profileDest.activeIcon,
                        inactiveIcon = profileDest.inactiveIcon,
                        isActive = isProfileActive,
                        activeColor = activeTextBlue,
                        inactiveColor = inactiveGray
                    ) {
                        navController.navigate(profileDest.route)
                    }
                }
            }
        }

        // Central Mic FAB overlapping the navbar
        val fabInteractionSource = remember { MutableInteractionSource() }
        val isPressed by fabInteractionSource.collectIsPressedAsState()
        
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-10).dp)
                .size(72.dp)
                .background(Color.White, CircleShape)
                .padding(5.dp)
                .background(ringLightBlue, CircleShape)
                .padding(4.dp)
                .background(accentPurple, CircleShape)
                .combinedClickable(
                    interactionSource = fabInteractionSource,
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAddClick()
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMicLongClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = isPressed, label = "fab_icon") { pressed ->
                Icon(
                    imageVector = if (pressed) Icons.Filled.Mic else Icons.Filled.Add,
                    contentDescription = if (pressed) "Catat Suara" else "Tambah Transaksi",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedNavItem(
    label: String,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    showBadge: Boolean = false,
    onClick: () -> Unit
) {
    val currentColor by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 300),
        label = "NavItemColorAnim"
    )

    // Spring bounce animation on press
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "NavItemScaleAnim"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    isPressed = true
                    onClick()
                    isPressed = false
                }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Icon with badge overlay
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = if (isActive) activeIcon else inactiveIcon,
                contentDescription = label,
                tint = currentColor,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
            // Badge notifikasi
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(x = 2.dp, y = (-2).dp)
                        .background(Color(0xFFFF3B30), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            color = currentColor,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Active dot indicator
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 3.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) activeColor else Color.Transparent
                )
        )
    }
}

