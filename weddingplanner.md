# Product Requirement Document (PRD)
**Modul Integrasi:** Wedding Planner Mode pada Aplikasi TrackIt
**Platform:** Android (Kotlin Native)
**Arsitektur:** Mengikuti arsitektur & konvensi aplikasi TrackIt yang sudah berjalan (lihat §3)
**Target Rilis:** Q4 2026
**Status Dokumen:** Revisi 8 — Ready for Development

---

## 0. Riwayat Revisi

| Versi | Tanggal | Perubahan |
| --- | --- | --- |
| v1 | — | Draft awal |
| v2 | — | Perbaikan rumus kalkulator katering, penambahan relasi `profileId` di seluruh entity, migrasi Room DB, kepatuhan UU PDP, permission runtime, definisi prioritas P0/P1/P2, KPI, Out of Scope, Risks & Assumptions, dependency eksternal, strategi testing |
| v3 | — | Klarifikasi positioning: produk murni **B2C** untuk pasangan calon pengantin, bukan tools/mode profesional untuk Wedding Organizer (WO). Menghapus seluruh referensi ke arah multi-tenant/multi-klien WO |
| v4 | — | Menghapus Modul 10 (On-Site QR Check-in) beserta seluruh dependensinya (permission CAMERA, ML Kit/CameraX, field check-in di `WeddingGuestEntity`). Produk ditegaskan sebagai **planner**, bukan alat operasional hari-H |
| v5 | — | Melonggarkan section arsitektur (§3) dan NFR teknis (§4.1, §6.2, §6.4, §6.8) agar tidak mendikte stack/pattern baru (MVVM/Compose/Room/Hilt/SQLCipher, dsb.) — modul ini **mengikuti arsitektur & konvensi TrackIt yang sudah eksis**, bukan membangun dari nol. Skema entity kini berstatus usulan struktur data, bukan spesifikasi final |
| v6 | — | Menghapus fitur **voice input** dari seluruh modul (Modul 1, Modul 4, entity, KPI, sprint plan, testing). Modul 4 diubah jadi form manual sepenuhnya dengan rasional eksplisit di §1.3 dan §5. `entrySource` dihapus dari `WeddingExpenseEntity` |
| v7 | — | Menambahkan **prinsip keberagaman adat & kustomisasi lintas-modul** (§1) sebagai desain inti, bukan catatan sampingan. Memperluas Modul 3 (checklist berkas per agama, bukan hanya Muslim), Modul 5 (mahar/uang adat extensible: Uang Panai, Sinamot, dll + opsi custom), Modul 6 (rundown acara jadi user-defined, bukan tab tetap Jawa-sentris), Modul 7 (peran panitia multi-preset). Menambah field `culturalPresetGroom`, `culturalPresetBride`, `religionDetail` di `WeddingProfileEntity`. Menambah kriteria multi-preset adat ke Definition of Done |
| v8 | — | Menambahkan **Lampiran A**: draf konten konkret 9 preset adat (Jawa, Sunda, Batak, Minang, Bugis-Makassar, Bali, Betawi, Tionghoa-Peranakan, Modern) beserta rundown, mahar/uang adat, seserahan, dan peran panitia khas — sebagai starting point seeding data agar Sprint 3–4 tidak mulai riset dari nol. Ditegaskan berstatus draf yang wajib divalidasi, bukan sumber final |

---

## 1. Executive Summary & Visi Produk
Aplikasi **TrackIt** awalnya dirancang sebagai aplikasi *smart voice expense tracker*. Untuk meningkatkan *Customer Lifetime Value* (LTV) dan menjawab kebutuhan generasi muda di Indonesia yang sedang mempersiapkan tahapan hidup krusial, TrackIt memperluas fungsinya dengan menambahkan modul **"Wedding Planner Mode"**.

Mode ini memungkinkan calon pengantin mengelola seluruh siklus persiapan pernikahan — dari legalitas KUA/Dukcapil, linimasa adat/nasional, *split-bill budgeting*, manajemen vendor, hingga RSVP tamu — dalam satu ekosistem native Android yang cepat dan *offline-first*, terintegrasi langsung dengan modul expense tracker TrackIt yang sudah ada.

> **Positioning produk:** Wedding Planner Mode adalah aplikasi **B2C mandiri (self-service)** yang digunakan langsung oleh calon pengantin (CPP/CPW) untuk keperluan pernikahan mereka sendiri. Produk ini **bukan** tools B2B/SaaS untuk Wedding Organizer (WO) profesional yang mengelola banyak klien. WO tetap muncul dalam aplikasi, tetapi hanya sebagai *entitas vendor eksternal* yang dicatat pengantin (mis. di Modul 9 Vendor Hub atau sebagai PIC tugas di Modul 2) — WO tidak login atau mengoperasikan aplikasi ini sendiri.

> **Prinsip Keberagaman Adat & Kustomisasi (baru, berlaku lintas-modul):** Indonesia memiliki ratusan adat pernikahan yang berbeda (Jawa, Sunda, Batak, Minang, Bugis-Makassar, Bali, Betawi, Dayak, Tionghoa-Peranakan, dan banyak lagi), termasuk pasangan dengan **pernikahan campuran** (dua adat berbeda) atau yang memilih format modern tanpa adat tertentu. Aplikasi ini **tidak boleh mengasumsikan satu adat sebagai default yang mengikat**. Setiap konten templated di seluruh modul — nama tahapan acara (Modul 6), kategori seserahan/mahar (Modul 5), peran panitia keluarga (Modul 7), dan daftar tugas (Modul 2) — wajib dapat **ditambah, diubah nama, dihapus, dan disusun ulang** oleh pengguna. Template bawaan (§5) berstatus *starting point/preset per pilihan adat*, bukan struktur tetap. Detail teknis lihat §5 Modul 2, 5, 6, 7 dan skema `culturalPreset` di §4.

### 1.1 Definisi Prioritas
Agar tidak ambigu di seluruh dokumen ini:

- **P0 (Wajib rilis):** Tanpa fitur ini, produk tidak layak dirilis (blocker MVP).
- **P1 (Penting, bisa menyusul):** Meningkatkan value signifikan, boleh di-*ship* di rilis minor setelah MVP jika waktu sprint tidak cukup.
- **P2 (Tahap lanjutan):** Nice-to-have, dijadwalkan setelah validasi metrik MVP positif.

### 1.2 Success Metrics / KPI (Q4 2026 – Q1 2027)

| Metrik | Target | Cara Ukur |
| --- | --- | --- |
| Adopsi Wedding Mode | ≥25% pengguna aktif TrackIt mengaktifkan mode ini dalam 90 hari | Event analytics `wedding_mode_activated` |
| Retensi 30 hari (kohort Wedding Mode) | ≥40% | Cohort retention dashboard |
| LTV kohort Wedding Mode vs kohort umum | +20% | Perbandingan ARPU 6 bulan |
| Pemanfaatan Kalkulator Katering & Budget Tracker | ≥60% pengguna aktif mengisi minimal 1 kategori anggaran | Rasio `wedding_expenses` terisi per profil aktif |
| Crash-free session rate modul ini | ≥99.5% | Crashlytics / Firebase |

### 1.3 Out of Scope (Rilis Q4 2026)
- Integrasi pembayaran langsung ke vendor (payment gateway) — hanya pencatatan, bukan transaksi riil.
- Integrasi resmi WhatsApp Business API — fitur "template WhatsApp" hanya memakai Android Share Intent (`ACTION_SEND`) ke aplikasi WhatsApp yang terpasang, bukan pengiriman otomatis/broadcast.
- **Mode/akun profesional untuk Wedding Organizer (WO).** Produk ini murni B2C untuk satu pasangan calon pengantin; tidak ada fitur pengelolaan banyak klien, tidak ada login WO, dan tidak ada rencana roadmap ke arah B2B. WO hanya muncul sebagai data vendor yang dicatat oleh pengantin.
- Sinkronisasi cloud real-time lintas device (couple mode kolaboratif) — hanya backup/restore manual ke Google Drive pribadi.
- **Fitur operasional hari-H (event execution),** termasuk QR check-in tamu, scanning kehadiran, atau alat bantu reception lainnya. TrackIt Wedding Planner Mode adalah aplikasi **perencanaan (planner)**, bukan aplikasi operasional di lokasi acara. Manajemen tamu (Modul 8) berhenti di tahap RSVP/data kuota, tidak sampai ke pelacakan kehadiran fisik.
- **Voice input untuk pencatatan pengeluaran pernikahan.** Meskipun TrackIt core punya fitur *smart voice expense tracker*, modul Wedding Planner ini **tidak mengadopsinya**: pencatatan anggaran pernikahan cenderung berupa transaksi besar dan jarang (DP vendor, pelunasan), dilakukan dengan tenang saat duduk membuka app — bukan transaksi kecil harian yang butuh kecepatan input suara sambil beraktivitas seperti expense tracker personal pada umumnya. Seluruh input di modul ini memakai form manual (lihat §5 Modul 4).
- Dukungan iOS.

---

## 2. Sasaran Pengguna & Problem Statement

### 2.1 Target User
* Calon Pengantin Pria (CPP) dan Calon Pengantin Wanita (CPW) usia 22–35 tahun di Indonesia.
* Calon pengantin yang mengurus pernikahan secara mandiri (*DIY Wedding*) maupun yang berkolaborasi dengan keluarga dan *Wedding Organizer* (WO).

### 2.2 Pain Points Utama di Indonesia
1. **Birokrasi Rumit & Berkas Tercecer:** Ketidaktahuan alur surat pengantar RT/RW, Formulir N1–N4 kelurahan, sertifikat kesehatan ELSIMIL, hingga surat rekomendasi numpang nikah KUA.
2. **Pembengkakan Anggaran (*Overbudget*):** Sulitnya memantau DP/termin vendor dan pembagian dana (*split-bill*) antara tabungan CPP, tabungan CPW, dan sumbangan orang tua.
3. **Koordinasi Keluarga & Tradisi Lokal:** Kompleksitas persiapan seserahan/hantaran, pembagian bahan seragam keluarga/among tamu, dan perebutan jatah kuota tamu orang tua vs pengantin.

### 2.3 Risks & Assumptions

| Risiko/Asumsi | Dampak | Mitigasi |
| --- | --- | --- |
| Asumsi: satu instalasi = satu pasangan (single active profile), berlaku permanen sesuai positioning B2C (§1) | Rendah | `profileId` tetap dipakai sebagai FK di skema (bukan untuk multi-tenant, murni agar relasi antar tabel eksplisit dan integritas data terjaga saat migrasi) |
| Data KTP/KK/ELSIMIL sangat sensitif — kebocoran berdampak hukum & reputasi | Tinggi | Enkripsi data sensitif wajib (mekanisme mengikuti standar yang sudah ada di TrackIt, lihat §3.1), audit keamanan sebelum rilis, kepatuhan UU PDP (lihat §6.1) |
| Konten templated (tugas, rundown, seserahan, panitia) berisiko terasa "Jawa-sentris" jika preset tidak representatif — berpotensi membuat pengguna dari adat lain merasa aplikasi tidak relevan | Sedang | Sediakan multi-preset adat sejak MVP (§4 `culturalPreset`, §5 Modul 2/5/6/7), bukan satu template tunggal; seluruh konten templated wajib fully-editable (lihat prinsip §1) |
| Google Play Policy soal Sensitive Permissions (Contacts) bisa menahan proses review | Sedang | Siapkan Data Safety form dan justifikasi penggunaan sejak awal submission |

---

## 3. Arsitektur Teknis Android (Kotlin Native)

> **Prinsip:** Wedding Planner Mode adalah **modul tambahan di dalam aplikasi TrackIt yang sudah production**, bukan aplikasi baru yang dibangun dari nol. Tim engineering **mengikuti pattern, layering, dan konvensi coding yang sudah dipakai di codebase TrackIt saat ini** (MVVM/Clean Architecture/MVI — apapun yang sudah berjalan), bukan yang didikte oleh PRD ini. Poin-poin di bawah adalah *kebutuhan fungsional/non-fungsional* yang harus dipenuhi modul ini; implementasi teknis detail (pilihan library DI, pattern state management, dsb.) mengikuti keputusan tim engineering agar konsisten dengan modul-modul TrackIt lain.

* **UI Toolkit:** mengikuti yang dipakai TrackIt saat ini (Compose atau XML/View-based) — modul ini tidak memaksa migrasi UI toolkit.
* **Local Persistence:** data disimpan lokal di database yang sama dengan yang dipakai TrackIt core (kemungkinan besar Room/SQLite) agar bisa terintegrasi dengan modul expense tracker yang sudah ada; skema entity di §4 adalah usulan struktur data, boleh disesuaikan ke konvensi naming/DAO yang sudah berlaku di codebase.
* **Enkripsi data sensitif:** wajib (lihat §6.2) — mekanisme spesifik (SQLCipher atau library lain) mengikuti standar keamanan yang sudah dipakai TrackIt untuk data finansial, jika sudah ada.
* **Background Tasks:** reminder tenggat waktu berkas & jatuh tempo termin vendor perlu terjadwal walau app tidak dibuka — gunakan mekanisme scheduling yang sudah dipakai TrackIt (WorkManager/AlarmManager atau lainnya).
* **Input Data:** seluruhnya melalui form manual (lihat §5 Modul 4 untuk rasionalnya) — modul ini tidak menggunakan infrastruktur voice input TrackIt.

### 3.1 Enkripsi Data Sensitif
- Data KTP/KK/finansial pernikahan wajib mengikuti standar enkripsi data sensitif yang sudah berlaku di TrackIt core. Jika belum ada standar tersebut untuk data setingkat ini, tim keamanan perlu menetapkannya sebelum Sprint 1 dimulai (lihat juga §6.1 kepatuhan UU PDP).
- Passphrase/kunci enkripsi disimpan di Android Keystore, bukan hardcoded atau SharedPreferences biasa.
- Opsional: biometric lock tambahan untuk membuka Wedding Mode, mengikuti pattern app-lock yang mungkin sudah tersedia di TrackIt.

### 3.2 External Dependencies (kebutuhan baru di luar yang sudah ada di TrackIt)

| Dependency | Kebutuhan | Catatan |
| --- | --- | --- |
| Android Share Intent (`ACTION_SEND`) | "WhatsApp Template Generator" | Bukan WhatsApp Business API resmi; gagal jika WhatsApp tidak terpasang → sediakan fallback copy-to-clipboard |
| Google Drive REST API (opsional, user-authorized) | Backup/restore manual | Perlu OAuth consent screen; data yang diunggah tetap terenkripsi. Cek dulu apakah TrackIt sudah punya fitur backup — jika ya, modul ini menumpang alur yang sama |
| Android Storage Access Framework (SAF) | Attachment dokumen legalitas | Tidak perlu permission storage klasik di Android 10+ |

> Dependency lain (persistence, DI) sengaja tidak didaftarkan di sini karena diasumsikan sudah tersedia di codebase TrackIt — tim engineering yang menentukan apakah perlu upgrade/library tambahan.

---

## 4. Skema Database Lokal (Usulan Struktur Data)

> **Catatan:** contoh kode Room di bawah ini menggambarkan **struktur data & relasi logis** yang dibutuhkan modul ini (entity apa saja, field apa saja, relasi ke mana) — bukan spesifikasi implementasi final. Jika TrackIt sudah punya konvensi penamaan tabel/kolom, base entity, atau bahkan memakai persistence layer selain Room, tim engineering silakan mengadaptasi selama relasi data dan kebutuhan fungsionalnya (lihat §5) tetap terpenuhi.
>
> **Perubahan dari v1:** seluruh entity anak kini memiliki `profileId` sebagai foreign key eksplisit ke `WeddingProfileEntity` (bukan hanya `WeddingPaymentTermEntity`), dengan index untuk performa query, dan `onDelete = CASCADE` agar konsisten saat profil dihapus (mis. saat pengguna memakai fitur "Hapus Semua Data" di §6.1). Ini murni untuk integritas relasi data B2C single-profile — **bukan** persiapan multi-tenant/WO (lihat §1.3 Out of Scope).

```kotlin
// 1. Profil Pasangan & Acara Utama
@Entity(tableName = "wedding_profiles")
data class WeddingProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val groomName: String,
    val brideName: String,
    val weddingDate: Long, // Epoch timestamp acara utama
    val totalBudgetCap: Double,
    val religionType: String, // ISLAM (KUA) / NON_ISLAM (Dukcapil) — menentukan alur legalitas negara di Modul 3
    val religionDetail: String? = null, // ISLAM, KRISTEN, KATOLIK, HINDU, BUDDHA, KONGHUCU, LAINNYA — opsional, untuk preset ritual/rundown yang lebih akurat, terpisah dari religionType yang hanya untuk alur KUA/Dukcapil
    val culturalPresetGroom: String? = null, // JAWA, SUNDA, BATAK, MINANG, BUGIS_MAKASSAR, BALI, BETAWI, DAYAK, TIONGHOA, CAMPURAN, MODERN_TANPA_ADAT, LAINNYA — preset awal untuk rundown/seserahan/panitia (§5 Modul 5/6/7), TIDAK mengunci konten, hanya starting point
    val culturalPresetBride: String? = null, // sama seperti di atas, untuk CPW — bisa berbeda dari CPP (pernikahan campuran adat)
    val isActive: Boolean = true, // Selalu 1 profil aktif per instalasi (produk B2C single-couple, §1.3)
    val createdAt: Long = System.currentTimeMillis()
)

// 2. Daftar Tugas & Timeline
@Entity(
    tableName = "wedding_tasks",
    foreignKeys = [ForeignKey(
        entity = WeddingProfileEntity::class,
        parentColumns = ["id"], childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("profileId")]
)
data class WeddingTaskEntity(
    @PrimaryKey val taskId: String = UUID.randomUUID().toString(),
    val profileId: String,
    val phaseMonth: Int, // 12, 6, 3, 1, 0 (Hari-H)
    val title: String,
    val description: String?,
    val pic: String, // GROOM, BRIDE, BOTH, FAMILY, WO
    val isCompleted: Boolean = false,
    val dueDate: Long?
)

// 3. Dokumen Legalitas & KUA
@Entity(
    tableName = "wedding_documents",
    foreignKeys = [ForeignKey(
        entity = WeddingProfileEntity::class,
        parentColumns = ["id"], childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("profileId")]
)
data class WeddingDocumentEntity(
    @PrimaryKey val docId: String = UUID.randomUUID().toString(),
    val profileId: String,
    val docName: String, // Surat RT/RW, N1, ELSIMIL, Akta, KK, dll
    val ownerType: String, // GROOM, BRIDE, TOGETHER
    val isCompleted: Boolean = false,
    val localFilePath: String?, // Uri SAF, bukan path absolut
    val adminCost: Double = 0.0
)

// 4. Anggaran & Transaksi (Terintegrasi TrackIt Core)
@Entity(
    tableName = "wedding_expenses",
    foreignKeys = [ForeignKey(
        entity = WeddingProfileEntity::class,
        parentColumns = ["id"], childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("profileId")]
)
data class WeddingExpenseEntity(
    @PrimaryKey val expenseId: String = UUID.randomUUID().toString(),
    val profileId: String,
    val category: String, // VENUE, CATERING, DECOR, MUA, DOKUMENTASI, SESERAHAN, dll
    val title: String,
    val totalEstimated: Double,
    val totalPaid: Double = 0.0,
    val paidBySource: String, // TABUNGAN_CPP, TABUNGAN_CPW, ORTU_CPP, ORTU_CPW
    val paymentStatus: String // UNPAID, PARTIAL_DP, FULLY_PAID
)

// 5. Pembayaran Termin / Cicilan Vendor
@Entity(
    tableName = "wedding_payment_terms",
    foreignKeys = [ForeignKey(
        entity = WeddingExpenseEntity::class,
        parentColumns = ["expenseId"], childColumns = ["expenseOwnerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("expenseOwnerId")]
)
data class WeddingPaymentTermEntity(
    @PrimaryKey val termId: String = UUID.randomUUID().toString(),
    val expenseOwnerId: String,
    val termName: String, // DP 1, DP 2, Pelunasan
    val amount: Double,
    val dueDate: Long,
    val isPaid: Boolean = false,
    val paidDate: Long?
)

// 6. Daftar Tamu & Undangan
@Entity(
    tableName = "wedding_guests",
    foreignKeys = [ForeignKey(
        entity = WeddingProfileEntity::class,
        parentColumns = ["id"], childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("profileId")]
)
data class WeddingGuestEntity(
    @PrimaryKey val guestId: String = UUID.randomUUID().toString(),
    val profileId: String,
    val guestName: String,
    val phoneNumber: String?,
    val groupAllocation: String, // KELUARGA_CPP, KELUARGA_CPW, TEMAN_CPP, TEMAN_CPW, VIP
    val sessionTarget: String, // SESI_1_AKAD, SESI_2_RESEPSI
    val estimatedPax: Int = 2,
    val rsvpStatus: String = "PENDING" // PENDING, ATTENDING, DECLINED
)
```

### 4.1 Migrasi Skema
- Prinsip yang wajib dipegang, terlepas dari mekanisme migrasi yang dipakai TrackIt: **perubahan skema tidak boleh menghilangkan data pengguna** (data finansial & dokumen legalitas berisiko tinggi jika hilang). Hindari pendekatan destruktif (drop & recreate) di rilis produksi.
- Ikuti mekanisme testing migrasi yang sudah menjadi standar di codebase TrackIt, jika sudah ada; jika belum, tim engineering menetapkan pendekatan yang sesuai dengan persistence layer yang dipakai.

---

## 5. Rincian Modul & Spesifikasi Fitur

### Modul 1: Dashboard & Mode Switcher (P0)
*(isi sama seperti draft awal — lihat detail komponen UI & logika bisnis)*
* Toggle Switch di App Bar (Personal Expense ⟷ Wedding Mode).
* Banner Countdown Hari-H (Bulan, Minggu, Hari, Jam).
* 3 Progress Bar: Kesiapan Tugas (%), Berkas Lengkap (%), Vendor Terbayar (%).
* Widget Finansial: Pagu Anggaran, Total Terbayar, Sisa Hutang, Status Anggaran (*Aman / Warning Overbudget*).
* Shortcut Cepat: Tambah Pengeluaran (form manual), Centang Tugas Terdekat.
* **Logika Bisnis:** peringatan visual merah jika `Total Terbayar + Estimasi Sisa > Total Pagu Anggaran`, dihitung reaktif via Room StateFlow.

### Modul 2: Timeline & Task Management (P0)
* Pengelompokan tugas per tahapan: H-12 s/d H-9, H-8 s/d H-6, H-5 s/d H-3, H-2 s/d H-1 Bulan, Hari-H, Pasca Acara.
* Filter Chips: PIC (CPP, CPW, Bersama, Panitia Keluarga, WO).
* Template tugas default (±30–40 item, mencakup birokrasi negara yang berlaku universal + item adat) disuntikkan otomatis saat profil dibuat, **disesuaikan dengan `culturalPresetGroom`/`culturalPresetBride`** yang dipilih pengguna saat onboarding (mis. preset Batak menambahkan item terkait Sinamot/Martonggo Raja, preset Bugis menambahkan Uang Panai/Mappacci, preset campuran menggabungkan item dari kedua adat). Seluruh item — apa pun presetnya — **dapat ditambah, diubah nama, atau dihapus penuh** oleh pengguna (lihat prinsip §1).
* Alarm lokal via `WorkManager` untuk tugas prioritas tinggi; hormati batasan Doze Mode / battery optimization Android 12+.

### Modul 3: Berkas KUA, Dukcapil & Tes Kesehatan (P0)
* **Alur Muslim (KUA):** Tab Checklist Berkas Pria (CPP) & Wanita (CPW): Surat RT/RW, Formulir N1/N2/N4, Surat Rekomendasi Nikah, Akta Kelahiran, KK, Ijazah, Pasfoto Latar Biru, Bukti Bayar PNBP.
* **Alur Non-Muslim (Dukcapil, pasca pemberkatan/upacara keagamaan):** checklist disesuaikan `religionDetail` (§4) karena persyaratan berbeda per agama, contoh:
  * Kristen/Katolik: Surat Keterangan Baptis, Surat Pengantar Gereja/Pastor, Surat Numpang Nikah dari gereja asal (jika beda paroki/jemaat).
  * Hindu: Surat Keterangan telah menikah secara Hindu dari pemangku/tokoh agama, Surat Widhi Widana (Bali).
  * Buddha: Surat Keterangan Pernikahan dari Wiharapala/Majelis Buddha.
  * Konghucu: Surat Keterangan Peneguhan Perkawinan dari Lithang/Majelis Agama Khonghucu.
  * Berkas dasar yang tetap sama lintas agama: Akta Kelahiran, KK, KTP, Pasfoto, dan pelaporan pernikahan ke Dukcapil (N1 dkk. dari kelurahan tetap relevan sebagai syarat administrasi sipil terlepas dari agama).
  * Daftar berkas per agama adalah **preset yang dapat disesuaikan** pengguna — beberapa daerah/instansi punya persyaratan tambahan berbeda.
* Sub-Checklist Kesehatan (berlaku untuk semua): Sertifikat ELSIMIL (BKKBN), Surat Sehat Puskesmas, Imunisasi Tetanus (TT).
* Attachment via SAF; file disimpan di app-scoped storage, bukan shared storage, agar aman dari akses aplikasi lain.
* Filter otomatis alur: KUA (Muslim) vs Dukcapil (Non-Muslim) berdasarkan `religionType`, dengan sub-checklist detail mengikuti `religionDetail`.
* Notifikasi jika berkas belum lengkap saat memasuki H-10 hari kerja sebelum akad/pemberkatan.

### Modul 4: Budgeting & Split-Bill Tracker (P0)
* Alokasi Anggaran per Kategori; Split-Bill Breakdown (grafik proporsi sumber dana); Log Termin & Pengingat Jatuh Tempo.
* **Form Tambah Pengeluaran (manual):** field Kategori (dropdown), Nominal, Jenis Pembayaran (DP/Pelunasan/Full), Sumber Dana (dropdown: Tabungan CPP/CPW/Ortu CPP/Ortu CPW), tanggal, catatan opsional.
* **Rasional tidak memakai voice input:** berbeda dari expense tracker harian TrackIt yang mengandalkan kecepatan input suara untuk transaksi kecil dan sering, transaksi anggaran pernikahan bernilai besar dan jarang (DP vendor, pelunasan) — pengguna cenderung mengisi dengan tenang sambil mengecek detail invoice/kontrak, sehingga form terstruktur lebih akurat dan lebih mudah dikoreksi dibanding hasil parsing suara. Lihat juga §1.3 Out of Scope.
* Validasi form: nominal wajib diisi (>0), kategori & sumber dana wajib dipilih sebelum tersimpan.

### Modul 5: Seserahan, Hantaran, Mahar & Uang Adat Tracker (P1)
* Tab Seserahan CPP→CPW dan Balasan/Angsul-angsul CPW→CPP — nama tab & kategori barang **dapat diubah/disesuaikan** karena istilah dan praktiknya berbeda antar adat (mis. "Sangjit" pada tradisi Tionghoa memiliki struktur pemberian dua arah yang berbeda dari seserahan Jawa/Sunda).
* Status per Kotak: *Dibeli → Wrapping → Selesai Siap Kirim*, item dapat ditambah bebas sesuai kebutuhan.
* **Detail Mahar/Uang Adat — daftar tipe extensible, bukan daftar tetap:**
  * Preset umum: Emas Logam Mulia (gramasi), Dinar/Dirham, Seperangkat Alat Sholat, Mata Uang/Nominal Custom.
  * Preset adat tambahan (muncul otomatis sesuai `culturalPreset`, tetap bisa diedit manual): **Uang Panai** (Bugis-Makassar), **Sinamot** (Batak), **Uang Jujur** (Lampung/sebagian Sumatra), atau kombinasi lain.
  * Selalu tersedia opsi **"Lainnya (custom)"** dengan field nama bebas + nominal/deskripsi, untuk adat yang belum ada di preset.
* Nilai aset mahar/seserahan/uang adat dipisahkan dari pengeluaran operasional pesta di laporan anggaran (Modul 4), karena secara adat sering dianggap milik pribadi mempelai, bukan biaya acara.

### Modul 6: Rangkaian Acara & Rundown Multi-Event (P0)
* **Struktur acara sepenuhnya user-defined, bukan tab tetap.** Pengguna menambah/menghapus/mengubah nama tab acara sendiri (misalnya: Lamaran, Sangjit, Mappacci, Martonggo Raja, Siraman, Midodareni, Akad Nikah, Pemberkatan, Mesangih, Resepsi, atau nama lain sesuai kebutuhan) — aplikasi tidak membatasi hanya pada rangkaian adat Jawa/Islam.
* Saat onboarding, jika pengguna memilih `culturalPresetGroom`/`culturalPresetBride`, aplikasi menyarankan (bukan memaksakan) urutan tab acara yang umum untuk preset tersebut sebagai titik awal yang bisa langsung diedit.
* Tabel Timetable menit-ke-menit per tab acara: Waktu, Durasi, Sesi Kegiatan, PIC, Teks Panduan MC.
* Export PDF / Share via WhatsApp Intent (lihat §3.2 — bukan API resmi).

### Modul 7: Panitia Keluarga & Seragam (P1)
* Bagan Panitia: preset umum (Saksi Nikah, Sambutan, Pembaca Doa, Penjaga Meja Kado/Angpao, Among Tamu) ditambah preset adat sesuai `culturalPreset` bila relevan (mis. peran **Suhut** & **Dongan Tubu** pada adat Batak, **Pagar Ayu/Pagar Bagus** pada tradisi Jawa) — semua peran dapat ditambah/diubah/dihapus bebas, termasuk membuat peran baru dengan nama sendiri.
* Pelacak Seragam/Kain: Penerima, Peran, Jatah Meteran, Status Jahit (*Belum Dibagi → Sedang Jahit → Siap Pakai*).

### Modul 8: Manajemen Tamu, RSVP & Kalkulator Katering (P0)
* Pembagian Kuota Undangan: Ortu CPP (%), Ortu CPW (%), Pengantin (%).
* Contact Importer: import dari buku telepon Android — **wajib runtime permission `READ_CONTACTS`** dengan penjelasan izin di UI sebelum prompt sistem (lihat §6.3).
* Template WhatsApp Generator via Share Intent (bukan pengiriman otomatis/broadcast).

**Kalkulator Katering Otomatis — rumus diperbaiki:**

Formula pada draft awal (`− 10%` sebagai pengurangan langsung, dan `× 4` tanpa penjelasan) ambigu secara matematis. Versi yang benar:

$$\text{Total Porsi} = \big(\text{Jumlah Undangan} \times \text{Estimasi Pax per Undangan}\big) \times (1 - \text{Buffer\%})$$

- `Estimasi Pax per Undangan` diambil dari rata-rata `estimatedPax` di `WeddingGuestEntity` (default 2), **bukan hardcoded ×2**.
- `Buffer%` default 10%, dapat diubah pengguna (representasi: tamu yang RSVP "Attending" biasanya tidak semua datang).

$$\text{Porsi Buffet (Prasmanan)} = 60\% \times \text{Total Porsi}$$

$$\text{Porsi per Stall (Gubukan)} = \frac{40\% \times \text{Total Porsi}}{\text{Jumlah Stall Aktif}}$$

> Catatan revisi: rumus asli `× 4` diganti pembagian dengan jumlah stall aktif yang di-input pengguna (mis. 4 booth = bakso, sate, dimsum, dessert), karena "×4" tanpa konteks jumlah booth justru **melipatgandakan** kebutuhan porsi, bukan membaginya — berisiko menyebabkan estimasi belanja katering membengkak signifikan (overbudget), padahal tujuan modul ini justru mencegah overbudget (§2.2 pain point #2).

### Modul 9: Vendor Hub & Arsip Kontrak (P1)
* Kategori Vendor: Venue, Katering, Dekorasi, MUA & Busana, Fotografi/Videografi, Musik, WO, Souvenir.
* Detail Item: PIC, WhatsApp, Instagram, Nilai Kontrak, lampiran MoU (PDF).
* Terhubung langsung ke modul pembayaran termin (Modul 4).

---

## 6. Non-Functional Requirements (NFR) & Security

### 6.1 Kepatuhan Hukum & Privasi Data (baru)
* Aplikasi memproses data pribadi kategori khusus (KTP, KK, data kesehatan ELSIMIL) sehingga tunduk pada **UU No. 27 Tahun 2022 tentang Pelindungan Data Pribadi (UU PDP)**.
* Wajib disediakan: halaman kebijakan privasi in-app, mekanisme *right to erasure* (hapus seluruh data profil & dokumen dari perangkat + backup Drive melalui satu tombol "Hapus Semua Data"), dan consent eksplisit saat pertama kali mengunggah dokumen KTP/KK.
* Data tidak dikirim ke server pihak ketiga mana pun kecuali atas aksi eksplisit pengguna (backup ke Google Drive pribadi miliknya sendiri).

### 6.2 Keamanan & Privasi Data
* Seluruh data anggaran, identitas dokumen (KTP/KK), dan kontak tamu wajib dienkripsi secara lokal, memakai mekanisme enkripsi yang sudah menjadi standar TrackIt untuk data sensitif jika sudah tersedia (mis. SQLCipher/EncryptedSharedPreferences atau setara). Kunci enkripsi dikelola via Android Keystore (§3.1).

### 6.3 Permission Runtime (baru)

| Permission | Modul | Justifikasi & Fallback |
| --- | --- | --- |
| `READ_CONTACTS` | Modul 8 | Import kontak tamu; jika ditolak, pengguna tetap bisa input manual |
| `POST_NOTIFICATIONS` (Android 13+) | Modul 2, 3, 4 | Reminder tugas & jatuh tempo; jika ditolak, reminder tetap tampil sebagai in-app badge |
| SAF document picker | Modul 3, 9 | Tidak butuh permission storage klasik di Android 10+ |

### 6.4 Kinerja & Responsivitas
* Antarmuka wajib tetap responsif (target *frame rate* stabil, idealnya 60 FPS) pada perangkat Android *entry-level* (RAM 3–4GB), konsisten dengan standar performa yang sudah dipegang TrackIt untuk modul-modul lain.
* Minimum SDK: Android 7.0 (API 24); Target SDK: Android 14+ (API 34+).

### 6.5 Offline-First Resilience
* 100% fungsionalitas inti (tugas, pencatatan pengeluaran, kalkulator katering, berkas) berjalan tanpa internet.
* Backup/restore manual via file terenkripsi ke penyimpanan lokal atau Google Drive pribadi.

### 6.6 Aksesibilitas (baru)
* Seluruh komponen interaktif memiliki `contentDescription` untuk TalkBack.
* Kontras warna minimum sesuai WCAG AA, khususnya pada indikator status merah/hijau (jangan hanya mengandalkan warna — sertakan ikon/teks).

### 6.7 Lokalisasi (baru)
* Rilis awal: Bahasa Indonesia sepenuhnya (termasuk format mata uang Rupiah & tanggal lokal).
* Teks disiapkan agar mudah diterjemahkan sejak awal (mengikuti pendekatan string resource yang sudah dipakai TrackIt) untuk mempermudah ekspansi bahasa daerah/Inggris pasca-MVP.

### 6.8 Strategi Testing (baru)
* Unit test untuk seluruh business logic kritikal: kalkulator katering (§5 Modul 8), perhitungan progress bar anggaran (§5 Modul 1), validasi form pengeluaran (§5 Modul 4).
* Test migrasi skema mengikuti standar yang sudah dipakai TrackIt (§4.1).
* UI test untuk flow onboarding profil dan tambah pengeluaran, memakai framework testing yang sudah dipakai di codebase TrackIt.
* Manual QA checklist khusus perangkat entry-level (RAM 3GB) untuk validasi NFR §6.4.

---

## 7. Rencana Rilis & Tahapan Pengembangan (Sprints)

| Sprint | Fokus Pengembangan | Deliverables Utama |
| --- | --- | --- |
| **Sprint 1** | *Core Framework & Dashboard* | Entity Room DB + Migration setup, Mode Switcher TrackIt, Setup Navigasi (mengikuti UI toolkit TrackIt existing, §3), Dashboard & Countdown Engine, Keystore encryption setup (§3.1, §6.2), Onboarding profil termasuk pilihan `culturalPreset`/`religionDetail` |
| **Sprint 2** | *Legalitas & Finansial* | Modul Berkas KUA/Dukcapil (termasuk checklist per `religionDetail`), Budgeting & Termin Tracker (form manual), Permission flow (§6.3), Privacy policy & consent screen (§6.1) |
| **Sprint 3** | *Rundown, Tamu & Katering* | Checklist Timeline Task berbasis preset adat (editable penuh), Multi-Event Rundown user-defined + Export Engine, Modul Tamu + Kalkulator Porsi Katering (rumus revisi §5 Modul 8) |
| **Sprint 4** | *Tradisi, Vendor & Hardening* | Modul Seserahan/Mahar/Uang Adat (multi-preset), Panitia Keluarga & Seragam (multi-preset), Vendor Hub, unit/UI test coverage (§6.8) |
| **Sprint 5 (baru)** | *QA Menyeluruh & Rilis* | Alpha testing, regression testing lintas device entry-level, migration test validation, Data Safety form Play Store, bug squashing, staged rollout |

### Definition of Done per Modul
Sebuah modul dianggap selesai jika: (1) memenuhi seluruh acceptance criteria di §5, (2) unit test kritikal lulus, (3) tidak ada P0/P1 bug terbuka, (4) lolos review aksesibilitas dasar (§6.6), (5) data yang disimpan sudah terenkripsi sesuai §6.2 bila menyangkut data sensitif, dan (6) khusus Modul 2, 5, 6, 7: tersedia minimal 3–4 preset adat berbeda (bukan hanya Jawa) dan seluruh item preset terbukti bisa ditambah/diubah/dihapus oleh pengguna (uji dengan skenario adat campuran).

---

## Lampiran A: Draf Konten Preset Adat (Starting Point untuk Sprint 3–4)

> **Status: draf awal, bukan sumber final.** Isinya disusun untuk mempercepat kerja desain/development (seeding data `culturalPreset`) agar tidak mulai dari nol, tapi **wajib divalidasi** dengan riset pengguna/narasumber adat sebelum jadi data produksi — praktik adat sangat bervariasi antar sub-etnis, daerah, strata sosial, dan bahkan keluarga (mis. adat Jawa Solo berbeda dengan Jawa Banyuwangi; adat Batak Toba berbeda dengan Batak Karo/Simalungun/Mandailing). Anggap tabel ini sebagai *template default yang di-generate saat pengguna memilih preset*, lalu 100% bisa diedit sesuai §1 Prinsip Kustomisasi.
>
> Sembilan preset di bawah mewakili kelompok adat dengan populasi/permintaan terbesar berdasarkan sebaran penduduk Indonesia; preset lain ditambahkan bertahap pasca-MVP berdasarkan permintaan pengguna nyata (lihat §2.3 Risks).

### A.1 Preset: Jawa (mayoritas Muslim)
| Aspek | Item Default |
| --- | --- |
| Rundown acara | Pasang Tarub & Bleketepe → Siraman → Midodareni → Akad Nikah → Panggih/Temu Manten → Resepsi |
| Mahar/uang adat | Seperangkat alat sholat, emas (gramasi custom), atau uang tunai simbolis |
| Item seserahan khas | Perlengkapan ibadah, kain batik, seperangkat kosmetik, buah-buahan/jajanan pasar |
| Peran panitia khas | Pagar Ayu & Pagar Bagus (among tamu), among tamu VIP |

### A.2 Preset: Sunda (mayoritas Muslim)
| Aspek | Item Default |
| --- | --- |
| Rundown acara | Ngeuyeuk Seureuh → Siraman → Ngeningan → Akad Nikah → Sawer Panganten → Resepsi |
| Mahar/uang adat | Seperangkat alat sholat atau emas, mirip preset Jawa |
| Item seserahan khas | Seperangkat sirih (mangle), pakaian, kosmetik |
| Peran panitia khas | Juru sawer (pemandu prosesi sawer) |

### A.3 Preset: Batak (Toba — representatif, sub-etnis lain seperti Karo/Simalungun/Mandailing berbeda dan perlu preset terpisah pasca-MVP)
| Aspek | Item Default |
| --- | --- |
| Rundown acara | Marhori-hori Dinding/Marhusip (penjajakan) → Martonggo Raja (musyawarah adat keluarga besar) → Pemberkatan (umumnya Kristen/Katolik, mengikuti `religionDetail`) → Pesta Unjuk/Ulaon Unjuk (resepsi adat) |
| Mahar/uang adat | **Sinamot** (uang adat dari pihak pria ke keluarga wanita — nominal dirundingkan saat Martonggo Raja), pemberian **Ulos** sebagai simbol adat |
| Item seserahan khas | Ulos untuk beberapa pihak keluarga (bukan seserahan barang seperti adat Jawa) |
| Peran panitia khas | **Suhut** (tuan rumah pesta/pihak yang punya hajat), **Dongan Tubu** (kerabat semarga), **Hula-hula** dan **Boru** (kelompok kekerabatan pihak istri/menantu) — ini struktur kekerabatan adat, bukan sekadar panitia acara |

### A.4 Preset: Minangkabau (matrilineal, mayoritas Muslim)
| Aspek | Item Default |
| --- | --- |
| Rundown acara | Maresek (penjajakan) → Manisiak/lamaran resmi (khas: dilakukan pihak perempuan ke pihak laki-laki) → Batimbang Tando (tukar tanda) → Akad Nikah → Manjapuik Marapulai (menjemput mempelai pria) → Basandiang (bersanding) → Baralek (resepsi) |
| Mahar/uang adat | Uang jemputan/**Bajapuik** (bervariasi per daerah di Sumatra Barat, ada yang menerapkan ada yang tidak — jadikan opsional, bukan wajib di preset) |
| Item seserahan khas | Barang hantaran standar + carano (wadah adat sirih) |
| Peran panitia khas | **Bundo Kanduang** (perwakilan adat perempuan), **Niniak Mamak** (pemuka adat) |

### A.5 Preset: Bugis-Makassar (mayoritas Muslim)
| Aspek | Item Default |
| --- | --- |
| Rundown acara | Mappesek-pesek (penjajakan) → Massuro/Madduta (lamaran resmi) → Mappacci (malam pacar/inai) → Akad Nikah → Resepsi |
| Mahar/uang adat | **Uang Panai** (uang belanja adat dari pihak pria ke keluarga wanita, nominal bisa signifikan — dorong pengguna mengisi field custom sesuai kesepakatan keluarga) + **Sompa** (mahar simbolis terpisah dari Uang Panai) |
| Item seserahan khas | Standar + perlengkapan Mappacci (daun pacar/inai, lilin) |
| Peran panitia khas | — (gunakan preset umum, tambahkan manual jika perlu) |

### A.6 Preset: Bali (mayoritas Hindu)
| Aspek | Item Default |
| --- | --- |
| Rundown acara | Mapadik (lamaran) → Mesangih/Metatah (potong gigi, umumnya sebelum menikah, bisa juga sebelum acara ini) → Upacara Pawiwahan (upacara pernikahan adat-agama Hindu) → Mejauman (kunjungan resmi ke rumah orang tua pihak wanita pasca menikah) |
| Mahar/uang adat | Umumnya tidak menonjolkan konsep "mahar" seperti adat lain — biarkan field kosong/opsional |
| Item seserahan khas | Sarana upacara (banten/sesajen) — kategori terpisah dari seserahan barang konsumtif |
| Peran panitia khas | **Pemangku** (pemimpin upacara keagamaan) |

### A.7 Preset: Betawi (mayoritas Muslim)
| Aspek | Item Default |
| --- | --- |
| Rundown acara | Ngedelengin (penjajakan) → Melamar → Bawa Tande Putus → Akad Nikah → **Palang Pintu** (pantun & silat di depan pintu) → Resepsi |
| Mahar/uang adat | Seperangkat alat sholat/emas, mirip preset umum Muslim |
| Item seserahan khas | Sirih Dare (wadah sirih hias), **Roti Buaya** (simbol kesetiaan, khas Betawi) |
| Peran panitia khas | Pemain Palang Pintu (silat & pantun) |

### A.8 Preset: Tionghoa-Peranakan (agama bervariasi: Buddha, Konghucu, Kristen, Katolik — ikuti `religionDetail`)
| Aspek | Item Default |
| --- | --- |
| Rundown acara | **Sangjit** (pertukaran hadiah formal antar keluarga sebelum menikah) → **Teh Pai** (upacara minum teh menghormati orang tua & keluarga besar) → Akad/Pemberkatan (sesuai `religionDetail`) → Resepsi/Gala Dinner |
| Mahar/uang adat | Angpao adat sebagai bagian dari Sangjit, nominal & simbolisme bervariasi per keluarga |
| Item seserahan khas | Sepasang lilin naga-phoenix, kue mangkok/kue adat, buah-buahan (jumlah genap dianggap membawa keberuntungan) |
| Peran panitia khas | — (gunakan preset umum) |

### A.9 Preset: Modern / Tanpa Adat Khusus
| Aspek | Item Default |
| --- | --- |
| Rundown acara | Lamaran/Engagement → Akad Nikah/Pemberkatan → Resepsi |
| Mahar/uang adat | Field kosong/opsional, custom bebas |
| Item seserahan khas | Minimal, generik (opsional — beberapa pasangan modern melewati seserahan sama sekali) |
| Peran panitia khas | Preset umum saja (Saksi, Among Tamu) |

### A.10 Adat Campuran (Pernikahan Lintas Etnis)
Ketika `culturalPresetGroom` ≠ `culturalPresetBride`, aplikasi **menggabungkan kedua daftar preset** (rundown, uang adat, seserahan, panitia dari kedua sisi) sebagai starting point, ditandai label asal (mis. "Sinamot — dari adat CPP" dan "Mappacci — dari adat CPW"), lalu pengguna menghapus/menyesuaikan sesuai kesepakatan keluarga. Ini menghindari aplikasi memaksakan salah satu adat sebagai "default" saat kedua pihak berbeda latar belakang.