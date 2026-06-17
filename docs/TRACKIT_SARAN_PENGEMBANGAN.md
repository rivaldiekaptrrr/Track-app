# 50 Rekomendasi Fitur TrackIt (Buku Putih Fullstack & Mobile Engineer)

Dokumen ini memuat cetak biru (*blueprint*) 50 fitur dan *micro-interactions* komprehensif untuk mengembangkan aplikasi pencatat keuangan **TrackIt**. Visi dokumen ini adalah menjadikan TrackIt setara dengan aplikasi *"Ultimate Finance Tracker"* kelas dunia.

---

## 🔴 FASE 1: Fundamental UX & Retensi Hari Pertama (Core)
*Fitur dasar agar aplikasi fungsional, tidak cacat logika di mata pengguna, dan mencegah pengguna menghapus aplikasi di minggu pertama.*

1. **Cloud Backup (Google Drive/Email Sync):** Wajib ada melindungi Data Room lokal. Kehilangan HP = kehilangan data krusial pengguna.
2. **Multi-Wallet/Rekening (Tunai, Bank, E-Wallet):** Pisahkan uang dompet asli dengan saldo Gopay. Pencatatan satu pintu merusak perhitungan uang riil.
3. **Interactive Onboarding (Hands-on):** Jangan tayangkan teks. Sambut user dengan: *"Tekan ikon mic dan ucapkan 'Makan Bakso 20 Ribu'."* agar instan paham.
4. **Haptic & Visual Feedback pada Voice Record:** Indikator berdenyut dan getar *(haptic)* saat UI mengenali suara agar user tahu HP-nya sedang aktif mendikte.
5. **Bypass Biometric untuk Quick Add Tile/Widget:** Layar tambah cepat di *quick settings* status bar harus instan, meniadakan delay lama *Face unlock*.
6. **Auto-Kategorisasi dari Teks:** AI pintar pembaca riwayat teks manual. Ketik *"Indomaret"*, kategori "Groceries" langsung *auto-selected*.
7. **Kalkulator Built-in Otomatis:** Saat menginput harga di keyboard khusus, izinkan *(45000+12000)* terisi langsung menjadi `57000`.
8. **Action-Driven Empty State:** Modifikasi beranda kosong bagi user baru dengan ilustrasi estetik berdimensi besar dan satu panah kuning menunjuk logo (+).
9. **Konversi Nominasi Cepat (Huruf ke Angka):** Keyboard *custom* di mana mengetik "50" > ditekan tombol "k" / "rb" menjadi `50.000` (mengurangi tap nol berulang).
10. **Toleransi Kesalahan Ketik (Fuzzy Search):** Mengetik *"mknan"* tetap sanggup menampilkan pencarian kategori *"Makanan"*.

---

## 🟡 FASE 2: Interaksi Bebas Friksi & Produktivitas (User-Friendly)
*Mengurangi rasa malas pengguna *"klik-klik"* HP dalam pemakaian berulang sehari-hari.*

11. **Bottom Navigation Bar:** Berhentilah pakai *Hamburger Menu (Drawer)* jadul, gunakan 4 tab navigasi di layar bawah agar ramah *one-hand usability*.
12. **Sticky Headers Tanggal pada List:** Pengelompokkan riwayat mutasi otomatis *"Hari Ini"*, *"Kemarin"*, dan *"14 Ags 2026"* persis navigasi riwayat *WhatsApp WhatsApp*.
13. **Swipe-to-Delete/Edit Gesture:** Sapukan baris data transaksi ke kiri dengan ibu jari untuk instan menghapusnya dalam satu detik.
14. **Undo via Snackbar Notifikasi:** Stop menggunakan "Pop-up apakah yakin? Yes/No" yang jadul. Langsung hapus namun beri notifikasi *"Terhapus - [UNDO]*" yang melayang selama 4 detik.
15. **Warna Kategori Paten:** Kategori spesifik *(Merah=Makanan, Biru=Transport)* harus abadi konsisten di seluruh aplikasi untuk meresapkan memori otak visual pengguna.
16. **Lampiran Foto pada Struk:** Pengguna dapat memotret tiket, bon restoran, dan melampirkannya (*attach/camera*) pada sebuah rincian transaksi sebagai bukti.
17. **Shared Wallet (Family Mode):** Sinkronisasi *Cloud* real-time yang membolehkan sepasang Suami/Istri membagikan & mencatat pada 1 "Rekening Finansial" yang sama dari HP berbeda.
18. **Custom Budget Cycle (Siklus Gajian Bebas):** Warga gajian tgl 25 Agustus dapat men-setting agar hitungan "*Bulan Ini/Siklus ini*" berawal dari tgl 25, bukan 1 September.
19. **Rollover Sisa Anggaran (Sisa tumpah):** Limit uang tersisa yang Rp 50.000 pada bulan lalu harus bertambah & pindah total menjamin *"Reward limit gabungan"* saldo bulan ini di UI Anggaran.
20. **Time/Month Filter Horizontal (Slider Header):** Navigasi `<   Bulan Juli   >` teratas di dasboard *(sekejap re-draw/render)* tanpa kalender Pop-up besar yang lamban nge-lag.

---

## 🟢 FASE 3: Kekayaan Analisa Keuangan & Pengecekan Riwayat
*Meningkatkan level aplikasi pembukuan agar lebih fungsional untuk pengguna tingkat lanjut.*

21. **Recurring Transaksi Otomatis (Langganan):** Setup 1 kali untuk potong tagihan Spotify/Kos tiap tgl 20. Tidak lelah meng-input bulanan mandiri.
22. **Filter & Sort Logika Berantai Lanjutan:** *Show Me Expenses: Category="Food" AND Value > "100.000" AND Date = "Past 30 Days"*.
23. **Multiple Currencies (Valuta Asing):** Bagi *remote workers* atau pehobi *traveling*, fasilitasi simpanan multi saldo berbeda (USD vs IDR) yang otomatis menggunakan live rate API.
24. **Catatan Rekap Hutang/Piutang (Debt Tracker):** Fitur isolasi hutang teman. Tampilkan WhatsApp Share Reminder ter-embed jika Piutang telat jatuh tempo.
25. **Export & Import CSV/Spreadsheet/PDF:** User dapat *Download* rekap 1 tahun dalam Exce untuk pivot secara presisi merdeka di laptop (keterbukaan data Cloud).
26. **Checkbox "Exclude from Stats":** Jika user hanya mindah bank *"10 Juta antar Rekening"*, kotak centang ini menjamin nominal raksasa tersebut terabaikan secara matematika pada proporsi per-visual pie chart (menghindari bias grafik).
27. **Analitik Sisa Batas Harian Cerdas:** Berdasar target bulanan dibagi tanggal, infokan user berapa *budget* pasti jika ingin belanja harian: *(Saldo Belanja Sisa/Hari: Rp 32.000)*.
28. **Pencatatan Pajak PPN/Diskon Otomatis Terpisah:** *Field Detail* khusus *(seperti kasir)* pemisahan uang Makan dan tambahan PPN 11% yang tersembunyi pada rincian struk restoran.
29. **Deteksi Lokasi Transaksi via Titik Geotagging:** Pengeluaran terekam mendeteksi *(Background GPS)* nama Mall/Restoran tersebut berada (mis: *"dibeli di PIM II"*).
30. **Dual-Entry Bookkeeping (Lite):** Pembukuan ganda dengan jurnal kasir *"Kredit Debit"* asli untuk User Mahasiswa Akuntansi/Pemilik Toko kecil (*advance toggle setting*).

---

## 🌟 FASE 4: Gamifikasi, Asisten AI & Habit Builders
*Keterikatan secara Psikologi agar Aplikasi Dicintai (Hooked) pengguna setiap harinya.*

31. **Savings Goals Target Aspirasi (Progress Bar nabung):** Siapkan slot dompet khusus "*Wishlist Beli Mesin Cuci 5Jt*", yang bisa disuntik *topup* dengan *Gamified Bar UI progress chart 40%*.
32. **Analisis Tren dengan Prediktif Asisten AI:** Robot *(Notifikasi pintar)* bertegur sapa *"Awas! Trend jajan di bulan ini naik secara curam 30% dari rata-rata sejarah pengeluaranmu"*
33. **Badges Gamification Trophy:** Sematkan lencana virtual saat target di capai *"🏅 Tangan Besi: Seminggu berhasil di bawah Limit"* untuk kepuasan rilis Endorfin di kepala.
34. **"Did You Know?" Edukasi Finansial Tips:** Meneruskan tips-literasi artikel 1-paragraf di beranda *(contoh pentingnya diversifikasi tabungan di SBN/Emas)* sesekali untuk nilai plus pendidikan.
35. **Voice AI Search (Pencaharian History Pintar):** Di beranda pencarian, tekan mic lalu ucapkan "*Seberapa banyak pengeluaranku beli bensin dua bulan terakhir?*" untuk instan *Filter-Search* AI.
36. **TTS Variasi Persona Audio Pembaca Angka:** Sistem "*Text-to-Speech*" *(membaca nominal)* tidak hanya bersuara Google-Robot formal Android, tetapi dibekali intonasi santai/kustom *"Voiceline"* khusus.
37. **Push Notifikasi Review Bulan Lalu (Setiap tgl 1):** Aplikasi secara agresif mengembalikan user yg tak aktif: *"Hey, Analisis Bulanan Agustusnya keluar, ayo cek berapa rupiah sisanya!"*.
38. **Share Rincian Transaksi Aesthetic (Snapgram):** Output 1 resep nota disajikuan sepeti gambar *"Spotify Tracks"* elegan yang siap User Post pameran di Instagram Stories / Status WA.
39. **Wrapped Ala Keuangan Spotify (Year in Review):** Ringkasan satu slide-show cantik interaktif pada akhir Desember, menunjukan *"Di kategori ini kamu menghabiskan total 30 Juta dalam setahun"*!
40. **Tampilan Mini-Widgets Layar Utama HP Berwarna:** Tidak perlu masuk aplikasi. Pasang *Component Widget Android* 2x2 Pie-chart mungil sisa pelacakan Anggaran interaktif di pojok Home Screen pengguna Androidnya.

---

## 🛡️ FASE 5: Visual Polish, Keamanan Tingkat Dewa & Personalisasi Terakhir
*Fungsi eksklusif keamanan privasi serta dukungan ekosistem UI paling muktahir tahun ini.*

41. **Integrasi Material You (Monet dynamic UI):** Semua tombol dan *theme app* otomatis bertransformasi senada mencocokkan wallpaper layarnya *(Native Android 12+ API)*.
42. **Sensor Privacy (Discrete Mode):** Di kereta desak, tekan satu lambang "Bola Mata" -> Semua nominal Rp 8.000.000 mendadak tertutup sensor sensor `*** ****`.
43. **App Shield (Recent Screen Blanking):** Menyuntikan perintah kode *`FLAG_SECURE` Android* agar saat melirik tombol *Overview/Task Manager HP*, isi UI halaman Keuangan Aplikasi nampak gelap (Blender Security).
44. **Split-Screen dan Dukungan Desktop Layout Tablet:** Menyajikan form di kiri dan List Data UI di kanan saat perangkat disajikan *Landscape / ChromeOS* / Tablet.
45. **Skeleton Loading Modern Framework:** *Goodbye Circular Progress Bar Spinner*. Saat Data *Room DB* asik memproses hitungan kalkulus akuntansi, Tampilkan animasi kerangka abu abu kotak/berbayang kelip (Skeleton UI).
46. **PIN Fallback Alternatif (Limit Threshold):** Tambahan Timeout Aplikasi *auto-locked Background* setelah 4-Menit; dan Pilihan masuk menggunakan 6-Digit PIN custom jika Biometrik sidik Jari tidak terbaca tangan basah.
47. **Custom Home-Icons Switch:** Mengubah muka desain ikon aplikasi logo TrakIt biru di layar Desktop User menjadi 5 tema berbeda *(Ikon celengan babi/Emas/Logo Hitam)*.
48. **Offline First Sync Logic Queue:** Keistimewaan aplikasi walau Cloud online mati -> antrian Room menyumpal log masuk transaksinya untuk dikirim (*WorkManager*) ke Google Drive saat Data Cellular terhubung 1 jam kemudian. 
49. **Goyang (Shake) HP Untuk Undo Darurat:** Gestur sensor Gyroscope Android merespons Goyangan kasar tangan untuk mem-bounc *back* (kembali/Undo) 1-lagkah kesalahan input Form salah pencet. 
50. **In-App Tutorial Floating Video:** Saat *User Flow* mandeg diam *(stuck di fitur rumit spt Setup Debt)*, Icon Robot kecil *(Helper)* muncul menampilkan PopUp GIF/animasi loop berukuran mungil sejenak cara mengeksekusi fiturnya.


---

## 🛠️ FASE 6: Standar Audit Keamanan & Kualitas Enterprise (NFR)
*Fokus pada ketahanan (resilience), optimasi performa, keamanan data (security), dan arsitektur kode kelas dunia untuk memastikan aplikasi bebas crash dan siap disebarkan ke jutaan pengguna.*

51. **Enkripsi File Backup & Database (SQLCipher):** Amankan file cadangan transaksi di storage eksternal menggunakan Android Keystore System dan gunakan SQLCipher pada Room DB untuk mencegah eksploitasi pencurian data pengguna.
52. **Strict Mode Biometric (No Device PIN Fallback):** Sediakan opsi keamanan agar aplikasi hanya bisa diakses menggunakan sidik jari/wajah asli, dan menolak fallback ke PIN/Pola HP untuk menghindari intrusi oleh pihak ketiga yang mengetahui PIN HP pengguna.
53. **Validasi Skema Migrasi Room (exportSchema = true):** Wajibkan eksport skema database ke dalam format JSON di repositori untuk mendeteksi *crash* fatal saat migrasi struktur tabel pada compile-time, bukan runtime.
54. **Clean Architecture & Separation of Concerns (SoC):** Hindari peletakkan logika berat seperti pengeksporan PDF/CSV dan logika Worker di `MainActivity`. Pindahkan *business logic* ke `UseCase/Interactor` yang dipanggil melalui `ViewModel`.
55. **Optimasi Waktu Cold Start (Baseline Profiles):** Sertakan Macrobenchmark Baseline Profiles untuk melakukan pre-kompilasi (AOT) kelas Jetpack Compose agar waktu startup pertama kali aplikasi menjadi 30-40% lebih cepat dan mulus.
56. **Pengurangan Ukuran APK (isShrinkResources):** Aktifkan instruksi `isShrinkResources = true` di file *build.gradle.kts* bersamaan dengan R8/ProGuard untuk otomatis menyapu bersih asset dan layout mati, sehingga ukuran unduh aplikasi lebih efisien.
57. **Manajemen *Race Condition* pada Auto-Backup:** Jangan letakkan eksekusi sinkronus backup langsung di dalam `onStop()`. Gunakan *Mutex*, Status Flag, atau selesaikan melaui *OneTimeWorkRequest* WorkManager yang terisolasi dari *interrupt* user.
58. **Sistem Pemantauan Crash (Observability):** Integrasikan Firebase Crashlytics atau Sentry yang diperkuat dengan custom tree *Timber* untuk memonitor bug/galat SQLite secara live dari HP pengguna tanpa perlu menebak-nebak penyebabnya.
59. **Keamanan Eksekusi *Combine Flows* (Type-Safety):** Hilangkan *array casting* yang berbahaya pada penggabungan `combine()` *Flows* (seperti `params[0] as Double`). Gunakan struktur *wrapper data-class* atau extension custom aman yang tidak membuat aplikasi force-close jika urutan pemanggilan tertukar.
60. **Penanganan Error Aliran Data (Flow .catch):** Selalu berikan perlindungan asinkron `.catch { }` sebelum `.collect()` saat membaca stream dari *Room DB*. Sebuah exception pada SQLite tak akan serta-merta membunuh UI/Aplikasi (*Crash*), melainkan menyajikan tampilan *Error State* elegan.
61. **Lokalisasi 100% (*String Resources* - i18n):** Pindahkan seluruh status *Hardcoded Text* (Contoh: `Text("Abaikan")`) ke dalam `res/values/strings.xml` yang memastikan kemudahan alih bahasa untuk basis pengguna di luar Indonesia di masa depan.
62. **Kepatuhan Aksesibilitas bagi Disabilitas (a11y):** Sertakan atribut `contentDescription` dan penanda *semantics* secara disiplin di setiap komponen grafis & Icon tombol interaktif Jetpack Compose agar aplikasi ramah alat bantu baca (TalkBack).
63. **Strategi Pengujian Tiga Lapis (*Testing Strat*):** Perkuat *Unit Testing* dengan *Paparazzi/Roborazzi (Screenshot Testing)* untuk mengunci inkonsistensi piksel UI, serta rancang *Automated Room Migration Test* untuk memproteksi migrasi database beresiko dari versi 1 ke 5.

---

**Catatan Perubahan (Changelog):**
1. Fix Race Condition Aman: Peluncuran (launch) dialog Speech API kini berjalan secara berurutan menunggu profileId didapatkan dari memori, sehingga 100% dijamin bebas bug salah profil.
2. BottomSheet Super Informatif: Header UI sekarang menampilkan dengan jelas kalimat seperti: Pilih Kategori untuk: "ssd komputer" • Rp 500.000
3. Timer dengan Animasi Progress Bar: Teks angka 7 detik digantikan dengan sebuah LinearProgressIndicator elegan yang menyusut perlahan. Saat tersisa 3 detik, warnanya akan berubah menjadi merah sebagai alert visual.
4. Pembelajaran Kata per Kata (Atomic Smart Dictionary): Jika sistem menangkap "ssd komputer", ia sekarang akan dipecah. Jika kata "komputer" sudah ada di database, sistem hanya akan menyisipkan kata "ssd" saja. Ini membuat kamus VoiceParser jauh lebih natural dan tidak redundan.