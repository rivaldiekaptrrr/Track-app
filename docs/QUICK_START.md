# ⚡ TrackIt — Quick Start

Panduan singkat untuk langsung menjalankan dan build aplikasi TrackIt.

---

## Opsi A: Versi Minimal (Tanpa Android Studio)

Jika Anda hanya ingin build APK via terminal tanpa install Android Studio.

### Yang Harus Disiapkan

| No | Komponen | Versi | Cara Cek di Terminal |
|----|----------|-------|---------------------|
| 1 | **Java JDK 17** | 17.x | `java --version` |
| 2 | **Android SDK** (Command-line Tools, Build-Tools 34, Platform 34) | — | `sdkmanager --version` |
| 3 | **Koneksi Internet** | — | Untuk download dependencies saat pertama kali |

### 3 Langkah Menjalankan

#### 1️⃣ Pastikan Java 17 & Android SDK Terinstall

Buka terminal (PowerShell / CMD), jalankan:

```bash
# Cek Java
java --version
# Output yang diharapkan: java 17.x.x ...

# Cek Android SDK
sdkmanager --version
# Output yang diharapkan: 20.0 (atau versi lainnya)
```

> ⚠️ Jika `java` belum ada, download JDK 17 dari [Oracle](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) atau [Adoptium](https://adoptium.net/).
>
> ⚠️ Jika `sdkmanager` belum ada, download [Android Command-line Tools](https://developer.android.com/studio#command-line-tools-only), lalu install SDK yang dibutuhkan:
> ```bash
> sdkmanager "platforms;android-34" "build-tools;34.0.0"
> ```

#### 2️⃣ Buat File `local.properties`

Buat file `local.properties` di **root folder proyek** (sejajar dengan `gradlew.bat`), isi dengan **1 baris**:

```properties
sdk.dir=C\:\\Android
```

> Sesuaikan path di atas dengan lokasi Android SDK di PC Anda.
> Cara mengetahui lokasi SDK: jalankan `Get-Command sdkmanager | Select-Object Source` di PowerShell, path SDK-nya adalah folder induk dari `cmdline-tools`.

#### 3️⃣ Jalankan Gradlew

```bash
# Masuk ke folder proyek, lalu jalankan:
.\gradlew assembleDebug
```

- Pertama kali akan download Gradle wrapper & dependencies (~95MB, butuh internet)
- Tunggu hingga **BUILD SUCCESSFUL**
- Hasil APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## Opsi B: Menggunakan Android Studio (Lebih Mudah)

Jika Anda ingin menjalankan langsung ke HP Android.

### Yang Harus Disiapkan

| No | Komponen | Download |
|----|----------|----------|
| 1 | **Android Studio** (Hedgehog+) | [developer.android.com/studio](https://developer.android.com/studio) |
| 2 | **HP Android** (Android 8.0+) + kabel USB | — |

> JDK 17 & SDK sudah bundled di Android Studio.

### 5 Langkah Menjalankan

#### 1️⃣ Install Android Studio
Download → jalankan installer → pilih **Standard Installation** → selesai.

#### 2️⃣ Aktifkan USB Debugging di HP
```
Settings → About Phone → ketuk "Build Number" 7x
→ kembali ke Settings → Developer Options → aktifkan USB Debugging
→ colokkan HP ke komputer via kabel USB
→ tekan "Allow" pada dialog di HP
```

#### 3️⃣ Buka Proyek
```
Buka Android Studio → File → Open → pilih folder proyek → OK
→ tunggu Gradle Sync selesai (butuh internet, ±5 menit pertama kali)
```

> ⚠️ Jika muncul error *"SDK location not found"*, buat file `local.properties` di root proyek (lihat Opsi A langkah 2).

#### 4️⃣ Jalankan
```
Pastikan HP muncul di dropdown device (toolbar atas)
→ klik tombol Run ▶️ (atau Shift+F10)
→ tunggu build ±2-5 menit (pertama kali)
→ app otomatis terinstall & terbuka di HP
```

#### 5️⃣ Selesai! 🎉
```
Biometric prompt muncul → scan sidik jari → masuk Dashboard
→ tekan (+) untuk tambah transaksi pertama
```

---

## Build APK (Opsional)

```bash
# Debug APK (untuk testing)
.\gradlew assembleDebug

# Release APK (untuk distribusi)
.\gradlew assembleRelease

# Bersihkan build
.\gradlew clean
```

Hasil APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## Kalau Ada Masalah?

| Masalah | Solusi Cepat |
|---------|-------------|
| `java` tidak dikenali | Install JDK 17 dan pastikan sudah masuk PATH sistem |
| `sdkmanager` tidak dikenali | Install Android Command-line Tools dan tambahkan ke PATH |
| Gradle Sync gagal | Cek koneksi internet → **File → Sync Project with Gradle Files** |
| "SDK location not found" | Buat/perbaiki file `local.properties` dengan path SDK yang benar |
| HP tidak terdeteksi | Install USB driver dari situs merek HP → cabut-colok ulang |
| Build error JDK | Pastikan `JAVA_HOME` mengarah ke JDK 17 |
| Error "Cannot resolve symbol" | **File → Invalidate Caches → Restart** (Android Studio) |

Untuk panduan lengkap, lihat [`SETUP_GUIDE.md`](./SETUP_GUIDE.md).
