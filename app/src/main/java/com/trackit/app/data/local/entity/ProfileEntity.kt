package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "person",
    val colorHex: String = "#1565C0",
    val createdAt: Long = System.currentTimeMillis()
)
