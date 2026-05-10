# 🚀 CI/CD & In-App Update — Developer Guide

> Dokumentasi ini menjelaskan dua hal utama: **bagaimana developer merilis versi baru** melalui GitHub Actions, dan **bagaimana user merasakan alur pembaruan** langsung dari dalam aplikasi TrackIt.

---

## Daftar Isi

- [Cara Developer Merilis Versi Baru](#cara-developer-merilis-versi-baru)
- [Alur CI/CD — Apa yang Terjadi di GitHub Actions](#alur-cicd--apa-yang-terjadi-di-github-actions)
- [User Flow: Fitur In-App Update](#user-flow-fitur-in-app-update)
- [Troubleshooting](#troubleshooting)

---

## Cara Developer Merilis Versi Baru

Setiap rilis dilakukan dengan cara yang **sederhana dan konsisten** — cukup 3 langkah:

### Langkah 1 — Naikkan Versi di `build.gradle.kts`

```kotlin
// app/build.gradle.kts
defaultConfig {
    versionCode = 3          // ← Selalu naikkan (integer, tidak boleh turun)
    versionName = "1.2.0"   // ← Harus sesuai dengan tag yang akan di-push
}
```

> ⚠️ **Wajib**: `versionCode` harus selalu naik di setiap rilis. Ini yang dipakai aplikasi untuk membandingkan apakah perlu update.

### Langkah 2 — Commit & Push ke `main`

```bash
git add .
git commit -m "chore: bump version to v1.2.0"
git push origin main
```

### Langkah 3 — Push Tag Versi (Ini yang Memicu Build Otomatis)

```bash
git tag v1.2.0
git push origin v1.2.0
```

Setelah tag di-push, GitHub Actions akan **otomatis** berjalan, mem-build APK, dan mengunggahnya ke halaman **Releases** di repositori.

---

## Alur CI/CD — Apa yang Terjadi di GitHub Actions

Setelah `git push origin v1.2.0` dijalankan, berikut proses yang berjalan otomatis:

```
Developer push tag v1.2.0
         │
         ▼
┌─────────────────────────────┐
│  GitHub Actions Triggered   │
│  (trigger: push tags: v*)   │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│  Checkout → JDK 17 → Gradle │
└──────────────┬──────────────┘
               │
       ┌───────┴───────┐
       ▼               ▼
 ┌──────────┐   ┌──────────────┐
 │ Run Lint │   │ Run UnitTest │  ← Boleh gagal, tidak stop build
 └──────────┘   └──────────────┘
       └───────┬───────┘
               ▼
┌─────────────────────────────┐
│  Build Release APK          │  ← HARUS sukses, jika gagal pipeline berhenti
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│  Rename: app-trackit-v*.apk │
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│  Create GitHub Release      │  ← APK diunggah sebagai aset publik
│  softprops/action-gh-release│
└─────────────────────────────┘
```

Hasilnya: APK akan muncul di halaman **`github.com/{username}/Track-app/releases`** dan dapat diakses publik melalui URL langsung.

---

## User Flow: Fitur In-App Update

Ini adalah alur lengkap yang **dirasakan oleh pengguna** saat ada versi baru tersedia.

### Skenario A — Pengecekan Otomatis Saat Buka Aplikasi

```
User membuka aplikasi TrackIt
         │
         ▼
Aplikasi diam-diam mengecek GitHub API
(berjalan di background, tidak ada loading)
https://api.github.com/repos/rivaldiekaptrrr/Track-app/releases/latest
         │
         ▼
  ┌──────────────────┐
  │ Ada update baru? │
  └────────┬─────────┘
           │
    ┌──────┴──────┐
    ▼             ▼
  [YA]          [TIDAK]
    │             │
    ▼             ▼
Dialog muncul  Tidak ada yang
otomatis       tampil ke user
```

**Tampilan Dialog:**
```
┌─────────────────────────────────────┐
│  🆕 Pembaruan Tersedia!             │
│                                     │
│  Versi v1.2.0 sudah tersedia.       │
│                                     │
│  [Catatan rilis dari GitHub]        │
│                                     │
│           [Nanti]  [Unduh Sekarang] │
└─────────────────────────────────────┘
```

---

### Skenario B — Pengecekan Manual dari Halaman Settings

User dapat memicu pengecekan secara manual kapan saja:

```
Settings → Tentang Aplikasi → [Cek Pembaruan]
         │
         ▼
Tombol berubah: loading spinner "Mengecek..."
         │
         ▼
  ┌──────────────────┐
  │ Ada update baru? │
  └────────┬─────────┘
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

---

### Skenario C — Proses Unduh & Pasang

Setelah user menekan tombol **"Unduh Sekarang"**:

```
User klik "Unduh Sekarang"
         │
         ▼
┌─────────────────────────────┐
│  Mengunduh...               │
│  ████████░░░░ 65%           │  ← Progress bar real-time
└─────────────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│  Unduhan selesai.           │
│  Siap dipasang.             │
│                    [Pasang] │
└─────────────────────────────┘
         │
         ▼
Cek izin "Install Unknown Apps"
         │
    ┌────┴────┐
    ▼         ▼
[Sudah ada]  [Belum ada]
    │         │
    ▼         ▼
Langsung    Diarahkan ke
buka        Settings Android
installer   untuk beri izin
    │         │
    └────┬────┘
         ▼
Android menampilkan
installer sistem
(Replace existing app)
         │
         ▼
Aplikasi ter-update ✅
```

---

### Catatan Teknis untuk Developer

| Komponen | Lokasi | Fungsi |
|----------|--------|--------|
| `AppUpdateChecker.kt` | `updater/` | Mengecek GitHub API, membandingkan versi |
| `AppUpdateDownloader.kt` | `updater/` | Mengunduh APK, expose progress via `Flow` |
| `UpdateViewModel.kt` | `updater/` | Menjembatani logika ke UI |
| `UpdateDialog.kt` | `ui/components/` | Dialog pop-up untuk semua skenario |
| `MainActivity.kt` | — | Trigger cek otomatis saat launch (silent) |
| `SettingsScreen.kt` | `ui/settings/` | Tombol cek manual + tampilan dialog |

**Perbandingan versi** menggunakan logika *semantic versioning*:
- `v1.2.0` vs `1.1.0` → Remote lebih baru ✅
- `v1.1.0` vs `1.1.0` → Sama, tidak update
- `v1.0.0` vs `1.1.0` → Remote lebih lama, tidak update

---

## Troubleshooting

### ❌ `403 Resource not accessible by integration` (GitHub Actions)

**Penyebab:** Token GitHub tidak memiliki izin menulis untuk membuat Release.

**Solusi (dua opsi):**

**Opsi 1 — Via Repository Settings:**
1. Buka repository → **Settings → Actions → General**
2. Di bagian **Workflow permissions** → pilih **"Read and write permissions"**
3. Klik **Save**

**Opsi 2 — Via file YAML (sudah diterapkan):**
```yaml
jobs:
  build:
    permissions:
      contents: write   # ← Baris ini WAJIB ada
```

---

### ❌ `Unresolved reference: BuildConfig`

**Penyebab:** `buildConfig` tidak diaktifkan di `build.gradle.kts`.

**Solusi:**
```kotlin
buildFeatures {
    compose = true
    buildConfig = true   // ← Tambahkan ini
}
```

---

### ❌ Dialog update tidak muncul padahal sudah push tag baru

**Kemungkinan penyebab:**
1. `versionName` di `build.gradle.kts` belum dinaikkan — aplikasi yang terpasang di HP masih versi lama, tapi `versionName`-nya sama dengan yang di GitHub.
2. Tidak ada koneksi internet di perangkat.
3. GitHub API rate limit — coba beberapa menit kemudian.

---

### ❌ Build Release APK gagal tapi Debug berhasil

**Kemungkinan penyebab:** ProGuard/R8 menghapus class yang dibutuhkan.

**Solusi:** Tambahkan rule di `proguard-rules.pro`:
```proguard
# Jaga class OkHttp dari minifikasi
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Jaga class updater
-keep class com.trackit.app.updater.** { *; }
```
