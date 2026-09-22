# Deskripsi Bisnis TrackIt untuk Pendaftaran Midtrans

Berikut adalah rancangan profil dan deskripsi bisnis yang bisa Anda gunakan (copy-paste atau sesuaikan) saat mendaftar atau melalui proses verifikasi/KYC di Midtrans. Deskripsi ini dibuat secara profesional agar pihak Midtrans (dan pihak bank akuisisi) mudah memahami model bisnis Anda.

---

## 1. Nama Aplikasi / Entitas Bisnis
**TrackIt** (atau sebutkan nama perusahaan/entitas Anda jika ada, misal: TrackIt App)

## 2. Kategori Bisnis
**Perangkat Lunak / Layanan Digital (Software / Digital Goods)** - Aplikasi Produktivitas & Manajemen Keuangan.

## 3. Deskripsi Singkat Bisnis (Executive Summary)
TrackIt adalah aplikasi mobile berbasis Android yang berfokus pada produktivitas dan manajemen kehidupan sehari-hari penggunanya. Saat ini, TrackIt memiliki dua fitur utama: 
1. **Expense Tracker:** Untuk mencatat, melacak, dan mengelola keuangan pribadi.
2. **Wedding Planner:** Alat bantu komprehensif bagi calon pengantin untuk merencanakan dan mengelola berbagai tugas persiapan pernikahan.

## 4. Model Bisnis (Cara Mendapatkan Pendapatan)
Kami beroperasi dengan model bisnis **B2C (Business-to-Consumer)** melalui penjualan perangkat lunak. Monetisasi dilakukan dengan sistem **One-Time Purchase (Pembelian Satu Kali untuk Akses Seumur Hidup)**, bukan sistem berlangganan (subscription). 

Pengguna dapat mengunduh dan menggunakan fitur dasar aplikasi secara gratis. Namun, untuk membuka kunci (unlock) akses ke fitur premium—seperti Modul Wedding Planner secara penuh—pengguna harus melakukan pembayaran satu kali di dalam aplikasi (In-App Payment).

## 5. Mengapa Menggunakan Midtrans? (Tujuan Penggunaan)
Kami membutuhkan layanan payment gateway dari Midtrans untuk memfasilitasi transaksi secara instan dan otomatis di dalam aplikasi. Midtrans akan digunakan untuk menerima pembayaran dari pelanggan di Indonesia menggunakan berbagai metode pembayaran yang populer, seperti **Virtual Account (VA), e-Wallet (GoPay, ShopeePay), dan QRIS**.

## 6. Alur Transaksi Pengguna (Customer Journey)
Untuk memberikan gambaran yang jelas kepada tim kepatuhan (Compliance) Midtrans, berikut adalah alur transaksi pelanggan kami:
1. **Pilih Layanan:** Pengguna yang menggunakan versi gratis memilih opsi "Upgrade ke Premium / Buka Akses Wedding" di dalam aplikasi TrackIt.
2. **Checkout:** Aplikasi menampilkan halaman ringkasan pembayaran dan memanggil layanan Midtrans Snap untuk menampilkan opsi metode pembayaran.
3. **Pembayaran:** Pelanggan memilih metode pembayaran (misal: QRIS atau GoPay) dan menyelesaikan transaksi.
4. **Verifikasi:** Midtrans memproses pembayaran dan mengirimkan notifikasi (Webhook) secara real-time ke sistem backend serverless kami.
5. **Akses Diberikan:** Sistem kami memverifikasi notifikasi dari Midtrans, memperbarui tingkat akses pengguna (Access Level) di database kami, dan fitur premium di aplikasi pengguna langsung terbuka secara otomatis tanpa perlu intervensi manual.

## 7. Kebijakan Pengembalian Dana (Refund Policy) & Pengiriman
- **Pengiriman (Delivery):** Barang berupa produk digital. Akses premium diberikan secara real-time (instan) ke akun pengguna segera setelah pembayaran dikonfirmasi berhasil oleh Midtrans.
- **Refund Policy:** Mengingat produk kami adalah barang digital berupa lisensi seumur hidup yang langsung aktif, semua pembelian bersifat final dan tidak dapat dikembalikan (No Refund), kecuali terdapat kesalahan dari sistem kami yang menyebabkan fitur tidak terbuka setelah pembayaran berhasil. Hal ini dicantumkan pada Syarat & Ketentuan di dalam aplikasi.

---

### Tips Tambahan Saat Proses Midtrans:
1. **Website / Link Aplikasi:** Biasanya Midtrans meminta link website atau link Play Store. Jika belum masuk Play Store, Anda bisa membuat **Landing Page sederhana** (misal menggunakan Carrd.co, GitHub Pages, atau Vercel secara gratis) yang berisi penjelasan aplikasi, screenshot, dan Terms & Conditions.
2. **KTP dan Rekening Bank:** Pastikan nama di KTP dan nama di buku tabungan rekening pencairan (settlement) yang Anda daftarkan di Midtrans sesuai, agar proses verifikasi lancar.
3. **Terms and Conditions (T&C) & Privacy Policy:** Midtrans (dan bank) sangat memperhatikan hal ini. Pastikan Anda memiliki T&C dan Privacy Policy yang bisa diakses publik (minimal diletakkan di Google Drive yang di-set *public* atau di Landing Page).
