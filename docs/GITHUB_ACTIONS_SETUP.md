# Tutorial GitHub Actions untuk Dual-Platform Build (Android & iOS)

## Prerequisites

- Akun GitHub
- Proyek sudah ada di repository GitHub

## Langkah-Langkah

### 1. Push Proyek ke GitHub (jika belum)

```bash
git init
git add .
git commit -m "feat: setup KMP & Compose Multiplatform"
git remote add origin https://github.com/USERNAME/REPO_NAME.git
git push -u origin main
```

### 2. Workflow File Multiplatform

File konfigurasi workflow berada di: [`.github/workflows/multiplatform-build.yml`](../.github/workflows/multiplatform-build.yml)

Workflow ini memiliki dua job yang berjalan secara paralel:
1. **`build-android` (`runs-on: ubuntu-latest`):** Menyusun rilis APK Android menggunakan Gradle.
2. **`build-ios` (`runs-on: macos-latest`):** Menyiapkan lingkungan Xcode & macOS untuk memvalidasi kompilasi framework iOS.
3. **`create-release` (`runs-on: ubuntu-latest`):** Otomatis membuat GitHub Release publik saat tag versi (`v*`) di-push.

### 3. Cara Rilis Versi Baru (Android + iOS)

Untuk memicu build rilis resmi ke user:

```bash
# 1. Pastikan semua perubahan sudah di-commit
git add .
git commit -m "chore: release version 3.6.0"

# 2. Buat git tag versi baru
git tag v3.6.0

# 3. Push commit dan tag ke GitHub
git push origin main
git push origin v3.6.0
```

### 4. Monitor & Unduh Hasil Build

1. Buka `https://github.com/USERNAME/REPO_NAME/actions`.
2. Klik workflow **Multiplatform Build (Android & iOS)** yang sedang berjalan.
3. Setelah selesai:
   - File APK rilis otomatis terlampir di tab **Releases** dan **Artifacts**.
   - Target framework iOS tervalidasi sukses.

## Kuota GitHub Actions Runner

- **Linux (`ubuntu-latest`):** Menggunakan 1x multiplier kuota menit gratis (sangat hemat).
- **macOS (`macos-latest`):** Menggunakan 10x multiplier kuota menit gratis untuk menjalankan Xcode iOS build.