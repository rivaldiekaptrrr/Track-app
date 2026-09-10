package com.trackit.app.util

import java.util.UUID

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun generateUUID(): String = UUID.randomUUID().toString()
