package com.trackit.app.ui.transaction

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.util.CategoryIconMapper
import com.trackit.app.util.DateUtils
import com.trackit.app.util.NumberUtils
import com.trackit.app.util.VoiceParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    startVoice: Boolean = false,
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = formState.date
    )

    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var isListening by remember { mutableStateOf(false) }
    var showHighlight by remember { mutableStateOf(false) }

    // TTS Setup
    val preferencesManager = remember { com.trackit.app.data.local.PreferencesManager(context) }
    val isTtsEnabled by preferencesManager.isTtsEnabled.collectAsState(initial = true)
    
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("id", "ID")
            }
        }
        tts = textToSpeech
        onDispose { textToSpeech.shutdown() }
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isListening = false
        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (spokenText != null) {
                val parseResult = VoiceParser.parse(spokenText, formState.categories)
                if (parseResult.isValid) {
                    viewModel.setFromVoice(
                        amount = parseResult.amount ?: 0L,
                        description = parseResult.description,
                        categoryName = parseResult.categoryName,
                        dateMillis = parseResult.dateMillis,
                        type = parseResult.type
                    )
                    showHighlight = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    
                    // Auto-save jika kategori berhasil terdeteksi
                    if (parseResult.categoryName != null) {
                        viewModel.saveTransaction()
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("✓ Terdeteksi: $spokenText. Silakan pilih kategori.")
                        }
                    }
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    Toast.makeText(context, "Nominal tidak terdeteksi, coba lagi", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            Toast.makeText(context, "Suara tidak terdengar, coba lagi", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            isListening = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan pengeluaran Anda, contoh: beli sayur 50 ribu")
                }
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                isListening = false
                Toast.makeText(context, "Fitur suara tidak didukung di perangkat ini", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin mikrofon diperlukan untuk fitur suara", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(startVoice) {
        if (startVoice && !formState.isEditing) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val formattedAmount = remember(formState.amount) {
        NumberUtils.formatWithThousandSeparators(formState.amount)
    }

    LaunchedEffect(showHighlight) {
        if (showHighlight) {
            delay(1000)
            showHighlight = false
        }
    }

    val highlightColor by animateColorAsState(
        targetValue = if (showHighlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "highlight_anim"
    )

    // Navigate back on save
    LaunchedEffect(formState.savedSuccessfully) {
        if (formState.savedSuccessfully) {
            if (isTtsEnabled && tts != null) {
                val categoryName = formState.categories.find { it.id == formState.selectedCategoryId }?.name ?: ""
                val typeText = if (formState.type == "INCOME") "pemasukan" else "pengeluaran"
                val textToSpeak = "Tersimpan, $typeText $categoryName ${formState.amount} rupiah"
                
                // Menunggu TTS selesai bicara (maksimal 5 detik agar tidak hang)
                withTimeoutOrNull(5000) {
                    suspendCancellableCoroutine { continuation ->
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {}
                            override fun onDone(utteranceId: String?) {
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                            override fun onError(utteranceId: String?) {
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                        })
                        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "SAVE_SUCCESS")
                    }
                }
            }
            onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (formState.isEditing) "Edit Transaksi" else "Tambah Transaksi",
                        fontWeight = FontWeight.SemiBold
                    )
                },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Type Selection
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = formState.type == "EXPENSE",
                    onClick = { viewModel.updateType("EXPENSE") },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Pengeluaran")
                }
                SegmentedButton(
                    selected = formState.type == "INCOME",
                    onClick = { viewModel.updateType("INCOME") },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Pemasukan")
                }
            }

            // Amount Input with Camera Icon
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (formState.type == "INCOME") 
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    else 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Nominal",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Rp",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = formattedAmount,
                            onValueChange = { viewModel.updateAmount(it) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0") },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = highlightColor,
                                unfocusedContainerColor = highlightColor,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalIconButton(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Catat dengan Suara",
                                tint = if (isListening) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    if (isListening) {
                        Text(
                            "Mendengarkan...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
                        )
                    }
                }
            }

            // Voice Education Marquee
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tips: \"Beli nasi goreng 25 ribu\" • \"Dapat gaji 5 juta\" • \"Kemarin bayar listrik 200 ribu\" • \"Gocap buat parkir\" ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            delayMillis = 0,
                            initialDelayMillis = 0
                        )
                    )
                }
            }

            // Description
            OutlinedTextField(
                value = formState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Deskripsi (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = highlightColor,
                    unfocusedContainerColor = highlightColor
                )
            )

            // Date Picker
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Tanggal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            DateUtils.formatDate(formState.date),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Category Selection
            Text(
                "Kategori",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.heightIn(max = 250.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filteredCategories = formState.categories.filter { 
                    it.type == formState.type && (!it.isHidden || it.id == formState.selectedCategoryId)
                }
                items(filteredCategories) { category ->
                    CategoryChip(
                        category = category,
                        isSelected = formState.selectedCategoryId == category.id,
                        onClick = { viewModel.updateCategory(category.id) }
                    )
                }
            }

            // Recurring Transaction Toggle
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Transaksi Berulang",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Switch(
                            checked = formState.isRecurring,
                            onCheckedChange = { viewModel.updateRecurring(it) }
                        )
                    }
                    if (formState.isRecurring) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("DAILY" to "Harian", "WEEKLY" to "Mingguan", "MONTHLY" to "Bulanan").forEach { (type, label) ->
                                FilterChip(
                                    selected = formState.recurringType == type,
                                    onClick = { viewModel.updateRecurringType(type) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // Error Message
            formState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Save Button
            Button(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.saveTransaction() 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !formState.isSaving,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Simpan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Date Picker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                viewModel.updateDate(it)
                            }
                            showDatePicker = false
                        }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Batal")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected)
            CategoryIconMapper.parseColor(category.colorHex).copy(alpha = 0.2f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "category_bg"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    CategoryIconMapper.parseColor(category.colorHex),
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CategoryIconMapper.parseColor(category.colorHex).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIconMapper.getIcon(category.iconName),
                contentDescription = category.name,
                tint = CategoryIconMapper.parseColor(category.colorHex),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}
