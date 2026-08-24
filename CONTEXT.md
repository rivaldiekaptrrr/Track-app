# Domain Glossary — TrackIt

Glosarium ini adalah sumber kebenaran tunggal (*single source of truth*) untuk terminologi di seluruh proyek TrackIt.
Jika sebuah istilah di kode bertentangan dengan definisi di sini, kode-lah yang harus diperbaiki.

---

## Core Architecture

* **Profile**: Workspace utama tingkat tertinggi. Satu instalasi aplikasi dapat memiliki banyak `Profile`. Setiap `Profile` memiliki satu `Mode`.
* **Mode**: Konteks operasional sebuah `Profile`. Saat ini ada dua:
  * `EXPENSE` — Pelacakan keuangan harian (pemasukan, pengeluaran, anggaran).
  * `WEDDING` — Manajemen proyek pernikahan (checklist, vendor, tamu, dsb).

---

## Financial Tracking (EXPENSE Mode)

* **Transaction**: Satu catatan keuangan (Pemasukan, Pengeluaran, atau Transfer) pada tanggal tertentu. Milik sebuah `Profile`.
* **Category**: Klasifikasi untuk sebuah `Transaction` (contoh: Makan, Transport). Milik sebuah `Profile`.
* **Budget**: Batas pengeluaran bulanan yang ditetapkan terhadap sebuah `Category`.

---

## Wedding Planning (WEDDING Mode)

> **ATURAN ISOLASI DOMAIN (Opsi A):**
> `WeddingExpense` dan `Transaction` adalah dua pulau yang **sepenuhnya terpisah**.
> Mode Wedding Planner melacak **Payables (tagihan)** — berapa target biaya dan sudah dibayar berapa.
> Mode Wedding Planner TIDAK melacak **Cashflow (arus kas)** — saldo dompet pengguna di dunia nyata.
> Pengguna yang ingin melacak keduanya harus mencatat secara manual di masing-masing mode.

* **WeddingProfile**: Konfigurasi metadata untuk sebuah `Profile` bertipe `WEDDING`. Berisi nama pengantin, total anggaran, agama, dan adat istiadat. Dihubungkan ke `ProfileEntity` induknya via kolom `profileId (Long)`. Relasi ini bersifat **1:1**.
* **WeddingExpense**: Kewajiban finansial yang berkaitan dengan pernikahan. Berbeda dari `Transaction`, entitas ini melacak *siklus pembayaran* (Estimasi Biaya vs Total Terbayar) dan mengidentifikasi pembayar (`paidBySource`: CPP, CPW, atau BERSAMA).
* **WeddingTask**: Item checklist yang harus diselesaikan sebelum hari-H, ditetapkan ke fase (bulan sebelum D-Day) dan Penanggung Jawab (PIC).
* **WeddingVendor**: Penyedia layanan pihak ketiga (Catering, Dekorasi, MUA, dll). Sering terkait dengan sebuah `WeddingExpense`.
* **WeddingGuest**: Undangan ke pernikahan. Dapat dikategorikan berdasarkan status VIP, RSVP, dan sesi (Akad/Resepsi/Keduanya).
* **WeddingCommitteeMember**: Anggota panitia internal (Saksi, Sambutan, Doa, dll) dari pihak keluarga atau teman.
* **WeddingPaymentTerm**: Jadwal cicilan untuk sebuah `WeddingExpense` (DP 1, DP 2, Pelunasan, dsb).
* **WeddingSeserahan**: Item yang dipertukarkan antara kedua keluarga (Seserahan CPP → CPW, Balasan CPW → CPP, atau Mahar).
* **WeddingDocument**: Dokumen administratif yang perlu disiapkan (KTP, Akte, Surat Keterangan, dll).
* **WeddingEvent**: Satu rangkaian acara dalam pernikahan (Lamaran, Siraman, Akad, Resepsi). Bersifat *user-defined*.
* **WeddingRundownItem**: Satu baris dalam timetable (`WeddingEvent`). Menit-ke-menit: waktu mulai, durasi, sesi, PIC, dan teks panduan MC.

---

## Sync Architecture

* **SyncManager**: Komponen singleton yang mengelola sinkronisasi dua arah antara Room (lokal) dan Firestore (remote) via HTTP/REST.
* **FirestoreRestClient**: HTTP client (OkHttp) yang berkomunikasi langsung dengan `firestore.googleapis.com`. Digunakan karena gRPC diblokir di beberapa jaringan.
* **FirestoreMapper**: Konverter antara entitas Kotlin dan format JSON Firestore REST yang *strongly-typed*.
