package com.trackit.app.di

import android.content.Context
import androidx.room.Room
import com.trackit.app.data.local.TrackItDatabase
import com.trackit.app.data.local.dao.BudgetSettingDao
import com.trackit.app.data.local.dao.CategoryDao
import com.trackit.app.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TrackItDatabase {
        return Room.databaseBuilder(
            context,
            TrackItDatabase::class.java,
            "trackit_database"
        )
        .addMigrations(TrackItDatabase.MIGRATION_1_2, TrackItDatabase.MIGRATION_2_3, TrackItDatabase.MIGRATION_3_4, TrackItDatabase.MIGRATION_4_5)
        .build()
    }

    @Provides
    fun provideTransactionDao(database: TrackItDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideCategoryDao(database: TrackItDatabase): CategoryDao =
        database.categoryDao()

    @Provides
    fun provideBudgetSettingDao(database: TrackItDatabase): BudgetSettingDao =
        database.budgetSettingDao()

    @Provides
    fun provideProfileDao(database: TrackItDatabase): com.trackit.app.data.local.dao.ProfileDao =
        database.profileDao()
}
