# Panduan Pengembang (Developer Guide): RBAC & Panel Super Admin

**Proyek:** TrackIt (Expense & Wedding Planner)  
**Versi:** 2.1.0  
**Target Pengembang:** Developer, Tester, & Owner Aplikasi  

---

## 1. Konsep Utama & Model Akses

Aplikasi TrackIt didistribusikan secara langsung dalam bentuk APK (Direct-Sale via platform seperti Trakteer atau Lynk.id, tanpa In-App Purchase Google Play Store). Untuk mengontrol akses pengguna setelah membeli, diterapkan sistem **Role-Based Access Control (RBAC)** berlapis yang dikendalikan melalui **Panel Super Admin di dalam aplikasi**.

### 5 Tingkat Akses (`AccessLevel`):
1. **`ADMIN`**: Hak akses tertinggi (Super Admin). Mengakses **Panel Admin** untuk membuka/mengubah hak akses pengguna lain.
2. **`NONE`** *(Default)*: Status awal pengguna baru setelah mendaftar. Terkunci di layar **Pending Verification** hingga akses diaktifkan oleh Admin.
3. **`EXPENSE`**: Akses khusus untuk fitur Pengelola Keuangan (Expense Tracker).
4. **`WEDDING`**: Akses khusus untuk fitur Perencana Pernikahan (Wedding Planner).
5. **`BOTH`**: Akses ke kedua modul (Expense & Wedding Planner), disertai layar pemilih modul dan switcher modul di Pengaturan.

---

## 2. Cara Kerja & Pembuatan Akun Super Admin

### A. Di Mana Password Admin Ditentukan?
Password **TIDAK di-hardcode** di dalam aplikasi. Pengautentikasian menggunakan **Firebase Authentication**. Password ditentukan sendiri oleh Anda saat pendaftaran pertama kali di aplikasi.

### B. Langkah Membuat / Mengaktifkan Akun Super Admin Pertama Kali
1. Buka aplikasi TrackIt (di Emulator atau HP Android).
2. Masuk ke layar **Daftar / Register** (atau *Login dengan Google* jika aktif).
3. Isi pendaftaran dengan email khusus Super Admin:
   * **Email:** `rivaldiekaptr2001@gmail.com`
   * **Password:** *(Bebas, buat password rahasia Anda sendiri)*
4. Klik **Daftar**.
5. **Selesai!** Aplikasi akan otomatis mengenali email tersebut sebagai Super Admin dan langsung membawa Anda ke layar **Panel Admin**.

### C. Mengapa Email Tersebut Otomatis Menjadi Super Admin?
Di dalam file [AuthRepository.kt](file:///c:/Rivaldi/Track-app/app/src/main/java/com/trackit/app/data/repository/AuthRepository.kt), terdapat konstanta dan logika verifikasi email:

```kotlin
// Lokasi: app/src/main/java/com/trackit/app/data/repository/AuthRepository.kt
const val ADMIN_EMAIL = "rivaldiekaptr2001@gmail.com"

// Saat user login / daftar, sistem mengecek email:
val isAdmin = email.equals(ADMIN_EMAIL, ignoreCase = true)
val accessLevel = if (isAdmin) AccessLevel.ADMIN else (existingDoc?.optString("accessLevel") ?: AccessLevel.NONE)
```

Setiap kali email `rivaldiekaptr2001@gmail.com` login/mendaftar, sistem otomatis menetapkan `"accessLevel": "ADMIN"` di dokumen Firestore dan menyimpannya di cache lokal (DataStore).

---

## 3. SOP Penggunaan Panel Admin (Cara Membuka Akses Pembeli)

Ketika ada pembeli baru yang membeli APK melalui Trakteer / Lynk.id:

1. **Pembeli Mendaftar di Aplikasi:**
   * Pembeli mendownload APK, lalu mendaftar akun menggunakan email mereka sendiri.
   * Akun pembeli otomatis berstatus `NONE` (Tampilan layar *Pending Verification*).
2. **Pembeli Mengirimkan Email Terdaftar ke Admin:**
   * Pembeli mengirimkan bukti bayar & email yang terdaftar di aplikasi kepada Anda.
3. **Admin Membuka Akses via Aplikasi:**
   * Buka aplikasi TrackIt dan login dengan email `rivaldiekaptr2001@gmail.com`.
   * Anda akan langsung masuk ke **Panel Admin**.
   * Cari email pembeli di kolom pencarian atau filter tab **NONE**.
   * Pada kartu nama pembeli, pilih tingkat akses yang dibeli (**EXPENSE**, **WEDDING**, atau **BOTH**).
   * Perubahan langsung tersimpan ke Firestore secara real-time.
4. **Pengguna Mengakses Fitur:**
   * Pembeli menekan tombol **"Cek Status Akses"** di layar *Pending Verification* HP mereka.
   * Layar aplikasi pembeli otomatis terbuka dan masuk ke modul yang sesuai tanpa perlu login ulang.

---

## 4. Lapisan Perlindungan Akses Komprehensif (Comprehensive Protection Layers)

Sistem menerapkan proteksi menyeluruh di setiap titik aplikasi:

| No | Titik Proteksi | Mekanisme Perlindungan & Fallback UX |
| :--- | :--- | :--- |
| **1** | **Dialog Buat Profil Baru (`ProfileFormDialog`)** | Opsi tipe profil yang di luar izin lisensi pengguna otomatis terkunci dengan ikon 🔒. Jika diklik, muncul pesan edukasi upgrade lisensi. |
| **2** | **Daftar & Switcher Profil (`BottomNavBar` & `ProfileManagementScreen`)** | Profil yang tidak sesuai lisensi ditandai badge 🔒 *Terkunci*. Klik pada profil tersebut diblokir dengan dialog peringatan lisensi. |
| **3** | **Auto-Correction Profil Startup (`MainActivity`)** | Jika akun berlisensi `WEDDING` tetapi profil aktif lokal adalah `EXPENSE`, sistem otomatis berpindah ke profil wedding (dan sebaliknya untuk lisensi `EXPENSE`). |
| **4** | **Proteksi Asisten Suara (`TransparentVoiceActivity`)** | Widget / shortcut asisten suara memeriksa lisensi sebelum membuka dialog suara. Pengguna `NONE` atau `WEDDING` diblokir dengan pesan informasi. |
| **5** | **Proteksi App Launcher Shortcut (`MainActivity`)** | Shortcut Android *voice_add_transaction* hanya diizinkan membuka form transaksi jika pengguna memiliki lisensi `EXPENSE`, `BOTH`, atau `ADMIN`. |
| **6** | **Proteksi Cloud Sync (`SyncManager` & `MainActivity`)** | Sinkronisasi awan `syncManager.startSync()` diblokir untuk akun berstatus `NONE`. |
| **7** | **Informasi Lisensi & Navigasi Modul (`SettingsScreen` & `WeddingSettingsScreen`)** | Menampilkan badge lisensi aktif (`EXPENSE` / `WEDDING` / `BOTH` / `ADMIN`) dan tombol *"Beralih Modul (Pilih Aplikasi)"* khusus untuk lisensi `BOTH`. |

---

## 5. Panduan Pengujian (Testing & QA Scenarios)

### Skenario 1: Verifikasi Akun Super Admin
* **Langkah:** Login/Register dengan `rivaldiekaptr2001@gmail.com`.
* **Hasil Yang Diharapkan:** Langsung masuk ke layar **Admin Dashboard**. Muncul daftar pengguna, filter tab status, dan tombol pengubah hak akses.

### Skenario 2: Verifikasi Pembeli Baru (Default Locked)
* **Langkah:** Register akun baru dengan email biasa (misal: `user1@gmail.com`).
* **Hasil Yang Diharapkan:** Pengguna masuk ke layar **Pending Verification** (Layar terkunci dengan informasi pembelian Trakteer/Lynk.id). Tombol *Offline / Lewati Login* tidak tersedia.

### Skenario 3: Aktivasi Akses oleh Admin & Tombol Cek Akses
* **Langkah:**
  1. Login sebagai Admin (`rivaldiekaptr2001@gmail.com`).
  2. Cari `user1@gmail.com`, ubah aksesnya menjadi **EXPENSE**.
  3. Di HP `user1@gmail.com`, tekan tombol **"Cek Status Akses"**.
* **Hasil Yang Diharapkan:** Layar *user1* langsung berpindah dari *Pending Verification* ke **Dashboard Expense**.

### Skenario 4: Proteksi Tambah & Ganti Profil
* **Langkah:** Login dengan akun berlisensi `WEDDING`. Masuk ke menu *Profil* -> *Tambah Profil Baru*.
* **Hasil Yang Diharapkan:** Opsi *Expense* terkunci dengan tanda 🔒. Jika diklik, muncul peringatan lisensi. Profil berjenis *Expense* di daftar profil juga terkunci dan tidak bisa di-switch.

### Skenario 5: Verifikasi Lisensi Both (Expense + Wedding)
* **Langkah:** Admin mengubah akses `user1@gmail.com` menjadi **BOTH**.
* **Hasil Yang Diharapkan:** Pengguna dapat berpindah modul secara fleksibel melalui layar **Pilih Modul** (*ModuleSelectionScreen*) atau tombol *"Beralih Modul"* di Pengaturan.

---

## 6. Konfigurasi Aturan Keamanan Firestore (Security Rules)

Wajib dipasang di **Firebase Console -> Firestore Database -> Rules** agar pengguna biasa tidak bisa mengubah status akses mereka sendiri:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    match /users/{userId} {
      // Pengguna biasa hanya boleh membaca dan mengubah data profile miliknya sendiri
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      // Super Admin dapat membaca & mengubah akses SELURUH pengguna
      allow read, write: if request.auth != null && request.auth.token.email == 'rivaldiekaptr2001@gmail.com';
    }
    
  }
}
```

---

## 7. Cara Mengubah atau Menambah Email Admin Baru

Jika di masa depan ingin mengubah email admin atau menambah email admin sekunder:

1. Buka file **`AuthRepository.kt`** di `app/src/main/java/com/trackit/app/data/repository/AuthRepository.kt`.
2. Ubah konstanta `ADMIN_EMAIL`:
   ```kotlin
   const val ADMIN_EMAIL = "emailanda@gmail.com"
   ```
3. Perbarui juga email admin pada **Firestore Security Rules** di Firebase Console.
