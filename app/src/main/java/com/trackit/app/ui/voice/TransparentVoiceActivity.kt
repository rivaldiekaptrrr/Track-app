package com.trackit.app.ui.voice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.CurrencyUtils
import com.trackit.app.util.VoiceParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TransparentVoiceActivity : ComponentActivity() {

    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var preferencesManager: PreferencesManager
    
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
                finish()
            }
        } else {
            // Canceled or failed
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Langsung jalankan pop up voice recognizer
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan catatan pengeluaran, contoh: 'Beli kopi 20 ribu'")
        }
        
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Fitur suara tidak didukung di perangkat ini", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun processVoiceInput(spokenText: String) {
        lifecycleScope.launch {
            val profileId = preferencesManager.activeProfileId.first()
            val categories = categoryRepository.getAllCategories(profileId).first()
            
            val parseResult = VoiceParser.parse(spokenText, categories)
            
            if (parseResult.amount > 0) {
                // Berhasil parse
                val entity = TransactionEntity(
                    amount = parseResult.amount,
                    description = parseResult.description,
                    categoryId = parseResult.categoryId,
                    date = System.currentTimeMillis(),
                    profileId = profileId,
                    type = "EXPENSE"
                )
                
                transactionRepository.insertTransaction(entity)
                
                val catName = categories.find { it.id == parseResult.categoryId }?.name ?: "Lainnya"
                Toast.makeText(
                    this@TransparentVoiceActivity, 
                    "Berhasil dicatat: $catName - ${CurrencyUtils.formatRupiah(parseResult.amount)}", 
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@TransparentVoiceActivity, 
                    "Gagal memahami nominal. Ucapan: '$spokenText'", 
                    Toast.LENGTH_LONG
                ).show()
            }
            
            finish()
        }
    }
    
    // Jangan izinkan transisi animasi saat menutup atau membuka
    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
