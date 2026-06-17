# 🎨 TrackIt: UI/UX Onboarding & Feature Discovery Strategy

Strategi ini dirancang untuk memastikan pengguna baru memahami seluruh fitur unggulan **TrackIt** tanpa merasa kewalahan, menjaga antarmuka tetap bersih (*clean UX*), dan memaksimalkan retensi pengguna.

---

## 🏛️ Filosofi Utama
1.  **Minimal Intrusion:** Tidak ada pop-up yang mengganggu alur kerja utama pengguna.
2.  **Contextual Learning:** Edukasi diberikan tepat saat fitur tersebut akan digunakan (*Just-in-Time*).
3.  **Progressive Disclosure:** Fitur canggih diperkenalkan secara bertahap setelah pengguna menguasai fungsi dasar.
4.  **Magic Experience:** Menonjolkan fitur AI dan Voice sebagai kejutan yang menyenangkan.

---

## 🔍 1. Logika Deteksi Pengguna (Backend Flow)
Aplikasi secara otomatis membedakan antara instalasi baru murni dan pengguna lama yang melakukan instalasi ulang.

*   **Pengecekan:** Saat aplikasi diluncurkan, sistem memeriksa Room Database lokal.
*   **Database Kosong:** Menjalankan alur **User Baru** (Carousel Onboarding).
*   **Database Terisi (Auto-Restore):** Menjalankan alur **User Lama** (Langsung ke Dashboard, lewati Onboarding).

---

## 🚀 2. Fase 1: First Impression (Onboarding)
Tujuan: Membangun kepercayaan dan menjelaskan nilai unik aplikasi.

### A. Walkthrough Carousel (Static/Animated)
Tampilan 3 slide yang dapat di-swipe saat pertama kali aplikasi dibuka:
1.  **Voice Tracking:** "Catat transaksi secepat berbicara."
2.  **AI Self-Learning:** "Semakin sering digunakan, semakin pintar aplikasinya."
3.  **100% Privacy:** "Data dan suara diproses sepenuhnya di HP kamu (Offline)."

### B. Interactive Dashboard Spotlight
Saat pertama kali masuk ke Dashboard:
*   **Educative Empty State:** Menampilkan ilustrasi mikrofon dengan teks: *"Belum ada data. Ayo coba catat transaksi pertamamu!"*
*   **Spotlight on FAB (+):** Layar sedikit gelap, menyorot tombol utama dengan instruksi: 
    *   `Tahan (Long-press) untuk Suara 🎤`
    *   `Ketuk (Single tap) untuk Input Manual ✏️`

---

## 💡 3. Fase 2: Natural Discovery (Edukasi Kontekstual)
Tujuan: Memperkenalkan fitur inti tanpa mengganggu navigasi.

| Fitur | Metode Penemuan (UX Pattern) |
| :--- | :--- |
| **Smart Calculator** | Teks *hint* samar pada kolom nominal: *"Contoh: 50.000 atau 20.000 + 15.000"*. |
| **Custom Categories** | Tombol **"+ Tambah Kategori"** di akhir daftar kategori yang ada. |
| **AI Self-Learning** | *Snackbar* notifikasi setelah input manual: *"Kategori disimpan! AI akan mengingat kata ini ke depannya."* |
| **Recurring Transaction** | *Checkbox* opsional di bagian bawah formulir input manual. |
| **Budget/Anggaran** | Teks CTA pada kartu saldo di Dashboard: *"Set Anggaran Bulanan untuk melihat progres"*. |
| **Export Report** | Ikon *Download* standar di pojok kanan atas layar Riwayat Transaksi. |

---

## 📈 4. Fase 3: Progressive Unlocking (Untuk Fitur Killer)
Tujuan: Mengingatkan fitur luar biasa yang tidak terlihat di UI utama.

*   **Voice Widget (Transparent):** Setelah pengguna sukses mencatat 5 transaksi via suara, tampilkan *banner* kecil yang dapat ditutup di Dashboard: *"Pasang Voice Widget di Home Screen untuk akses lebih cepat!"*
*   **Multi-Profile Switcher:** Memberikan animasi "pulse" (denyut) tipis pada ikon profil di navigasi bawah setelah beberapa hari penggunaan.

---

## ⚙️ 5. Fase 4: Settings & Power-User Tools
Tujuan: Tempat penyimpanan fitur teknis dan pengaturan privasi.

Menu **Settings** akan menjadi pusat kendali untuk:
1.  **Keamanan:** Toggle aktivasi sidik jari (Biometric Auto-Lock).
2.  **Backup & Restore:** Status sinkronisasi ke akun Google Drive.
3.  **Tips & Trik:** Halaman panduan singkat untuk fitur tersembunyi seperti:
    *   **Batch Voice Input:** Cara menyebutkan banyak transaksi sekaligus.
    *   **Gesture Shortcuts:** Panduan navigasi cepat.

---

*Dokumen ini merupakan hasil rangkuman konsultasi strategi UI/UX TrackIt.*
