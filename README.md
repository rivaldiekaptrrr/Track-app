# 🎤 TrackIt — Smart Voice Expense Tracker

> Aplikasi pencatatan keuangan pribadi berbasis Android dengan teknologi **Offline Voice Tracking** (Speech-to-Text), **Natural Machine Learning** untuk kategorisasi otomatis, keamanan biometrik, dan notifikasi anggaran cerdas.

---

## 📋 Daftar Isi

- [Tentang Aplikasi](#-tentang-aplikasi)
- [Fitur Utama](#-fitur-utama)
- [Tech Stack](#-tech-stack)
- [Arsitektur Sistem](#-arsitektur-sistem)
- [Struktur Proyek](#-struktur-proyek)
- [Alur Pengguna](#-alur-pengguna)
- [Skema Database](#-skema-database)
- [Cara Menjalankan](#-cara-menjalankan)
- [Konfigurasi & Perizinan](#-konfigurasi--perizinan)
- [Screenshot Fitur](#-screenshot-fitur)

---

## 🎯 Tentang Aplikasi

**TrackIt** adalah aplikasi pencatatan keuangan pribadi yang dirancang untuk menghilangkan hambatan dalam mencatat pemasukan dan pengeluaran. Dengan memanfaatkan **Android SpeechRecognizer secara Offline**, pengguna cukup mengucapkan transaksi menggunakan bahasa natural (misal: *"beli sayur 50 ribu"* atau *"dapat gaji 5 juta"*) dan form akan terisi otomatis beserta kategorinya!

### Target Pengguna
- 🎓 Mahasiswa yang ingin mengelola uang saku bulanan dengan praktis.
- 💼 Pekerja kantoran yang sibuk dan ingin mencatat pengeluaran secepat berbicara.
- 🔒 Pengguna yang peduli privasi — semua proses pengenalan suara dan data disimpan **100% offline** di perangkat.

---

## ✨ Fitur Utama

### 🔴 Fase 1 — Core (Fondasi Utama)

| Fitur | Deskripsi |
|-------|-----------|
| **Pemasukan & Pengeluaran** | Mendukung pencatatan dua tipe transaksi dengan filter UI pintar. |
| **Dashboard** | Menampilkan total pengeluaran bulan ini, sisa anggaran, progress bar, dan daftar transaksi terbaru. |
| **Kategori Transaksi** | 12 kategori bawaan (8 Pengeluaran, 4 Pemasukan). |
| **Visualisasi Data** | Donut pie chart menampilkan persentase pengeluaran per kategori. |
| **Penyimpanan Offline** | Semua data tersimpan di Room Database lokal — tidak memerlukan internet. |

### 🟡 Fase 2 — Smart (Otomatisasi Suara)

| Fitur | Deskripsi |
|-------|-----------|
| **Offline Voice Tracking** | Pengenalan suara tanpa internet menggunakan Android SpeechRecognizer. |
| **Natural Language Parser** | Memahami bahasa gaul ("gocap", "cepek", "gopek") dan angka huruf ("dua juta"). |
| **Machine Learning Natural** | Aplikasi otomatis "belajar" kata kunci baru jika pengguna memilih kategori secara manual setelah ucapan gagal dikenali. |
| **Voice Feedback (TTS)** | Robot asisten membacakan konfirmasi setelah transaksi sukses tersimpan. |
| **Quick Settings Tile** | Pintasan menambahkan transaksi suara langsung dari panel notifikasi (status bar) tanpa membuka aplikasi! |

### 🟢 Fase 3 — Security (Keamanan & Privasi)

| Fitur | Deskripsi |
|-------|-----------|
| **Biometric Login** | Autentikasi sidik jari / face unlock sebelum akses dashboard. |
| **Privacy Screen** | `FLAG_SECURE` mencegah konten aplikasi terlihat di layar Recent Apps. |

### 🔵 Fase 4 — Advanced (Laporan & Notifikasi)

| Fitur | Deskripsi |
|-------|-----------|
| **Export PDF** | Laporan bulanan ke format PDF (A4). |
| **Budget Alert** | Notifikasi otomatis dikirim jika ≥ 80% anggaran tercapai. |
| **Transaksi Berulang** | Transaksi otomatis harian/mingguan/bulanan via WorkManager. |

---

## 🛠 Tech Stack

| Komponen | Teknologi |
|----------|-----------|
| **Bahasa** | Kotlin 1.9.22 |
| **UI Framework** | Jetpack Compose + Material Design 3 |
| **Arsitektur** | MVVM (Model-View-ViewModel) |
| **Local Database** | Room Database 2.6.1 |
| **Dependency Injection** | Hilt (Dagger) 2.50 |
| **Reactive Streams** | Kotlin Coroutines + Flow |
| **Voice Engine** | Android SpeechRecognizer & TextToSpeech (Offline) |
| **Keamanan** | AndroidX Biometric API 1.1.0 |
| **Background Tasks** | WorkManager 2.9.0 |
| **Build System** | Gradle 8.5 + AGP 8.2.2 + KSP |
| **Min SDK** | Android 8.0 (API 26) |
| **Target SDK** | Android 14 (API 34) |

---

## 🏗 Arsitektur Sistem

TrackIt menggunakan arsitektur **MVVM (Model-View-ViewModel)** dengan Clean Architecture principles:

```
┌───────────────────────────────────────────────┐
│                   UI Layer                     │
│  ┌─────────────┐  ┌──────────────────────┐    │
│  │  Composable  │──│    ViewModel         │    │
│  │  Screens     │  │  (StateFlow/Flow)    │    │
│  └─────────────┘  └──────────┬───────────┘    │
│                               │                │
├───────────────────────────────┼────────────────┤
│                   Data Layer  │                │
│  ┌────────────────────────────▼──────────┐     │
│  │          Repository                   │     │
│  │  (Abstraksi akses data)               │     │
│  └────────────────────┬──────────────────┘     │
│                       │                        │
│  ┌────────────────────▼──────────────────┐     │
│  │        Room Database                  │     │
│  │  ┌──────┐  ┌──────────┐  ┌────────┐  │     │
│  │  │ DAOs │  │ Entities │  │Queries │  │     │
│  │  └──────┘  └──────────┘  └────────┘  │     │
│  └───────────────────────────────────────┘     │
│                                                │
├────────────────────────────────────────────────┤
│              External Services                 │
│  ┌───────────────┐  ┌───────────────┐          │
│  │SpeechRecognizer│  │ TextToSpeech  │          │
│  │ (STT Offline)  │  │ (TTS Engine)  │          │
│  └───────────────┘  └───────────────┘          │
└────────────────────────────────────────────────┘
```

---

## 🔄 Alur Pengguna

### Alur 1: Menambah Transaksi via Suara (Ajaib 🪄)
```
Buka App → Biometric Auth → Dashboard → Tekan FAB (+)
→ Tekan ikon Mikrofon 🎤 → Ucapkan: "beli sate ayam 25 ribu"
→ AI memproses kalimat → Nominal terisi 25.000, Kategori terisi Makanan
→ Klik Simpan → TTS berkata: "Tersimpan, pengeluaran Makanan 25.000 rupiah"
```

### Alur 2: Fitur Self-Learning (Belajar Otomatis)
```
Ucapkan: "bayar langganan gym 150 ribu"
→ Nominal terisi 150.000, Kategori KOSONG (karena 'gym' belum dikenal)
→ Anda memilih kategori "Kesehatan" secara manual → Simpan
→ Sistem otomatis menambahkan kata "langganan gym" ke dalam otak kategori "Kesehatan".
→ Besok saat Anda menyebut "gym" lagi, aplikasi otomatis memasukannya ke "Kesehatan"!
```

---

## 🗄 Skema Database

### Tabel `transactions`
| Kolom | Tipe | Keterangan |
|-------|------|------------|
| `id` | Long (PK) | Auto-increment |
| `amount` | Double | Nominal transaksi |
| `description` | String | Deskripsi opsional |
| `categoryId` | Long? (FK) | Referensi ke `categories.id` |
| `type` | String | `INCOME` atau `EXPENSE` |
| `date` | Long | Epoch milliseconds |
| `isRecurring` | Boolean | Apakah transaksi berulang |

### Tabel `categories`
| Kolom | Tipe | Keterangan |
|-------|------|------------|
| `id` | Long (PK) | Auto-increment |
| `name` | String | Nama kategori (misal: "Makanan") |
| `iconName` | String | Nama ikon Material (misal: "restaurant") |
| `colorHex` | String | Warna hex (misal: "#E8963B") |
| `customKeywords`| String | Kata kunci AI (*comma separated*) |
| `type` | String | `INCOME` atau `EXPENSE` |

---

## 🚀 Cara Menjalankan

### Prasyarat
- **Android Studio** Hedgehog (2023.1.1) atau lebih baru
- Perangkat Android / Emulator (API 26+)
- *Catatan: Untuk SpeechRecognizer offline, pastikan sudah mengunduh Bahasa Indonesia di pengaturan HP Anda (Settings > System > Language > Speech).*

### Langkah-langkah
1. **Clone / Buka** folder di Android Studio.
2. **Tunggu Gradle Sync** selesai.
3. **Klik Run** ▶️ pada konfigurasi `app`.
4. Jika HP tidak mendukung offline voice, fitur input manual tetap dapat berjalan sempurna.

---

## 🔐 Konfigurasi & Perizinan

| Perizinan | Kegunaan |
|-----------|----------|
| `RECORD_AUDIO` | Akses mikrofon untuk Speech-to-Text |
| `USE_BIOMETRIC` | Autentikasi sidik jari / face unlock |
| `POST_NOTIFICATIONS` | Mengirim notifikasi budget alert |

---

*Dibuat dengan ❤️ menggunakan Kotlin, Jetpack Compose, dan Material Design 3*
