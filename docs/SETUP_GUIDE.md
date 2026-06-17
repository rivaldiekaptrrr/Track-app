# 🚀 Panduan Setup & Menjalankan TrackIt

Dokumen ini menjelaskan secara lengkap semua yang perlu disiapkan dan langkah-langkah untuk menjalankan aplikasi **TrackIt — Smart Expense Tracker** dari awal hingga aplikasi berjalan di perangkat Android.

---

## 📋 Daftar Isi

1. [Prasyarat yang Harus Disiapkan](#1--prasyarat-yang-harus-disiapkan)
2. [Instalasi Android Studio](#2--instalasi-android-studio)
3. [Konfigurasi SDK & Tools](#3--konfigurasi-sdk--tools)
4. [Membuka Proyek](#4--membuka-proyek)
5. [Gradle Sync & Dependency](#5--gradle-sync--dependency)
6. [Menyiapkan Perangkat / Emulator](#6--menyiapkan-perangkat--emulator)
7. [Menjalankan Aplikasi](#7--menjalankan-aplikasi)
8. [Build APK](#8--build-apk)
9. [Troubleshooting](#9--troubleshooting)

---

## 1. 📦 Prasyarat yang Harus Disiapkan

Sebelum memulai, pastikan semua komponen berikut sudah terinstal di komputer Anda:

### Perangkat Lunak (Software)

| No | Komponen | Versi Minimum | Keterangan |
|----|----------|---------------|------------|
| 1 | **Android Studio** | Hedgehog 2023.1.1+ | IDE resmi untuk pengembangan Android |
| 2 | **JDK (Java Development Kit)** | JDK 17 | Biasanya sudah bundled dengan Android Studio |
| 3 | **Git** | 2.x | Untuk version control (opsional) |

### Android SDK Components

| No | Komponen | Versi | Cara Install |
|----|----------|-------|--------------|
| 1 | **Android SDK Platform** | API 34 (Android 14) | SDK Manager → SDK Platforms |
| 2 | **Android SDK Build-Tools** | 34.0.0 | SDK Manager → SDK Tools |
| 3 | **Android SDK Platform-Tools** | Terbaru | SDK Manager → SDK Tools |
| 4 | **Android Emulator** | Terbaru | SDK Manager → SDK Tools (jika pakai emulator) |
| 5 | **Google Play Services** | Terbaru | SDK Manager → SDK Tools |

### Spesifikasi Komputer Minimum

| Komponen | Minimum | Rekomendasi |
|----------|---------|-------------|
| **RAM** | 8 GB | 16 GB |
| **Disk Space** | 10 GB kosong | 20 GB kosong |
| **Prosesor** | Intel i5 / AMD Ryzen 5 | Intel i7 / AMD Ryzen 7 |
| **OS** | Windows 10 64-bit | Windows 11 64-bit |

### Perangkat Android (untuk testing di HP fisik)

| Komponen | Syarat |
|----------|--------|
| **Versi Android** | Minimum Android 8.0 (Oreo / API 26) |
| **Kabel USB** | Kabel data (bukan kabel charging saja) |
| **Developer Options** | Harus diaktifkan di HP |
| **USB Debugging** | Harus diaktifkan di HP |

---

## 2. 💻 Instalasi Android Studio

### Langkah-langkah:

1. **Download** Android Studio dari situs resmi:
   ```
   https://developer.android.com/studio
   ```

2. **Jalankan installer** dan ikuti wizard:
   - Pilih **Standard Installation** (rekomendasi)
   - Android Studio akan otomatis mengunduh SDK, Emulator, dan tools yang diperlukan

3. **Setelah instalasi selesai**, buka Android Studio dan tunggu proses setup awal selesai

4. **Verifikasi JDK** sudah terkonfigurasi:
   - Buka **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**
   - Pastikan **Gradle JDK** mengarah ke **JDK 17** (bundled)

---

## 3. ⚙️ Konfigurasi SDK & Tools

### Melalui SDK Manager:

1. Buka Android Studio → **Tools → SDK Manager**

2. **Tab "SDK Platforms"** — Centang dan install:
   - ✅ Android 14.0 (API 34)
   - ✅ Android 8.0 (API 26) — untuk testing di versi minimum

3. **Tab "SDK Tools"** — Centang dan install:
   - ✅ Android SDK Build-Tools 34
   - ✅ Android SDK Command-line Tools
   - ✅ Android SDK Platform-Tools
   - ✅ Android Emulator (jika menggunakan emulator)
   - ✅ Google Play services

4. Klik **Apply** → tunggu download selesai

### Verifikasi via Terminal (opsional):
```bash
# Cek SDK terinstall
sdkmanager --list

# Cek ADB (Android Debug Bridge)
adb version
```

---

## 4. 📂 Membuka Proyek

### Langkah-langkah:

1. **Buka Android Studio**

2. Pilih **File → Open** (atau "Open" di Welcome Screen)

3. **Navigasi** ke folder proyek:
   ```
   D:\App\Track_it
   ```

4. Klik **OK** — Android Studio akan mendeteksi proyek Gradle secara otomatis

5. Jika muncul dialog **"Trust this project?"** → Klik **Trust Project**

6. Tunggu hingga Android Studio selesai mengindeks file

---

## 5. 🔄 Gradle Sync & Dependency

### Proses Otomatis:

Setelah proyek dibuka, Android Studio akan otomatis menjalankan **Gradle Sync** untuk mengunduh semua dependency. Proses ini memerlukan **koneksi internet**.

### Dependency yang Akan Diunduh:

| Kategori | Library | Ukuran ± |
|----------|---------|----------|
| **Compose** | Compose BOM 2024.01.00, Material 3 | ~30 MB |
| **Room** | Room Runtime, KTX, Compiler | ~5 MB |
| **Hilt** | Hilt Android, Compiler, Navigation | ~10 MB |
| **ML Kit** | Text Recognition (On-Device) | ~20 MB |
| **CameraX** | Core, Camera2, Lifecycle, View | ~15 MB |
| **Biometric** | AndroidX Biometric | ~2 MB |
| **WorkManager** | Work Runtime KTX | ~3 MB |
| **Lainnya** | Navigation, Lifecycle, Core KTX | ~10 MB |
| **Total (±)** | | **~95 MB** |

### Jika Gradle Sync Gagal:

1. **Cek koneksi internet** — semua library diunduh dari Maven Central & Google
2. **File → Sync Project with Gradle Files** — coba sync ulang
3. **File → Invalidate Caches / Restart** — bersihkan cache
4. Pastikan **proxy/firewall** tidak memblokir akses ke:
   - `https://dl.google.com/dl/android/maven2/`
   - `https://repo.maven.apache.org/maven2/`

### Indikator Sync Berhasil:
- Bar progress di bawah hilang
- Tidak ada error merah di panel **Build** output
- Struktur folder `app/src/main/java/com/trackit/app/` terlihat di Project Explorer

---

## 6. 📱 Menyiapkan Perangkat / Emulator

Anda bisa menjalankan aplikasi di **HP fisik** atau **Emulator**. Berikut cara menyiapkan keduanya:

### Opsi A: HP Fisik (Rekomendasi untuk fitur Biometric & Kamera)

#### Langkah 1 — Aktifkan Developer Options:
1. Buka **Settings → About Phone**
2. Ketuk **Build Number** sebanyak **7 kali** berturut-turut
3. Muncul pesan: *"You are now a developer!"*

#### Langkah 2 — Aktifkan USB Debugging:
1. Buka **Settings → Developer Options**
2. Aktifkan **USB Debugging** → Klik **OK** pada dialog konfirmasi

#### Langkah 3 — Hubungkan ke Komputer:
1. Sambungkan HP ke komputer menggunakan **kabel USB data**
2. Pada HP, jika muncul dialog *"Allow USB Debugging?"* → Centang **Always allow** → Klik **Allow**
3. Di Android Studio, HP Anda akan muncul di dropdown **device selector** di toolbar atas

#### Langkah 4 — Install USB Driver (jika HP tidak terdeteksi):
| Merek HP | Driver |
|----------|--------|
| Samsung | Samsung USB Driver (`https://developer.samsung.com/android-usb-driver`) |
| Xiaomi | Mi USB Driver |
| OPPO/Vivo/Realme | MediaTek / Qualcomm USB Driver |
| Google Pixel | Google USB Driver (via SDK Manager) |

---

### Opsi B: Emulator Android

#### Langkah 1 — Buat Virtual Device:
1. Buka **Tools → Device Manager** di Android Studio
2. Klik **Create Device**
3. Pilih hardware: **Pixel 6** (rekomendasi)
4. Klik **Next**

#### Langkah 2 — Pilih System Image:
1. Pilih **API 34 (Android 14)** — klik **Download** jika belum ada
2. Pilih varian **x86_64** (lebih cepat)
3. Klik **Next → Finish**

#### Langkah 3 — Konfigurasi Emulator (opsional):
- **RAM**: Set minimal 2 GB
- **Internal Storage**: Set minimal 2 GB
- **Camera**: Pilih **Emulated** atau **Webcam** (untuk testing OCR)

#### ⚠️ Catatan Emulator:
| Fitur | Dukungan di Emulator |
|-------|---------------------|
| Kamera / OCR | ✅ Bisa (dengan webcam atau file gambar) |
| Biometric | ⚠️ Terbatas (bisa disimulasikan via Extended Controls → Fingerprint) |
| Dark Mode | ✅ Bisa (Settings → Display → Dark Theme) |
| Notifikasi | ✅ Bisa |

---

## 7. ▶️ Menjalankan Aplikasi

### Langkah-langkah:

1. **Pastikan perangkat terhubung** — muncul di device selector toolbar

2. **Pilih konfigurasi** `app` di dropdown Run Configuration (biasanya sudah default)

3. **Klik tombol Run** ▶️ (atau tekan `Shift + F10`)

4. **Tunggu proses build** — build pertama bisa memakan waktu **2-5 menit**

5. **APK otomatis terinstall** dan aplikasi akan terbuka di perangkat

### Yang Terjadi Saat Pertama Kali Dibuka:

```
1. Layar Biometric Lock muncul (jika perangkat mendukung)
   → Scan sidik jari / face unlock

2. Masuk ke Dashboard (kosong, belum ada transaksi)
   → Total pengeluaran: Rp 0

3. 8 kategori default sudah terisi otomatis:
   Makanan, Transportasi, Hiburan, Tagihan,
   Belanja, Kesehatan, Pendidikan, Lainnya

4. WorkManager sudah terjadwal:
   → Budget Check: setiap 6 jam
   → Recurring Transaction: setiap 24 jam
```

### Verifikasi Fitur:

| No | Test | Cara Verifikasi |
|----|------|-----------------|
| 1 | Dashboard | Buka app → lihat summary card & daftar kosong |
| 2 | Tambah Transaksi | Tekan FAB (+) → isi form → simpan → cek dashboard |
| 3 | Edit Transaksi | Tekan transaksi di daftar → ubah → simpan |
| 4 | Hapus Transaksi | Tekan ikon 🗑 → konfirmasi → cek dashboard |
| 5 | OCR Scan | Tekan kamera di form → potret struk → cek nominal |
| 6 | Pie Chart | Tekan ikon chart → lihat grafik |
| 7 | Dark Mode | Ubah tema sistem ke Dark → cek tampilan berubah |
| 8 | Biometric | Kill app → buka lagi → cek prompt biometric |
| 9 | Budget Alert | Set anggaran rendah → tambah transaksi > 80% → tunggu notif |
| 10 | Export PDF | Settings → Ekspor PDF → cek file PDF |

---

## 8. 📦 Build APK

### Debug APK (untuk testing):
```bash
# Di terminal / PowerShell, dari folder proyek:
cd D:\App\Track_it
.\gradlew assembleDebug
```
**Output:** `app/build/outputs/apk/debug/app-debug.apk`

### Release APK (untuk distribusi):
```bash
# Buat keystore terlebih dahulu (sekali saja):
keytool -genkey -v -keystore trackit-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias trackit

# Build release:
.\gradlew assembleRelease
```
**Output:** `app/build/outputs/apk/release/app-release.apk`

> ⚠️ Untuk release APK, Anda perlu mengkonfigurasi signing di `app/build.gradle.kts` dengan keystore yang dibuat.

### Install APK Manual:
```bash
# Via ADB (HP terhubung via USB):
adb install app/build/outputs/apk/debug/app-debug.apk

# Atau kirim file APK ke HP via:
# - WhatsApp / Telegram
# - Google Drive
# - Kabel USB (copy file)
```

---

## 9. 🔧 Troubleshooting

### Masalah Umum & Solusinya:

| No | Masalah | Solusi |
|----|---------|-------|
| 1 | **Gradle Sync gagal** | Cek internet → File → Sync Project with Gradle Files |
| 2 | **"SDK location not found"** | Buat file `local.properties` di root proyek dengan isi: `sdk.dir=C\:\\Users\\[USER]\\AppData\\Local\\Android\\Sdk` |
| 3 | **Build error "JDK not found"** | File → Settings → Gradle → Set Gradle JDK ke JDK 17 |
| 4 | **HP tidak terdeteksi** | Install USB driver → aktifkan USB Debugging → cabut & colokkan ulang kabel |
| 5 | **Emulator lambat/hang** | Aktifkan **Hardware Acceleration** (HAXM/Hyper-V) di BIOS |
| 6 | **"INSTALL_FAILED_OLDER_SDK"** | HP Anda di bawah API 26. Gunakan HP dengan Android 8.0+ |
| 7 | **Kamera tidak berfungsi** | Berikan izin kamera secara manual di Settings HP → Apps → TrackIt → Permissions |
| 8 | **Biometric tidak muncul** | HP tidak punya sidik jari/face unlock terdaftar. Daftarkan terlebih dahulu di Settings HP |
| 9 | **Notifikasi tidak muncul** | Android 13+ memerlukan izin notifikasi. Berikan izin saat dialog muncul |
| 10 | **"Cannot resolve symbol"** | File → Invalidate Caches → Restart Android Studio |

### Membuat `local.properties` (jika belum ada):

Buat file `local.properties` di root folder proyek (`D:\App\Track_it\`) dengan isi:

```properties
# Sesuaikan path SDK dengan lokasi di komputer Anda
sdk.dir=C\:\\Users\\Admin\\AppData\\Local\\Android\\Sdk
```

> **Cara mengetahui lokasi SDK:** Android Studio → Tools → SDK Manager → lihat "Android SDK Location" di bagian atas.

---

## ✅ Checklist Kesiapan

Gunakan checklist ini untuk memastikan semuanya siap sebelum menjalankan aplikasi:

- [ ] Android Studio terinstal (versi Hedgehog 2023.1.1+)
- [ ] JDK 17 terkonfigurasi
- [ ] Android SDK API 34 terinstal
- [ ] Android SDK Build-Tools 34 terinstal
- [ ] Proyek sudah dibuka di Android Studio
- [ ] Gradle Sync berhasil (tidak ada error)
- [ ] Perangkat Android terhubung ATAU emulator sudah dibuat
- [ ] USB Debugging aktif (jika pakai HP fisik)
- [ ] File `local.properties` ada dengan path SDK yang benar
- [ ] Koneksi internet tersedia (untuk Gradle Sync pertama kali)

---

*Jika mengalami kendala yang tidak tercantum di atas, silakan buat issue atau hubungi developer.*
