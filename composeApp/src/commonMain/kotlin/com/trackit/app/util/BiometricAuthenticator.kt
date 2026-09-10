package com.trackit.app.util

expect class BiometricAuthenticator {
    fun isBiometricAvailable(): Boolean
    fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
}
