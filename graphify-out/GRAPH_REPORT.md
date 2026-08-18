# Graph Report - Track-app  (2026-08-18)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 567 nodes · 1111 edges · 27 communities (24 shown, 3 thin omitted)
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS · INFERRED: 4 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e35f72ae`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- TransactionRepository
- DateUtils
- PreferencesManager
- Intent
- CategoryBudgetEntity
- CategoryEntity
- TrackItDatabase
- MainActivity.kt
- ProfileEntity
- TransactionViewModel
- TransparentVoiceActivity.kt
- AddEditTransactionScreen.kt
- GDriveBackupManager
- VoiceParser
- SettingsViewModel
- AppUpdateCheckerTest
- DashboardViewModel.kt
- UpdateViewModel
- ReminderSchedulerTest
- VoiceWidgetProvider.kt
- TrackItApp.kt
- NetworkModule.kt
- gradlew

## God Nodes (most connected - your core abstractions)
1. `CategoryEntity` - 49 edges
2. `TransactionRepository` - 39 edges
3. `TransactionEntity` - 38 edges
4. `TransactionViewModel` - 28 edges
5. `TransactionDao` - 27 edges
6. `PreferencesManager` - 25 edges
7. `ProfileEntity` - 23 edges
8. `CategoryRepository` - 21 edges
9. `DateUtils` - 19 edges
10. `VoiceParser` - 18 edges

## Surprising Connections (you probably didn't know these)
- `SettingsScreen()` --calls--> `ExportReportDialog()`  [INFERRED]
  app/src/main/java/com/trackit/app/ui/settings/SettingsScreen.kt → app/src/main/java/com/trackit/app/ui/settings/ExportReportDialog.kt
- `MainActivity` --references--> `TransactionRepository`  [EXTRACTED]
  app/src/main/java/com/trackit/app/MainActivity.kt → app/src/main/java/com/trackit/app/data/repository/TransactionRepository.kt
- `TransparentVoiceActivity` --references--> `TransactionRepository`  [EXTRACTED]
  app/src/main/java/com/trackit/app/ui/voice/TransparentVoiceActivity.kt → app/src/main/java/com/trackit/app/data/repository/TransactionRepository.kt
- `DailyReminderWorkerTest` --references--> `TransactionRepository`  [EXTRACTED]
  app/src/test/java/com/trackit/app/worker/DailyReminderWorkerTest.kt → app/src/main/java/com/trackit/app/data/repository/TransactionRepository.kt
- `TransparentVoiceActivity` --references--> `PreferencesManager`  [EXTRACTED]
  app/src/main/java/com/trackit/app/ui/voice/TransparentVoiceActivity.kt → app/src/main/java/com/trackit/app/data/local/PreferencesManager.kt

## Import Cycles
- None detected.

## Communities (27 total, 3 thin omitted)

### Community 0 - "TransactionRepository"
Cohesion: 0.06
Nodes (10): CategorySpending, Flow, TransactionDao, TransactionEntity, Flow, TransactionRepository, CsvExporter, Context (+2 more)

### Community 1 - "DateUtils"
Cohesion: 0.06
Nodes (29): BiometricLockScreen(), CategoryBudgetCard(), CategoryBudgetScreen(), CategoryBreakdownItem(), ChartScreen(), PieChart(), CategoryChartData, ChartUiState (+21 more)

### Community 2 - "PreferencesManager"
Cohesion: 0.07
Nodes (18): Flow, PreferencesManager, BudgetCheckWorker, CoroutineWorker, Result, DailyReminderWorker, CoroutineWorker, Result (+10 more)

### Community 3 - "Intent"
Cohesion: 0.09
Nodes (20): VoiceTileService, UpdateDialog(), ExportReportDialog(), RestoreGDriveDialog(), SettingsScreen(), AppUpdateDownloader, Done, Downloading (+12 more)

### Community 4 - "CategoryBudgetEntity"
Cohesion: 0.08
Nodes (11): CategoryBudgetDao, Flow, CategoryBudgetEntity, CategoryBudgetRepository, Flow, CategoryBudgetItem, CategoryBudgetUiState, CategoryBudgetViewModel (+3 more)

### Community 5 - "CategoryEntity"
Cohesion: 0.10
Nodes (10): CategoryDao, Flow, CategoryEntity, CategoryRepository, Flow, CategoryFormDialog(), CategoryManagementViewModel, CustomKeywordScreen() (+2 more)

### Community 6 - "TrackItDatabase"
Cohesion: 0.11
Nodes (11): BudgetSettingDao, Flow, BudgetSettingEntity, com, TrackItDatabase, BudgetRepository, Flow, DatabaseModule (+3 more)

### Community 7 - "MainActivity.kt"
Cohesion: 0.08
Nodes (15): Bundle, com, MainActivity, BiometricPrompt, AddTransaction, CategoryBudget, Chart, CustomKeywords (+7 more)

### Community 8 - "ProfileEntity"
Cohesion: 0.12
Nodes (11): Flow, ProfileDao, ProfileEntity, Flow, ProfileRepository, StateFlow, ViewModel, ProfileFormDialog() (+3 more)

### Community 9 - "TransactionViewModel"
Cohesion: 0.09
Nodes (5): com, StateFlow, ViewModel, TransactionFormState, TransactionViewModel

### Community 10 - "TransparentVoiceActivity.kt"
Cohesion: 0.21
Nodes (8): CategoryChip(), CategorySelectionBottomSheet(), Bundle, UtteranceProgressListener, TransparentVoiceActivity, UtteranceProgressListener, VoiceParseResult, ComponentActivity

### Community 11 - "AddEditTransactionScreen.kt"
Cohesion: 0.14
Nodes (11): AnnotatedString, AddEditTransactionScreen(), UtteranceProgressListener, CategoryChip(), UtteranceProgressListener, ThousandSeparatorVisualTransformation, OffsetMapping, NumberUtils (+3 more)

### Community 12 - "GDriveBackupManager"
Cohesion: 0.29
Nodes (6): DriveBackupFile, GDriveBackupManager, Context, Result, Drive, GoogleSignInAccount

### Community 14 - "SettingsViewModel"
Cohesion: 0.14
Nodes (4): StateFlow, ViewModel, SettingsUiState, SettingsViewModel

### Community 15 - "AppUpdateCheckerTest"
Cohesion: 0.22
Nodes (4): AppUpdateChecker, AppUpdateCheckerTest, OkHttpClient, Call

### Community 16 - "DashboardViewModel.kt"
Cohesion: 0.21
Nodes (5): DashboardUiState, DashboardViewModel, StateFlow, ViewModel, TransactionWithCategory

### Community 17 - "UpdateViewModel"
Cohesion: 0.27
Nodes (4): StateFlow, ViewModel, UpdateUiState, UpdateViewModel

### Community 19 - "VoiceWidgetProvider.kt"
Cohesion: 0.46
Nodes (5): Context, VoiceWidgetProvider, AppWidgetManager, AppWidgetProvider, IntArray

### Community 20 - "TrackItApp.kt"
Cohesion: 0.70
Nodes (4): TrackItApp, Application, Configuration, HiltWorkerFactory

### Community 22 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Knowledge Gaps
- **8 isolated node(s):** `BottomNavDestination`, `Idle`, `CategoryBudget`, `Chart`, `CustomKeywords` (+3 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `CategoryEntity` connect `CategoryEntity` to `TransactionRepository`, `DateUtils`, `CategoryBudgetEntity`, `TrackItDatabase`, `ProfileEntity`, `TransactionViewModel`, `TransparentVoiceActivity.kt`, `AddEditTransactionScreen.kt`, `VoiceParser`, `DashboardViewModel.kt`?**
  _High betweenness centrality (0.136) - this node is a cross-community bridge._
- **Why does `TransactionRepository` connect `TransactionRepository` to `DateUtils`, `PreferencesManager`, `CategoryBudgetEntity`, `CategoryEntity`, `MainActivity.kt`, `ProfileEntity`, `TransactionViewModel`, `TransparentVoiceActivity.kt`, `DashboardViewModel.kt`?**
  _High betweenness centrality (0.131) - this node is a cross-community bridge._
- **Why does `TransactionEntity` connect `TransactionRepository` to `PreferencesManager`, `TrackItDatabase`, `ProfileEntity`, `TransactionViewModel`, `TransparentVoiceActivity.kt`, `DashboardViewModel.kt`?**
  _High betweenness centrality (0.096) - this node is a cross-community bridge._
- **What connects `BottomNavDestination`, `Idle`, `CategoryBudget` to the rest of the system?**
  _8 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `TransactionRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.05888376856118792 - nodes in this community are weakly interconnected._
- **Should `DateUtils` be split into smaller, more focused modules?**
  _Cohesion score 0.061952861952861954 - nodes in this community are weakly interconnected._
- **Should `PreferencesManager` be split into smaller, more focused modules?**
  _Cohesion score 0.07198228128460686 - nodes in this community are weakly interconnected._