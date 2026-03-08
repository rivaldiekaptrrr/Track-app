package com.trackit.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = false
)
abstract class TrackItDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetSettingDao(): BudgetSettingDao

    companion object {
        fun getDefaultCategories(): List<CategoryEntity> = listOf(
            CategoryEntity(name = "Makanan", iconName = "restaurant", colorHex = "#E8963B"),
            CategoryEntity(name = "Transportasi", iconName = "directions_car", colorHex = "#3D6373"),
            CategoryEntity(name = "Hiburan", iconName = "movie", colorHex = "#C24D6E"),
            CategoryEntity(name = "Tagihan", iconName = "receipt_long", colorHex = "#7B61D9"),
            CategoryEntity(name = "Belanja", iconName = "shopping_bag", colorHex = "#1B6B4F"),
            CategoryEntity(name = "Kesehatan", iconName = "local_hospital", colorHex = "#4EADAD"),
            CategoryEntity(name = "Pendidikan", iconName = "school", colorHex = "#D4A843"),
            CategoryEntity(name = "Lainnya", iconName = "more_horiz", colorHex = "#8B6BB5"),
        )
    }
}
