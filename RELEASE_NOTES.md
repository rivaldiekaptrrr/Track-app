* **Perbaikan Kritis In-App Installer**:
    * Menambahkan izin `REQUEST_INSTALL_PACKAGES` di AndroidManifest untuk mendukung pemasangan APK pembaruan di Android 8.0+ tanpa crash.
    * Menambahkan penanganan proteksi try-catch dan navigasi aman saat meminta izin instalasi aplikasi tidak dikenal (*unknown sources*).
* **Penyempurnaan Stabilitas & Performa**:
    * Peningkatan kestabilan proses unduhan pembaruan langsung dari dialog aplikasi.
