package com.trackit.app.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object AddTransaction : Screen("add_transaction?ocrAmount={ocrAmount}") {
        fun createRoute(ocrAmount: Double? = null): String {
            return if (ocrAmount != null) {
                "add_transaction?ocrAmount=$ocrAmount"
            } else {
                "add_transaction"
            }
        }
    }
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long): String {
            return "edit_transaction/$transactionId"
        }
    }
    data object Chart : Screen("chart")
    data object Settings : Screen("settings")
    data object ScanReceipt : Screen("scan_receipt")
}
