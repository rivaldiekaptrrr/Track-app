
---

# 📄 Product Requirement Document (PRD)

**Nama Produk:** Smart Expense Tracker (Nama Proyek: *FinSnap* atau *TrackIt*)
**Platform:** Android (Mobile)
**Bahasa Pemrograman:** Kotlin (Modern Android Development)

## 1. Pendahuluan

### 1.1. Latar Belakang & Visi Produk

Mencatat keuangan pribadi seringkali terasa melelahkan karena harus dilakukan secara manual, sehingga banyak orang berhenti di tengah jalan. Visi dari aplikasi ini adalah menciptakan pengalaman pencatatan keuangan yang instan, aman, dan tanpa hambatan (*frictionless*) dengan memanfaatkan teknologi *on-device AI* (Kecerdasan Buatan di dalam HP) dan UI yang modern.

### 1.2. Target Pengguna

* Mahasiswa atau pekerja kantoran yang ingin mengelola anggaran bulanan.
* Orang yang malas mengetik data pengeluaran secara manual.
* Pengguna yang sangat peduli dengan privasi data (lebih suka data disimpan di HP, bukan di server pihak ketiga).

---

## 2. Tumpukan Teknologi (Tech Stack) & Arsitektur

* **Arsitektur:** MVVM (Model-View-ViewModel)
* **UI Framework:** Jetpack Compose & Material Design 3
* **Local Database:** Room Database & Kotlin Coroutines/Flow
* **Dependency Injection:** Hilt
* **Machine Learning:** Google ML Kit (Text Recognition API - *On-Device*)
* **Background Tasks:** WorkManager
* **Security:** Android Biometric API

---

## 3. Fase Pengembangan & Persyaratan Fitur (Feature Requirements)

### 🔴 Fase 1: Core (Fondasi Utama)

Fokus pada fungsi dasar agar aplikasi bisa digunakan untuk mencatat pengeluaran.

* **FR 1.1 - Dashboard Utama:** Menampilkan total pengeluaran bulan ini, sisa anggaran, dan daftar transaksi terbaru (menggunakan *LazyColumn* di Compose).
* **FR 1.2 - CRUD Pengeluaran:** Pengguna dapat Menambah (Create), Membaca (Read), Memperbarui (Update), dan Menghapus (Delete) transaksi.
* **FR 1.3 - Kategori Pengeluaran:** Transaksi harus memiliki kategori (Makanan, Transportasi, Hiburan, Tagihan, dll).
* **FR 1.4 - Visualisasi Data:** Menampilkan grafik *Pie Chart* atau *Bar Chart* sederhana untuk melihat persentase pengeluaran per kategori.
* **FR 1.5 - Local Storage:** Semua data disimpan secara *offline* menggunakan Room Database.

### 🟡 Fase 2: Smart (Otomatisasi dengan AI)

Fokus mengurangi usaha pengguna dalam memasukkan data.

* **FR 2.1 - Smart Scan (OCR):** Pengguna dapat memotret struk belanja menggunakan kamera aplikasi (CameraX).
* **FR 2.2 - Ekstraksi Teks:** Menggunakan ML Kit Text Recognition untuk mendeteksi total angka dari foto struk dan mengisinya secara otomatis ke form nominal pengeluaran.
* **FR 2.3 - Date Picker Otomatis:** Tanggal transaksi secara default disetel ke hari ini (System Time), namun bisa diubah menggunakan komponen *DatePicker* dari Compose.

### 🟢 Fase 3: Security (Keamanan & Privasi)

Fokus melindungi data sensitif pengguna.

* **FR 3.1 - Biometric Login:** Saat aplikasi dibuka atau kembali dari *background*, aplikasi akan meminta autentikasi (Sidik Jari / Face Unlock) menggunakan BiometricPrompt API sebelum menampilkan dashboard.
* **FR 3.2 - Privacy Screen:** Mencegah tampilan aplikasi terlihat di *Recent Apps* (layar *multitasking* Android) untuk menghindari kebocoran informasi saldo.

### 🔵 Fase 4: Advanced (Laporan & Notifikasi)

Fokus pada fitur tingkat lanjut untuk menambah nilai aplikasi.

* **FR 4.1 - Export Data:** Kemampuan mengekspor daftar transaksi bulanan ke dalam format PDF (menggunakan PDFDocument Android) agar bisa dibagikan atau dicetak.
* **FR 4.2 - Budget Alert (Notifikasi):** Menggunakan WorkManager untuk mengecek total pengeluaran secara berkala di latar belakang. Jika pengeluaran melebihi 80% dari batas anggaran yang ditentukan, aplikasi akan mengirimkan notifikasi lokal.
* **FR 4.3 - Transaksi Berulang (Recurring):** Pengguna dapat mengatur transaksi otomatis (misal: bayar kos setiap tanggal 1) dan WorkManager akan mengeksekusi penambahan data tersebut ke database tanpa interaksi pengguna.

---

## 4. Alur Pengguna (User Flow) Utama

Berikut adalah contoh *flow* untuk skenario paling penting: **Menambah Pengeluaran via Struk**.

1. Pengguna membuka aplikasi -> Layar *Biometric Prompt* muncul.
2. Pengguna memindai sidik jari -> Berhasil masuk ke *Dashboard*.
3. Pengguna menekan tombol *Floating Action Button* (FAB) "Tambah Transaksi".
4. Pengguna memilih ikon "Kamera" di form input.
5. Kamera terbuka -> Pengguna memotret struk.
6. *Loading UI* muncul sesaat -> ML Kit memproses gambar secara *offline*.
7. Aplikasi kembali ke form input -> Kolom "Nominal" sudah terisi angka terbesar dari struk (misal: Rp 150.000).
8. Pengguna memilih kategori "Belanja" dan menekan "Simpan".
9. Data masuk ke Room DB -> *Flow* memicu *ViewModel* -> *Dashboard UI* dan *Pie Chart* langsung ter-update.

---

## 5. Kriteria Penerimaan (Acceptance Criteria)

Aplikasi dianggap siap rilis (MVP / *Minimum Viable Product*) jika memenuhi syarat berikut:

1. Aplikasi tidak mengalami *crash* saat layar diputar (*Screen Rotation*) berkat manajemen *State* yang baik di ViewModel.
2. Pemindaian struk (ML Kit) berjalan kurang dari 2 detik pada perangkat dengan spesifikasi menengah.
3. Grafik dan UI berjalan mulus pada 60 FPS tanpa hambatan saat di-*scroll*.
4. Aplikasi mendukung **Dark Mode** dan **Light Mode** secara otomatis mengikuti pengaturan sistem Android.

---