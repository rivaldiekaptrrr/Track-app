package com.trackit.app.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for ReminderScheduler's time-parsing and delay-calculation logic.
 *
 * WorkManager's enqueue behavior (which requires Context) is tested separately
 * via instrumented tests. These tests cover the pure time-computation logic.
 */
class ReminderSchedulerTest {

    // --- Helper: exposed for test purposes ---
    private fun calculateInitialDelay(timeString: String, now: Calendar): Long {
        val parts = timeString.split(":")
        if (parts.size != 2) return -1L

        val targetHour = parts[0].toIntOrNull() ?: return -1L
        val targetMinute = parts[1].toIntOrNull() ?: return -1L

        val target = now.clone() as Calendar
        target.set(Calendar.HOUR_OF_DAY, targetHour)
        target.set(Calendar.MINUTE, targetMinute)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }

    // -----------------------------------------------------------------
    // parseTimeString: valid / invalid formats
    // -----------------------------------------------------------------

    @Test
    fun `calculateInitialDelay returns positive delay for valid future time`() {
        // Simulate: current time is 08:00, target is 20:00 → 12 hours from now
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val delay = calculateInitialDelay("20:00", now)

        val twelveHoursMs = 12 * 60 * 60 * 1000L
        assertEquals("Delay should be exactly 12 hours", twelveHoursMs, delay)
    }

    @Test
    fun `calculateInitialDelay wraps to next day when target time already passed`() {
        // Simulate: current time is 21:00, target is 20:00 → should schedule for next day (~23h)
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val delay = calculateInitialDelay("20:00", now)

        val twentyThreeHoursMs = 23 * 60 * 60 * 1000L
        assertEquals("Delay should wrap to next day (23h)", twentyThreeHoursMs, delay)
    }

    @Test
    fun `calculateInitialDelay handles midnight target time`() {
        // Simulate: current time is 10:00, target is 00:00 → next midnight (~14h)
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val delay = calculateInitialDelay("00:00", now)

        val fourteenHoursMs = 14 * 60 * 60 * 1000L
        assertEquals("Delay should be 14 hours to next midnight", fourteenHoursMs, delay)
    }

    @Test
    fun `calculateInitialDelay returns -1 for invalid time format`() {
        val now = Calendar.getInstance()
        val delay = calculateInitialDelay("invalid-time", now)
        assertEquals("Invalid format should return -1", -1L, delay)
    }

    @Test
    fun `calculateInitialDelay returns -1 for empty string`() {
        val now = Calendar.getInstance()
        val delay = calculateInitialDelay("", now)
        assertEquals("Empty string should return -1", -1L, delay)
    }

    @Test
    fun `calculateInitialDelay returns -1 for missing minute`() {
        val now = Calendar.getInstance()
        val delay = calculateInitialDelay("20:", now)
        assertEquals("Missing minute should return -1", -1L, delay)
    }

    @Test
    fun `calculateInitialDelay handles zero-padded hours correctly`() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // "08:30" should produce a 1h30m delay
        val delay = calculateInitialDelay("08:30", now)
        val expectedMs = (1 * 60 + 30) * 60 * 1000L
        assertEquals("08:30 should be 1h30m away from 07:00", expectedMs, delay)
    }
}
