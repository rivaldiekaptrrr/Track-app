package com.trackit.app.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object AddTransaction : Screen("add_transaction?startVoice={startVoice}") {
        fun createRoute(startVoice: Boolean = false): String {
            return "add_transaction?startVoice=$startVoice"
        }
    }
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long): String {
            return "edit_transaction/$transactionId"
        }
    }
    data object Chart : Screen("chart")
    data object Settings : Screen("settings")
    data object CustomKeywords : Screen("custom_keywords")
}
