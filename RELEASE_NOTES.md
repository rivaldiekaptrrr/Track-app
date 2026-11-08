* **Fitur Baru: Cek Saldo e-Money via NFC (Mode Expense)**:
    * Dukungan pembacaan saldo dan nomor kartu nirsentuh untuk 6 penerbit kartu uang elektronik utama di Indonesia: Mandiri e-Money (Chip G1 & G2), BCA Flazz, BNI TapCash, BRI Brizzi, Bank DKI JakCard, dan Bank Mega MegaCash.
    * Implementasi engine APDU modern berbasis Android `IsoDep` (`NfcAdapter.enableReaderMode`) dengan penanganan timeout dan ekstraksi Little/Big-Endian yang cepat dan stabil.
    * Desain kartu fisik realistis sesuai proporsi ISO/IEC 7810 ID-1 dengan grafis chip kontak mikro emas, palet warna korporat resmi masing-masing bank, dan tipografi saldo tegas.
    * Modal Bottom Sheet pemindai dengan animasi radar NFC lembut, respons getaran haptic, dan tombol pintas *"Catat Transaksi Kartu Ini"*.
* **Manajemen & Penonaktifan Transaksi Berulang**:
    * Penyempurnaan kontrol dan konfirmasi peringatan visual saat mematikan sakelar transaksi berulang di form edit transaksi.
    * State tracking `wasOriginallyRecurring` untuk menjamin pemutusan rantai repetisi masa depan di Room DB dan Firestore.
* **Optimalisasi Lingkungan & Pengetahuan Proyek**:
    * Migrasi penuh penyimpanan knowledge graph arsitektur proyek ke server MCP Memory.
