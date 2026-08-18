package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Satu "Tab Acara" dalam rangkaian pernikahan.
 * Contoh: Lamaran, Siraman, Akad Nikah, Resepsi, dll.
 * Sepenuhnya user-defined — tidak ada struktur tetap.
 */
@Entity(
    tableName = "wedding_events",
    foreignKeys = [ForeignKey(
        entity = WeddingProfileEntity::class,
        parentColumns = ["id"], childColumns = ["weddingProfileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("weddingProfileId")]
)
data class WeddingEventEntity(
    @PrimaryKey val eventId: String = UUID.randomUUID().toString(),
    val weddingProfileId: String,
    val eventName: String,          // Nama acara bebas (Akad Nikah, Resepsi, Sangjit, dll.)
    val eventDate: Long,            // Epoch timestamp tanggal acara
    val eventLocation: String? = null,
    val sortOrder: Int = 0
)

/**
 * Satu baris rundown (timetable) dalam sebuah WeddingEventEntity.
 * Menit-ke-menit: Waktu, Durasi, Sesi, PIC, Teks MC.
 */
@Entity(
    tableName = "wedding_rundown_items",
    foreignKeys = [ForeignKey(
        entity = WeddingEventEntity::class,
        parentColumns = ["eventId"], childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("eventId")]
)
data class WeddingRundownItemEntity(
    @PrimaryKey val itemId: String = UUID.randomUUID().toString(),
    val eventId: String,
    val timeStart: String,          // "08:00" — string HH:mm
    val durationMinutes: Int = 15,
    val sessionTitle: String,       // nama sesi kegiatan
    val pic: String? = null,        // CPP, CPW, MC, Keluarga, WO, dll.
    val mcScript: String? = null,   // teks panduan MC (opsional)
    val sortOrder: Int = 0
)
