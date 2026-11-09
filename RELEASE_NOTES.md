* **Fitur Baru: Pembacaan Transaksi Terakhir e-Money & Auto-Categorization NFC (v4.2.0)**:
    * **Riwayat Transaksi Terakhir**: Ekstraksi instan transaksi terakhir langsung dari chip kartu nirsentuh BCA Flazz (SFI 4 & 5 records), Mandiri e-Money (G1 & G2), BNI TapCash, dan BRI Brizzi mencakup nominal, sisa saldo, timestamp, dan ID terminal transaksi.
    * **Resolusi Lokasi Cerdas**: Konversi otomatis ID terminal/merchant hex menjadi nama lokasi manusiawi (seperti *"Stasiun KRL Tangerang"*, *"Gerbang Tol Pasteur"*) menggunakan basis data lokal Room `nfc_locations` dan sinkronisasi remote API lookup.
    * **Pencocokan & Pembelajaran Kategori Mandiri**: Sistem cerdas mengekstraksi kata kunci transit penting (Stasiun, Tol, Halte, Terminal, Bandara, dsb.) dan memetakannya ke kategori yang sesuai. Pengguna dapat memilih kategori secara interaktif saat lokasi belum dikenal dan sistem akan otomatis mengingat kata kunci tersebut untuk transaksi selanjutnya.
    * **UI/UX Transaksi Terakhir di Bottom Sheet**: Kartu ringkasan transaksi terakhir dengan rincian nominal pengeluaran, waktu, lokasi, status pencocokan kategori, serta tombol 1-tap *"Catat Transaksi Ini"* atau dialog pemilihan kategori ramah pengguna.
* **Fitur Cek Saldo e-Money via NFC (Mode Expense)**:
    * Dukungan pembacaan saldo dan nomor kartu nirsentuh untuk 6 penerbit kartu uang elektronik utama di Indonesia: Mandiri e-Money (Chip G1 & G2), BCA Flazz, BNI TapCash, BRI Brizzi, Bank DKI JakCard, dan Bank Mega MegaCash.
    * Implementasi engine APDU modern berbasis Android `IsoDep` (`NfcAdapter.enableReaderMode`) dengan penanganan timeout dan ekstraksi Little/Big-Endian yang cepat dan stabil.
    * Desain kartu fisik realistis sesuai proporsi ISO/IEC 7810 ID-1 dengan grafis chip kontak mikro emas, palet warna korporat resmi masing-masing bank, dan tipografi saldo tegas.
    * Modal Bottom Sheet pemindai dengan animasi radar NFC lembut, respons getaran haptic, dan tombol pintas *"Catat Transaksi Kartu Ini"*.
* **Manajemen & Penonaktifan Transaksi Berulang**:
    * Penyempurnaan kontrol dan konfirmasi peringatan visual saat mematikan sakelar transaksi berulang di form edit transaksi.
    * State tracking `wasOriginallyRecurring` untuk menjamin pemutusan rantai repetisi masa depan di Room DB dan Firestore.
