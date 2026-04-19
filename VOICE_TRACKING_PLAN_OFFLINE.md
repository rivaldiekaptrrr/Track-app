# 🎤 Rencana Implementasi: Offline Voice Expense Tracker

Dokumen ini berisi langkah-langkah detail untuk mengganti fitur *Scan Struk (ML Kit OCR)* menjadi fitur *Pencatatan Berbasis Suara (Voice-to-Text)* yang beroperasi 100% secara lokal (Offline) tanpa memerlukan koneksi internet, menggunakan metode *Regex Pattern Matching*.

---

## 🎯 Tujuan
Mengizinkan pengguna menekan tombol mikrofon dan mengucapkan kalimat seperti **"beli sayur 50 ribu"**, lalu aplikasi akan secara otomatis mengisi form transaksi dengan data:
*   **Nominal:** 50.000
*   **Kategori:** Makanan
*   **Deskripsi:** beli sayur

---

## 🏗️ Arsitektur Sistem

1.  **Input:** Android `SpeechRecognizer` (Bawaan OS).
2.  **Pemrosesan (Parser):** Custom Kotlin `VoiceParserUtil` menggunakan Regular Expression (Regex) dan kamus *keyword*.
3.  **State Management:** `TransactionViewModel`.
4.  **UI:** `AddEditTransactionScreen` (dengan Compose).

---

## 🛠️ Langkah-Langkah Eksekusi (Step-by-Step)

### Tahap 1: Pembersihan Kode Lama (Clean Up)
Agar ukuran APK lebih kecil, kita harus menghapus library kamera dan OCR yang sudah tidak dipakai.
1.  Buka `app/build.gradle.kts`. Hapus dependensi untuk **CameraX** dan **Google ML Kit Text Recognition**.
2.  Hapus file `ScanReceiptScreen.kt` di folder UI.
3.  Hapus semua *routing* navigasi yang mengarah ke `ScanReceiptScreen` di `TrackItNavHost.kt` dan `Screen.kt`.

### Tahap 2: Konfigurasi Permission
Aplikasi membutuhkan akses ke mikrofon.
1.  Buka `AndroidManifest.xml`.
2.  Tambahkan izin: `<uses-permission android:name="android.permission.RECORD_AUDIO" />`

### Tahap 3: Membuat Otak Pemrosesan (`VoiceParser.kt`)
Membuat utility class murni Kotlin untuk mengekstrak informasi dari teks Bahasa Indonesia.
1.  Buat file `VoiceParser.kt` di folder `util`.
2.  **Logika Ekstrak Nominal:** 
    *   Cari angka dalam teks.
    *   Deteksi kata pengali (ribu -> x1000, juta -> x1.000.000, cepek -> 100.000, gocap -> 50.000).
3.  **Logika Kategori (Dictionary Matching):**
    *   `Makanan`: "sayur", "makan", "minum", "kopi", "bakso", "nasi".
    *   `Transportasi`: "bensin", "gojek", "grab", "parkir", "tol", "kereta".
    *   `Hiburan`: "nonton", "bioskop", "netflix", "game", "main".
    *   `Tagihan`: "listrik", "air", "wifi", "pulsa", "kuota".
4.  **Logika Deskripsi:** Sisa dari kalimat atau seluruh kalimat mentah.

### Tahap 4: Mengintegrasikan `SpeechRecognizer`
Membuat fungsi *helper* di Jetpack Compose untuk memicu *Speech to Text*.
1.  Buat file `VoiceRecognizerHelper.kt` atau masukkan langsung ke dalam UI layer.
2.  Gunakan `Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)` dengan bahasa yang dikunci ke "id-ID" (Bahasa Indonesia).
3.  Siapkan *Callback* untuk menangani teks hasil suara atau menangani error (misal: "Suara tidak terdengar").

### Tahap 5: Update UI & Hubungkan Semuanya
Merombak layar penambahan transaksi.
1.  Buka `AddEditTransactionScreen.kt`.
2.  Minta *runtime permission* mikrofon saat pengguna masuk ke layar ini atau saat tombol mikrofon ditekan pertama kali (menggunakan Accompanist Permissions atau `rememberLauncherForActivityResult`).
3.  Ganti tombol ikon "Kamera" menjadi ikon "Mikrofon".
4.  Buat animasi sederhana (seperti warna berubah merah) saat mikrofon sedang merekam.
5.  Ketika hasil teks keluar -> Lempar ke `VoiceParser` -> Lempar hasilnya ke State (Nominal, Deskripsi, Kategori). Form UI akan otomatis terisi.

---

## ✅ Kriteria Sukses (Acceptance Criteria)
1. Aplikasi tidak *crash* jika pengguna menolak izin mikrofon.
2. Kalimat "beli bensin 20 ribu" harus terisi sebagai nominal `20000` dan kategori `Transportasi`.
3. Fitur berjalan lancar meskipun internet dimatikan (mode pesawat).
