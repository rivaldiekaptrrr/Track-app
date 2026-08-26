package com.trackit.app.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Login : Screen("login")
    data object AddTransaction : Screen("add_transaction?startVoice={startVoice}") {
        fun createRoute(startVoice: Boolean = false): String {
            return "add_transaction?startVoice=$startVoice"
        }
    }
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: String): String {
            return "edit_transaction/$transactionId"
        }
    }
    data object Chart : Screen("chart")
    data object Settings : Screen("settings")
    data object CustomKeywords : Screen("custom_keywords")
    data object ProfileManagement : Screen("profile_management")
    data object CategoryBudget : Screen("category_budget")

    // Wedding Planner Screens
    data object WeddingDashboard : Screen("wedding_dashboard")
    data object WeddingTasks : Screen("wedding_tasks")
    data object WeddingDocuments : Screen("wedding_documents")
    data object WeddingBudget : Screen("wedding_budget")
    data object WeddingGuests : Screen("wedding_guests")
    data object WeddingVendors : Screen("wedding_vendors")
    data object WeddingSeserahan : Screen("wedding_seserahan")
    data object WeddingCommittee : Screen("wedding_committee")
    data object WeddingRundown : Screen("wedding_rundown")
    data object WeddingSettings : Screen("wedding_settings")
}

