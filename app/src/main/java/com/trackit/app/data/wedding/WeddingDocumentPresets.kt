package com.trackit.app.data.wedding

import com.trackit.app.data.local.entity.WeddingDocumentEntity
import java.util.UUID

/**
 * Preset berkas legalitas berdasarkan agama & alur KUA/Dukcapil.
 * Berstatus starting point yang fully editable oleh pengguna.
 */
object WeddingDocumentPresets {

    private fun universalDocs(weddingProfileId: String): List<WeddingDocumentEntity> = listOf(
        doc(weddingProfileId, "Akta Kelahiran", "BOTH", 0.0, 0),
        doc(weddingProfileId, "Kartu Keluarga (KK)", "BOTH", 0.0, 1),
        doc(weddingProfileId, "KTP / e-KTP", "BOTH", 0.0, 2),
        doc(weddingProfileId, "Pasfoto 3x4 latar biru/merah", "BOTH", 0.0, 3),
        doc(weddingProfileId, "Sertifikat ELSIMIL (BKKBN)", "BOTH", 0.0, 4),
        doc(weddingProfileId, "Surat Keterangan Sehat (Puskesmas)", "BOTH", 0.0, 5),
        doc(weddingProfileId, "Imunisasi Tetanus (TT)", "BRIDE", 0.0, 6),
        doc(weddingProfileId, "Surat Pengantar RT/RW", "BOTH", 0.0, 7),
    )

    private fun kuaDocs(weddingProfileId: String): List<WeddingDocumentEntity> = listOf(
        doc(weddingProfileId, "Formulir N1 — Ket. Nikah Kelurahan (CPP)", "GROOM", 30000.0, 10),
        doc(weddingProfileId, "Formulir N2 — Izin Ortu/Wali (CPP)", "GROOM", 0.0, 11),
        doc(weddingProfileId, "Formulir N4 — Ket. Asal-Usul (CPP)", "GROOM", 0.0, 12),
        doc(weddingProfileId, "Surat Rekomendasi KUA setempat (CPP)", "GROOM", 0.0, 13),
        doc(weddingProfileId, "Bukti Bayar PNBP Nikah Rp 600.000", "GROOM", 600000.0, 14),
        doc(weddingProfileId, "Formulir N1 — Ket. Nikah Kelurahan (CPW)", "BRIDE", 30000.0, 20),
        doc(weddingProfileId, "Formulir N2 — Izin Wali/Ortu (CPW)", "BRIDE", 0.0, 21),
        doc(weddingProfileId, "Formulir N4 — Ket. Asal-Usul (CPW)", "BRIDE", 0.0, 22),
        doc(weddingProfileId, "Surat Rekomendasi Numpang Nikah (jika beda KUA)", "BRIDE", 0.0, 23),
        doc(weddingProfileId, "Surat Ket. Belum Pernah Menikah", "BOTH", 0.0, 30),
    )

    private fun kristenDocs(weddingProfileId: String): List<WeddingDocumentEntity> = listOf(
        doc(weddingProfileId, "Surat Keterangan Baptis (CPP)", "GROOM", 0.0, 10),
        doc(weddingProfileId, "Surat Keterangan Baptis (CPW)", "BRIDE", 0.0, 11),
        doc(weddingProfileId, "Surat Pengantar Gereja / Pastor", "BOTH", 0.0, 12),
        doc(weddingProfileId, "Surat Numpang Nikah (jika beda jemaat/paroki)", "BOTH", 0.0, 13),
        doc(weddingProfileId, "N1 dari Kelurahan", "BOTH", 30000.0, 14),
        doc(weddingProfileId, "Bukti Bayar Pencatatan Perkawinan Dukcapil", "BOTH", 0.0, 15),
    )

    private fun hinduDocs(weddingProfileId: String): List<WeddingDocumentEntity> = listOf(
        doc(weddingProfileId, "Surat Ket. Pernikahan dari Pemangku / PHDI", "BOTH", 0.0, 10),
        doc(weddingProfileId, "Surat Widhi Widana (Bali)", "BOTH", 0.0, 11),
        doc(weddingProfileId, "N1 dari Kelurahan", "BOTH", 30000.0, 12),
        doc(weddingProfileId, "Bukti Bayar Pencatatan Perkawinan Dukcapil", "BOTH", 0.0, 13),
    )

    private fun buddhaDocs(weddingProfileId: String): List<WeddingDocumentEntity> = listOf(
        doc(weddingProfileId, "Surat Ket. dari Wihara / Majelis Buddha", "BOTH", 0.0, 10),
        doc(weddingProfileId, "N1 dari Kelurahan", "BOTH", 30000.0, 11),
        doc(weddingProfileId, "Bukti Bayar Pencatatan Perkawinan Dukcapil", "BOTH", 0.0, 12),
    )

    private fun konghucuDocs(weddingProfileId: String): List<WeddingDocumentEntity> = listOf(
        doc(weddingProfileId, "Surat Peneguhan Perkawinan dari Lithang / MATAKIN", "BOTH", 0.0, 10),
        doc(weddingProfileId, "N1 dari Kelurahan", "BOTH", 30000.0, 11),
        doc(weddingProfileId, "Bukti Bayar Pencatatan Perkawinan Dukcapil", "BOTH", 0.0, 12),
    )

    fun getPreset(
        weddingProfileId: String,
        religionType: String,
        religionDetail: String?
    ): List<WeddingDocumentEntity> {
        val universal = universalDocs(weddingProfileId)
        val specific = when {
            religionType == "ISLAM" -> kuaDocs(weddingProfileId)
            religionDetail == "KRISTEN" || religionDetail == "KATOLIK" -> kristenDocs(weddingProfileId)
            religionDetail == "HINDU" -> hinduDocs(weddingProfileId)
            religionDetail == "BUDDHA" -> buddhaDocs(weddingProfileId)
            religionDetail == "KONGHUCU" -> konghucuDocs(weddingProfileId)
            else -> kristenDocs(weddingProfileId)
        }
        return universal + specific
    }

    private fun doc(
        weddingProfileId: String,
        name: String,
        owner: String,
        cost: Double = 0.0,
        order: Int = 0
    ) = WeddingDocumentEntity(
        docId = UUID.randomUUID().toString(),
        weddingProfileId = weddingProfileId,
        docName = name,
        ownerType = owner,
        adminCost = cost,
        sortOrder = order
    )
}
