* **Pembaruan UI Layar Pemilihan Modul (Akses Penuh)**:
    * Redesain tampilan layar selamat datang untuk akun dengan lisensi *Full Access* menggunakan tema terang (*Soft Sage Greens*) yang elegan dan profesional.
    * Menghilangkan elemen visual emoji dan menggantinya dengan ikon Material resmi, typography Plus Jakarta Sans, serta kartu modul yang lebih clean dan modern.
* **Perbaikan Deteksi Format Installer**:
    * Memperbaiki logika parser aset rilis pembaruan aplikasi agar secara spesifik mengunduh berkas `.apk` (mengabaikan berkas bundle `.aab`) untuk mencegah galat saat mengurai paket.
* **Keamanan & Kompatibilitas Sistem**:
    * Penambahan izin `REQUEST_INSTALL_PACKAGES` dan proteksi navigasi instalasi unknown sources pada Android 8.0+.
