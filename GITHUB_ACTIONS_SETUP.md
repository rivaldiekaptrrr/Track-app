# Tutorial GitHub Actions untuk Build Android

## Prerequisites

- Akun GitHub
- Project sudah ada di repository GitHub

## Langkah-Langkah

### 1. Push Project ke GitHub (jika belum)

```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/USERNAME/REPO_NAME.git
git push -u origin main
```

### 2. Buat Workflow File

Buat file di: `.github/workflows/android-build.yml`

```yaml
name: Android Build

on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v2

      - name: Make gradlew executable
        run: chmod +x gradlew

      - name: Build debug APK
        run: ./gradlew assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug.apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

### 3. Push Workflow ke GitHub

```bash
git add .github/
git commit -m "Add GitHub Actions workflow"
git push origin main
```

### 4. Monitor Build

1. Buka https://github.com/USERNAME/REPO_NAME/actions
2. Klik pada workflow yang sedang berjalan
3. Lihat progress build

### 5. Download APK

Setelah build selesai (biasanya 5-10 menit):
1. Buka tab "Actions"
2. Klik workflow yang sudah berhasil
3. Klik "app-debug.apk" di bagian Artifacts
4. Download ke HP/PC

## Cara Kerja

Setiap kali kamu push ke `main/master`:
1. GitHub Actions otomatis jalankan build
2. Build APK di server GitHub (bukan di PC kamu)
3. Hasilnya bisa di-download

## Gratis?

- GitHub Actions: 2000 menit/bulan gratis (public repo)
- Private repo: 2000 menit/bulan gratis
- Cukup untuk development daily

## Troubleshooting

### Jika Build Gagal
- Buka tab "Actions" → klik failed → lihat error log
- Perbaiki error di PC, push ulang

### Jika Butuh Release APK
Ganti `assembleDebug` menjadi `assembleRelease` di workflow:
```yaml
- name: Build release APK
  run: ./gradlew assembleRelease
```
Path menjadi: `app/build/outputs/apk/release/app-release.apk`