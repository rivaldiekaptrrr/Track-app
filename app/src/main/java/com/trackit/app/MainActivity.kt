package com.trackit.app

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import com.trackit.app.util.WeddingPdfExporter
import com.trackit.app.util.WeddingCsvExporter
import com.trackit.app.worker.BudgetCheckWorker
import com.trackit.app.worker.RecurringTransactionWorker
import com.trackit.app.data.repository.ProfileRepository
import com.trackit.app.data.repository.WeddingExpenseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.trackit.app.data.repository.AuthRepository
import com.trackit.app.data.repository.AccessLevel

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    companion object {
        // Toggle untuk Developer: Set 'false' agar bisa mengambil screenshot dokumentasi.
        // Wajib di-set ke 'true' kembali saat rilis ke Production (FR 3.2 Privacy).
        const val ENABLE_ANTI_SCREENSHOT = false
    }

    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var database: com.trackit.app.data.local.TrackItDatabase
    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var preferencesManager: com.trackit.app.data.local.PreferencesManager
    @Inject lateinit var syncManager: com.trackit.app.util.SyncManager
    @Inject lateinit var weddingExpenseRepository: WeddingExpenseRepository
    @Inject lateinit var authRepository: AuthRepository

    private var isAuthenticated by mutableStateOf(false)
    private var isBiometricAvailable by mutableStateOf(false)
    private var isGoingToSystemSettings = false
    private var isSafeToAutoBackup = false

    private var pendingExportProfileId: String? = null
    private var pendingExportProfileName: String? = null

    private val createWeddingPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { handleWeddingExportUri(it, isPdf = true) }
    }

    private val createWeddingCsvLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { handleWeddingExportUri(it, isPdf = false) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        if (intent.getBooleanExtra("START_VOICE_IMMEDIATELY", false)) {
            isAuthenticated = true
        }

        // FR 3.2 - Privacy Screen: Hide content in Recent Apps
        if (ENABLE_ANTI_SCREENSHOT) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        // Seed default categories on first launch
        seedCategories()

        // Schedule periodic workers
        scheduleWorkers()

        // For already-logged-in users: start sync eagerly so DashboardViewModel's
        // initial isSyncing=true is read before any composable renders.
        // startSync() is idempotent and safely no-ops if not in online mode.
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
            lifecycleScope.launch {
                val level = preferencesManager.accessLevel.first()
                if (level != AccessLevel.NONE && level != AccessLevel.ADMIN) {
                    syncManager.startSync()
                }
            }
        }

        // Schedule daily reminder if enabled
        lifecycleScope.launch {
            val isEnabled = preferencesManager.isDailyReminderEnabled.first()
            if (isEnabled) {
                val time = preferencesManager.dailyReminderTime.first()
                com.trackit.app.worker.ReminderScheduler.scheduleReminder(this@MainActivity, time)
            }
        }

        lifecycleScope.launch {
            val accessLevel = preferencesManager.accessLevel.first()
            val activeProfileId = preferencesManager.activeProfileId.first()
            val activeProfile = profileRepository.getProfileById(activeProfileId)
            val allProfiles = profileRepository.getAllProfiles().first()

            // Auto-correct active profile if it violates license
            if (accessLevel == AccessLevel.WEDDING && activeProfile?.mode == "EXPENSE") {
                val weddingProfile = allProfiles.firstOrNull { it.mode == "WEDDING" }
                if (weddingProfile != null) {
                    preferencesManager.setActiveProfileId(weddingProfile.id)
                }
            } else if (accessLevel == AccessLevel.EXPENSE && activeProfile?.mode == "WEDDING") {
                val expenseProfile = allProfiles.firstOrNull { it.mode == "EXPENSE" }
                if (expenseProfile != null) {
                    preferencesManager.setActiveProfileId(expenseProfile.id)
                }
            }

            isSafeToAutoBackup = true
        }

        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = com.trackit.app.data.local.ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                com.trackit.app.data.local.ThemeMode.SYSTEM -> isSystemInDarkTheme()
                com.trackit.app.data.local.ThemeMode.LIGHT -> false
                com.trackit.app.data.local.ThemeMode.DARK -> true
            }
            
            TrackItTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val updateViewModel: com.trackit.app.updater.UpdateViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    val updateUiState by updateViewModel.uiState.collectAsState()

                    LaunchedEffect(Unit) {
                        updateViewModel.checkForUpdate(silent = true)
                    }

                    if (updateUiState.updateInfo != null && updateUiState.updateInfo!!.isUpdateAvailable) {
                        com.trackit.app.ui.components.UpdateDialog(
                            updateInfo = updateUiState.updateInfo!!,
                            downloadState = updateUiState.downloadState,
                            onStartDownload = { updateViewModel.startDownload() },
                            onInstall = { updateViewModel.installUpdate(it) },
                            onDismiss = { updateViewModel.dismissUpdateDialog() },
                            onResetDownload = { updateViewModel.resetDownloadState() }
                        )
                    }

                    var biometricError by remember { mutableStateOf<String?>(null) }
                    isBiometricAvailable = remember { checkBiometricAvailability() }
                    
                    val bypassBiometric by preferencesManager.bypassBiometricOnce.collectAsState(initial = false)
                    val isBiometricEnabled by preferencesManager.isBiometricEnabled.collectAsState(initial = false)
                    val hasSeenWelcome by preferencesManager.hasSeenWelcome.collectAsState(initial = false)

                    LaunchedEffect(bypassBiometric) {
                        if (bypassBiometric) {
                            isAuthenticated = true
                            preferencesManager.setBypassBiometricOnce(false)
                        }
                    }

                    val requiresBiometric = isBiometricAvailable && isBiometricEnabled && !bypassBiometric && !isAuthenticated

                    if (!requiresBiometric) {
                        val startVoice = intent.getBooleanExtra("START_VOICE_IMMEDIATELY", false)
                        val navController = rememberNavController()

                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        val isUserLoggedIn = currentUser != null
                        val hasSeenWelcomeFinal = hasSeenWelcome
                        val isAdminEmail = currentUser?.email.equals(com.trackit.app.data.repository.ADMIN_EMAIL, ignoreCase = true)

                        // For non-logged-in users, skip Firestore fetch entirely
                        // For logged-in users, we fetch latest access level
                        var fetchedAccessLevel by remember { mutableStateOf<String?>(if (!isUserLoggedIn) "" else null) }

                        // Fetch latest access level from Firestore on launch (for logged-in users)
                        LaunchedEffect(currentUser?.uid) {
                            if (currentUser != null) {
                                val level = if (isAdminEmail) {
                                    AccessLevel.ADMIN
                                } else {
                                    try {
                                        authRepository.fetchOrCreateUserDoc(currentUser)
                                    } catch (e: Exception) {
                                        // Network error: fall back to locally cached value
                                        android.util.Log.w("MainActivity", "fetchOrCreateUserDoc failed, using cache: ${e.message}")
                                        preferencesManager.accessLevel.first()
                                    }
                                }
                                preferencesManager.setAccessLevel(level)
                                // Start background sync for non-admin content users
                                if (level != AccessLevel.NONE && level != AccessLevel.ADMIN) {
                                    syncManager.startSync()
                                }
                                fetchedAccessLevel = level
                            }
                        }

                        // Show loading spinner while we wait for Firestore response for logged-in user
                        if (isUserLoggedIn && fetchedAccessLevel == null) {
                            androidx.compose.foundation.layout.Box(
                                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                            return@Surface
                        }

                        val effectiveAccessLevel = if (isAdminEmail) AccessLevel.ADMIN else (fetchedAccessLevel ?: "")

                        val isExpenseAccess = effectiveAccessLevel in listOf(
                            AccessLevel.EXPENSE,
                            AccessLevel.BOTH,
                            AccessLevel.ADMIN
                        )

                        val startDest = when {
                            !isUserLoggedIn -> {
                                if (!hasSeenWelcomeFinal) Screen.Welcome.route else Screen.Login.route
                            }
                            effectiveAccessLevel == AccessLevel.ADMIN -> Screen.AdminDashboard.route
                            effectiveAccessLevel == AccessLevel.NONE -> Screen.PendingVerification.route
                            startVoice && isExpenseAccess -> Screen.AddTransaction.createRoute(startVoice = true)
                            effectiveAccessLevel == AccessLevel.EXPENSE -> Screen.Dashboard.route
                            effectiveAccessLevel == AccessLevel.WEDDING -> Screen.Dashboard.route
                            effectiveAccessLevel == AccessLevel.BOTH -> Screen.ModuleSelection.route
                            else -> Screen.Dashboard.route
                        }

                        android.util.Log.d("MainActivity", "startDest=$startDest effectiveLevel=$effectiveAccessLevel isLoggedIn=$isUserLoggedIn")

                        TrackItNavHost(
                            navController = navController,
                            startDestination = startDest,
                            authRepository = authRepository,
                            preferencesManager = preferencesManager,
                            onExportPdf = { title, startDate, endDate, typeFilter ->
                                exportPdf(title, startDate, endDate, typeFilter)
                            },
                            onExportCsv = { title, startDate, endDate, typeFilter ->
                                exportCsv(title, startDate, endDate, typeFilter)
                            },
                            onExportWeddingPdf = { profileId, profileName ->
                                exportWeddingPdf(profileId, profileName)
                            },
                            onExportWeddingCsv = { profileId, profileName ->
                                exportWeddingCsv(profileId, profileName)
                            }
                        )
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
                            if (!bypassBiometric) {
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

        // Start cloud sync listener if online mode is enabled and user has valid license
        lifecycleScope.launch {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val accessLevel = preferencesManager.accessLevel.first()
            if (user != null && accessLevel != AccessLevel.NONE && accessLevel.isNotEmpty()) {
                syncManager.startSync()
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autentikasi Diperlukan")
            .setSubtitle("Gunakan sidik jari atau PIN/Sandi untuk masuk")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun seedCategories() {
        // No hardcoded default profile - profiles start at 0 until created by user
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

    private fun exportPdf(
        title: String = "Laporan Keuangan",
        startDate: Long = DateUtils.getStartOfMonth(),
        endDate: Long = DateUtils.getEndOfMonth(),
        typeFilter: String = "ALL"
    ) {
        lifecycleScope.launch {
            val activeProfileId = preferencesManager.activeProfileId.first()
            val isExpenseOnly = preferencesManager.isExpenseOnlyMode.first()
            val transactions = transactionRepository
                .getTransactionsByDateRange(startDate, endDate, activeProfileId)
            val categories = categoryRepository.getAllCategories(activeProfileId).first()
            val categoryMap = categories.associateBy { it.id }

            PdfExporter.exportReport(
                context = this@MainActivity,
                transactions = transactions,
                categories = categoryMap,
                title = title,
                startDate = startDate,
                endDate = endDate,
                typeFilter = typeFilter,
                showIncomeColumn = !isExpenseOnly
            )
        }
    }

    private fun exportWeddingPdf(weddingProfileId: String, profileName: String) {
        pendingExportProfileId = weddingProfileId
        pendingExportProfileName = profileName
        val safeName = profileName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val fileName = "Wedding_${safeName}_${java.text.SimpleDateFormat("ddMMyyyy", java.util.Locale.US).format(java.util.Date())}.pdf"
        createWeddingPdfLauncher.launch(fileName)
    }

    private fun exportWeddingCsv(weddingProfileId: String, profileName: String) {
        pendingExportProfileId = weddingProfileId
        pendingExportProfileName = profileName
        val safeName = profileName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val fileName = "Wedding_${safeName}_${java.text.SimpleDateFormat("ddMMyyyy", java.util.Locale.US).format(java.util.Date())}.csv"
        createWeddingCsvLauncher.launch(fileName)
    }

    private fun handleWeddingExportUri(uri: android.net.Uri, isPdf: Boolean) {
        val profileId = pendingExportProfileId ?: return
        val profileName = pendingExportProfileName ?: return
        lifecycleScope.launch {
            try {
                val expenses = weddingExpenseRepository.getAllByProfile(profileId).first()
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    if (isPdf) {
                        WeddingPdfExporter.writeToStream(expenses, profileName, outputStream)
                    } else {
                        WeddingCsvExporter.writeToStream(expenses, profileName, outputStream)
                    }
                }
                android.widget.Toast.makeText(this@MainActivity, "Berhasil menyimpan laporan", android.widget.Toast.LENGTH_SHORT).show()
                
                // Optionally open the file
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, if (isPdf) "application/pdf" else "text/csv")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(android.content.Intent.createChooser(intent, "Buka Laporan"))
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@MainActivity, "Gagal menyimpan: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun exportCsv(
        title: String = "Laporan Keuangan",
        startDate: Long = DateUtils.getStartOfMonth(),
        endDate: Long = DateUtils.getEndOfMonth(),
        typeFilter: String = "ALL"
    ) {
        lifecycleScope.launch {
            val activeProfileId = preferencesManager.activeProfileId.first()
            val isExpenseOnly = preferencesManager.isExpenseOnlyMode.first()
            val transactions = transactionRepository
                .getTransactionsByDateRange(startDate, endDate, activeProfileId)
            val categories = categoryRepository.getAllCategories(activeProfileId).first()
            val categoryMap = categories.associateBy { it.id }

            CsvExporter.exportReport(
                context = this@MainActivity,
                transactions = transactions,
                categories = categoryMap,
                title = title,
                startDate = startDate,
                endDate = endDate,
                typeFilter = typeFilter,
                showIncomeColumn = !isExpenseOnly
            )
        }
    }

    override fun onResume() {
        super.onResume()
        isGoingToSystemSettings = false
        
        lifecycleScope.launch {
            val pendingRestore = preferencesManager.pendingRestore.first()
            if (pendingRestore && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                preferencesManager.setPendingRestore(false)
                preferencesManager.setBypassBiometricOnce(true)
                
                BackupManager.isRestoring = true
                BackupManager.restoreFromAutoBackup(this@MainActivity)
                
                val pm = packageManager
                val restartIntent = pm.getLaunchIntentForPackage(packageName)
                val mainIntent = android.content.Intent.makeRestartActivityTask(restartIntent!!.component)
                startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Only perform auto-backup when database is populated or safe
        if (isSafeToAutoBackup && !BackupManager.isRestoring) {
            BackupManager.autoBackup(this)
        }
        
        // Lock the app when it goes to background, unless going to settings
        if (isBiometricAvailable && !isGoingToSystemSettings) {
            isAuthenticated = false
        }
    }
}
