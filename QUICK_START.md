# ⚡ TrackIt — Quick Start (Versi Minimal)

Panduan singkat untuk langsung menjalankan aplikasi TrackIt.

---

## Yang Harus Disiapkan

| No | Komponen | Download |
|----|----------|----------|
| 1 | **Android Studio** (Hedgehog+) | [developer.android.com/studio](https://developer.android.com/studio) |
| 2 | **HP Android** (Android 8.0+) + kabel USB | — |

> Itu saja. JDK 17 & SDK sudah bundled di Android Studio.

---

## 5 Langkah Menjalankan

### 1️⃣ Install Android Studio
Download → jalankan installer → pilih **Standard Installation** → selesai.

### 2️⃣ Aktifkan USB Debugging di HP
```
Settings → About Phone → ketuk "Build Number" 7x
→ kembali ke Settings → Developer Options → aktifkan USB Debugging
→ colokkan HP ke komputer via kabel USB
→ tekan "Allow" pada dialog di HP
```

### 3️⃣ Buka Proyek
```
Buka Android Studio → File → Open → pilih folder D:\App\Track_it → OK
→ tunggu Gradle Sync selesai (butuh internet, ±5 menit pertama kali)
```

> ⚠️ Jika muncul error *"SDK location not found"*, buat file `local.properties` di root proyek:
> ```properties
> sdk.dir=C\:\\Users\\Admin\\AppData\\Local\\Android\\Sdk
> ```
> Sesuaikan path dengan lokasi SDK Anda (cek di **Tools → SDK Manager**).

### 4️⃣ Jalankan
```
Pastikan HP muncul di dropdown device (toolbar atas)
→ klik tombol Run ▶️ (atau Shift+F10)
→ tunggu build ±2-5 menit (pertama kali)
→ app otomatis terinstall & terbuka di HP
```

### 5️⃣ Selesai! 🎉
```
Biometric prompt muncul → scan sidik jari → masuk Dashboard
→ tekan (+) untuk tambah transaksi pertama
```

---

## Build APK (Opsional)

```bash
cd D:\App\Track_it
.\gradlew assembleDebug
```

Hasil APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## Kalau Ada Masalah?

| Masalah | Solusi Cepat |
|---------|-------------|
| Gradle Sync gagal | Cek internet → **File → Sync Project with Gradle Files** |
| HP tidak terdeteksi | Install USB driver dari situs merek HP → cabut-colok ulang |
| Build error JDK | **File → Settings → Gradle → Gradle JDK** → pilih JDK 17 |
| Error "Cannot resolve symbol" | **File → Invalidate Caches → Restart** |

Untuk panduan lengkap, lihat [`SETUP_GUIDE.md`](./SETUP_GUIDE.md).
