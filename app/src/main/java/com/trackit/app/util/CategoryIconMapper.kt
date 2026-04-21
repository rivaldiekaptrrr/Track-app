package com.trackit.app.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconMapper {
    fun getIcon(iconName: String): ImageVector {
        return when (iconName) {
            // Food & Drink
            "restaurant" -> Icons.Default.Restaurant
            "fastfood" -> Icons.Default.Fastfood
            "local_pizza" -> Icons.Default.LocalPizza
            "coffee" -> Icons.Default.Coffee
            "local_drink" -> Icons.Default.LocalDrink
            "icecream" -> Icons.Default.Icecream
            "bakery_dining" -> Icons.Default.BakeryDining
            "wine_bar" -> Icons.Default.WineBar
            
            // Transport
            "directions_car" -> Icons.Default.DirectionsCar
            "directions_bike" -> Icons.Default.DirectionsBike
            "directions_bus" -> Icons.Default.DirectionsBus
            "directions_subway" -> Icons.Default.DirectionsSubway
            "local_taxi" -> Icons.Default.LocalTaxi
            "flight" -> Icons.Default.Flight
            "train" -> Icons.Default.Train
            "electric_car" -> Icons.Default.ElectricCar
            
            // Bills & Finance
            "receipt_long", "receipt" -> Icons.Default.Receipt
            "payments" -> Icons.Default.Payments
            "account_balance" -> Icons.Default.AccountBalance
            "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
            "credit_card" -> Icons.Default.CreditCard
            "savings" -> Icons.Default.Savings
            "monetization_on" -> Icons.Default.MonetizationOn
            "price_check" -> Icons.Default.PriceCheck
            "atm" -> Icons.Default.Atm
            
            // Shopping
            "shopping_bag" -> Icons.Default.ShoppingBag
            "shopping_cart" -> Icons.Default.ShoppingCart
            "storefront" -> Icons.Default.Storefront
            "checkroom" -> Icons.Default.Checkroom
            "card_giftcard" -> Icons.Default.CardGiftcard
            "sell" -> Icons.Default.Sell
            
            // Entertainment
            "movie" -> Icons.Default.Movie
            "music_note" -> Icons.Default.MusicNote
            "sports_esports" -> Icons.Default.SportsEsports
            "games" -> Icons.Default.Games
            "celebration" -> Icons.Default.Celebration
            "theater_comedy" -> Icons.Default.TheaterComedy
            "tv" -> Icons.Default.Tv
            "casino" -> Icons.Default.Casino
            
            // Health
            "local_hospital" -> Icons.Default.LocalHospital
            "healing" -> Icons.Default.Healing
            "vaccines" -> Icons.Default.Vaccines
            "medical_information" -> Icons.Default.MedicalInformation
            "psychology" -> Icons.Default.Psychology
            
            // Education
            "school" -> Icons.Default.School
            "library_books" -> Icons.Default.LibraryBooks
            "menu_book" -> Icons.Default.MenuBook
            "auto_stories" -> Icons.Default.AutoStories
            
            // Home & Utilities
            "home" -> Icons.Default.Home
            "kitchen" -> Icons.Default.Kitchen
            "chair" -> Icons.Default.Chair
            "bed" -> Icons.Default.Bed
            "lightbulb" -> Icons.Default.Lightbulb
            "water_drop" -> Icons.Default.WaterDrop
            "bolt" -> Icons.Default.Bolt
            "construction" -> Icons.Default.Construction
            
            // Lifestyle & Work
            "pets" -> Icons.Default.Pets
            "fitness_center" -> Icons.Default.FitnessCenter
            "child_care" -> Icons.Default.ChildCare
            "spa" -> Icons.Default.Spa
            "brush" -> Icons.Default.Brush
            "camera_alt" -> Icons.Default.CameraAlt
            "phone_iphone" -> Icons.Default.PhoneIphone
            "work" -> Icons.Default.Work
            "laptop_mac" -> Icons.Default.LaptopMac
            "computer" -> Icons.Default.Computer
            "print" -> Icons.Default.Print
            
            // Social & Others
            "groups" -> Icons.Default.Groups
            "volunteer_activism" -> Icons.Default.VolunteerActivism
            "public" -> Icons.Default.Public
            "handshake" -> Icons.Default.Handshake
            "favorite" -> Icons.Default.Favorite
            "stars" -> Icons.Default.Stars
            "more_horiz" -> Icons.Default.MoreHoriz
            "add_circle" -> Icons.Default.AddCircle
            "build" -> Icons.Default.Build
            
            else -> Icons.Default.Category
        }
    }

    /**
     * Returns a list of all supported icon names for selection.
     */
    fun getAllIcons(): List<String> {
        return listOf(
            "restaurant", "fastfood", "local_pizza", "coffee", "local_drink", "icecream", "bakery_dining", "wine_bar",
            "directions_car", "directions_bike", "directions_bus", "directions_subway", "local_taxi", "flight", "train", "electric_car",
            "receipt", "payments", "account_balance", "account_balance_wallet", "credit_card", "savings", "monetization_on", "price_check", "atm",
            "shopping_bag", "shopping_cart", "storefront", "checkroom", "card_giftcard", "sell",
            "movie", "music_note", "sports_esports", "games", "celebration", "theater_comedy", "tv", "casino",
            "local_hospital", "healing", "vaccines", "medical_information", "psychology",
            "school", "library_books", "menu_book", "auto_stories",
            "home", "kitchen", "chair", "bed", "lightbulb", "water_drop", "bolt", "construction",
            "pets", "fitness_center", "child_care", "spa", "brush", "camera_alt", "phone_iphone", "work", "laptop_mac", "computer", "print",
            "groups", "volunteer_activism", "public", "handshake", "favorite", "stars", "more_horiz", "add_circle", "build"
        )
    }

    fun parseColor(colorHex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color.Gray
        }
    }
}
