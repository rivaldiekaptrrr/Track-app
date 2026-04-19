# Panduan Alur Kerja (Workflow) CI/CD Developer

Dokumen ini akan memandu kamu memahami bagaimana CI/CD (Continuous Integration / Continuous Deployment) yang sudah kita atur menggunakan GitHub Actions bekerja, dan apa yang harus kamu lakukan sebagai developer sehari-hari.

## 🎯 Apa Itu CI/CD dalam Project Ini?

- **CI (Continuous Integration):** Setiap kali kamu menyimpan kode ke GitHub, GitHub akan otomatis mengecek apakah kodemu error, memastikan standar kode (*linting*) sudah sesuai, dan mengetes kode (*unit test*).
- **CD (Continuous Deployment/Delivery):** Setelah kode dipastikan aman, GitHub Actions otomatis membuatkan file `APK` yang siap untuk di-download dan dites di HP, tanpa kamu harus mem-build manual (`Build -> Build APK`) di Android Studio kamu.

---

## 🔄 Alur Kerja Kamu Sehari-Hari

Sebagai developer, alur kerja kamu sekarang menjadi lebih modern dan profesional:

### 1. Ngoding Seperti Biasa
Kamu tetap membuat fitur atau memperbaiki bug di Android Studio seperti biasa di komputer lokal kamu.

### 2. Commit dan Push Kode
Setelah selesai dengan suatu fitur atau perubahan, kamu kirim perubahan tersebut ke GitHub:
```bash
git add .
git commit -m "feat: menambah fitur login"
git push origin main
```
*(Atau kamu bisa nge-push ke branch lain, lalu bikin Pull Request ke branch main)*

### 3. Biarkan Server CI/CD Bekerja (Tinggal Ngopi ☕)
Begitu kodemu masuk ke repository GitHub, GitHub Actions langsung "terpicu" (*triggered*) dan berjalan di server mereka. Script akan melakukan tahapan dari file `.github/workflows/android-build.yml` yang tadi kita buat:
1. Men-download kode terbarumu ke server mereka.
2. Menginstall Java & Gradle.
3. Mengecek *Linting* (Kualitas/Kerapian Kode).
4. Menjalankan *Unit Test*.
5. Menyatukan semua kodemu menjadi file **APK**.

### 4. Pantau di GitHub Actions
Untuk melihat robot pekerja ini sedang ngapain, buka link ini di browser-mu:
👉 **[https://github.com/rivaldisinkoprima/Track_It/actions](https://github.com/rivaldisinkoprima/Track_It/actions)**

Di tab **"Actions"** ini, kamu akan melihat daftar pekerjaan (*workflow runs*). Klik pada *commit* terakhirmu untuk melihat proses instalasi dan build berjalan secara *live* step-by-step.

### 5. Terima Hasilnya: Download APK atau Fix Error
Setelah loading beberapa menit, proses di tab Actions akan selesai dengan dua kemungkinan:

- **Jika Sukses (Centang Hijau ✅):** 
  Scroll halaman *workflow run* tersebut ke paling bawah. Di bagian menu **Artifacts**, akan ada file bernama **`app-debug`**. Klik namanya untuk men-download `.zip` berisi `.apk` aplikasi kamu. Kamu bisa langsung kirim APK itu ke HP-mu atau tim QA untuk diuji coba.
  
- **Jika Gagal (Silang Merah ❌):** 
  Jangan panik! Ini hal yang biasa. Klik pada tahapan yang ada logo silang merahnya untuk membaca *log* error-nya (apakah salah ketik syntax, dependensi kurang, atau tes gagal). Perbaiki masalah tersebut di Android Studio-mu, lalu **Ulangi dari Langkah 2** (*commit* & *push* lagi).

---

## 💡 Best Practice & Trik "Pro"

1. **Cek Lokal Sebelum Push:** Sebelum melakukan `git push`, pastikan kodemu setidaknya bisa di-Run atau tidak merah-merah (error) di Android Studio. Kalau di laptop sendiri saja sudah error, pasti di Actions akan langsung gagal.
2. **Hindari Push Langsung ke Main:** Jika ke depannya kamu kerja dalam tim, jangan pernah push langsung ke `main`. Buatlah branch baru (misal: `git checkout -b fitur-keren`), push ke branch tersebut, lalu buat **Pull Request (PR)**. CI/CD akan berjalan, dan hanya kalau hijau ✅ (Lulus) baru teman-mu akan me-merge kode kamu ke `main`.
3. **Membaca Log Error:** Biasakan membaca log error perlahan. Biasanya petunjuk utama file mana dan baris ke berapa yang salah sangat jelas tertulis di dalam blok terminal GitHub Actions.
