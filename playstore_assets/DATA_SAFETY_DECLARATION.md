# Panduan Pengisian Form Data Safety (Google Play Console)

Dokumen ini berisi panduan rincian jawaban untuk mengisi kuesioner **Data Safety (Keamanan Data)** pada Google Play Console.

---

## 1. Overview Pertanyaan & Jawaban

| Pertanyaan Play Console | Jawaban | Keterangan |
|---|---|---|
| **Apakah aplikasi Anda mengumpulkan atau membagikan jenis data pengguna yang memenuhi syarat?** | **Ya** | Karena aplikasi memiliki opsi akun Firebase Auth & Cloud Sync. |
| **Apakah semua data pengguna dikumpulkan disinkronkan / dikodekan saat transit (encrypted in transit)?** | **Ya** | Seluruh komunikasi jaringan menggunakan **HTTPS/TLS**. |
| **Apakah Anda menyediakan metode bagi pengguna untuk meminta penghapusan data mereka?** | **Ya** | Menyediakan fitur Hapus Akun & Data Permanen di menu *Pengaturan*. |

---

## 2. Rincian Jenis Data yang Dideklarasikan

### A. Financial Info (Informasi Keuangan)
- **Data:** Purchase history / Financial records (Catatan Transaksi & Anggaran)
- **Collected (Dikumpulkan):** Ya (jika menggunakan Mode Cloud Sync)
- **Shared (Dibagikan):** Tidak
- **Ephemeral (Sementara):** Tidak
- **Required or Optional:** Optional (Hanya jika pengguna membuat akun cloud)
- **Purposes (Tujuan):** App functionality (Fungsi aplikasi), Analytics (Analisis statistik internal pengguna).

### B. Personal Info (Informasi Pribadi)
- **Data:** Name, Email address, User IDs
- **Collected (Dikumpulkan):** Ya
- **Shared (Dibagikan):** Tidak
- **Encrypted in Transit:** Ya
- **Purposes:** Account management (Manajemen akun pengguna), Developer communications.

### C. Contacts (Kontak)
- **Collected:** **TIDAK**
- **Penjelasan:** Aplikasi menggunakan pemilih kontak bawaan sistem (`PickContact()`). Data kontak tidak pernah dikumpulkan atau dikirim ke server luar.

### D. Audio Files / Voice (Suara)
- **Collected:** **TIDAK**
- **Penjelasan:** Fitur perintah suara diproses secara lokal di perangkat (`SpeechRecognizer` offline). Rekaman suara tidak pernah dikumpulkan atau disimpan.

### E. Photos / Files (Foto & Dokumen)
- **Collected:** **TIDAK**
- **Penjelasan:** Ekspor dokumen PDF/CSV dibuat secara lokal dan disimpan di folder unduhan pengguna.

---

## 3. Deklarasi Izin & Kebijakan Khusus (App Content Declarations)

1. **Financial Features Declaration:**
   - Pilih: *Personal Finance / Expense Tracker*.
2. **Target Audience and Content:**
   - Target Age: 18+ (Dewasa/Umum).
3. **News Apps / COVID-19:**
   - Pilih: *Bukan aplikasi berita* dan *Bukan aplikasi COVID-19*.
