package com.trackit.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trackit.app.data.local.dao.BudgetSettingDao
import com.trackit.app.data.local.dao.CategoryDao
import com.trackit.app.data.local.dao.TransactionDao
import com.trackit.app.data.local.dao.WeddingDocumentDao
import com.trackit.app.data.local.dao.WeddingExpenseDao
import com.trackit.app.data.local.dao.WeddingGuestDao
import com.trackit.app.data.local.dao.WeddingPaymentTermDao
import com.trackit.app.data.local.dao.WeddingProfileDao
import com.trackit.app.data.local.dao.WeddingTaskDao
import com.trackit.app.data.local.entity.BudgetSettingEntity
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.local.entity.WeddingDocumentEntity
import com.trackit.app.data.local.entity.WeddingExpenseEntity
import com.trackit.app.data.local.entity.WeddingGuestEntity
import com.trackit.app.data.local.entity.WeddingPaymentTermEntity
import com.trackit.app.data.local.entity.WeddingProfileEntity
import com.trackit.app.data.local.entity.WeddingTaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetSettingEntity::class,
        com.trackit.app.data.local.entity.ProfileEntity::class,
        com.trackit.app.data.local.entity.CategoryBudgetEntity::class,
        WeddingProfileEntity::class,
        WeddingTaskEntity::class,
        WeddingDocumentEntity::class,
        WeddingExpenseEntity::class,
        WeddingPaymentTermEntity::class,
        WeddingGuestEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class TrackItDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetSettingDao(): BudgetSettingDao
    abstract fun profileDao(): com.trackit.app.data.local.dao.ProfileDao
    abstract fun categoryBudgetDao(): com.trackit.app.data.local.dao.CategoryBudgetDao
    abstract fun weddingProfileDao(): WeddingProfileDao
    abstract fun weddingTaskDao(): WeddingTaskDao
    abstract fun weddingDocumentDao(): WeddingDocumentDao
    abstract fun weddingExpenseDao(): WeddingExpenseDao
    abstract fun weddingPaymentTermDao(): WeddingPaymentTermDao
    abstract fun weddingGuestDao(): WeddingGuestDao

    companion object {
        /**
         * Migration from version 1 to 2: Add customKeywords column to categories table.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN customKeywords TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN type TEXT NOT NULL DEFAULT 'EXPENSE'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN type TEXT NOT NULL DEFAULT 'EXPENSE'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create profiles table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `profiles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `iconName` TEXT NOT NULL, 
                        `colorHex` TEXT NOT NULL, 
                        `createdAt` INTEGER NOT NULL
                    )
                """)
                
                // Add default profile
                val time = System.currentTimeMillis()
                db.execSQL("INSERT INTO profiles (id, name, iconName, colorHex, createdAt) VALUES (1, 'Pribadi', 'person', '#1565C0', $time)")

                // Add profileId to existing tables and map existing data to Profile 1
                db.execSQL("ALTER TABLE categories ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE transactions ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
                
                // Recreate budget_settings table to change primary key from id to profileId
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `budget_settings_new` (
                        `profileId` INTEGER PRIMARY KEY NOT NULL, 
                        `monthlyBudget` REAL NOT NULL
                    )
                """)
                db.execSQL("INSERT INTO budget_settings_new (profileId, monthlyBudget) SELECT 1, monthlyBudget FROM budget_settings")
                db.execSQL("DROP TABLE budget_settings")
                db.execSQL("ALTER TABLE budget_settings_new RENAME TO budget_settings")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `category_budgets` (
                        `categoryId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `alertPercentage` REAL NOT NULL DEFAULT 0.9,
                        `lastWarningMonth` TEXT NOT NULL DEFAULT '',
                        `profileId` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`categoryId`),
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add mode & weddingProfileId columns to profiles table
                db.execSQL("ALTER TABLE profiles ADD COLUMN mode TEXT NOT NULL DEFAULT 'EXPENSE'")
                db.execSQL("ALTER TABLE profiles ADD COLUMN weddingProfileId TEXT")

                // Create wedding_profiles table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wedding_profiles` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `groomName` TEXT NOT NULL,
                        `brideName` TEXT NOT NULL,
                        `weddingDate` INTEGER NOT NULL,
                        `totalBudgetCap` REAL NOT NULL DEFAULT 0,
                        `religionType` TEXT NOT NULL DEFAULT 'ISLAM',
                        `religionDetail` TEXT,
                        `culturalPresetGroom` TEXT,
                        `culturalPresetBride` TEXT,
                        `createdAt` INTEGER NOT NULL
                    )
                """)

                // Create wedding_tasks table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wedding_tasks` (
                        `taskId` TEXT NOT NULL PRIMARY KEY,
                        `weddingProfileId` TEXT NOT NULL,
                        `phaseMonth` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `pic` TEXT NOT NULL DEFAULT 'BOTH',
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `dueDate` INTEGER,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`weddingProfileId`) REFERENCES `wedding_profiles`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wedding_tasks_weddingProfileId` ON `wedding_tasks` (`weddingProfileId`)")

                // Create wedding_documents table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wedding_documents` (
                        `docId` TEXT NOT NULL PRIMARY KEY,
                        `weddingProfileId` TEXT NOT NULL,
                        `docName` TEXT NOT NULL,
                        `ownerType` TEXT NOT NULL DEFAULT 'BOTH',
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `localFilePath` TEXT,
                        `adminCost` REAL NOT NULL DEFAULT 0,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`weddingProfileId`) REFERENCES `wedding_profiles`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wedding_documents_weddingProfileId` ON `wedding_documents` (`weddingProfileId`)")

                // Create wedding_expenses table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wedding_expenses` (
                        `expenseId` TEXT NOT NULL PRIMARY KEY,
                        `weddingProfileId` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `totalEstimated` REAL NOT NULL,
                        `totalPaid` REAL NOT NULL DEFAULT 0,
                        `paidBySource` TEXT NOT NULL DEFAULT 'BERSAMA',
                        `paymentStatus` TEXT NOT NULL DEFAULT 'UNPAID',
                        `notes` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`weddingProfileId`) REFERENCES `wedding_profiles`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wedding_expenses_weddingProfileId` ON `wedding_expenses` (`weddingProfileId`)")

                // Create wedding_payment_terms table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wedding_payment_terms` (
                        `termId` TEXT NOT NULL PRIMARY KEY,
                        `expenseId` TEXT NOT NULL,
                        `termName` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `dueDate` INTEGER NOT NULL,
                        `isPaid` INTEGER NOT NULL DEFAULT 0,
                        `paidDate` INTEGER,
                        FOREIGN KEY(`expenseId`) REFERENCES `wedding_expenses`(`expenseId`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wedding_payment_terms_expenseId` ON `wedding_payment_terms` (`expenseId`)")

                // Create wedding_guests table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wedding_guests` (
                        `guestId` TEXT NOT NULL PRIMARY KEY,
                        `weddingProfileId` TEXT NOT NULL,
                        `guestName` TEXT NOT NULL,
                        `phoneNumber` TEXT,
                        `groupAllocation` TEXT NOT NULL DEFAULT 'TEMAN_CPP',
                        `sessionTarget` TEXT NOT NULL DEFAULT 'KEDUANYA',
                        `estimatedPax` INTEGER NOT NULL DEFAULT 2,
                        `rsvpStatus` TEXT NOT NULL DEFAULT 'PENDING',
                        FOREIGN KEY(`weddingProfileId`) REFERENCES `wedding_profiles`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wedding_guests_weddingProfileId` ON `wedding_guests` (`weddingProfileId`)")
            }
        }

        fun getDefaultCategories(): List<CategoryEntity> = listOf(
            // Expense Categories
            CategoryEntity(name = "Makanan", iconName = "restaurant", colorHex = "#E8963B", type = "EXPENSE"),
            CategoryEntity(name = "Transportasi", iconName = "directions_car", colorHex = "#3D6373", type = "EXPENSE"),
            CategoryEntity(name = "Hiburan", iconName = "movie", colorHex = "#C24D6E", type = "EXPENSE"),
            CategoryEntity(name = "Tagihan", iconName = "receipt_long", colorHex = "#7B61D9", type = "EXPENSE"),
            CategoryEntity(name = "Belanja", iconName = "shopping_bag", colorHex = "#1B6B4F", type = "EXPENSE"),
            CategoryEntity(name = "Kesehatan", iconName = "local_hospital", colorHex = "#4EADAD", type = "EXPENSE"),
            CategoryEntity(name = "Pendidikan", iconName = "school", colorHex = "#D4A843", type = "EXPENSE"),
            CategoryEntity(name = "Lainnya", iconName = "more_horiz", colorHex = "#8B6BB5", type = "EXPENSE"),
            
            // Income Categories
            CategoryEntity(name = "Gaji", iconName = "payments", colorHex = "#2E7D32", type = "INCOME"),
            CategoryEntity(name = "Bonus", iconName = "card_giftcard", colorHex = "#F57F17", type = "INCOME"),
            CategoryEntity(name = "Investasi", iconName = "trending_up", colorHex = "#1565C0", type = "INCOME"),
            CategoryEntity(name = "Lainnya Masuk", iconName = "add_circle", colorHex = "#00838F", type = "INCOME")
        )
    }
}
