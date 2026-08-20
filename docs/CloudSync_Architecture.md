# TrackIt - Cloud Sync Architecture (Dual-Engine)

Dokumen ini menjelaskan arsitektur sinkronisasi *cloud* pada aplikasi TrackIt. Fitur ini memungkinkan pengguna untuk memilih menggunakan aplikasi secara **Offline-First** (100% lokal) atau **Online-Sync** (tersinkronisasi secara *real-time* ke Firebase Firestore).

## 1. Latar Belakang & Masalah (Kenapa Tidak Murni NoSQL?)
Awalnya, terdapat rencana untuk mengimplementasikan *Strategy Pattern* di mana Repositori akan diganti sepenuhnya dari *Room (SQLite)* menjadi *Firestore (NoSQL)* ketika mode online diaktifkan.

Namun, TrackIt memiliki fitur **Dashboard dan Statistik** yang sangat kompleks, yang bergantung pada kueri agregasi SQL tingkat lanjut seperti:
- `SUM(amount) ... GROUP BY category`
- Penggabungan filter berdasarkan rentang waktu (`startOfMonth` hingga `endOfMonth`).

Firestore (NoSQL) **tidak memiliki fitur bawaan** untuk melakukan agregasi sekompleks ini tanpa harus menarik seluruh dokumen ke memori klien, yang mana akan sangat lambat dan memakan banyak kuota internet pengguna.

## 2. Solusi: Arsitektur "SyncManager" (Room + Sinkronisasi Latar Belakang)
Untuk mempertahankan kecepatan kilat *Room* dan kemampuan komputasi grafiknya, kita menggunakan pendekatan **SyncManager**. 
- **Room (SQLite)** tetap menjadi sumber kebenaran tunggal (*Single Source of Truth*) untuk antarmuka pengguna (UI) dan logika statistik.
- **SyncManager** bertindak sebagai jembatan *real-time* di latar belakang yang mendengarkan (*listen*) perubahan pada Firestore, dan mengirimkan (*push*) perubahan lokal ke Firestore.

### Alur Kerja:
1. **Mode Offline:** `SyncManager` tertidur. Aplikasi bekerja 100% menggunakan Room. Tidak ada data yang keluar dari perangkat.
2. **Saat Mode Online Diaktifkan:**
   - `SyncManager` menjalankan `performInitialSync()` untuk mengunggah seluruh transaksi lokal dan data *wedding* ke akun Firestore pengguna.
   - `SyncManager` membuka *SnapshotListener* ke Firestore. Jika pengguna pasangannya di HP lain (dengan akun yang sama) menambahkan pengeluaran pernikahan, Firestore akan mengirimkan pembaruan.
   - `SyncManager` secara diam-diam menyimpan pembaruan tersebut ke dalam basis data lokal Room.
   - Karena UI menggunakan *Flow* dari Room, grafik dan daftar transaksi akan langsung ter-update otomatis secara *real-time* tanpa perlu di-*refresh*.

## 3. Penanganan Konflik dan ID (Primary Keys)
Menggabungkan data dari dua perangkat SQLite yang berbeda memiliki satu tantangan utama: **Bentrok ID Otomatis (Auto-generate ID Collision)**. 
Jika HP A dan HP B sama-sama membuat transaksi pertama mereka, keduanya akan memiliki `ID = 1` di database lokal mereka.

Penyelesaian yang diterapkan:
- **Untuk Modul Wedding (`WeddingExpense` & `WeddingTask`):** Menggunakan `String` berbasis `UUID.randomUUID()` sebagai *Primary Key*. Hal ini menjamin tidak akan pernah ada bentrok ID antar perangkat di Firestore.
- **Untuk Transaksi Utama (`TransactionEntity`):** Karena sudah terlanjur menggunakan `Long` auto-generate, kita menggunakan kombinasi `createdAt` + `profileId` sebagai ID Dokumen unik di Firestore. Saat mengunduh data dari Cloud, `SyncManager` menggunakan fungsi `getByCreatedAt()` pada DAO untuk mengecek apakah transaksi ini sudah ada (update) atau baru (insert).

## 4. Komponen Kunci
- `SyncPreferences.kt`: Menyimpan status *toggle* (Online/Offline) dan UID pengguna dari Firebase Auth menggunakan DataStore.
- `AuthRepository.kt`: Menangani fungsi Login (Email/Google), Register, dan Logout.
- `SyncManager.kt`: Otak sinkronisasi yang berjalan di *CoroutineScope*. Mengandung *listeners* Firestore untuk `transactions`, `wedding_expenses`, dan `wedding_tasks`.
- **Injeksi Repositori:** Fungsi *insert, update, delete* pada `TransactionRepository`, `WeddingTaskRepository`, dan `WeddingExpenseRepository` telah disuntikkan pemanggilan (*hook*) ke `SyncManager.push...()` untuk memicu unggahan ke *cloud* sesaat setelah data berhasil disimpan di lokal.

## 5. Prasyarat Pengujian
Bagi pengembang yang akan melanjutkan pengembangan atau pengetesan sinkronisasi:
1. Pastikan **Email/Password Authentication** diaktifkan di Firebase Console > Authentication > Sign-in method.
2. File `google-services.json` harus diletakkan di dalam modul `app/`.
3. Direkomendasikan menggunakan dua perangkat/emulator sekaligus, melakukan *login* dengan akun yang sama, dan melihat perubahan secara ajaib muncul di layar perangkat lain dalam hitungan milidetik.
