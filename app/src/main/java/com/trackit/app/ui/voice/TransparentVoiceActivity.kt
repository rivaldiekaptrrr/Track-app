package com.trackit.app.ui.voice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TransparentVoiceActivity : ComponentActivity() {

    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var preferencesManager: PreferencesManager
    
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    
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
                        finishActivityCleanly()
                    }
                    override fun onError(utteranceId: String?) {
                        finishActivityCleanly()
                    }
                })
            }
        }
        
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
            finishActivityCleanly()
        }
    }

    private fun processVoiceInput(spokenText: String) {
        lifecycleScope.launch {
            val profileId = preferencesManager.activeProfileId.first()
            val categories = categoryRepository.getAllCategories(profileId).first()
            
            val parseResult = VoiceParser.parse(spokenText, categories)
            
            if (parseResult.isValid && parseResult.amount != null) {
                // Berhasil parse
                val matchedCategory = categories.find { it.name.equals(parseResult.categoryName, ignoreCase = true) }

                val entity = TransactionEntity(
                    amount = parseResult.amount.toDouble(),
                    description = parseResult.description,
                    categoryId = matchedCategory?.id,
                    date = parseResult.dateMillis ?: System.currentTimeMillis(),
                    profileId = profileId,
                    type = parseResult.type
                )
                
                transactionRepository.insert(entity)
                
                val catName = matchedCategory?.name ?: "Lainnya"
                val textToSpeak = "Tercatat, $catName, ${CurrencyUtils.formatRupiah(entity.amount)}"
                Toast.makeText(this@TransparentVoiceActivity, textToSpeak, Toast.LENGTH_LONG).show()
                
                val isTtsEnabled = preferencesManager.isTtsEnabled.first()
                if (isTtsEnabled && isTtsReady && tts != null) {
                    tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "SAVE_SUCCESS")
                } else {
                    finishActivityCleanly()
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
    
    private fun finishActivityCleanly() {
        runOnUiThread {
            finish()
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
    
    // Jangan izinkan transisi animasi saat menutup atau membuka
    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
