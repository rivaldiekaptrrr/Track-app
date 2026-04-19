package com.trackit.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.util.CategoryIconMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomKeywordViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { list ->
                _categories.value = list
            }
        }
    }

    fun updateCustomKeywords(category: CategoryEntity, newKeywords: String) {
        viewModelScope.launch {
            val updatedCategory = category.copy(customKeywords = newKeywords.trim())
            categoryRepository.update(updatedCategory)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomKeywordScreen(
    onNavigateBack: () -> Unit,
    viewModel: CustomKeywordViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var currentKeywords by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kata Kunci Suara", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Tambahkan kata kunci khusus untuk setiap kategori agar deteksi suara lebih akurat. Pisahkan dengan koma.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { category ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.clickable {
                            selectedCategory = category
                            currentKeywords = category.customKeywords
                            showDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CategoryIconMapper.parseColor(category.colorHex).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconMapper.getIcon(category.iconName),
                                    contentDescription = category.name,
                                    tint = CategoryIconMapper.parseColor(category.colorHex),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (category.customKeywords.isEmpty()) "Belum ada kata kunci khusus" 
                                           else category.customKeywords,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showDialog && selectedCategory != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Edit Kata Kunci: ${selectedCategory?.name}") },
                text = {
                    Column {
                        Text(
                            "Masukkan kata kunci yang sering Anda ucapkan untuk kategori ini. Pisahkan dengan koma (contoh: kos, pdam, kontrakan)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = currentKeywords,
                            onValueChange = { currentKeywords = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("kos, kontrakan") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedCategory?.let {
                                viewModel.updateCustomKeywords(it, currentKeywords)
                            }
                            showDialog = false
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
