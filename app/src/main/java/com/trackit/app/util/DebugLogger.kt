package com.trackit.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    enum class Level { INFO, SUCCESS, ERROR }

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun log(message: String, level: Level = Level.INFO) {
        val time = dateFormat.format(Date())
        val prefix = when (level) {
            Level.SUCCESS -> "[OK]"
            Level.ERROR   -> "[ERR]"
            Level.INFO    -> "[INF]"
        }
        val formattedMessage = "[$time] $prefix $message"
        _logs.update { currentLogs ->
            (listOf(formattedMessage) + currentLogs).take(100)
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
