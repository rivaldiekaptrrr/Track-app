package com.trackit.app.ui.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.ProfileEntity
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.local.entity.WeddingProfileEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.ProfileRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.data.repository.WeddingDocumentRepository
import com.trackit.app.data.repository.WeddingProfileRepository
import com.trackit.app.data.repository.WeddingTaskRepository
import com.trackit.app.data.wedding.WeddingDocumentPresets
import com.trackit.app.data.wedding.WeddingTaskPresets
import com.trackit.app.util.CategoryIconMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class ProfileManagementUiState(
    val profiles: List<ProfileEntity> = emptyList(),
    val activeProfileId: Long = 1L,
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileManagementViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val preferencesManager: PreferencesManager,
    private val weddingProfileRepository: WeddingProfileRepository,
    private val weddingDocumentRepository: WeddingDocumentRepository,
    private val weddingTaskRepository: WeddingTaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileManagementUiState())
    val uiState: StateFlow<ProfileManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                profileRepository.getAllProfiles(),
                preferencesManager.activeProfileId
            ) { profiles, activeId ->
                ProfileManagementUiState(
                    profiles = profiles,
                    activeProfileId = activeId,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun saveProfile(profile: ProfileEntity, weddingProfile: WeddingProfileEntity? = null) {
        viewModelScope.launch {
            if (profile.id == 0L) {
                if (profile.mode == "WEDDING" && weddingProfile != null) {
                    // 1. Simpan profil utama terlebih dahulu untuk mendapatkan ID-nya
                    val newProfileId = profileRepository.insert(profile.copy(weddingProfileId = weddingProfile.id))
                    // 2. Simpan wedding profile dengan profileId yang sudah terikat
                    weddingProfileRepository.insert(weddingProfile.copy(profileId = newProfileId))
                    // 3. Auto-seed berkas legalitas sesuai agama
                    val docs = WeddingDocumentPresets.getPreset(
                        weddingProfileId = weddingProfile.id,
                        religionType = weddingProfile.religionType,
                        religionDetail = weddingProfile.religionDetail
                    )
                    weddingDocumentRepository.insertAll(docs)
                    // 4. Auto-seed tugas timeline sesuai adat
                    val tasks = WeddingTaskPresets.getPreset(
                        weddingProfileId = weddingProfile.id,
                        culturalPresetGroom = weddingProfile.culturalPresetGroom,
                        culturalPresetBride = weddingProfile.culturalPresetBride
                    )
                    weddingTaskRepository.insertAll(tasks)
                } else {
                    val newId = profileRepository.insert(profile)
                    // Seed default categories untuk expense profile
                    val defaults = com.trackit.app.data.local.TrackItDatabase
                        .getDefaultCategories()
                        .map { it.copy(profileId = newId) }
                    categoryRepository.insertAll(defaults)
                }
            } else {
                profileRepository.update(profile)
            }
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.profiles.size <= 1) return@launch // Can't delete last profile

            // If deleting the active profile, switch to first remaining profile
            if (profile.id == state.activeProfileId) {
                val fallback = state.profiles.first { it.id != profile.id }
                preferencesManager.setActiveProfileId(fallback.id)
            }
            profileRepository.delete(profile)
        }
    }

    fun switchProfile(profileId: Long) {
        viewModelScope.launch {
            preferencesManager.setActiveProfileId(profileId)
        }
    }
}

// ─── Profile Icon & Color Palette ────────────────────────────────────────────

private val profileColors = listOf(
    "#1565C0", "#D32F2F", "#2E7D32", "#E65100",
    "#6A1B9A", "#00838F", "#795548", "#37474F",
    "#C62828", "#283593", "#00695C", "#F9A825"
)

private val profileIcons = listOf(
    "person", "work", "home", "favorite",
    "groups", "school", "savings", "storefront",
    "fitness_center", "spa", "pets", "star"
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedProfile by remember { mutableStateOf<ProfileEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<ProfileEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Profil", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedProfile = ProfileEntity(name = "", iconName = "person", colorHex = "#1565C0")
                    showDialog = true
                },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Profil Baru") }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Profil yang tersimpan",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(uiState.profiles) { profile ->
                val isActive = profile.id == uiState.activeProfileId
                val containerColor by animateColorAsState(
                    targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer
                                  else MaterialTheme.colorScheme.surface,
                    animationSpec = tween(300),
                    label = "profile_card_color"
                )

                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isActive) 2.dp else 1.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.switchProfile(profile.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Icon Circle
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CategoryIconMapper.parseColor(profile.colorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconMapper.getIcon(profile.iconName),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (isActive) {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Aktif Sekarang",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Edit button
                        IconButton(onClick = {
                            selectedProfile = profile
                            showDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Delete button (only show if more than 1 profile)
                        if (uiState.profiles.size > 1) {
                            IconButton(onClick = { showDeleteConfirm = profile }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Profile Form Dialog
    if (showDialog && selectedProfile != null) {
        ProfileFormDialog(
            profile = selectedProfile!!,
            onDismiss = { showDialog = false },
            onSave = { updated, weddingProfile ->
                viewModel.saveProfile(updated, weddingProfile)
                showDialog = false
            }
        )
    }

    // Delete Confirm Dialog
    showDeleteConfirm?.let { profile ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Hapus Profil?") },
            text = { Text("Profil \"${profile.name}\" beserta semua kategorinya akan dihapus. Transaksi tidak akan hilang.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProfile(profile)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Batal") }
            }
        )
    }
}

// ─── Profile Form Dialog ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFormDialog(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    onSave: (ProfileEntity, WeddingProfileEntity?) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var selectedIcon by remember { mutableStateOf(profile.iconName) }
    var selectedColor by remember { mutableStateOf(profile.colorHex) }
    var selectedMode by remember { mutableStateOf(profile.mode) } // "EXPENSE" or "WEDDING"
    var submitted by remember { mutableStateOf(false) }
    
    // Wedding onboarding fields
    var groomName by remember { mutableStateOf("") }
    var brideName by remember { mutableStateOf("") }
    var budgetCap by remember { mutableStateOf("") }
    var religionType by remember { mutableStateOf("ISLAM") }
    var culturalPresetGroom by remember { mutableStateOf("MODERN") }
    var culturalPresetBride by remember { mutableStateOf("MODERN") }
    var showDatePicker by remember { mutableStateOf(false) }
    var weddingDateMillis by remember { mutableStateOf(System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)) }
    var expandedReligion by remember { mutableStateOf(false) }
    var expandedCultureGroom by remember { mutableStateOf(false) }
    var expandedCultureBride by remember { mutableStateOf(false) }
    
    val religions = listOf("ISLAM", "KRISTEN", "KATOLIK", "HINDU", "BUDDHA", "KONGHUCU")
    val cultures = listOf("MODERN", "JAWA", "SUNDA", "BATAK", "MINANG", "BUGIS", "BALI", "TIONGHOA")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(if (profile.id == 0L) "Buat Profil Baru" else "Edit Profil", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // === Mode Selector (only for new profile) ===
                if (profile.id == 0L) {
                    Text("Tipe Profil", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isExpense = selectedMode == "EXPENSE"
                        val isWedding = selectedMode == "WEDDING"
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isExpense) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedMode = "EXPENSE" }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AccountBalanceWallet, null, Modifier.size(18.dp), tint = if (isExpense) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Expense", color = if (isExpense) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isWedding) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { 
                                    selectedMode = "WEDDING"
                                    selectedIcon = "favorite"
                                    selectedColor = "#C62828"
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Favorite, null, Modifier.size(18.dp), tint = if (isWedding) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Wedding", color = if (isWedding) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Preview
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(CategoryIconMapper.parseColor(selectedColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CategoryIconMapper.getIcon(selectedIcon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; submitted = false },
                    label = { Text("Nama Profil") },
                    isError = submitted && name.isBlank(),
                    supportingText = { if (submitted && name.isBlank()) Text("Nama profil tidak boleh kosong") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Color Picker
                Text("Warna Profil", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                    items(profileColors) { colorHex ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CategoryIconMapper.parseColor(colorHex))
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Icon Picker
                if (selectedMode == "EXPENSE") {
                    Text("Ikon Profil", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(profileIcons) { iconName ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedIcon == iconName) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconMapper.getIcon(iconName),
                                    contentDescription = null,
                                    tint = if (selectedIcon == iconName) MaterialTheme.colorScheme.onPrimaryContainer
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // === Wedding Detail Fields ===
                if (selectedMode == "WEDDING") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Detail Pengantin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            
                            OutlinedTextField(
                                value = groomName,
                                onValueChange = { groomName = it },
                                label = { Text("Nama Mempelai Pria (CPP)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = brideName,
                                onValueChange = { brideName = it },
                                label = { Text("Nama Mempelai Wanita (CPW)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            // Tanggal Pernikahan
                            val dateFormatter = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID"))
                            val dateString = dateFormatter.format(java.util.Date(weddingDateMillis))
                            OutlinedTextField(
                                value = dateString,
                                onValueChange = {},
                                label = { Text("Tanggal Pernikahan") },
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePicker = true },
                                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Anggaran - format rupiah otomatis
                            OutlinedTextField(
                                value = budgetCap,
                                onValueChange = { newValue ->
                                    val unformatted = newValue.replace(Regex("[^0-9]"), "")
                                    budgetCap = if (unformatted.isEmpty()) "" else
                                        java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(unformatted.toLongOrNull() ?: 0L)
                                },
                                label = { Text("Anggaran Maksimal (Rp)") },
                                placeholder = { Text("Contoh: 150.000.000") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Dropdown Agama
                            ExposedDropdownMenuBox(expanded = expandedReligion, onExpandedChange = { expandedReligion = it }) {
                                OutlinedTextField(
                                    value = religionType, onValueChange = {}, readOnly = true,
                                    label = { Text("Agama") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedReligion) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(expanded = expandedReligion, onDismissRequest = { expandedReligion = false }) {
                                    religions.forEach { rel ->
                                        DropdownMenuItem(text = { Text(rel) }, onClick = { religionType = rel; expandedReligion = false })
                                    }
                                }
                            }

                            // Dropdown Adat Pria
                            ExposedDropdownMenuBox(expanded = expandedCultureGroom, onExpandedChange = { expandedCultureGroom = it }) {
                                OutlinedTextField(
                                    value = culturalPresetGroom, onValueChange = {}, readOnly = true,
                                    label = { Text("Adat Pria (CPP)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCultureGroom) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(expanded = expandedCultureGroom, onDismissRequest = { expandedCultureGroom = false }) {
                                    cultures.forEach { cul ->
                                        DropdownMenuItem(text = { Text(cul) }, onClick = { culturalPresetGroom = cul; expandedCultureGroom = false })
                                    }
                                }
                            }

                            // Dropdown Adat Wanita
                            ExposedDropdownMenuBox(expanded = expandedCultureBride, onExpandedChange = { expandedCultureBride = it }) {
                                OutlinedTextField(
                                    value = culturalPresetBride, onValueChange = {}, readOnly = true,
                                    label = { Text("Adat Wanita (CPW)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCultureBride) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(expanded = expandedCultureBride, onDismissRequest = { expandedCultureBride = false }) {
                                    cultures.forEach { cul ->
                                        DropdownMenuItem(text = { Text(cul) }, onClick = { culturalPresetBride = cul; expandedCultureBride = false })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitted = true
                    if (name.isNotBlank()) {
                        val savedProfile = profile.copy(
                            name = name.trim(),
                            iconName = selectedIcon,
                            colorHex = selectedColor,
                            mode = selectedMode
                        )
                        val weddingProfile = if (selectedMode == "WEDDING") {
                            WeddingProfileEntity(
                                groomName = groomName.ifBlank { name.trim() },
                                brideName = brideName.ifBlank { "Pasangan" },
                                weddingDate = weddingDateMillis,
                                totalBudgetCap = budgetCap.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 0.0,
                                religionType = religionType,
                                culturalPresetGroom = culturalPresetGroom,
                                culturalPresetBride = culturalPresetBride
                            )
                        } else null
                        onSave(savedProfile, weddingProfile)
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = weddingDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { weddingDateMillis = it }
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } }
        ) { DatePicker(state = dpState) }
    }
}
