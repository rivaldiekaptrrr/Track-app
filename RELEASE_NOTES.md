* **Integrasi Otomatis Midtrans Payment Gateway**:
    * Dukungan pembayaran instan Midtrans Snap (QRIS, Virtual Account, E-Wallet) langsung dari dalam aplikasi.
    * Pemrosesan otomatis lisensi via backend serverless Vercel & webhook Firestore tanpa perlu verifikasi manual admin.
* **Sistem Lisensi & Kontrol Akses (RBAC Multi-Modul)**:
    * Pembagian akses modular: **Expense Tracker**, **Wedding Planner**, dan **Full Access (Paket Hemat)**.
    * Proteksi akses per fitur dan auto-redirect setelah pembayaran terkonfirmasi.
* **Redesain Halaman Pembayaran & Pemilihan Paket**:
    * Tampilan antarmuka baru bertema terang (*Soft Sage Greens*) yang bersih dan modern.
    * Penerapan strategi *Price Anchoring* & *Strikethrough Price* dengan badge diskon (*Promo Rilis* & *Hemat 30%*).
* **Peningkatan Stabilitas & Realtime Sync**:
    * Auto-polling background status pembayaran dan realtime listener Firestore.
    * Penyesuaian profil dan backup sinkronisasi akun.
