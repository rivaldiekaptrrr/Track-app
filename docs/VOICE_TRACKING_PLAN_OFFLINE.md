# 🎤 Rencana Implementasi: Offline Voice Expense Tracker (v2 - Final)

Dokumen ini berisi langkah-langkah detail untuk mengganti fitur **Scan Struk (ML Kit OCR + CameraX)** menjadi fitur **Pencatatan Berbasis Suara (Voice-to-Text)** yang 100% Offline menggunakan Android `SpeechRecognizer` + Regex Pattern Matching.

> **Catatan:** Dokumen ini dibuat berdasarkan hasil *deep scan* terhadap seluruh source code project, bukan hanya berdasarkan asumsi.

---

## 🎯 Tujuan

Pengguna menekan tombol mikrofon dan mengucapkan kalimat seperti:
- **"beli sayur 50 ribu"** → Nominal: 50.000 | Kategori: Makanan | Deskripsi: beli sayur
- **"bayar wifi 300 ribu"** → Nominal: 300.000 | Kategori: Tagihan | Deskripsi: bayar wifi
- **"nonton bioskop 35 ribu"** → Nominal: 35.000 | Kategori: Hiburan | Deskripsi: nonton bioskop
- **"bensin 20 ribu"** → Nominal: 20.000 | Kategori: Transportasi | Deskripsi: bensin

Setelah diproses, form transaksi otomatis terisi. Pengguna tetap bisa mereview/mengedit sebelum menyimpan.

---

## 🏗️ Arsitektur Sistem

```
[Tombol Mikrofon] → [SpeechRecognizer (Android OS)] → Teks mentah
       ↓
[VoiceParser.kt (Regex + Dictionary)] → { nominal, kategori, deskripsi }
       ↓
[TransactionViewModel] → Update State
       ↓
[AddEditTransactionScreen] → Form terisi otomatis
```

---

## 📋 Daftar File yang Terdampak

### File yang DIHAPUS:
| File | Alasan |
|------|--------|
| `ui/scan/ScanReceiptScreen.kt` (478 baris) | Seluruh logika kamera + OCR tidak dipakai lagi |

### File yang DIMODIFIKASI:
| File | Perubahan |
|------|-----------|
| `app/build.gradle.kts` | Hapus dependency CameraX (4 baris) & ML Kit (1 baris) |
| `AndroidManifest.xml` | Hapus permission CAMERA & uses-feature camera. Tambah permission RECORD_AUDIO |
| `ui/navigation/Screen.kt` | Hapus route `ScanReceipt`. Ubah parameter `ocrAmount` → `voiceAmount` + `voiceDescription` + `voiceCategoryId` |
| `ui/navigation/TrackItNavHost.kt` | Hapus import & composable ScanReceipt. Ubah callback `onOpenCamera` → tidak dibutuhkan lagi (voice langsung di layar form). Ubah `ocrAmount` handling → voice result handling |
| `ui/transaction/AddEditTransactionScreen.kt` | Hapus parameter `onOpenCamera`. Ganti ikon kamera → ikon mikrofon. Tambah logika SpeechRecognizer + permission RECORD_AUDIO. Tambah animasi saat merekam |
| `ui/transaction/TransactionViewModel.kt` | Hapus `setAmountFromOcr()`. Tambah `setFromVoice(amount, description, categoryId)` |

### File yang DIBUAT BARU:
| File | Fungsi |
|------|--------|
| `util/VoiceParser.kt` | Regex parser untuk mengekstrak nominal, kategori, dan deskripsi dari teks Bahasa Indonesia |
| `ui/settings/CustomKeywordScreen.kt` | Layar UI baru untuk pengguna menambah/menghapus keyword custom per kategori |

---

## 🛠️ Langkah-Langkah Eksekusi (Step-by-Step)

### Tahap 1: Pembersihan Dependency (`app/build.gradle.kts`)

Hapus baris 89-97:
```diff
-    // ML Kit - Text Recognition
-    implementation("com.google.mlkit:text-recognition:16.0.0")
-
-    // CameraX
-    val cameraxVersion = "1.3.1"
-    implementation("androidx.camera:camera-core:$cameraxVersion")
-    implementation("androidx.camera:camera-camera2:$cameraxVersion")
-    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
-    implementation("androidx.camera:camera-view:$cameraxVersion")
```

**Dampak:** Ukuran APK berkurang ±50MB (ML Kit model + CameraX libraries).

---

### Tahap 2: Update Permission (`AndroidManifest.xml`)

```diff
-    <uses-feature
-        android:name="android.hardware.camera"
-        android:required="false" />
-
-    <uses-permission android:name="android.permission.CAMERA" />
+    <uses-permission android:name="android.permission.RECORD_AUDIO" />
```

**Catatan:** Permission `USE_BIOMETRIC` dan `POST_NOTIFICATIONS` tetap dipertahankan.

---

### Tahap 3: Hapus File Lama

Hapus seluruh file: `app/src/main/java/com/trackit/app/ui/scan/ScanReceiptScreen.kt`

Hapus juga folder `ui/scan/` jika kosong setelahnya.

---

### Tahap 4: Buat `VoiceParser.kt` (File Baru - Inti Logika)

**Lokasi:** `app/src/main/java/com/trackit/app/util/VoiceParser.kt`

#### 4.1 Data Class untuk Hasil Parsing
```kotlin
data class VoiceParseResult(
    val amount: Long?,           // Nominal dalam rupiah (contoh: 50000)
    val categoryName: String?,   // Nama kategori yang cocok (contoh: "Makanan")
    val description: String,     // Deskripsi / kalimat mentah
    val isValid: Boolean         // Apakah minimal nominal berhasil diekstrak
)
```

#### 4.2 Logika Ekstrak Nominal
Parser harus menangani berbagai format angka Bahasa Indonesia:

| Input Ucapan | Hasil |
|-------------|-------|
| "50 ribu" | 50.000 |
| "50ribu" (tanpa spasi) | 50.000 |
| "1 juta" | 1.000.000 |
| "1,5 juta" / "satu setengah juta" | 1.500.000 |
| "2 juta 500 ribu" | 2.500.000 |
| "500" (angka polos) | 500 |
| "lima puluh ribu" | 50.000 |
| "seratus ribu" | 100.000 |
| "dua ratus lima puluh ribu" | 250.000 |

**Strategi Parsing (2 pass):**
1. **Pass 1 - Angka Numerik:** Cari pola regex `(\d+[.,]?\d*)\s*(ribu|rb|juta|jt)` dalam teks.
2. **Pass 2 - Angka Kata:** Jika Pass 1 gagal, cari kata bilangan: "seribu", "seratus", "lima puluh", "dua puluh", dll. Konversi ke angka.

**Kata pengali yang harus dikenali:**
- `ribu`, `rb`, `rebu` → ×1.000
- `juta`, `jt` → ×1.000.000
- `setengah` → ×0.5 (khusus: "setengah juta" = 500.000)

**Kata bilangan yang harus dikenali:**
- `satu`/`se` = 1, `dua` = 2, `tiga` = 3, `empat` = 4, `lima` = 5
- `enam` = 6, `tujuh` = 7, `delapan` = 8, `sembilan` = 9
- `sepuluh` = 10, `sebelas` = 11, `belas` = suffix ×10+
- `puluh` = suffix ×10, `ratus` = suffix ×100
- `seratus` = 100, `seribu` = 1000

**Pengenalan Bahasa Gaul / Slang Angka (Auto-Correction):**
- `gocap`/`selawe` = 50.000 / 25.000
- `cepek`/`gopek` = 100.000 / 500
- `seceng`/`ceng` = 1.000
- `noban`/`goban` = 20.000 / 50.000
- **Override Logic:** Jika pengguna menyebut >1 angka berdekatan (typo sebut: "lima puluh... eh dua puluh ribu"), parser akan mengambil angka **terakhir** yang valid ("dua puluh ribu").

#### 4.3 Logika Deteksi Kategori (Dictionary Matching)

Mapping keyword → nama kategori **harus sesuai dengan data di `TrackItDatabase.getDefaultCategories()`**:

| Kategori (Exact DB Name) | Keywords |
|--------------------------|----------|
| **Makanan** | makan, sayur, buah, nasi, bakso, mie, kopi, minum, jajan, snack, gorengan, soto, sate, ayam, ikan, tahu, tempe, roti, susu, es, warung, resto, restoran, cafe, kantin |
| **Transportasi** | bensin, parkir, tol, gojek, grab, ojek, ojol, bus, kereta, angkot, taksi, taxi, bbm, solar, pertalite |
| **Hiburan** | nonton, bioskop, netflix, game, main, spotify, youtube, konser, wisata, liburan, rekreasi, karaoke |
| **Tagihan** | listrik, air, wifi, internet, pulsa, kuota, token, pdam, gas, iuran, sewa, kos, kontrakan, cicilan, kredit, pajak, asuransi |
| **Belanja** | belanja, baju, celana, sepatu, tas, online, shopee, tokopedia, lazada, beli, fashion, pakaian, kosmetik, skincare |
| **Kesehatan** | obat, dokter, rumah sakit, apotek, farmasi, vitamin, klinik, medis, cek, lab, operasi |
| **Pendidikan** | buku, sekolah, kuliah, kursus, les, spp, ukt, semester, tuition, seminar |
| **Lainnya** | *(fallback jika tidak ada keyword yang cocok)* |

> **Fitur Custom Keyword:** Selain keyword default di atas, `VoiceParser` juga akan membaca daftar keyword tambahan (Custom Keywords) yang disimpan pengguna di dalam Database/DataStore, sehingga parser bisa terus belajar.

**Aturan Prioritas:** Jika ada >1 kategori cocok, ambil yang keyword-nya muncul paling awal di kalimat. Prioritaskan Custom Keyword buatan pengguna dibandingkan Default Keyword.

#### 4.4 Logika Deskripsi
- Deskripsi = seluruh kalimat mentah hasil speech recognition.
- Contoh: ucapan "beli sayur di pasar 50 ribu" → deskripsi = "beli sayur di pasar 50 ribu"

#### 4.5 Deteksi Tanggal Pintar (Time-Travel Parsing)
- Cari keyword waktu dalam teks:
  - `"kemarin"` → Set parameter `date` ke H-1.
  - `"kemarin lusa"` → Set parameter `date` ke H-2.
  - Jika tidak ada keyword waktu → Set ke hari ini (default).

---

### Tahap 5: Fitur Kelola Kata Kunci (Menu Settings)

Agar aplikasi semakin pintar dan adaptif terhadap gaya bahasa pengguna, kita tambahkan menu untuk mengatur kata kunci (*keyword*) manual:

#### 5.1 Update Database (Room / DataStore)
- Modifikasi `CategoryEntity.kt` dengan menambahkan field baru: `val customKeywords: String = ""` (berisi keyword dipisah koma, contoh: `"kos,kontrakan,pdam"`).
- Buat *Preferences DataStore* untuk menyimpan opsi **"Aktifkan Respon Suara (TTS)"** (boolean, default: true).
- Update `TrackItDatabase` version menjadi 2 dan buatkan logika *Migration* agar data lama tidak hilang.

#### 5.2 Layar UI Baru (`CustomKeywordScreen.kt` & Settings)
- Buat halaman baru yang dapat diakses dari halaman Pengaturan (`SettingsScreen`).
- Tampilkan daftar kategori. Jika sebuah kategori diklik, muncul dialog untuk memasukkan kata-kata (keywords) baru.
- Contoh interaksi: Pengguna memilih kategori **Tagihan**, lalu mengetik kata **"kos"** dan menyimpannya. Mulai saat itu, jika pengguna merekam suara "bayar kos", sistem akan langsung memetakannya ke **Tagihan**.
- **Di Halaman Settings Utama:** Tambahkan *Switch/Toggle* untuk mengaktifkan/menonaktifkan fitur **Respon Suara (TTS)**.
- **Pintasan Offline:** Tambahkan satu baris tombol informasi *"Download Paket Suara Offline"*. Tombol ini men-trigger Intent `ACTION_VOICE_INPUT_SETTINGS` agar pengguna diarahkan ke pengaturan OS untuk mengunduh paket Bahasa Indonesia.

#### 5.3 Update VoiceParser
- Saat `VoiceParser` dipanggil di `ViewModel`, ambil juga daftar lengkap `CategoryEntity` (yang sudah memiliki data `customKeywords`). Gabungkan keyword bawaan (*hardcoded*) dengan `customKeywords` milik pengguna sebelum melakukan deteksi.

---

### Tahap 6: Update Navigation (`Screen.kt` & `TrackItNavHost.kt`)

#### 5.1 `Screen.kt`
```diff
- data object ScanReceipt : Screen("scan_receipt")

  data object AddTransaction : Screen("add_transaction?ocrAmount={ocrAmount}") {
```
Ubah parameter route `ocrAmount` → hapus karena voice result tidak lagi lewat navigasi, melainkan langsung di dalam `AddEditTransactionScreen`.

#### 5.2 `TrackItNavHost.kt`
- Hapus `import com.trackit.app.ui.scan.ScanReceiptScreen`
- Hapus seluruh `composable(Screen.ScanReceipt.route) { ... }` block (baris 91-102)
- Hapus callback `onOpenCamera` dari `AddEditTransactionScreen` (baris 58-60 dan 72-74)
- Hapus logika `ocrAmount` / `savedOcrAmount` (baris 43-56) → Ganti dengan parameter kosong karena voice diproses langsung di screen

---

### Tahap 7: Update ViewModel (`TransactionViewModel.kt`)

```diff
- fun setAmountFromOcr(amount: Double) {
-     _formState.update { it.copy(amount = amount.toLong().toString()) }
- }

+ fun setFromVoice(amount: Long, description: String?, categoryName: String?, dateMillis: Long?) {
+     _formState.update { state ->
+         val matchedCategoryId = categoryName?.let { name ->
+             state.categories.find {
+                 it.name.equals(name, ignoreCase = true)
+             }?.id
+         }
+         state.copy(
+             amount = amount.toString(),
+             description = description ?: state.description,
+             selectedCategoryId = matchedCategoryId ?: state.selectedCategoryId,
+             date = dateMillis ?: state.date
+         )
+     }
+ }
```

**Penting:** Fungsi ini mencocokkan `categoryName` (String dari VoiceParser) dengan daftar `categories` yang sudah di-load dari database ke state. Ini menjamin nama kategori selalu sinkron dengan data di Room DB.

---

### Tahap 8: Update UI (`AddEditTransactionScreen.kt`)

#### 7.1 Hapus Parameter Lama
```diff
  fun AddEditTransactionScreen(
-     ocrAmount: Double? = null,
      onNavigateBack: () -> Unit,
-     onOpenCamera: () -> Unit,
      viewModel: TransactionViewModel = hiltViewModel()
  )
```

#### 7.2 Tambah Speech Recognizer & TTS Logic
Di dalam Composable, tambahkan:
1. **Permission Launcher** untuk `RECORD_AUDIO` menggunakan `rememberLauncherForActivityResult`.
2. **Speech Intent Launcher** menggunakan `ActivityResultContracts.StartActivityForResult()` yang memicu `Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)` dengan:
   - `EXTRA_LANGUAGE` = `"id-ID"` (Bahasa Indonesia)
   - `EXTRA_LANGUAGE_MODEL` = `LANGUAGE_MODEL_FREE_FORM`
   - `EXTRA_PROMPT` = `"Ucapkan pengeluaran Anda, contoh: beli sayur 50 ribu"`
3. **State `isListening`** untuk menandakan apakah sedang merekam atau tidak.
4. **Haptic Feedback:** Gunakan `LocalView.current.performHapticFeedback` untuk memberikan getaran singkat (*audio cue*) saat *mic* mulai dan selesai.
5. **Text-To-Speech (TTS):** Inisiasi `TextToSpeech` instance. Jika hasil sukses dan opsi TTS di *Settings* aktif, panggil `tts.speak("Transaksi tersimpan", TextToSpeech.QUEUE_FLUSH, null, null)`.

#### 7.3 Ganti Ikon Kamera → Mikrofon
```diff
- FilledTonalIconButton(onClick = onOpenCamera) {
-     Icon(Icons.Default.CameraAlt, contentDescription = "Pindai Struk")
- }

+ FilledTonalIconButton(onClick = { /* launch speech recognizer */ }) {
+     Icon(
+         if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
+         contentDescription = "Catat dengan Suara"
+     )
+ }
```

#### 7.4 Animasi Visual Saat Merekam (Efek Magic)
- Saat `isListening = true`:
  - Warna ikon mikrofon berubah jadi **merah** (animasi `animateColorAsState`).
  - Teks status muncul di bawah tombol: *"Mendengarkan..."*
- Saat hasil suara diterima:
  - Tampilkan **Snackbar** singkat: *"✓ Terdeteksi: beli sayur 50 ribu"*
  - Berikan animasi **Visual Highlight** (warna background *TextField* nominal & deskripsi berkedip hijau lembut selama 1 detik) untuk menandakan mana data yang diubah oleh AI secara otomatis.

#### 7.5 Flow Hasil Voice
```
Tombol Mikrofon ditekan
  → Haptic feedback (getar)
  → Cek permission RECORD_AUDIO (minta jika belum)
  → Launch SpeechRecognizer Intent
  → Terima hasil teks (String) & haptic feedback selesai
  → Panggil VoiceParser.parse(teks)
  → Panggil viewModel.setFromVoice(result.amount, result.description, result.categoryName, result.dateMillis)
  → Form otomatis terisi dengan efek Visual Highlight hijau
  → TTS mengucapkan: "Tersimpan, pengeluaran [kategori] [nominal] rupiah" (jika setting aktif).
```

---

### Tahap 9: Akses Kilat (App Shortcut & Quick Settings Tile)

Untuk memberikan UX *Enterprise-level*, kita memungkinkan pengguna mencatat suara tanpa membuka aplikasi:

#### 9.1 App Shortcut (Home Screen)
- Buat file `res/xml/shortcuts.xml`.
- Tambahkan shortcut statis: *"Catat dengan Suara"* yang men-trigger intent langsung ke halaman `AddEditTransactionScreen` dengan flag khusus `START_VOICE_IMMEDIATELY = true`.
- Daftarkan `shortcuts.xml` di `AndroidManifest.xml` pada elemen `<activity>`.

#### 9.2 Quick Settings Tile (Notification Bar)
- Buat *Service* baru `VoiceTileService` yang mengekstend `TileService`.
- Daftarkan di `AndroidManifest.xml` dengan permission `BIND_QUICK_SETTINGS_TILE`.
- Saat tile di-klik pengguna dari bar atas, *service* ini akan meluncurkan `MainActivity` yang langsung membuka *dialog/bottom sheet* transparan untuk merekam suara secara instan tanpa mengganggu layar utama.

---

### Tahap 10: Error Handling & Edge Cases

| Skenario | Penanganan |
|----------|------------|
| Pengguna menolak izin mikrofon | Tampilkan Snackbar: "Izin mikrofon diperlukan untuk fitur suara" |
| SpeechRecognizer tidak tersedia di device | Tampilkan Toast: "Fitur suara tidak didukung di perangkat ini". Pengguna tetap bisa input manual |
| Ucapan tidak mengandung angka | `VoiceParser` mengembalikan `isValid = false`. Tampilkan Snackbar: "Nominal tidak terdeteksi, coba ucapkan lagi" |
| Ucapan tidak cocok dengan kategori manapun | Kategori default ke **"Lainnya"** (tidak auto-select, biarkan pengguna pilih) |
| Suara tidak terdengar / timeout | Intent SpeechRecognizer otomatis mengembalikan error. Tampilkan Snackbar: "Suara tidak terdengar, coba lagi" |
| Ucapan ambigu ("beli makan di toko baju") | Kategori diambil dari keyword pertama yang cocok ("makan" → Makanan) |

---

### Tahap 11: Pembersihan Akhir

1. Hapus `LaunchedEffect(ocrAmount)` di `AddEditTransactionScreen.kt` (baris 48-50).
2. Pastikan tidak ada *import* yang mengarah ke file/class yang sudah dihapus (ScanReceiptScreen, CameraX, ML Kit).
3. Jalankan **Build** untuk memastikan tidak ada *compile error*.

---

## ✅ Kriteria Sukses (Acceptance Criteria)

1. ✅ Aplikasi tidak *crash* jika pengguna menolak izin mikrofon.
2. ✅ Kalimat **"beli bensin 20 ribu"** terisi sebagai nominal `20000`, kategori `Transportasi`, deskripsi `beli bensin 20 ribu`.
3. ✅ Kalimat **"bayar listrik 300 ribu"** terisi sebagai nominal `300000`, kategori `Tagihan`.
4. ✅ Kalimat **"nonton 50 ribu"** terisi sebagai nominal `50000`, kategori `Hiburan`.
5. ✅ Kalimat tanpa keyword kategori (misal **"50 ribu"**) tetap mengisi nominal tanpa auto-select kategori.
6. ✅ Fitur berjalan lancar meskipun internet dimatikan (mode pesawat). *(Catatan: SpeechRecognizer bawaan Android BISA offline jika user sudah download bahasa Indonesia di Settings → Language)*
7. ✅ Ukuran APK lebih kecil dari sebelumnya (karena ML Kit + CameraX dihapus).
8. ✅ CI/CD GitHub Actions tetap hijau ✅ setelah perubahan.

---

## ⚠️ Catatan Penting

### Tentang Offline Speech Recognition
`SpeechRecognizer` Android **secara default membutuhkan internet** karena menggunakan server Google. Namun, sejak Android 11+, pengguna bisa mendownload paket bahasa offline di:
**Settings → System → Language → Speech → Offline Speech Recognition → Download "Bahasa Indonesia"**

Jika paket bahasa belum didownload dan tidak ada internet, `SpeechRecognizer` akan mengembalikan error. Kita akan menangani ini di error handling dan menampilkan pesan yang jelas kepada pengguna.
