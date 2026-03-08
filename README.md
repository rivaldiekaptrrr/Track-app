# 📱 TrackIt — Smart Expense Tracker

> Aplikasi pencatatan keuangan pribadi berbasis Android dengan teknologi AI on-device untuk pemindaian struk otomatis, keamanan biometrik, dan notifikasi anggaran cerdas.

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

**TrackIt** adalah aplikasi pencatatan keuangan pribadi yang dirancang untuk menghilangkan hambatan dalam mencatat pengeluaran. Dengan memanfaatkan **AI on-device** (ML Kit), pengguna cukup memotret struk belanja dan nominal akan terisi otomatis — tanpa perlu mengetik manual.

### Target Pengguna
- 🎓 Mahasiswa yang ingin mengelola uang saku bulanan
- 💼 Pekerja kantoran yang ingin mengontrol anggaran
- 🔒 Pengguna yang peduli privasi — semua data disimpan offline di perangkat

---

## ✨ Fitur Utama

### 🔴 Fase 1 — Core (Fondasi Utama)

| Fitur | Deskripsi |
|-------|-----------|
| **Dashboard** | Menampilkan total pengeluaran bulan ini, sisa anggaran, progress bar, dan daftar transaksi terbaru |
| **CRUD Transaksi** | Tambah, lihat, edit, dan hapus transaksi pengeluaran |
| **Kategori Pengeluaran** | 8 kategori bawaan: Makanan, Transportasi, Hiburan, Tagihan, Belanja, Kesehatan, Pendidikan, Lainnya |
| **Visualisasi Data** | Donut pie chart menampilkan persentase pengeluaran per kategori dengan detail breakdown |
| **Penyimpanan Offline** | Semua data tersimpan di Room Database lokal — tidak memerlukan internet |
| **Dark/Light Mode** | Otomatis mengikuti pengaturan tema sistem Android dengan Material 3 Dynamic Color |

### 🟡 Fase 2 — Smart (Otomatisasi dengan AI)

| Fitur | Deskripsi |
|-------|-----------|
| **Smart Scan (OCR)** | Memotret struk belanja menggunakan CameraX dan memproses gambar secara offline |
| **Ekstraksi Teks Otomatis** | ML Kit Text Recognition mendeteksi angka terbesar dari struk sebagai total belanja |
| **Auto-fill Form** | Nominal dari struk otomatis terisi ke form transaksi |
| **Tanggal Otomatis** | Tanggal default diset ke hari ini, dapat diubah via Material 3 DatePicker |

### 🟢 Fase 3 — Security (Keamanan & Privasi)

| Fitur | Deskripsi |
|-------|-----------|
| **Biometric Login** | Autentikasi sidik jari / face unlock menggunakan BiometricPrompt API sebelum akses dashboard |
| **Privacy Screen** | `FLAG_SECURE` mencegah konten aplikasi terlihat di layar Recent Apps / multitasking |
| **Graceful Degradation** | Jika perangkat tidak mendukung biometrik, lock screen dilewati otomatis |

### 🔵 Fase 4 — Advanced (Laporan & Notifikasi)

| Fitur | Deskripsi |
|-------|-----------|
| **Export PDF** | Mengekspor laporan transaksi bulanan ke format PDF (A4) menggunakan `PdfDocument` API |
| **Budget Alert** | WorkManager mengecek pengeluaran setiap 6 jam — notifikasi lokal dikirim jika ≥ 80% anggaran |
| **Transaksi Berulang** | Mendukung transaksi otomatis harian/mingguan/bulanan via WorkManager |
| **Share PDF** | Laporan PDF dapat langsung dibagikan via Intent (WhatsApp, Email, dll) |

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
| **Machine Learning** | Google ML Kit Text Recognition (On-Device) |
| **Kamera** | CameraX 1.3.1 |
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
│  ┌──────────┐  ┌──────────┐  ┌─────────────┐  │
│  │ ML Kit   │  │ CameraX  │  │ WorkManager │  │
│  │ (OCR)    │  │ (Kamera) │  │ (Bg Tasks)  │  │
│  └──────────┘  └──────────┘  └─────────────┘  │
└────────────────────────────────────────────────┘
```

### Aliran Data

```
User Action → Composable → ViewModel → Repository → Room DAO → SQLite
                                              ↕
                                        Flow (Reactive)
                                              ↕
UI Update ← Composable ← StateFlow ← ViewModel ← Repository
```

---

## 📁 Struktur Proyek

```
app/src/main/
├── AndroidManifest.xml          # Perizinan & komponen
├── java/com/trackit/app/
│   ├── TrackItApp.kt            # @HiltAndroidApp + WorkManager config
│   ├── MainActivity.kt          # Entry point, biometric, FLAG_SECURE, PDF export
│   │
│   ├── data/
│   │   ├── local/
│   │   │   ├── TrackItDatabase.kt        # Room DB + default categories
│   │   │   ├── dao/
│   │   │   │   ├── TransactionDao.kt     # CRUD + query agregasi bulanan
│   │   │   │   ├── CategoryDao.kt        # CRUD kategori
│   │   │   │   └── BudgetSettingDao.kt   # Pengaturan anggaran
│   │   │   └── entity/
│   │   │       ├── TransactionEntity.kt  # Tabel transaksi
│   │   │       ├── CategoryEntity.kt     # Tabel kategori
│   │   │       └── BudgetSettingEntity.kt# Tabel anggaran (singleton)
│   │   └── repository/
│   │       ├── TransactionRepository.kt  # Abstraksi akses transaksi
│   │       ├── CategoryRepository.kt     # Abstraksi akses kategori
│   │       └── BudgetRepository.kt       # Abstraksi akses anggaran
│   │
│   ├── di/
│   │   └── DatabaseModule.kt    # Hilt module: DB + DAO providers
│   │
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Color.kt         # Palet warna Light/Dark + Chart colors
│   │   │   ├── Theme.kt         # Material 3 theme + Dynamic Color
│   │   │   └── Type.kt          # Typography definitions
│   │   ├── navigation/
│   │   │   ├── Screen.kt        # Route definitions
│   │   │   └── TrackItNavHost.kt# NavHost dengan semua route
│   │   ├── dashboard/
│   │   │   ├── DashboardScreen.kt   # Halaman utama
│   │   │   └── DashboardViewModel.kt# State management dashboard
│   │   ├── transaction/
│   │   │   ├── AddEditTransactionScreen.kt # Form tambah/edit transaksi
│   │   │   └── TransactionViewModel.kt    # State management form
│   │   ├── chart/
│   │   │   ├── ChartScreen.kt       # Pie chart + breakdown
│   │   │   └── ChartViewModel.kt    # Data agregasi chart
│   │   ├── scan/
│   │   │   └── ScanReceiptScreen.kt # Kamera + OCR
│   │   ├── settings/
│   │   │   ├── SettingsScreen.kt    # Pengaturan anggaran & PDF
│   │   │   └── SettingsViewModel.kt # State management settings
│   │   └── biometric/
│   │       └── BiometricLockScreen.kt # Halaman biometric lock
│   │
│   ├── worker/
│   │   ├── BudgetCheckWorker.kt          # Cek anggaran periodik
│   │   └── RecurringTransactionWorker.kt # Auto-insert transaksi berulang
│   │
│   └── util/
│       ├── DateUtils.kt         # Format tanggal Indonesia + helper
│       ├── CurrencyUtils.kt     # Format Rupiah (Rp)
│       ├── CategoryIconMapper.kt# Mapping ikon & warna kategori
│       └── PdfExporter.kt       # Generate & share PDF laporan
│
└── res/
    ├── values/
    │   ├── strings.xml          # String resources (Bahasa Indonesia)
    │   └── themes.xml           # Tema Android (base)
    └── xml/
        └── file_paths.xml       # FileProvider config untuk PDF
```

---

## 🔄 Alur Pengguna

### Alur 1: Menambah Transaksi Manual
```
Buka App → Biometric Auth → Dashboard → Tekan FAB (+)
→ Isi Nominal → Pilih Kategori → Pilih Tanggal → Simpan
→ Kembali ke Dashboard (data otomatis ter-update)
```

### Alur 2: Menambah Transaksi via Struk (OCR)
```
Buka App → Biometric Auth → Dashboard → Tekan FAB (+)
→ Tekan ikon Kamera 📷 → Arahkan ke struk → Ambil foto
→ ML Kit memproses offline → Nominal terisi otomatis
→ Pilih Kategori → Simpan → Dashboard ter-update
```

### Alur 3: Melihat Statistik
```
Dashboard → Tekan ikon Pie Chart → Lihat donut chart
→ Lihat breakdown per kategori dengan persentase
```

### Alur 4: Ekspor PDF
```
Dashboard → Settings → Tekan "Ekspor PDF Bulan Ini"
→ PDF dibuat → Dialog share muncul
→ Kirim via WhatsApp / Email / simpan
```

### Alur 5: Notifikasi Budget Alert
```
(Background) WorkManager cek setiap 6 jam
→ Jika pengeluaran ≥ 80% anggaran
→ Notifikasi lokal dikirim: "⚠️ Peringatan Anggaran!"
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
| `date` | Long | Epoch milliseconds |
| `createdAt` | Long | Timestamp pembuatan |
| `isRecurring` | Boolean | Apakah transaksi berulang |
| `recurringType` | String? | `DAILY`, `WEEKLY`, atau `MONTHLY` |
| `recurringDayOfMonth` | Int? | Tanggal untuk recurring bulanan |

### Tabel `categories`
| Kolom | Tipe | Keterangan |
|-------|------|------------|
| `id` | Long (PK) | Auto-increment |
| `name` | String | Nama kategori (misal: "Makanan") |
| `iconName` | String | Nama ikon Material (misal: "restaurant") |
| `colorHex` | String | Warna hex (misal: "#E8963B") |

### Tabel `budget_settings`
| Kolom | Tipe | Keterangan |
|-------|------|------------|
| `id` | Int (PK) | Selalu `1` (singleton) |
| `monthlyBudget` | Double | Batas anggaran bulanan |

### Kategori Default (Pre-populated)
| Nama | Ikon | Warna |
|------|------|-------|
| Makanan | 🍴 restaurant | #E8963B |
| Transportasi | 🚗 directions_car | #3D6373 |
| Hiburan | 🎬 movie | #C24D6E |
| Tagihan | 🧾 receipt_long | #7B61D9 |
| Belanja | 🛍 shopping_bag | #1B6B4F |
| Kesehatan | 🏥 local_hospital | #4EADAD |
| Pendidikan | 🎓 school | #D4A843 |
| Lainnya | ⋯ more_horiz | #8B6BB5 |

---

## 🚀 Cara Menjalankan

### Prasyarat
- **Android Studio** Hedgehog (2023.1.1) atau lebih baru
- **JDK 17**
- **Android SDK** API 26-34
- Perangkat Android / Emulator (API 26+)

### Langkah-langkah
1. **Clone / Buka** folder `Track_it` di Android Studio
2. **Tunggu Gradle Sync** selesai (unduh dependency otomatis)
3. **Hubungkan** perangkat Android via USB atau jalankan Emulator
4. **Klik Run** ▶️ pada konfigurasi `app`
5. Aplikasi akan terinstall dan terbuka dengan layar biometric

### Build APK
```bash
# Debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔐 Konfigurasi & Perizinan

### Perizinan Android (AndroidManifest.xml)
| Perizinan | Kegunaan |
|-----------|----------|
| `CAMERA` | Memotret struk untuk OCR |
| `USE_BIOMETRIC` | Autentikasi sidik jari / face unlock |
| `POST_NOTIFICATIONS` | Mengirim notifikasi budget alert |

### Komponen Manifest
| Komponen | Kegunaan |
|----------|----------|
| `FileProvider` | Berbagi file PDF via Intent |
| `InitializationProvider` | Disable default WorkManager (Hilt mengelola) |

---

## 📸 Screenshot Fitur

> **Catatan:** Screenshot dapat ditambahkan setelah build pertama berhasil.

| Layar | Deskripsi |
|-------|-----------|
| Biometric Lock | Layar autentikasi sidik jari dengan gradient background |
| Dashboard | Summary card gradient, progress bar anggaran, daftar transaksi |
| Tambah Transaksi | Form input nominal (+ tombol kamera), kategori grid, date picker |
| Statistik | Donut pie chart + breakdown per kategori |
| Pindai Struk | Camera preview dengan tombol capture |
| Pengaturan | Konfigurasi anggaran, ekspor PDF, info aplikasi |

---

## 📄 Lisensi

Proyek ini dikembangkan untuk keperluan internal / submission akademik.

---

*Dibuat dengan ❤️ menggunakan Kotlin, Jetpack Compose, dan Material Design 3*
