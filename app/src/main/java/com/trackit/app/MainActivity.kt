package com.trackit.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.trackit.app.data.local.TrackItDatabase
import com.trackit.app.data.local.entity.BudgetSettingEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.ui.biometric.BiometricLockScreen
import com.trackit.app.ui.navigation.Screen
import com.trackit.app.ui.navigation.TrackItNavHost
import com.trackit.app.ui.theme.TrackItTheme
import com.trackit.app.util.BackupManager
import com.trackit.app.util.CurrencyUtils
import com.trackit.app.util.DateUtils
import com.trackit.app.util.PdfExporter
import com.trackit.app.util.CsvExporter
import com.trackit.app.worker.BudgetCheckWorker
import com.trackit.app.worker.RecurringTransactionWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var database: com.trackit.app.data.local.TrackItDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FR 3.2 - Privacy Screen: Hide content in Recent Apps
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Seed default categories on first launch
        seedCategories()

        // Schedule periodic workers
        scheduleWorkers()

        var showRestoreDialog by mutableStateOf(false)
        lifecycleScope.launch {
            val transactions = transactionRepository.getAllTransactions().first()
            if (transactions.isEmpty() && BackupManager.getAutoBackupFile() != null) {
                showRestoreDialog = true
            }
        }

        setContent {
            TrackItTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticated by remember { mutableStateOf(false) }
                    var biometricError by remember { mutableStateOf<String?>(null) }
                    val biometricAvailable = remember { checkBiometricAvailability() }

                    if (!biometricAvailable) {
                        // Skip biometric if not available
                        isAuthenticated = true
                    }

                    if (isAuthenticated) {
                        val navController = rememberNavController()
                        val startVoice = intent.getBooleanExtra("START_VOICE_IMMEDIATELY", false)
                        val startDest = if (startVoice) Screen.AddTransaction.createRoute(startVoice = true) else Screen.Dashboard.route
                        
                        TrackItNavHost(
                            navController = navController,
                            startDestination = startDest,
                            onExportPdf = { exportPdf() },
                            onExportCsv = { exportCsv() }
                        )

                        if (showRestoreDialog) {
                            AlertDialog(
                                onDismissRequest = { showRestoreDialog = false },
                                title = { Text("Cadangan Lokal Ditemukan") },
                                text = { Text("Kami menemukan file cadangan transaksi lama Anda di folder Documents. Apakah Anda ingin memulihkannya?") },
                                confirmButton = {
                                    Button(onClick = {
                                        BackupManager.isRestoring = true
                                        BackupManager.restoreFromAutoBackup(this@MainActivity)
                                        showRestoreDialog = false
                                        
                                        val pm = packageManager
                                        val restartIntent = pm.getLaunchIntentForPackage(packageName)
                                        val mainIntent = android.content.Intent.makeRestartActivityTask(restartIntent!!.component)
                                        startActivity(mainIntent)
                                        Runtime.getRuntime().exit(0)
                                    }) {
                                        Text("Ya, Pulihkan")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showRestoreDialog = false }) {
                                        Text("Abaikan")
                                    }
                                }
                            )
                        }
                    } else {
                        BiometricLockScreen(
                            onAuthenticate = {
                                showBiometricPrompt(
                                    onSuccess = { isAuthenticated = true },
                                    onError = { biometricError = it }
                                )
                            },
                            errorMessage = biometricError
                        )

                        // Auto-trigger biometric on first display
                        LaunchedEffect(Unit) {
                            showBiometricPrompt(
                                onSuccess = { isAuthenticated = true },
                                onError = { biometricError = it }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkBiometricAvailability(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Autentikasi gagal. Coba lagi.")
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autentikasi Diperlukan")
            .setSubtitle("Gunakan sidik jari, face unlock, atau sandi HP untuk masuk")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun seedCategories() {
        lifecycleScope.launch {
            val categoryDao = database.categoryDao()
            if (categoryDao.getCount() == 0) {
                categoryDao.insertAll(TrackItDatabase.getDefaultCategories())
                // Also seed default budget setting
                database.budgetSettingDao().insert(BudgetSettingEntity(monthlyBudget = 0.0))
            }
        }
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(this)

        // Budget Check - Every 6 hours
        val budgetWork = PeriodicWorkRequestBuilder<BudgetCheckWorker>(
            6, TimeUnit.HOURS
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        ).build()

        workManager.enqueueUniquePeriodicWork(
            BudgetCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            budgetWork
        )

        // Recurring Transactions - Daily
        val recurringWork = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
            1, TimeUnit.DAYS
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        ).build()

        workManager.enqueueUniquePeriodicWork(
            RecurringTransactionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            recurringWork
        )
    }

    private fun exportPdf() {
        lifecycleScope.launch {
            val startOfMonth = DateUtils.getStartOfMonth()
            val endOfMonth = DateUtils.getEndOfMonth()
            val monthYear = DateUtils.formatMonthYear(System.currentTimeMillis())

            val transactions = transactionRepository
                .getTransactionsByMonth(startOfMonth, endOfMonth)
                .first()

            val categories = categoryRepository.getAllCategories().first()
            val categoryMap = categories.associateBy { it.id }

            val totalSpent = transactionRepository
                .getTotalSpentInMonthSync(startOfMonth, endOfMonth)

            PdfExporter.exportMonthlyReport(
                context = this@MainActivity,
                transactions = transactions,
                categories = categoryMap,
                monthYear = monthYear,
                totalSpent = totalSpent
            )
        }
    }
    
    private fun exportCsv() {
        lifecycleScope.launch {
            val startOfMonth = DateUtils.getStartOfMonth()
            val endOfMonth = DateUtils.getEndOfMonth()
            val monthYear = DateUtils.formatMonthYear(System.currentTimeMillis())

            val transactions = transactionRepository
                .getTransactionsByMonth(startOfMonth, endOfMonth)
                .first()

            val categories = categoryRepository.getAllCategories().first()
            val categoryMap = categories.associateBy { it.id }

            CsvExporter.exportMonthlyReport(
                context = this@MainActivity,
                transactions = transactions,
                categories = categoryMap,
                monthYear = monthYear
            )
        }
    }

    override fun onStop() {
        super.onStop()
        // Always perform auto-backup when app goes to background
        BackupManager.autoBackup(this)
    }
}
