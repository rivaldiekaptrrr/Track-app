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
import com.trackit.app.data.local.entity.BudgetSettingEntity
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetSettingEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class TrackItDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetSettingDao(): BudgetSettingDao

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
