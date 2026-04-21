package com.trackit.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    @ColumnInfo(defaultValue = "")
    val customKeywords: String = "", // Comma-separated custom keywords for voice matching
    @ColumnInfo(defaultValue = "EXPENSE")
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    @ColumnInfo(defaultValue = "0")
    val isHidden: Boolean = false, // Fitur Visibilitas
    @ColumnInfo(defaultValue = "1")
    val profileId: Long = 1
)
