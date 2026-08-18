package com.trackit.app.data.wedding

import com.trackit.app.data.local.entity.WeddingTaskEntity
import java.util.UUID

/**
 * Template tugas default ±35 item, mencakup birokrasi universal + adat.
 * Disuntikkan saat profil Wedding dibuat. Fully editable oleh pengguna.
 */
object WeddingTaskPresets {

    // phaseMonth: bulan sebelum hari-H (12, 6, 3, 1, 0=hari-H)
    fun getPreset(
        weddingProfileId: String,
        culturalPresetGroom: String? = null,
        culturalPresetBride: String? = null
    ): List<WeddingTaskEntity> {
        val tasks = mutableListOf<WeddingTaskEntity>()
        var order = 0

        fun task(phase: Int, title: String, desc: String? = null, pic: String = "BOTH") =
            WeddingTaskEntity(
                taskId = UUID.randomUUID().toString(),
                weddingProfileId = weddingProfileId,
                phaseMonth = phase,
                title = title,
                description = desc,
                pic = pic,
                sortOrder = order++
            )

        // === H-12 — Perencanaan Awal ===
        tasks += task(12, "Tentukan tanggal & venue pernikahan", pic = "BOTH")
        tasks += task(12, "Buat anggaran awal & sumber dana (CPP/CPW/Ortu)", pic = "BOTH")
        tasks += task(12, "Daftarkan pernikahan ke KUA / Catatan Sipil", pic = "BOTH")
        tasks += task(12, "Mulai buat daftar tamu kasar", pic = "BOTH")
        tasks += task(12, "Survey & booking catering + katering", "Bandingkan minimal 3 vendor", pic = "BOTH")
        tasks += task(12, "Survey & booking fotografer / videografer", pic = "BOTH")
        tasks += task(12, "Survey & booking vendor dekorasi", pic = "BOTH")

        // === H-6 — Persiapan Detail ===
        tasks += task(6, "Pesan busana pengantin (fitting pertama)", pic = "BRIDE")
        tasks += task(6, "Pesan busana besan / seragam keluarga", pic = "BOTH")
        tasks += task(6, "Finalisasi & cetak undangan", pic = "BOTH")
        tasks += task(6, "Buat rundown acara bersama WO / keluarga", pic = "BOTH")
        tasks += task(6, "Booking honeymoon / hotel malam pertama", pic = "GROOM")
        tasks += task(6, "Persiapan seserahan / hantaran", "Tentukan isi dan wrapping", pic = "GROOM")
        tasks += task(6, "Siapkan perhitungan dan pembelian mahar", pic = "GROOM")
        tasks += task(6, "Selesaikan semua berkas KUA / Dukcapil", pic = "BOTH")

        // === H-3 — Finalisasi ===
        tasks += task(3, "Konfirmasi jumlah tamu final ke katering", pic = "BOTH")
        tasks += task(3, "Kirim undangan (fisik & digital)", pic = "BOTH")
        tasks += task(3, "Gladi resik / geladi bersih", pic = "BOTH")
        tasks += task(3, "Fitting busana kedua (terakhir)", pic = "BRIDE")
        tasks += task(3, "Lunasi DP semua vendor", pic = "GROOM")
        tasks += task(3, "Buat seating plan / layout meja", pic = "BOTH")
        tasks += task(3, "Siapkan goodie bag / souvenir", pic = "BOTH")
        tasks += task(3, "Brief panitia keluarga & among tamu", pic = "FAMILY")

        // === H-1 Bulan ===
        tasks += task(1, "Cek ulang kontrak & jadwal semua vendor", pic = "BOTH")
        tasks += task(1, "Siapkan amplop mahar & dokumen nikah untuk KUA", pic = "GROOM")
        tasks += task(1, "Pesan transportasi hari-H (pengantin & keluarga)", pic = "GROOM")
        tasks += task(1, "Istirahat cukup & jaga kesehatan", pic = "BOTH")
        tasks += task(1, "Konfirmasi saksi nikah (2 orang dari masing-masing pihak)", pic = "BOTH")

        // === Hari-H (phase=0) ===
        tasks += task(0, "Serahkan dokumen ke penghulu / pemuka agama", pic = "GROOM")
        tasks += task(0, "Sesi MUA & busana pengantin", "Mulai minimal H-4 jam sebelum akad", pic = "BRIDE")
        tasks += task(0, "Akad nikah / pemberkatan", pic = "BOTH")
        tasks += task(0, "Sesi foto bersama keluarga & tamu VIP", pic = "BOTH")
        tasks += task(0, "Resepsi & sambutan tamu", pic = "BOTH")
        tasks += task(0, "Pembayaran lunas ke semua vendor", pic = "GROOM")

        // === Tambahan berdasarkan adat ===
        val presets = setOf(culturalPresetGroom, culturalPresetBride)

        if (presets.any { it in setOf("BATAK") }) {
            tasks += task(6, "Martonggo Raja / rapat keluarga besar Batak", pic = "FAMILY")
            tasks += task(3, "Siapkan Sinamot (uang adat Batak)", pic = "GROOM")
            tasks += task(1, "Acara Paulak Une (kunjungan keluarga)", pic = "BOTH")
        }

        if (presets.any { it in setOf("BUGIS_MAKASSAR") }) {
            tasks += task(6, "Negosiasi & finalisasi Uang Panai", pic = "GROOM")
            tasks += task(3, "Acara Mappacci (ritual malam sebelum nikah)", pic = "BRIDE")
            tasks += task(3, "Persiapan Erang-erang (seserahan Bugis)", pic = "GROOM")
        }

        if (presets.any { it in setOf("JAWA") }) {
            tasks += task(3, "Acara Siraman & Midodareni (malam sebelum nikah)", pic = "BRIDE")
            tasks += task(3, "Persiapan Pasang Tarub / tebu wulung", pic = "FAMILY")
        }

        if (presets.any { it in setOf("MINANG") }) {
            tasks += task(6, "Musyawarah keluarga (Marapulai & Anak Daro)", pic = "FAMILY")
            tasks += task(3, "Manjapuik Marapulai (jemput pengantin pria)", pic = "BRIDE")
        }

        if (presets.any { it in setOf("BALI") }) {
            tasks += task(6, "Koordinasi Pedanda / pemangku untuk ritual", pic = "FAMILY")
            tasks += task(3, "Mekala-kalaan / Mewidhi Widana", pic = "BOTH")
        }

        if (presets.any { it in setOf("TIONGHOA") }) {
            tasks += task(6, "Upacara Sangjit (seserahan Tionghoa)", pic = "GROOM")
            tasks += task(3, "Persiapan Tea Ceremony", pic = "BOTH")
        }

        return tasks
    }
}
