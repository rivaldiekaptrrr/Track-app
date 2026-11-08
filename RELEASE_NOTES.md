* **Manajemen & Penonaktifan Transaksi Berulang (Mode Expense)**:
    * Penyempurnaan UI form edit transaksi dengan kartu kontrol frekuensi dan status aktif jadwal repetisi.
    * Menambahkan konfirmasi peringatan visual saat mematikan sakelar repetisi untuk memastikan penghentian jadwal otomatis berikutnya sebelum disimpan.
    * State tracking `wasOriginallyRecurring` pada ViewModel untuk memastikan pemutusan rantai transaksi berulang berjalan konsisten.
* **Optimalisasi Lingkungan & Pengetahuan Proyek**:
    * Migrasi penuh penyimpanan knowledge graph arsitektur proyek ke server MCP Memory.
    * Pembersihan modul dan artefak lama Graphify.
