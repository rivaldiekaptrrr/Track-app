# Play Store Listing Assets Directory

Folder ini berisi seluruh panduan, dokumen teks deskripsi, serta direktori penampung aset gambar untuk persiapan rilis **TrackIt** di Google Play Console.

---

## 📁 Struktur Direktori Assets

```
playstore_assets/
├── STORE_LISTING.md             # Teks Judul, Deskripsi Singkat, Deskripsi Lengkap, & Keyword
├── DATA_SAFETY_DECLARATION.md   # Panduan jawaban kuesioner Data Safety Play Console
├── README.md                    # Panduan utama folder aset ini
├── icon/                        # [TIDAK ADA / MENYUSUL] Tempat menyimpan App Icon (512x512 PNG)
├── feature_graphic/             # [TIDAK ADA / MENYUSUL] Tempat menyimpan Banner Promosi (1024x500 PNG/JPG)
└── screenshots/
    ├── phone/                   # [TIDAK ADA / MENYUSUL] Screenshots HP (Min. 4 gambar)
    └── tablet/                  # [TIDAK ADA / MENYUSUL] Screenshots Tablet 7" & 10" (Opsional)
```

---

## 🎨 Spesifikasi Gambar yang Dibutuhkan Google Play Console

1. **App Icon (`playstore_assets/icon/app_icon.png`):**
   - **Ukuran:** 512 x 512 piksel (PNG 32-bit dengan alpha).
   - **Ukuran File Maksimum:** 1 MB.
   - **Bentuk:** Sudut melengkung akan disesuaikan otomatis oleh Google Play Store. Simpan dalam bentuk persegi datar tanpa efek bayangan luar berlebih.

2. **Feature Graphic (`playstore_assets/feature_graphic/feature_graphic.png`):**
   - **Ukuran:** 1024 x 500 piksel (PNG atau JPG).
   - **Ukuran File Maksimum:** 15 MB.
   - **Fungsi:** Hero banner utama yang muncul di bagian atas halaman Play Store.

3. **Phone Screenshots (`playstore_assets/screenshots/phone/`):**
   - **Jumlah:** Minimal 4 tangkapan layar, Maksimal 8 tangkapan layar.
   - **Resolusi:** Minimal 1080 x 1920 piksel (Rasio 16:9 portrait) atau resolusi HD setara.
   - **Saran Konten:**
     1. *Screen 1:* Dashboard utama & Saldo Keuangan.
     2. *Screen 2:* Fitur Input Suara / Voice Tracking.
     3. *Screen 3:* Grafik Analisis Interaktif & Budget Kategori.
     4. *Screen 4:* Wedding Planner & Rekap Anggaran Pernikahan.

4. **Tablet Screenshots (`playstore_assets/screenshots/tablet/`):**
   - **Ukuran:** Tangkapan layar dari tablet 7 inci dan 10 inci.

---

> [!NOTE]
> Anda dapat langsung menempatkan file gambar yang sudah dibuat ke dalam subfolder `icon/`, `feature_graphic/`, dan `screenshots/` sesuai spesifikasi di atas.
