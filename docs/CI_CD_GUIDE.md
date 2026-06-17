# 🚀 CI/CD & In-App Update — Panduan Lengkap TrackIt

> Dokumentasi ini mencakup semua yang perlu kamu ketahui sebagai developer:
> **bagaimana alur kerja sehari-hari**, **cara merilis ke user**, dan **bagaimana user merasakan proses update** langsung dari dalam aplikasi.

---

## Daftar Isi

- [Gambaran Umum Sistem](#gambaran-umum-sistem)
- [Alur 1 — Developer: Build Internal (tanpa update ke user)](#alur-1--developer-build-internal-tanpa-update-ke-user)
- [Alur 2 — Rilis Resmi: Update ke User](#alur-2--rilis-resmi-update-ke-user)
- [User Flow: Fitur In-App Update](#user-flow-fitur-in-app-update)
- [Catatan Teknis Komponen Updater](#catatan-teknis-komponen-updater)
- [Best Practice & Tips](#best-practice--tips)
- [Troubleshooting](#troubleshooting)

---

## Gambaran Umum Sistem

Sistem CI/CD TrackIt berjalan di **GitHub Actions** dan menggunakan dua alur yang berbeda dalam **satu file workflow** (`.github/workflows/android-build.yml`). Alur dipilih secara otomatis berdasarkan apakah kamu push kode biasa atau push **tag versi** (`v*`).

```
Push ke main  →  Build + Test + APK Artifact  (Internal/Developer)
Push tag v*   →  Build + Test + APK Artifact + GitHub Release  (Publik/User)
```

Fitur **"Cek Pembaruan"** di dalam aplikasi hanya akan mendeteksi update dari **GitHub Release** yang dibuat oleh Alur 2.

---

## Alur 1 — Developer: Build Internal (tanpa update ke user)

Gunakan alur ini saat kamu sedang dalam proses pengembangan, perbaikan bug, atau ingin membagikan APK untuk testing internal tanpa mengganggu user yang sudah terpasang aplikasinya.

### Kapan digunakan?
- Sedang mengembangkan fitur baru yang belum selesai
- Ingin tes APK secara internal / QA sebelum rilis resmi
- Perbaikan kode tanpa perlu menaikkan versi publik

### Langkah-langkah

#### 1. Ngoding seperti biasa
Lakukan perubahan kode di Android Studio. **Tidak perlu mengubah `versionCode` atau `versionName`** di `build.gradle.kts`.

#### 2. Commit & Push ke `main`
```bash
git add .
git commit -m "feat: menambah fitur X (WIP)"
git push origin main
```

#### 3. GitHub Actions berjalan otomatis
Setelah push, GitHub Actions akan menjalankan pipeline berikut:

```
git push origin main
        │
        ▼
┌─────────────────────────┐
│  GitHub Actions Aktif   │
│  trigger: push (main)   │
└──────────┬──────────────┘
           │
   ┌───────┴───────┐
   ▼               ▼
┌──────────┐  ┌──────────────┐
│ Lint     │  │ Unit Test    │  ← continue-on-error: true
└──────────┘  └──────────────┘
   └───────┬───────┘
           ▼
┌─────────────────────────┐
│  Build Release APK      │  ← HARUS sukses
└──────────┬──────────────┘
           ▼
┌─────────────────────────┐
│  Upload ke Artifacts    │  ← nama file: app-trackit-main.apk
└─────────────────────────┘
           │
           ✗  Tidak membuat GitHub Release
              (step ini di-skip karena bukan push tag)
```

#### 4. Download APK untuk testing
- Buka tab **Actions** di repositori GitHub
- Klik workflow run yang baru saja selesai
- Scroll ke bawah → bagian **Artifacts** → klik **`app-trackit-main`** untuk download `.zip` berisi APK

> ⚠️ **Penting:** User yang sudah install aplikasi **tidak akan menerima notifikasi update** dari alur ini. Fitur "Cek Pembaruan" di aplikasi tidak mendeteksinya karena tidak ada GitHub Release baru.

---

## Alur 2 — Rilis Resmi: Update ke User

Gunakan alur ini saat fitur sudah matang dan siap dibagikan ke semua user. Alur ini yang akan **memicu notifikasi update** muncul di dalam aplikasi user.

### Kapan digunakan?
- Fitur baru sudah selesai dan siap produksi
- Perbaikan bug kritikal yang harus segera diterima user
- Rilis versi minor/major baru

### Langkah-langkah

#### 1. Naikkan versi di `app/build.gradle.kts`

```kotlin
defaultConfig {
    // ...
    versionCode = 5          // ← Wajib dinaikkan (integer, tidak boleh turun!)
    versionName = "1.2.0"    // ← Harus sesuai dengan tag yang akan di-push
    // ...
}
```

> ⚠️ **Aturan `versionCode`:** Angka ini harus **selalu bertambah** di setiap rilis. Android menggunakan nilai ini untuk mendeteksi apakah APK baru bisa menimpa yang lama. Jika nilainya sama atau lebih kecil, instalasi update akan ditolak oleh Android.
>
> ℹ️ **Aturan `versionName`:** Harus **sama** dengan tag yang akan kamu buat di langkah 3 (tanpa prefix `v`). Contoh: tag `v1.2.0` → `versionName = "1.2.0"`.

#### 2. Commit & Push ke `main`

```bash
git add .
git commit -m "chore: bump version to v1.2.0"
git push origin main
```

#### 3. Buat dan Push Tag Versi — ⚡ Ini yang Memicu Rilis!

```bash
git tag -a v1.2.0 -m "Catatan Rilis: Menambahkan fitur X dan memperbaiki bug Y"
git push origin v1.2.0
```

> 💡 **Tips Penting:** Gunakan flag `-a` (annotated tag) dan `-m` (message) seperti contoh di atas. Pesan yang Anda tuliskan di dalam tanda kutip tersebut akan **otomatis menjadi teks Catatan Rilis** yang muncul di dalam pop-up aplikasi TrackIt!

Setelah perintah ini, GitHub Actions akan berjalan dengan **alur penuh** termasuk pembuatan GitHub Release:

```
git push origin v1.2.0
        │
        ▼
┌─────────────────────────┐
│  GitHub Actions Aktif   │
│  trigger: push tag v*   │
└──────────┬──────────────┘
           │
   ┌───────┴───────┐
   ▼               ▼
┌──────────┐  ┌──────────────┐
│ Lint     │  │ Unit Test    │  ← continue-on-error: true
└──────────┘  └──────────────┘
   └───────┬───────┘
           ▼
┌─────────────────────────┐
│  Build Release APK      │  ← HARUS sukses, jika gagal pipeline berhenti
└──────────┬──────────────┘
           ▼
┌─────────────────────────┐
│  Rename APK             │
│  → app-trackit-v1.2.0.apk│
└──────────┬──────────────┘
           ▼
┌─────────────────────────┐
│  Upload ke Artifacts    │
└──────────┬──────────────┘
           ▼
┌─────────────────────────┐   ← HANYA berjalan saat push tag v*
│  Create GitHub Release  │
│  APK diunggah sebagai   │
│  aset publik            │
└─────────────────────────┘
```

APK sekarang tersedia publik di:
`https://github.com/rivaldiekaptrrr/Track-app/releases`

---

## User Flow: Fitur In-App Update

Setelah GitHub Release berhasil dibuat (Alur 2), beginilah yang dirasakan oleh pengguna.

### Skenario A — Pengecekan Otomatis Saat Buka Aplikasi

Saat user membuka aplikasi, sistem secara diam-diam mengecek GitHub API di *background* tanpa loading screen apapun:

```
User membuka aplikasi TrackIt
        │
        ▼
Aplikasi mengecek GitHub API (background, silent)
https://api.github.com/repos/rivaldiekaptrrr/Track-app/releases/latest
        │
        ▼
  ┌─────────────────┐
  │ Ada versi baru? │
  └────────┬────────┘
           │
    ┌──────┴──────┐
    ▼             ▼
  [YA]          [TIDAK]
    │             │
    ▼             ▼
Dialog muncul  Tidak ada yang
otomatis       tampil ke user
```

**Tampilan dialog yang muncul:**
```
┌──────────────────────────────────────────┐
│  🆕 Pembaruan Tersedia!                  │
│                                          │
│  Versi v1.2.0 sudah tersedia.            │
│                                          │
│  [Catatan rilis dari GitHub]             │
│                                          │
│               [Nanti]  [Unduh Sekarang]  │
└──────────────────────────────────────────┘
```

### Skenario B — Pengecekan Manual dari Pengaturan

User juga dapat memicu pengecekan secara manual:

```
Pengaturan → Tentang Aplikasi → [Cek Pembaruan]
        │
        ▼
Tombol berubah → loading spinner "Mengecek..."
        │
        ▼
  ┌─────────────────┐
  │ Ada versi baru? │
  └────────┬────────┘
           │
    ┌──────┴──────┐
    ▼             ▼
  [YA]          [TIDAK]
    │             │
    ▼             ▼
Dialog muncul   Muncul pesan:
(sama seperti   "Tidak dapat terhubung
 Skenario A)     atau tidak ada pembaruan."
```

### Skenario C — Proses Unduh & Pasang

Setelah user menekan **"Unduh Sekarang"**:

```
User klik "Unduh Sekarang"
        │
        ▼
┌───────────────────────────┐
│  Mengunduh...             │
│  ████████░░░░ 65%         │  ← Progress bar real-time
└───────────────────────────┘
        │
        ▼
Unduhan selesai → Siap dipasang → Klik [Pasang]
        │
        ▼
Cek izin "Install Unknown Apps"
        │
   ┌────┴────┐
   ▼         ▼
[Sudah ada] [Belum ada]
   │         │
   ▼         ▼
Langsung   Diarahkan ke Settings Android
buka       untuk memberi izin terlebih dahulu
installer
   └────┬────┘
        ▼
Android menampilkan installer sistem
(Replace / timpa aplikasi lama)
        │
        ▼
Aplikasi ter-update ✅  (data lama tetap aman)
```

---

## Catatan Teknis Komponen Updater

| Komponen | Lokasi | Fungsi |
|---|---|---|
| `AppUpdateChecker.kt` | `updater/` | Hit GitHub API, bandingkan versi dengan semantic versioning |
| `AppUpdateDownloader.kt` | `updater/` | Download APK via DownloadManager, expose progress via `Flow` |
| `UpdateViewModel.kt` | `updater/` | Jembatan logika updater ke UI |
| `UpdateDialog.kt` | `ui/components/` | Semua tampilan dialog update (tersedia, downloading, selesai) |
| `MainActivity.kt` | — | Trigger pengecekan otomatis saat launch (silent, background) |
| `SettingsScreen.kt` | `ui/settings/` | Tombol cek manual + menampilkan dialog |

**Logika perbandingan versi** (semantic versioning):
- Tag `v1.2.0` vs versi lokal `1.1.5` → Remote lebih baru ✅ → Dialog muncul
- Tag `v1.1.5` vs versi lokal `1.1.5` → Sama → Tidak ada notifikasi
- Tag `v1.0.0` vs versi lokal `1.1.5` → Remote lebih lama → Tidak ada notifikasi

---

## Best Practice & Tips

1. **Cek di lokal sebelum push:** Pastikan kode bisa di-Run atau tidak ada error di Android Studio. Kalau di laptop sendiri sudah merah, di GitHub Actions pasti gagal.
2. **Hindari push langsung ke `main`:** Jika bekerja dalam tim, buat branch baru (`git checkout -b fitur-baru`), push ke branch tersebut, lalu buat **Pull Request**. CI akan berjalan, dan hanya kalau hijau ✅ baru di-merge ke `main`.
3. **Urutan yang benar saat rilis:** Selalu ubah `versionCode`/`versionName` → commit → baru buat tag. Jangan buat tag sebelum versi diubah di kode.
4. **Jangan turunkan `versionCode`:** Ini akan menyebabkan Android menolak instalasi dan user tidak bisa update.
5. **Membaca log error:** Buka tab **Actions** → klik run yang gagal → klik step yang merah → baca log error secara perlahan. Biasanya file dan baris yang bermasalah tertulis jelas.

---

## Troubleshooting

### ❌ `403 Resource not accessible by integration` (GitHub Actions)

**Penyebab:** Token GitHub tidak punya izin untuk membuat Release.

**Solusi (Opsi 1 — Via Repository Settings):**
1. Buka repository → **Settings → Actions → General**
2. Di bagian **Workflow permissions** → pilih **"Read and write permissions"**
3. Klik **Save**

**Solusi (Opsi 2 — sudah diterapkan di workflow):**
```yaml
jobs:
  build:
    permissions:
      contents: write   # ← Baris ini WAJIB ada
```

---

### ❌ `Unresolved reference: BuildConfig`

**Penyebab:** `buildConfig` belum diaktifkan di `build.gradle.kts`.

**Solusi:**
```kotlin
buildFeatures {
    compose = true
    buildConfig = true   // ← Tambahkan baris ini
}
```

---

### ❌ Dialog update tidak muncul padahal sudah push tag baru

**Kemungkinan penyebab:**
1. `versionName` di `build.gradle.kts` belum dinaikkan — aplikasi di HP masih versi lama, tapi `versionName`-nya sama dengan yang ada di GitHub Release.
2. Tidak ada koneksi internet di perangkat.
3. GitHub API rate limit — coba beberapa menit kemudian.

---

### ❌ Build Release APK gagal tapi Debug berhasil

**Penyebab:** ProGuard/R8 menghapus class yang dibutuhkan saat minifikasi.

**Solusi:** Tambahkan rule di `proguard-rules.pro`:
```proguard
# Jaga class OkHttp dari minifikasi
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Jaga class updater
-keep class com.trackit.app.updater.** { *; }
```
