package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "wedding_tasks",
    foreignKeys = [ForeignKey(
        entity = WeddingProfileEntity::class,
        parentColumns = ["id"], childColumns = ["weddingProfileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("weddingProfileId")]
)
data class WeddingTaskEntity(
    @PrimaryKey val taskId: String = UUID.randomUUID().toString(),
    val weddingProfileId: String,
    val phaseMonth: Int, // 12, 6, 3, 1, 0 (Hari-H)
    val title: String,
    val description: String? = null,
    val pic: String = "BOTH", // GROOM, BRIDE, BOTH, FAMILY, WO
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val sortOrder: Int = 0
)
