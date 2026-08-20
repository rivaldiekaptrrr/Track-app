package com.trackit.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun log(message: String) {
        val time = dateFormat.format(Date())
        val formattedMessage = "[$time] $message"
        _logs.update { currentLogs -> 
            // Keep only the last 100 logs to prevent memory issues
            (listOf(formattedMessage) + currentLogs).take(100)
        }
    }
    
    fun clear() {
        _logs.value = emptyList()
    }
}
