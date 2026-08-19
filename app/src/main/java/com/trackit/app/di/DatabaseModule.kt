package com.trackit.app.di

import android.content.Context
import androidx.room.Room
import com.trackit.app.data.local.TrackItDatabase
import com.trackit.app.data.local.dao.BudgetSettingDao
import com.trackit.app.data.local.dao.CategoryDao
import com.trackit.app.data.local.dao.TransactionDao
import com.trackit.app.data.local.dao.WeddingDocumentDao
import com.trackit.app.data.local.dao.WeddingExpenseDao
import com.trackit.app.data.local.dao.WeddingGuestDao
import com.trackit.app.data.local.dao.WeddingPaymentTermDao
import com.trackit.app.data.local.dao.WeddingProfileDao
import com.trackit.app.data.local.dao.WeddingTaskDao
import com.trackit.app.data.local.dao.WeddingVendorDao
import com.trackit.app.data.local.dao.WeddingSeserahanDao
import com.trackit.app.data.local.dao.WeddingCommitteeDao
import com.trackit.app.data.local.dao.WeddingEventDao
import com.trackit.app.data.local.dao.WeddingRundownItemDao
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
        .addMigrations(
            TrackItDatabase.MIGRATION_1_2,
            TrackItDatabase.MIGRATION_2_3,
            TrackItDatabase.MIGRATION_3_4,
            TrackItDatabase.MIGRATION_4_5,
            TrackItDatabase.MIGRATION_5_6,
            TrackItDatabase.MIGRATION_6_7,
            TrackItDatabase.MIGRATION_7_8,
            TrackItDatabase.MIGRATION_8_9,
            TrackItDatabase.MIGRATION_9_10
        )
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

    @Provides
    fun provideCategoryBudgetDao(database: TrackItDatabase): com.trackit.app.data.local.dao.CategoryBudgetDao =
        database.categoryBudgetDao()

    @Provides
    fun provideWeddingProfileDao(database: TrackItDatabase): WeddingProfileDao =
        database.weddingProfileDao()

    @Provides
    fun provideWeddingTaskDao(database: TrackItDatabase): WeddingTaskDao =
        database.weddingTaskDao()

    @Provides
    fun provideWeddingDocumentDao(database: TrackItDatabase): WeddingDocumentDao =
        database.weddingDocumentDao()

    @Provides
    fun provideWeddingExpenseDao(database: TrackItDatabase): WeddingExpenseDao =
        database.weddingExpenseDao()

    @Provides
    fun provideWeddingPaymentTermDao(database: TrackItDatabase): WeddingPaymentTermDao =
        database.weddingPaymentTermDao()

    @Provides
    fun provideWeddingGuestDao(database: TrackItDatabase): WeddingGuestDao =
        database.weddingGuestDao()

    @Provides
    fun provideWeddingVendorDao(database: TrackItDatabase): WeddingVendorDao =
        database.weddingVendorDao()

    @Provides
    fun provideWeddingSeserahanDao(database: TrackItDatabase): WeddingSeserahanDao =
        database.weddingSeserahanDao()

    @Provides
    fun provideWeddingCommitteeDao(database: TrackItDatabase): WeddingCommitteeDao =
        database.weddingCommitteeDao()

    @Provides
    fun provideWeddingEventDao(database: TrackItDatabase): WeddingEventDao =
        database.weddingEventDao()

    @Provides
    fun provideWeddingRundownItemDao(database: TrackItDatabase): WeddingRundownItemDao =
        database.weddingRundownItemDao()
}

