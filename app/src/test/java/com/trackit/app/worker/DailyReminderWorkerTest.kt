package com.trackit.app.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.DateUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for DailyReminderWorker's core business logic.
 *
 * Uses Robolectric for Android Context, MockK for dependency mocking.
 * Tests verify:
 *  1. Worker returns SUCCESS when reminder is disabled.
 *  2. Worker checks transaction count when enabled.
 *  3. Worker returns SUCCESS (whether or not it fires a notification).
 *  4. Worker returns RETRY on unexpected exception.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyReminderWorkerTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var transactionRepository: TransactionRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferencesManager = mockk()
        transactionRepository = mockk()
    }

    private fun buildWorker(): DailyReminderWorker {
        return TestListenableWorkerBuilder<DailyReminderWorker>(context)
            .setWorkerFactory(
                com.trackit.app.worker.DailyReminderWorkerFactory(
                    transactionRepository = transactionRepository,
                    preferencesManager = preferencesManager
                )
            )
            .build() as DailyReminderWorker
    }

    @Test
    fun `returns SUCCESS immediately when reminder is disabled`() = runTest {
        // GIVEN: feature is disabled
        coEvery { preferencesManager.isDailyReminderEnabled } returns flowOf(false)

        val worker = buildWorker()
        val result = worker.doWork()

        // THEN: short-circuit — no DB access
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { transactionRepository.countExpensesForDaySync(any(), any(), any()) }
    }

    @Test
    fun `returns SUCCESS when no expenses exist for today`() = runTest {
        // GIVEN: feature enabled, no expenses today
        coEvery { preferencesManager.isDailyReminderEnabled } returns flowOf(true)
        coEvery { preferencesManager.activeProfileId } returns flowOf(1L)
        coEvery {
            transactionRepository.countExpensesForDaySync(any(), any(), eq(1L))
        } returns 0

        val worker = buildWorker()
        val result = worker.doWork()

        // THEN: succeeds (notification should have been sent, but we test result here)
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `returns SUCCESS when expenses already exist for today — no notification needed`() = runTest {
        // GIVEN: feature enabled, user already logged expenses
        coEvery { preferencesManager.isDailyReminderEnabled } returns flowOf(true)
        coEvery { preferencesManager.activeProfileId } returns flowOf(1L)
        coEvery {
            transactionRepository.countExpensesForDaySync(any(), any(), eq(1L))
        } returns 3

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `returns RETRY on unexpected exception from repository`() = runTest {
        // GIVEN: repository throws
        coEvery { preferencesManager.isDailyReminderEnabled } returns flowOf(true)
        coEvery { preferencesManager.activeProfileId } returns flowOf(1L)
        coEvery {
            transactionRepository.countExpensesForDaySync(any(), any(), any())
        } throws RuntimeException("DB error")

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }
}
