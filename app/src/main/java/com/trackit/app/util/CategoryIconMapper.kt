package com.trackit.app.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconMapper {
    fun getIcon(iconName: String): ImageVector {
        return when (iconName) {
            "restaurant" -> Icons.Default.Restaurant
            "directions_car" -> Icons.Default.DirectionsCar
            "movie" -> Icons.Default.Movie
            "receipt_long" -> Icons.Default.Receipt
            "shopping_bag" -> Icons.Default.ShoppingBag
            "local_hospital" -> Icons.Default.LocalHospital
            "school" -> Icons.Default.School
            "more_horiz" -> Icons.Default.MoreHoriz
            else -> Icons.Default.Category
        }
    }

    fun parseColor(colorHex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color.Gray
        }
    }
}
