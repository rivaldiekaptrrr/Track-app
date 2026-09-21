# Kebijakan Privasi (Privacy Policy) - TrackIt

**Tanggal Efektif:** 14 September 2026  
**Aplikasi:** TrackIt (Smart Expense Tracker & Wedding Planner)  
**Pengembang:** TrackIt Team  
**Kontak Pengembang:** support@trackit.app / privacy@trackit.app

---

## 1. Pendahuluan
Kami di **TrackIt** menghormati privasi Anda dan berkomitmen untuk melindungi data pribadi dan keuangan Anda. Kebijakan Privasi ini menjelaskan bagaimana informasi Anda dikumpulkan, digunakan, disimpan, dan dilindungi saat Anda menggunakan aplikasi **TrackIt** di perangkat seluler Android.

Dengan menggunakan aplikasi TrackIt, Anda menyetujui praktik yang dijelaskan dalam Kebijakan Privasi ini.

---

## 2. Informasi yang Kami Kumpulkan & Cara Penggunaannya

### A. Data Keuangan & Transaksi
- **Data yang dikumpulkan:** Catatan pengeluaran, pemasukan, nama kategori, anggaran bulanan, serta daftar tamu & budget pernikahan yang Anda masukkan.
- **Tujuan:** Menyediakan fungsi utama aplikasi yaitu pencatatan keuangan pribadi, analisis statistik visual, dan perencanaan anggaran.
- **Penyimpanan:** Data ini disimpan secara lokal di perangkat Anda menggunakan SQLite (Room Database). Jika Anda mengaktifkan **Mode Cloud Sync**, data akan disinkronkan secara aman ke basis data **Firebase Firestore** pribadi Anda.

### B. Input Suara (Voice Recognition / TTS)
- **Data yang dikumpulkan:** Perintah suara sementara saat Anda menggunakan fitur pencatatan transaksi berbasis suara.
- **Tujuan:** Mengonversi ucapan Anda menjadi teks angka dan keterangan transaksi secara otomatis.
- **Pemrosesan:** Pemrosesan suara dilakukan **100% secara offline** di perangkat menggunakan API `SpeechRecognizer` bawaan Android OS. **Kami tidak merekam, menyimpan, atau mengirimkan rekaman suara Anda ke server mana pun.**

### C. Kontak (Pilihan Tamu Pernikahan)
- **Data yang dikumpulkan:** Nama dan nomor telepon kontak yang **Anda pilih secara manual** saat menambahkan tamu pernikahan dari buku telepon.
- **Tujuan:** Mempermudah pengisian daftar tamu dan pengiriman undangan via WhatsApp.
- **Pemrosesan:** Aplikasi menggunakan fitur pemilih kontak bawaan sistem (`ActivityResultContracts.PickContact()`). Aplikasi **TIDAK meminta izin membaca seluruh kontak** (`READ_CONTACTS`) dan **TIDAK mengunggah buku alamat kontak Anda ke server.**

### D. Autentikasi & Akun
- **Data yang dikumpulkan:** Alamat email, Nama Pengguna, dan User ID (UID) unik yang disediakan oleh Firebase Authentication saat Anda mendaftar atau masuk menggunakan akun Google / Email.
- **Tujuan:** Otentikasi identitas pengguna dan sinkronisasi data antar perangkat.

### E. Keamanan Biometrik
- **Data yang dikumpulkan:** Verifikasi sidik jari / pemindaian wajah.
- **Pemrosesan:** Verifikasi biometrik diproses **sepenuhnya oleh sistem operasi Android** (`BiometricPrompt`). Aplikasi TrackIt **TIDAK PERNAH mengakses, membaca, atau menyimpan data biometrik Anda.**

---

## 3. Pembagian Data Kepada Pihak Ketiga
Kami **TIDAK PERNAH dan TIDAK AKAN PERNAH** menjual, menyewakan, atau memperdagangkan data pribadi maupun keuangan Anda kepada pihak ketiga atau pengiklan.

Kami hanya menggunakan penyedia layanan pihak ketiga yang tepercaya untuk mendukung fungsi aplikasi:
- **Google Firebase (Firebase Auth & Firestore):** Digunakan untuk autentikasi pengguna dan penyimpanan cloud terenkripsi. Kebijakan privasi Google Firebase berlaku di sini: [Google Privacy Policy](https://policies.google.com/privacy).

---

## 4. Keamanan Data
Kami menerapkan standar keamanan teknis yang ketat untuk melindungi data Anda:
- Komunikasi jaringan antara aplikasi dan Firebase dilindungi oleh enkripsi **HTTPS/TLS**.
- Data lokal disimpan di ruang penyimpanan terisolasi milik aplikasi yang tidak dapat diakses oleh aplikasi lain di perangkat Anda.

---

## 5. Hak Pengguna & Penghapusan Akun (Account Deletion)
Anda memiliki kendali penuh atas data Anda sendiri:
1. **Ekspor Data:** Anda dapat mengekspor seluruh catatan keuangan Anda ke dalam format PDF atau CSV kapan saja melalui menu *Pengaturan -> Ekspor Laporan*.
2. **Penghapusan Akun & Data (Mandatory):** 
   - Anda dapat menghapus seluruh akun dan data cloud Anda secara permanen langsung dari dalam aplikasi melalui menu **Pengaturan -> Hapus Akun & Data Permanen**.
   - Penghapusan akun akan menghapus kredensial Firebase Auth Anda dan memusnahkan seluruh dokumen transaksi cloud Anda secara otomatis.

---

## 6. Privasi Anak-Anak
Aplikasi TrackIt tidak ditujukan untuk anak-anak di bawah usia 13 tahun. Kami tidak secara sadar mengumpulkan data pribadi dari anak-anak di bawah 13 tahun.

---

## 7. Perubahan Kebijakan Privasi
Kami dapat memperbarui Kebijakan Privasi ini dari waktu ke waktu. Perubahan akan dipublikasikan di halaman ini dengan tanggal revisi terbaru.

---

## 8. Hubungi Kami
Jika Anda memiliki pertanyaan atau kendala terkait Kebijakan Privasi ini, silakan hubungi kami di:
- **Email:** support@trackit.app
- **Website:** https://trackit.app
