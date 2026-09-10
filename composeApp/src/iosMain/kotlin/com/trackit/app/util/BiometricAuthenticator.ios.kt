package com.trackit.app.util

import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics

actual class BiometricAuthenticator {
    actual fun isBiometricAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)
    }

    actual fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val context = LAContext()
        context.evaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = title
        ) { success, error ->
            if (success) {
                onSuccess()
            } else {
                onError(error?.localizedDescription ?: "Autentikasi Biometrik gagal")
            }
        }
    }
}
