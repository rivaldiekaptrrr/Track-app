package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "wedding_documents",
    foreignKeys = [ForeignKey(
        entity = WeddingProfileEntity::class,
        parentColumns = ["id"], childColumns = ["weddingProfileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("weddingProfileId")]
)
data class WeddingDocumentEntity(
    @PrimaryKey val docId: String = UUID.randomUUID().toString(),
    val weddingProfileId: String,
    val docName: String,
    val ownerType: String = "BOTH", // GROOM, BRIDE, TOGETHER
    val isCompleted: Boolean = false,
    val localFilePath: String? = null, // SAF Uri
    val adminCost: Double = 0.0,
    val sortOrder: Int = 0
)
