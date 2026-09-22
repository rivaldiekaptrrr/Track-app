package com.trackit.app.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.repository.AccessLevel
import com.trackit.app.data.repository.UserInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar on success/error
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0D47A1), Color(0xFF1E88E5))
                        )
                    )
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Panel Super Admin",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 19.sp
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFD54F)
                                ) {
                                    Text(
                                        "PRO",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = Color(0xFF3E2723),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                "Kelola lisensi & hak akses pengguna",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadUsers() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Search & Filters Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { 
                            Text(
                                "Cari email atau nama pembeli...",
                                fontSize = 14.sp
                            ) 
                        },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Search, 
                                contentDescription = "Cari",
                                tint = MaterialTheme.colorScheme.primary
                            ) 
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(
                                        Icons.Default.Close, 
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    // Stats Quick Counters
                    val allUsers = uiState.users
                    val pendingCount = allUsers.count { it.accessLevel == AccessLevel.NONE }
                    val activeCount = allUsers.count { it.accessLevel != AccessLevel.NONE && it.accessLevel != AccessLevel.ADMIN }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickStatCard(
                            label = "Total Akun",
                            count = allUsers.size,
                            color = Color(0xFF1565C0),
                            isSelected = uiState.selectedFilter == AdminFilter.ALL,
                            onClick = { viewModel.setFilter(AdminFilter.ALL) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickStatCard(
                            label = "Antrean Pending",
                            count = pendingCount,
                            color = Color(0xFFE65100),
                            isSelected = uiState.selectedFilter == AdminFilter.PENDING,
                            onClick = { viewModel.setFilter(AdminFilter.PENDING) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickStatCard(
                            label = "Akses Aktif",
                            count = activeCount,
                            color = Color(0xFF2E7D32),
                            isSelected = uiState.selectedFilter == AdminFilter.BOTH || uiState.selectedFilter == AdminFilter.EXPENSE || uiState.selectedFilter == AdminFilter.WEDDING,
                            onClick = { viewModel.setFilter(AdminFilter.BOTH) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Filter Tabs / Chips
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(AdminFilter.values()) { filter ->
                            val isSelected = uiState.selectedFilter == filter
                            val count = when (filter) {
                                AdminFilter.ALL -> allUsers.size
                                AdminFilter.PENDING -> pendingCount
                                AdminFilter.EXPENSE -> allUsers.count { it.accessLevel == AccessLevel.EXPENSE }
                                AdminFilter.WEDDING -> allUsers.count { it.accessLevel == AccessLevel.WEDDING }
                                AdminFilter.BOTH -> allUsers.count { it.accessLevel == AccessLevel.BOTH }
                                AdminFilter.ADMIN -> allUsers.count { it.accessLevel == AccessLevel.ADMIN }
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFilter(filter) },
                                label = {
                                    Text(
                                        "${filter.label} ($count)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Main List Content
            val filteredUsers = viewModel.filteredUsers
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Mengambil data pengguna dari cloud...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                filteredUsers.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            if (uiState.errorMessage != null) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    uiState.errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.loadUsers() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Coba Lagi")
                                }
                            } else {
                                Icon(
                                    if (uiState.selectedFilter == AdminFilter.PENDING) Icons.Default.CheckCircleOutline else Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    when {
                                        uiState.searchQuery.isNotEmpty() -> "Tidak ada pengguna yang cocok dengan '${uiState.searchQuery}'"
                                        uiState.selectedFilter == AdminFilter.PENDING -> "Semua antrean telah diproses 🎉"
                                        else -> "Belum ada akun pengguna terdaftar"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredUsers, key = { it.uid }) { user ->
                            CleanUserAccessCard(
                                userInfo = user,
                                isUpdating = uiState.updatingUid == user.uid,
                                onUpdateAccess = { newLevel ->
                                    viewModel.updateUserAccess(user.uid, newLevel)
                                }
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    label: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else color.copy(alpha = 0.07f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "$count",
                fontWeight = FontWeight.ExtraBold,
                color = color,
                fontSize = 18.sp
            )
            Text(
                label,
                color = color.copy(alpha = 0.85f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleanUserAccessCard(
    userInfo: UserInfo,
    isUpdating: Boolean,
    onUpdateAccess: (String) -> Unit
) {
    val isAdmin = userInfo.accessLevel == AccessLevel.ADMIN
    val isPending = userInfo.accessLevel == AccessLevel.NONE

    val accessOptions = listOf(
        AccessLevel.NONE to ("🔒 Kunci (Pending NONE)"),
        AccessLevel.EXPENSE to ("💰 Khusus Expense"),
        AccessLevel.WEDDING to ("💍 Khusus Wedding"),
        AccessLevel.BOTH to ("⭐ Full Access (Keduanya)")
    )

    var expandedDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar + Info + Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar with color depending on access
                val avatarColor = when (userInfo.accessLevel) {
                    AccessLevel.ADMIN -> listOf(Color(0xFF311B92), Color(0xFF512DA8))
                    AccessLevel.NONE -> listOf(Color(0xFFE65100), Color(0xFFFFA000))
                    AccessLevel.EXPENSE -> listOf(Color(0xFF0D47A1), Color(0xFF1976D2))
                    AccessLevel.WEDDING -> listOf(Color(0xFF4A148C), Color(0xFF7B1FA2))
                    AccessLevel.BOTH -> listOf(Color(0xFF1B5E20), Color(0xFF388E3C))
                    else -> listOf(Color.Gray, Color.DarkGray)
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(avatarColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userInfo.displayName.firstOrNull()?.uppercaseChar()?.toString()
                            ?: userInfo.email.firstOrNull()?.uppercaseChar()?.toString()
                            ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (userInfo.displayName.isNotEmpty()) {
                        Text(
                            userInfo.displayName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        userInfo.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))
                AccessBadge(accessLevel = userInfo.accessLevel)
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(10.dp))

            // Bottom Action: Dropdown Menu / Admin Label
            if (isAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF311B92),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Akun Super Admin Utama (Akses Penuh)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF311B92)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Hak Akses:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Dropdown Box Selector
                    Box {
                        val currentLabel = accessOptions.firstOrNull { it.first == userInfo.accessLevel }?.second
                            ?: "Pilih Akses"

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isUpdating) { expandedDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isUpdating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    currentLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Dropdown Menu popup
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            accessOptions.forEach { (level, label) ->
                                val isCurrent = userInfo.accessLevel == level
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = {
                                        if (isCurrent) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        expandedDropdown = false
                                        if (userInfo.accessLevel != level) {
                                            onUpdateAccess(level)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessBadge(accessLevel: String) {
    val (color, label) = when (accessLevel) {
        AccessLevel.NONE -> Color(0xFFE65100) to "⏳ PENDING"
        AccessLevel.EXPENSE -> Color(0xFF1565C0) to "💰 EXPENSE"
        AccessLevel.WEDDING -> Color(0xFF6A1B9A) to "💍 WEDDING"
        AccessLevel.BOTH -> Color(0xFF2E7D32) to "⭐ FULL ACCESS"
        AccessLevel.ADMIN -> Color(0xFF311B92) to "👑 ADMIN"
        else -> Color.Gray to accessLevel
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
