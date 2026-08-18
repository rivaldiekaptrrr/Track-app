package com.trackit.app.data.wedding

import com.trackit.app.data.local.entity.WeddingEventEntity
import com.trackit.app.data.local.entity.WeddingRundownItemEntity
import java.util.UUID

/**
 * Preset rundown per culturalPreset. Ini hanya titik awal (starting point)
 * yang 100% dapat diedit oleh pengguna sesuai prinsip kustomisasi PRD §1.
 */
object WeddingRundownPresets {

    data class RundownPreset(
        val eventName: String,
        val items: List<RundownItem>
    )
    data class RundownItem(val time: String, val durationMin: Int, val title: String, val pic: String? = null)

    fun getPresetsForCultural(cultural: String?, weddingDate: Long): List<Pair<WeddingEventEntity, List<WeddingRundownItemEntity>>> {
        val c = cultural?.uppercase() ?: "MODERN"
        val events = when (c) {
            "JAWA"  -> jawaPlan()
            "SUNDA" -> sundaPlan()
            "BATAK" -> batakPlan()
            "MINANG" -> minangPlan()
            "BUGIS", "BUGIS_MAKASSAR" -> bugisPlan()
            "BALI"  -> baliPlan()
            "TIONGHOA", "TIONGHOA_PERANAKAN" -> tionghoaPlan()
            else    -> modernPlan()  // MODERN, BETAWI, CAMPURAN, LAINNYA
        }
        return buildEntities(events, weddingDate)
    }

    private fun buildEntities(
        plans: List<RundownPreset>,
        weddingDate: Long
    ): List<Pair<WeddingEventEntity, List<WeddingRundownItemEntity>>> {
        return plans.mapIndexed { eventIdx, plan ->
            val eventId = UUID.randomUUID().toString()
            val event = WeddingEventEntity(
                eventId = eventId,
                weddingProfileId = "",          // diisi saat seed dengan profileId nyata
                eventName = plan.eventName,
                eventDate = weddingDate,
                sortOrder = eventIdx
            )
            val items = plan.items.mapIndexed { itemIdx, item ->
                WeddingRundownItemEntity(
                    eventId = eventId,
                    timeStart = item.time,
                    durationMinutes = item.durationMin,
                    sessionTitle = item.title,
                    pic = item.pic,
                    sortOrder = itemIdx
                )
            }
            event to items
        }
    }

    // ── JAWA ──────────────────────────────────────────────────────────────────
    private fun jawaPlan() = listOf(
        RundownPreset("Siraman", listOf(
            RundownItem("09:00", 30, "Prosesi Siraman CPP", "Keluarga CPP"),
            RundownItem("09:30", 30, "Prosesi Siraman CPW", "Keluarga CPW"),
            RundownItem("10:00", 20, "Pecah Telur & Potong Rambut", "Kedua Orang Tua"),
            RundownItem("10:20", 30, "Doa Penutup & Santap Siang", "Pemuka Agama")
        )),
        RundownPreset("Midodareni", listOf(
            RundownItem("19:00", 30, "Penerimaan Tamu Midodareni", "Among Tamu"),
            RundownItem("19:30", 60, "Sungkeman CPW ke Orang Tua", "Keluarga CPW"),
            RundownItem("20:30", 30, "Doa & Penutupan", "Pemuka Agama")
        )),
        RundownPreset("Akad Nikah", listOf(
            RundownItem("08:00", 30, "Persiapan & Kedatangan Tamu", "Among Tamu"),
            RundownItem("08:30", 15, "Pembukaan & Pembacaan Ayat Suci", "MC"),
            RundownItem("08:45", 30, "Akad/Ijab Kabul", "Penghulu KUA"),
            RundownItem("09:15", 20, "Pembacaan Doa & Sungkeman", "Pemuka Agama"),
            RundownItem("09:35", 30, "Sesi Foto Keluarga & Resepsi Kecil", "Fotografer")
        )),
        RundownPreset("Resepsi", listOf(
            RundownItem("11:00", 30, "Dekorasi & Persiapan Pengantin", "Tim Dekorasi"),
            RundownItem("11:30", 15, "Kedatangan Pengantin (Kirab/Gending)", "MC"),
            RundownItem("11:45", 90, "Penerimaan Tamu Undangan", "Among Tamu"),
            RundownItem("13:15", 30, "Makan Bersama & Hiburan", "MC"),
            RundownItem("13:45", 30, "Sesi Foto Tamu & Pengantin", "Fotografer"),
            RundownItem("14:15", 15, "Penutupan & Pamit Tamu", "MC")
        ))
    )

    // ── SUNDA ─────────────────────────────────────────────────────────────────
    private fun sundaPlan() = listOf(
        RundownPreset("Ngaras", listOf(
            RundownItem("09:00", 45, "Prosesi Ngaras (sungkeman CPW)", "Keluarga CPW"),
            RundownItem("09:45", 30, "Doa & Ngawinkeun Seserahan", "Pemuka Agama")
        )),
        RundownPreset("Akad Nikah", listOf(
            RundownItem("08:00", 20, "Pembukaan & Tilawah", "MC"),
            RundownItem("08:20", 30, "Akad/Ijab Kabul", "Penghulu KUA"),
            RundownItem("08:50", 20, "Sungkeman & Doa", "Pemuka Agama"),
            RundownItem("09:10", 30, "Sesi Foto Keluarga", "Fotografer")
        )),
        RundownPreset("Resepsi", listOf(
            RundownItem("11:00", 20, "Kedatangan Pengantin", "MC"),
            RundownItem("11:20", 90, "Penerimaan Tamu", "Among Tamu"),
            RundownItem("12:50", 40, "Makan Bersama & Hiburan Sunda", "MC"),
            RundownItem("13:30", 30, "Sesi Foto & Pamit", "Fotografer")
        ))
    )

    // ── BATAK ─────────────────────────────────────────────────────────────────
    private fun batakPlan() = listOf(
        RundownPreset("Martonggo Raja", listOf(
            RundownItem("18:00", 30, "Marhata Adat (kesepakatan keluarga)", "Suhut"),
            RundownItem("18:30", 60, "Makan Bersama Keluarga Besar", "Keluarga CPP"),
            RundownItem("19:30", 30, "Doa & Pemberkatan Keluarga", "Pemuka Agama")
        )),
        RundownPreset("Akad/Pemberkatan", listOf(
            RundownItem("09:00", 30, "Pembukaan & Lagu Rohani", "MC"),
            RundownItem("09:30", 45, "Pemberkatan Pernikahan", "Pendeta/Pemuka Agama"),
            RundownItem("10:15", 30, "Sungkeman & Tukar Cincin", "CPP & CPW"),
            RundownItem("10:45", 30, "Martumpol (Janji Pernikahan)", "Suhut"),
            RundownItem("11:15", 30, "Sesi Foto Keluarga", "Fotografer")
        )),
        RundownPreset("Pesta Adat & Resepsi", listOf(
            RundownItem("12:00", 30, "Kirab Pengantin & Tortor", "Tim Adat"),
            RundownItem("12:30", 60, "Marhata Sinamot (penyerahan resmi)", "Dongan Tubu"),
            RundownItem("13:30", 60, "Makan Siang & Hiburan Adat", "MC"),
            RundownItem("14:30", 45, "Penerimaan Tamu & Ulos Adat", "Suhut"),
            RundownItem("15:15", 15, "Penutupan", "MC")
        ))
    )

    // ── MINANG ────────────────────────────────────────────────────────────────
    private fun minangPlan() = listOf(
        RundownPreset("Malam Bainai", listOf(
            RundownItem("20:00", 30, "Pembukaan & Tari Persembahan", "MC"),
            RundownItem("20:30", 30, "Prosesi Malam Bainai CPW", "Ibu-Ibu Keluarga"),
            RundownItem("21:00", 30, "Doa & Penutupan", "Pemuka Agama")
        )),
        RundownPreset("Akad Nikah", listOf(
            RundownItem("08:00", 20, "Pembukaan & Tilawah", "MC"),
            RundownItem("08:20", 30, "Ijab Kabul", "Penghulu KUA"),
            RundownItem("08:50", 20, "Sungkeman & Doa", "Pemuka Agama"),
            RundownItem("09:10", 30, "Sesi Foto Keluarga", "Fotografer")
        )),
        RundownPreset("Resepsi Adat Minang", listOf(
            RundownItem("11:00", 30, "Kirab Pengantin (Arak-arakan)", "Niniak Mamak"),
            RundownItem("11:30", 90, "Penerimaan Tamu & Makan Bajamba", "Among Tamu"),
            RundownItem("13:00", 30, "Tari Piring & Hiburan Minang", "Grup Seni"),
            RundownItem("13:30", 30, "Sesi Foto & Pamit", "Fotografer")
        ))
    )

    // ── BUGIS ─────────────────────────────────────────────────────────────────
    private fun bugisPlan() = listOf(
        RundownPreset("Mappacci", listOf(
            RundownItem("20:00", 20, "Pembukaan & Sambutan Keluarga", "MC"),
            RundownItem("20:20", 60, "Prosesi Mappacci CPW", "Tokoh Adat"),
            RundownItem("21:20", 20, "Doa & Penutupan", "Pemuka Agama")
        )),
        RundownPreset("Akad Nikah", listOf(
            RundownItem("08:00", 20, "Pembukaan & Tilawah", "MC"),
            RundownItem("08:20", 30, "Proses Madduppa Baté (penyambutan)", "Keluarga CPP"),
            RundownItem("08:50", 30, "Ijab Kabul", "Penghulu KUA"),
            RundownItem("09:20", 20, "Sungkeman & Doa", "Pemuka Agama"),
            RundownItem("09:40", 30, "Foto Keluarga", "Fotografer")
        )),
        RundownPreset("Resepsi Adat Bugis", listOf(
            RundownItem("11:00", 30, "Kirab Pengantin", "MC"),
            RundownItem("11:30", 90, "Penerimaan Tamu & Hidangan Bugis", "Among Tamu"),
            RundownItem("13:00", 30, "Mappasikarawa & Hiburan", "Tokoh Adat"),
            RundownItem("13:30", 30, "Sesi Foto & Penutupan", "Fotografer")
        ))
    )

    // ── BALI ──────────────────────────────────────────────────────────────────
    private fun baliPlan() = listOf(
        RundownPreset("Mesangih / Mewidhi Widana", listOf(
            RundownItem("09:00", 60, "Upacara Mewidhi Widana", "Pemangku"),
            RundownItem("10:00", 30, "Mapamit & Ngulapan", "Keluarga CPW"),
            RundownItem("10:30", 30, "Doa Pejati & Natab Banten", "Pemangku")
        )),
        RundownPreset("Resepsi Adat Bali", listOf(
            RundownItem("11:00", 30, "Kirab Pengantin (Barong/Legong)", "Grup Seni"),
            RundownItem("11:30", 90, "Penerimaan Tamu & Sajian Bali", "Among Tamu"),
            RundownItem("13:00", 30, "Tari Kecak/Joged & Hiburan", "Grup Seni"),
            RundownItem("13:30", 30, "Sesi Foto & Penutupan", "Fotografer")
        ))
    )

    // ── TIONGHOA ──────────────────────────────────────────────────────────────
    private fun tionghoaPlan() = listOf(
        RundownPreset("Sangjit (Seserahan Tionghoa)", listOf(
            RundownItem("10:00", 30, "Kedatangan Rombongan CPP ke Rumah CPW", "MC"),
            RundownItem("10:30", 45, "Prosesi Sangjit (penyerahan barang)", "Tokoh Keluarga"),
            RundownItem("11:15", 30, "Makan Bersama & Foto Keluarga", "Fotografer")
        )),
        RundownPreset("Pemberkatan / Akad", listOf(
            RundownItem("09:00", 30, "Pembukaan & Musik Erhu/Tradisional", "MC"),
            RundownItem("09:30", 45, "Pemberkatan / Akad Nikah", "Pendeta/Penghulu"),
            RundownItem("10:15", 30, "Tea Ceremony (Penghormatan Orang Tua)", "CPP & CPW"),
            RundownItem("10:45", 30, "Sesi Foto Keluarga", "Fotografer")
        )),
        RundownPreset("Resepsi", listOf(
            RundownItem("18:00", 30, "Cocktail Hour & Registrasi Tamu", "Among Tamu"),
            RundownItem("18:30", 20, "Kirab Pengantin", "MC"),
            RundownItem("18:50", 90, "Makan Malam & Hiburan", "MC"),
            RundownItem("20:20", 30, "Pemotongan Kue & Sesi Foto", "Fotografer"),
            RundownItem("20:50", 30, "Penutupan & Pamit Tamu", "MC")
        ))
    )

    // ── MODERN ────────────────────────────────────────────────────────────────
    private fun modernPlan() = listOf(
        RundownPreset("Akad Nikah", listOf(
            RundownItem("08:00", 20, "Pembukaan & Sambutan", "MC"),
            RundownItem("08:20", 30, "Akad/Ijab Kabul", "Penghulu KUA"),
            RundownItem("08:50", 20, "Sungkeman & Doa", "Pemuka Agama"),
            RundownItem("09:10", 30, "Sesi Foto Keluarga", "Fotografer")
        )),
        RundownPreset("Resepsi", listOf(
            RundownItem("11:00", 30, "Persiapan & Sound Check", "Tim Teknis"),
            RundownItem("11:30", 15, "Kedatangan Pengantin", "MC"),
            RundownItem("11:45", 90, "Penerimaan Tamu", "Among Tamu"),
            RundownItem("13:15", 45, "Makan Siang & Hiburan", "MC"),
            RundownItem("14:00", 30, "Sesi Foto & Penutupan", "Fotografer")
        ))
    )
}
