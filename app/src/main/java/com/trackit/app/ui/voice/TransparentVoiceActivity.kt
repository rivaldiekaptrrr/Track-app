package com.trackit.app.ui.voice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.ui.theme.TrackItTheme
import com.trackit.app.util.CategoryIconMapper
import com.trackit.app.util.CurrencyUtils
import com.trackit.app.util.VoiceParseResult
import com.trackit.app.util.VoiceParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class TransparentVoiceActivity : ComponentActivity() {

    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var preferencesManager: PreferencesManager
    
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var profileId: Long = 1L
    
    private val showCategorySelector = mutableStateOf(false)
    private val pendingParseResult = mutableStateOf<VoiceParseResult?>(null)
    private val availableCategories = mutableStateOf<List<CategoryEntity>>(emptyList())
    
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull() ?: ""

            if (spokenText.isNotEmpty()) {
                processVoiceInput(spokenText)
            } else {
                Toast.makeText(this, "Suara tidak terdengar jelas", Toast.LENGTH_SHORT).show()
                finishActivityCleanly()
            }
        } else {
            // Canceled or failed
            finishActivityCleanly()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize TTS
        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("id", "ID")
                isTtsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (!showCategorySelector.value) {
                            finishActivityCleanly()
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        if (!showCategorySelector.value) {
                            finishActivityCleanly()
                        }
                    }
                })
            }
        }
        
        setContent {
            // Menggunakan MaterialTheme bawaan Compose agar tidak merusak status bar transparansi
            MaterialTheme {
                val showSelector by showCategorySelector
                val parseResult by pendingParseResult
                val categories by availableCategories

                Box(modifier = Modifier.fillMaxSize().background(Color.Transparent))

                if (showSelector && parseResult != null) {
                    CategorySelectionBottomSheet(
                        categories = categories.filter { it.type == parseResult!!.type && !it.isHidden },
                        keyword = extractUnknownKeyword(parseResult!!.description),
                        amount = CurrencyUtils.formatRupiah(parseResult!!.amount!!.toDouble()),
                        onCategorySelected = { category ->
                            handleCategorySelected(parseResult!!, category)
                        },
                        onDismiss = {
                            handleTimeoutOrDismiss(parseResult!!)
                        }
                    )
                }
            }
        }
        
        lifecycleScope.launch {
            profileId = preferencesManager.activeProfileId.first()
            
            // Langsung jalankan pop up voice recognizer
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan catatan pengeluaran, contoh: 'Beli kopi 20 ribu'")
            }
            
            runOnUiThread {
                try {
                    speechRecognizerLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@TransparentVoiceActivity, "Fitur suara tidak didukung di perangkat ini", Toast.LENGTH_SHORT).show()
                    finishActivityCleanly()
                }
            }
        }
    }

    private fun processVoiceInput(spokenText: String) {
        lifecycleScope.launch {
            val categories = categoryRepository.getAllCategories(profileId).first()
            val batchResults = VoiceParser.parseBatch(spokenText, categories)
            
            if (batchResults.size > 1) {
                saveBatchTransactions(batchResults, categories)
            } else {
                val parseResult = VoiceParser.parse(spokenText, categories)
                
                if (parseResult.isValid && parseResult.amount != null) {
                    val matchedCategory = categories.find { it.name.equals(parseResult.categoryName, ignoreCase = true) }
                    
                    if (matchedCategory != null) {
                        saveTransaction(parseResult, matchedCategory)
                    } else {
                        // Tampilkan BottomSheet
                        availableCategories.value = categories
                        pendingParseResult.value = parseResult
                        showCategorySelector.value = true
                        
                        val keyword = extractUnknownKeyword(parseResult.description)
                        val textToSpeak = if (keyword.isNotEmpty()) {
                            "Kategori tidak dikenali. Pilih kategori untuk $keyword."
                        } else {
                            "Kategori tidak dikenali. Pilih kategori."
                        }
                        
                        val isTtsEnabled = preferencesManager.isTtsEnabled.first()
                        if (isTtsEnabled && isTtsReady && tts != null) {
                            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "SELECT_CATEGORY")
                        }
                    }
                } else {
                    Toast.makeText(
                        this@TransparentVoiceActivity, 
                        "Gagal memahami nominal. Ucapan: '$spokenText'", 
                        Toast.LENGTH_LONG
                    ).show()
                    finishActivityCleanly()
                }
            }
        }
    }

    private fun saveBatchTransactions(batchResults: List<VoiceParseResult>, categories: List<CategoryEntity>) {
        lifecycleScope.launch {
            val fallbackCategory = categories.find { it.name.equals("Lainnya", ignoreCase = true) }
            
            batchResults.filter { it.amount != null }.forEach { result ->
                val matchedCategory = categories.find { it.name.equals(result.categoryName, ignoreCase = true) } ?: fallbackCategory
                
                val entity = TransactionEntity(
                    amount = result.amount!!.toDouble(),
                    description = result.description,
                    categoryId = matchedCategory?.id,
                    date = result.dateMillis ?: System.currentTimeMillis(),
                    profileId = profileId,
                    type = result.type
                )
                transactionRepository.insert(entity)
            }
            
            val textToSpeak = "Tersimpan, ${batchResults.size} transaksi"
            Toast.makeText(this@TransparentVoiceActivity, textToSpeak, Toast.LENGTH_LONG).show()
            
            val isTtsEnabled = preferencesManager.isTtsEnabled.first()
            if (isTtsEnabled && isTtsReady && tts != null) {
                tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "SAVE_SUCCESS")
            } else {
                finishActivityCleanly()
            }
        }
    }

    private fun saveTransaction(parseResult: VoiceParseResult, category: CategoryEntity, isFallback: Boolean = false) {
        lifecycleScope.launch {
            val entity = TransactionEntity(
                amount = parseResult.amount!!.toDouble(),
                description = parseResult.description,
                categoryId = category.id,
                date = parseResult.dateMillis ?: System.currentTimeMillis(),
                profileId = profileId,
                type = parseResult.type
            )
            
            transactionRepository.insert(entity)
            
            val prefix = if (isFallback) "Disimpan ke " else "Tercatat, "
            val textToSpeak = "$prefix${category.name}, ${CurrencyUtils.formatRupiah(entity.amount)}"
            Toast.makeText(this@TransparentVoiceActivity, textToSpeak, Toast.LENGTH_LONG).show()
            
            val isTtsEnabled = preferencesManager.isTtsEnabled.first()
            if (isTtsEnabled && isTtsReady && tts != null) {
                tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "SAVE_SUCCESS")
            } else {
                finishActivityCleanly()
            }
        }
    }

    private fun handleCategorySelected(parseResult: VoiceParseResult, category: CategoryEntity) {
        showCategorySelector.value = false
        
        lifecycleScope.launch {
            val rawKeyword = extractUnknownKeyword(parseResult.description)
            
            if (rawKeyword.isNotEmpty()) {
                val newWords = rawKeyword.split(" ").map { it.trim() }.filter { it.length > 2 }
                val existingList = if (category.customKeywords.isEmpty()) emptyList() else category.customKeywords.split(",").map { it.trim().lowercase() }
                
                val toAdd = newWords.filter { it.lowercase() !in existingList }
                
                if (toAdd.isNotEmpty()) {
                    val newKeywords = if (existingList.isEmpty()) {
                        toAdd.joinToString(", ")
                    } else {
                        category.customKeywords + ", " + toAdd.joinToString(", ")
                    }
                    categoryRepository.update(category.copy(customKeywords = newKeywords))
                }
            }
            
            saveTransaction(parseResult, category)
        }
    }

    private fun handleTimeoutOrDismiss(parseResult: VoiceParseResult) {
        showCategorySelector.value = false
        lifecycleScope.launch {
            val categories = categoryRepository.getAllCategories(profileId).first()
            val fallbackCategory = categories.find { it.name.equals("Lainnya", ignoreCase = true) } 
                ?: categories.firstOrNull { it.type == parseResult.type }
                
            if (fallbackCategory != null) {
                saveTransaction(parseResult, fallbackCategory, isFallback = true)
            } else {
                finishActivityCleanly()
            }
        }
    }

    private fun extractUnknownKeyword(description: String): String {
        val stopWords = listOf("beli", "bayar", "buat", "untuk", "harga", "ongkos", "biaya", "lagi", "yang", "dan", "dari", "ke", "di", "belanja", "pengeluaran")
        
        // Memecah berdasarkan spasi, koma, dan titik agar angka seperti "30.000" 
        // atau kata yang menempel tanda baca (misal "shampo,") terpisah dengan baik.
        var words = description.lowercase().split(Regex("[\\s,.]+"))
        
        words = words.filter { it.isNotBlank() }
        words = words.filter { it !in stopWords }
        
        val amountWords = listOf("ribu", "ratus", "juta", "gocap", "cepek", "gopek", "seceng", "noban", "goban", "selawe", "rupiah", "rp", "perak", "jt", "rb", "rebu")
        words = words.filter { it !in amountWords }
        
        // Filter paling penting: Buang semua kata yang mengandung angka sama sekali.
        // Ini akan membuang "30", "000", "rp30", "30k", dll.
        words = words.filter { !it.contains(Regex("\\d")) }
        
        return words.joinToString(" ").trim()
    }

    private fun finishActivityCleanly() {
        runOnUiThread {
            finish()
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
    
    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionBottomSheet(
    categories: List<CategoryEntity>,
    keyword: String,
    amount: String,
    onCategorySelected: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalSeconds = 7
    var timeLeft by remember { mutableStateOf(totalSeconds) }
    val progress by animateFloatAsState(
        targetValue = timeLeft / totalSeconds.toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "timer_progress"
    )

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Pilih Kategori untuk:",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = if (keyword.isNotEmpty()) "\"$keyword\" • $amount" else amount,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (timeLeft > 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(categories) { category ->
                    CategoryChip(category = category, isSelected = false, onClick = { onCategorySelected(category) })
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
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
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CategoryIconMapper.parseColor(category.colorHex).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIconMapper.getIcon(category.iconName),
                contentDescription = category.name,
                tint = CategoryIconMapper.parseColor(category.colorHex),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
