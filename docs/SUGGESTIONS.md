# Saran Pengembangan Aplikasi TrackIt

Dokumen ini berisi saran-saran fitur tambahan dan perbaikan code untuk pengembangan aplikasi TrackIt selanjutnya.

---

## 1. Fitur Keuangan Dasar

### 1.1 Tambahan Jenis Transaksi
- **Pemasukan (Income)** - Saat ini hanya pengeluaran, tambahkan transaksi masuk
- **Transfer** - Antar kategori/wallet
- **Pinjaman/Hutang** - Catat utang piutang

### 1.2 Multi-Wallet / Akun
- Buat tabel `accounts` (Dompet Toko, Bank BCA, Dana, dll)
- Setiap transaksi bisa specify sumber dana
- Filter transaksi berdasarkan wallet

### 1.3 Anggaran Lebih Lanjut
- **Anggaran per kategori** - Bukan hanya total, tapi per-kategori
- **Anggaran Mingguan** - Diluar bulanan
- **Budget Rollover** - Sisa anggaran bulan lalu bisa di-rollover

---

## 2. Fitur Pencatatan & Organisir

### 2.1 Tag & Label
- Tambah kolom `tags` di transaksi (JSON array)
- Filter transaksi berdasarkan tag
- Contoh: #belanjaBulanan, # Liburan, # Mendesak

### 2.2 Lokasi
- Simpan lokasi saat transaksi dibuat (GPS)
- Tampilkan di detail transaksi
- Filter berdasarkan lokasi

### 2.3 Foto Struk
- Selain OCR, simpan foto struk ke storage
- Tampilkan foto di detail transaksi
- Fitur gallery struk

### 2.4 Catatan Tambahan
- Rich text notes untuk transaksi
- Tambah attachment (receipt photo)

---

## 3. Fitur Analytics & Laporan

### 3.1 Visualisasi Lebih Lengkap
- **Line Chart** - Tren pengeluaran per bulan
- **Bar Chart** - Perbandingan bulan ke bulan
- **Calendar View** - Heatmap pengeluaran harian

### 3.2 Laporan Lebih Lengkap
- **Laporan Mingguan**
- **Laporan Tahunan**
- **Export ke CSV/Excel** - Selain PDF

### 3.3 Analisis Otomatis
- **Kategori terbesar** - Insights otomatis
- **Prediksi pengeluaran bulan depan** - Berdasarkan history
- **Anomali detection** - Jika ada transaksi tidak biasa

---

## 4. Fitur Smart & AI

### 4.1 OCR Canggih
- Deteksi tanggal dari struk
- Deteksi nama toko/merchant
- Deteksi item-item yang dibeli
- Simpan receipt image

### 4.2 Suggestion Otomatis
- **Smart category** - Prediksi kategori berdasarkan deskripsi
- **Smart amount** - Suggestion nominal dari history
- **Recurring detection** - Deteksi transaksi berulang otomatis

### 4.3 Natural Language Input
- Ketik: "Makan siang 50rb" → Otomatis buat transaksi
- Parse tanggal dari teks: "Beli groceries kemaren"

---

## 5. Fitur Keamanan

### 5.1 Backup & Restore
- **Export database** ke file lokal
- **Import database** dari file
- **Auto-backup** ke cloud (Google Drive, dll)

### 5.2 Keamanan Lanjutan
- **PIN fallback** - Jika biometric gagal
- **Auto-lock timeout** - Lock setelah N menit tidak aktif
- **Encrypted database** - Room dengan SQLCipher

---

## 6. Fitur Interaksi & Social

### 6.1 Reminder & Notifikasi
- **Reminder手动** - "Jangan lupa catat pengeluaran hari ini"
- **Recurring reminder** - Notifikasi untuk tagihan berulang
- **Budget warning** - Lebih granular (50%, 75%, 90%)

### 6.2 Widget
- **Home screen widget** - Quick add transaksi
- **Budget widget** - Tampilkan sisa budget

### 6.3 Share & Export
- Share transaksi individual
- Share ringkasan ke social media

---

## 7. Perbaikan Kode & Arsitektur

### 7.1 Error Handling
```kotlin
// Tambahkan try-catch dengan user-friendly message
try {
    // operation
} catch (e: Exception) {
    showSnackbar("Terjadi kesalahan. Silakan coba lagi.")
    logException(e)
}
```

### 7.2 State Management
- Gunakan ** sealed class** untuk UI State
- Tambahkan **Loading/Error/Success** states
- Implement **Refresh token** pattern

### 7.3 Testing
- Tambah unit test untuk ViewModel
- Tambah instrumented test untuk DAO
- Setup CI/CD dengan GitHub Actions

### 7.4 Dependency Injection
- Pisahkan Repository dari Room
- Gunakan **UseCase** pattern untuk business logic

### 7.5 Security Best Practices
- Hapus `FLAG_SECURE` di debug build
- Jangan expose API keys di code
- Validasi input user

---

## 8. UI/UX Improvements

### 8.1 Onboarding
- First-time user tutorial
- Demo cara scan struk
- Input budget awal

### 8.2 Accessibility
- Support screen reader
- High contrast mode
- Font size scaling

### 8.3 Animasi & Transisi
- Shared element transitions
- Skeleton loading states
- Pull-to-refresh

### 8.4 Empty States
- Tampilan khusus saat belum ada transaksi
- Tutorial untuk input pertama

---

## 9. Database Schema Extensions

### 9.1 Tabel Baru yang Disarankan

```kotlin
// Account Entity
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val balance: Double,
    val colorHex: String,
    val iconName: String
)

// Tag Entity
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val colorHex: String
)

// Transaction-Tag Relation
@Entity(tableName = "transaction_tags", primaryKeys = ["transactionId", "tagId"])
data class TransactionTagCrossRef(
    val transactionId: Long,
    val tagId: Long
)

// Recurring Template
@Entity(tableName = "recurring_templates")
data class RecurringTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val description: String,
    val amount: Double,
    val categoryId: Long?,
    val type: String, // DAILY, WEEKLY, MONTHLY
    val dayOfMonth: Int?,
    val isActive: Boolean
)
```

---

## 10. Fitur Berikutnya (Roadmap)

| Prioritas | Fitur | Estimasi Effort |
|-----------|-------|-----------------|
| Tinggi | Multi-wallet | Medium |
| Tinggi | Anggaran per kategori | Low |
| Tinggi | Backup/Restore | Medium |
| Sedang | Tags & Filter | Medium |
| Sedang | Foto struk | Low |
| Sedang | Widget | Medium |
| Rendah | Analytics AI | High |
| Rendah | Cloud sync | High |

---

## 11. Referensi App Sejenis

- **Wallet by BudgetBakers** - Referensi UI
- **Money Manager** - Referensi fitur accounting
- **Spendee** - Referensi UI/UX modern
- **MoneyLover** - Referensi pasar Indonesia

---

## Catatan

- Semua saran di atas **tidak harus diimplementasikan sekaligus**
- Prioritaskan berdasarkan kebutuhan user
- Lakukan riset pasar sebelum implementasi fitur besar

---

*Dokumen ini dibuat untuk membantu pengembangan TrackIt ke depan.*
