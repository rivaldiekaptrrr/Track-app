package com.trackit.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))

    fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

    fun formatMonthYear(millis: Long): String = monthYearFormat.format(Date(millis))

    fun getStartOfMonth(calendar: Calendar = Calendar.getInstance()): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfMonth(calendar: Calendar = Calendar.getInstance()): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun todayMillis(): Long = getStartOfDay()
}

object CurrencyUtils {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    fun formatRupiah(amount: Double): String {
        return currencyFormat.format(amount)
    }
}
