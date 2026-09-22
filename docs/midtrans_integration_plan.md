# Rencana Implementasi Integrasi Payment Gateway Midtrans (Auto-Payment RBAC)

Dokumen ini merupakan panduan komprehensif implementasi integrasi Midtrans Snap & Webhook Notification untuk monetisasi otomatis aplikasi **Track-It**. Sistem ini dirancang dengan prinsip **Zero Fixed Monthly Cost ($0/bulan)** menggunakan layanan *Free Tier* (Firebase Auth + Firestore REST Client + Vercel Serverless Function).

---

## 📌 1. Pendahuluan & Ringkasan Arsitektur

### Target Fitur:
- **Monetisasi Lisensi RBAC**: Pembelian paket `EXPENSE` (Catatan Keuangan), `WEDDING` (Wedding Planner), dan `BOTH` (Full Access).
- **Proses 100% Otomatis**: Pengguna bayar via QRIS / E-Wallet / Virtual Account -> Lisensi aktif dalam < 3 detik tanpa verifikasi manual Admin.
- **Biaya Operasional**: **Rp 0 / bulan** (Hanya potongan fee transaksi Midtrans saat ada penjualan berhasil).

```
┌─────────────────────────┐
│   📱 Android App        │
│  (PendingVerification)  │
└────────────┬────────────┘
             │ (1) Minta Token Bayar
             ▼
┌─────────────────────────┐       (2) Minta Snap Token      ┌─────────────────────────┐
│ 🌐 Vercel Serverless    ├────────────────────────────────►│ 💳 Midtrans Payment     │
│   (Zero-Cost Node.js)   │◄────────────────────────────────┤   (QRIS / VA / E-Wallet)│
└────────────┬────────────┘       (3) Snap Token & URL      └────────────┬────────────┘
             │                                                           │
             │                                                           │ (4) User Bayar QRIS
             │                                                           │
             │ (5) Webhook Callback (Notifikasi Bayar Lunas)             │
             └───────────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────┐
│ 🔥 Firebase Firestore   │
│  users/{uid}.accessLevel│ ───► (6) Instant Real-time Redirect di Android App!
└─────────────────────────┘
```

---

## 🚀 2. Tahap 1: Pendaftaran & Konfigurasi Akun Midtrans

### 2.1 Pendaftaran Akun Midtrans (Sandbox & Production)
1. Kunjungi portal resmi Midtrans: [https://dashboard.midtrans.com/register](https://dashboard.midtrans.com/register).
2. Isi formulir pendaftaran:
   - **Business Name**: Track-It / Personal Developer
   - **Email & No. HP**: Email aktif untuk notifikasi pencairan dana.
3. Konfirmasi email pendaftaran.
4. Setelah masuk ke Dashboard Midtrans, Anda secara default berada di mode **SANDBOX** (Lingkungan pengujian).

### 2.2 Pengambilan API Keys (Sandbox)
1. Di Dashboard Midtrans Sandbox, buka menu **Settings** -> **Access Keys**.
2. Catat kredensial berikut:
   - **Merchant ID** (Contoh: `G123456789`)
   - **Client Key** (Contoh: `SB-Mid-client-xxxxxxxxx`) -> Digunakan di Android / Frontend.
   - **Server Key** (Contoh: `SB-Mid-server-xxxxxxxxx`) -> **SANGAT RAHASIA**, hanya disimpan di Vercel Environment Variables.

### 2.3 Konfigurasi Notification / Webhook URL di Midtrans
1. Buka menu **Settings** -> **Configuration**.
2. Pada kolom **Payment Notification URL**, masukkan URL webhook Vercel (akan dibuat di Tahap 3):
   - Contoh URL Sandbox: `https://trackit-payment-api.vercel.app/api/midtrans-webhook`
3. Pada **Finish Redirect URL**, isi: `https://trackit-payment-api.vercel.app/api/finish`
4. Pilih **Save**.

---

## 🛠️ 3. Tahap 2: Pembuatan Backend Webhook Gratis (Vercel Serverless Function)

Backend dibuat menggunakan Vercel (Node.js/TypeScript) yang 100% gratis tanpa biaya bulanan (limit 100.000 request/hari).

### 3.1 Struktur Project Backend (`trackit-payment-api`)
```
trackit-payment-api/
├── package.json
├── vercel.json
└── api/
    ├── create-snap-token.ts    <-- Dipanggil oleh App Android untuk minta Token Bayar
    └── midtrans-webhook.ts     <-- Dipanggil oleh Midtrans saat user selesai bayar
```

### 3.2 Pembuatan API `create-snap-token.ts` (Generate Pembayaran)
API ini menerima request dari Android App yang berisi `userId`, `email`, dan `accessLevel` (`EXPENSE`, `WEDDING`, atau `BOTH`), lalu meminta Snap Token ke Midtrans Core API.

```typescript
// api/create-snap-token.ts
import { VercelRequest, VercelResponse } from '@vercel/node';
import axios from 'axios';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method Not Allowed' });

  const { userId, email, accessLevel } = req.body;
  
  const priceMap: Record<string, { price: number; name: string }> = {
    EXPENSE: { price: 29000, name: 'Paket Catatan Keuangan (Expense)' },
    WEDDING: { price: 49000, name: 'Paket Wedding Planner' },
    BOTH: { price: 69000, name: 'Paket Full Access (Expense + Wedding)' },
  };

  const selectedItem = priceMap[accessLevel];
  if (!selectedItem) return res.status(400).json({ error: 'Access level tidak valid' });

  const orderId = `TRACKIT-${accessLevel}-${userId.substring(0, 5)}-${Date.now()}`;
  const serverKey = process.env.MIDTRANS_SERVER_KEY || '';
  const authHeader = Buffer.from(serverKey + ':').toString('base64');

  const payload = {
    transaction_details: {
      order_id: orderId,
      gross_amount: selectedItem.price,
    },
    item_details: [
      {
        id: accessLevel,
        price: selectedItem.price,
        quantity: 1,
        name: selectedItem.name,
      }
    ],
    customer_details: {
      email: email,
    },
    custom_field1: userId,       // UID Firebase User
    custom_field2: accessLevel,  // EXPENSE / WEDDING / BOTH
  };

  try {
    const response = await axios.post(
      'https://app.sandbox.midtrans.com/snap/v1/transactions',
      payload,
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Basic ${authHeader}`,
        }
      }
    );

    return res.status(200).json({
      token: response.data.token,
      redirect_url: response.data.redirect_url,
      orderId: orderId
    });
  } catch (error: any) {
    return res.status(500).json({ error: error.message });
  }
}
```

### 3.3 Pembuatan API `midtrans-webhook.ts` (Auto-Upgrade Lisensi Firestore)
API ini menerima callback saat pembayaran terkonfirmasi lunas (`settlement` atau `capture`).

```typescript
// api/midtrans-webhook.ts
import { VercelRequest, VercelResponse } from '@vercel/node';
import crypto from 'crypto';
import admin from 'firebase-admin';

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert({
      projectId: process.env.FIREBASE_PROJECT_ID,
      clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
      privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
    }),
  });
}

const db = admin.firestore();

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') return res.status(405).send('Method Not Allowed');

  const body = req.body;
  const serverKey = process.env.MIDTRANS_SERVER_KEY || '';

  // 1. Verifikasi Keamanan Signature SHA-512 dari Midtrans
  const signatureKey = body.signature_key;
  const statusCode = body.status_code;
  const grossAmount = body.gross_amount;
  const orderId = body.order_id;

  const expectedSignature = crypto
    .createHash('sha512')
    .update(`${orderId}${statusCode}${grossAmount}${serverKey}`)
    .digest('hex');

  if (signatureKey !== expectedSignature) {
    return res.status(403).json({ error: 'Invalid Signature' });
  }

  // 2. Cek apakah status pembayaran lunas
  const transactionStatus = body.transaction_status;
  const fraudStatus = body.fraud_status;

  if (transactionStatus === 'settlement' || (transactionStatus === 'capture' && fraudStatus === 'accept')) {
    const userId = body.custom_field1;
    const accessLevel = body.custom_field2;

    if (userId && accessLevel) {
      // 3. Update lisensi pengguna di Firestore secara real-time
      await db.collection('users').doc(userId).update({
        accessLevel: accessLevel,
        updatedAt: new Date().toISOString(),
        paymentInfo: {
          orderId: orderId,
          grossAmount: grossAmount,
          paymentType: body.payment_type,
          paidAt: body.settlement_time || new Date().toISOString()
        }
      });
      console.log(`[SUCCESS] Access level user ${userId} di-upgrade ke ${accessLevel}`);
    }
  }

  return res.status(200).json({ status: 'OK' });
}
```

---

## 📱 4. Tahap 3: Modifikasi Aplikasi Android (Frontend UI & Logic)

### 4.1 Desain UI Pemilihan Paket di `PendingVerificationScreen.kt`
Layar penunggunan aktivasi telah diubah menjadi layar checkout paket lengkap dengan 3 kartu pilihan (`EXPENSE`, `WEDDING`, `BOTH`) serta strategi **Price Anchoring & Strikethrough Price**:

- **Kartu 1: Expense Tracker** -> Harga: **Rp 49.000** (Coret: ~Rp 69.000~ • Label: *Promo Rilis*)
- **Kartu 2: Wedding Planner** -> Harga: **Rp 49.000** (Coret: ~Rp 69.000~ • Label: *Promo Rilis*)
- **Kartu 3: Full Access (Bundle)** -> Harga: **Rp 69.000** (Coret: ~Rp 98.000~ • Label: *Hemat 30% / Hemat Rp 29.000 dibanding beli satuan*)

### 4.2 Integrasi Snap Web Payment Dialog
Saat user memilih paket dan mengklik **"Aktifkan [Nama Paket] — [Harga]"**:
1. App Android memanggil `POST https://track-it-backend-sand.vercel.app/api/create-snap-token`.
2. Menerima `redirect_url` dari Midtrans.
3. Membuka browser / Chrome Custom Tabs dengan URL pembayaran Midtrans.
4. User menyelesaikan pembayaran via QRIS, Virtual Account, ShopeePay, Gopay, dll.
5. Auto-polling & listener Firestore di background langsung mendeteksi update lisensi dan mengarahkan user ke dashboard.

---

## 🧪 5. Tahap 4: Pengujian Sandbox (Current State)

1. **Jalankan Aplikasi Android**: Registrasi akun dummy baru (`testuser@gmail.com`).
2. **Masuk ke Layar Payment**: Pilih Paket **Full Access (Rp 69.000)** -> Tekan **Aktifkan**.
3. **Pilih Metode Pembayaran di UI Midtrans**:
   - Pilih **QRIS** -> Gunakan simulator QRIS Midtrans: [https://simulator.sandbox.midtrans.com/qris/scan](https://simulator.sandbox.midtrans.com/qris/scan).
   - Atau pilih **Virtual Account BCA** -> Gunakan simulator VA Midtrans: [https://simulator.sandbox.midtrans.com/bca/va/index](https://simulator.sandbox.midtrans.com/bca/va/index).
4. **Eksekusi Bayar di Simulator**:
   - Dalam < 3 detik setelah pembayaran dikonfirmasi di simulator, Vercel Webhook memproses request -> Firestore mengupdate `accessLevel = "BOTH"`.
   - Layar Android App berpindah otomatis dari `PendingVerificationScreen` ke Dashboard utama.

---

## 🚀 6. Tahap 5: Panduan Langkah Setelah Akun Midtrans Di-ACC (Go-Live Production Checklist)

Setelah proses verifikasi dokumen (KTP, Buku Tabungan, dll.) disetujui oleh Midtrans (biasanya 2-3 hari kerja), lakukan langkah-langkah berikut secara berurutan untuk beralih dari mode Sandbox ke Production:

### 📋 Checklist Transisi ke Production:

#### 1️⃣ Beralih ke Mode Production di Dashboard Midtrans
1. Login ke [Midtrans Dashboard](https://dashboard.midtrans.com).
2. Di pojok kiri atas, ubah switch toggle dari **SANDBOX** ke **PRODUCTION**.
3. Masuk ke menu **Settings** ➔ **Access Keys**:
   - Catat **Merchant ID Production**.
   - Catat **Client Key Production** (`Mid-client-xxxxxxxx`).
   - Catat **Server Key Production** (`Mid-server-xxxxxxxx`) *(Rahasia, jangan disebarkan)*.

#### 2️⃣ Konfigurasi Webhook / Notification URL di Production
1. Di Dashboard Midtrans (Mode Production), buka menu **Settings** ➔ **Configuration**.
2. Pada field **Payment Notification URL**, masukkan URL webhook backend:
   ```text
   https://track-it-backend-sand.vercel.app/api/midtrans-webhook
   ```
3. Pada field **Finish Redirect URL**, isi:
   ```text
   https://track-it-backend-sand.vercel.app/api/finish
   ```
4. Pastikan **Notification Settings** aktif untuk event transaksi berhasil (`settlement`, `capture`).
5. Klik **Save Changes**.

#### 3️⃣ Update Backend Vercel ke Production (Tempat Ganti URL & Key)
> **PENTING: Di mana URL diganti?**  
> Aplikasi Android **TIDAK PERLU** diubah URL-nya (karena Android selalu memanggil Backend Vercel Anda). Yang menghubungi Midtrans adalah **Backend Vercel**.

1. **Ganti Server Key di Dashboard Vercel**:
   - Buka Dashboard [Vercel](https://vercel.com) ➔ Buka project `track-it-backend`.
   - Masuk ke menu **Settings** ➔ **Environment Variables**.
   - Ganti nilai `MIDTRANS_SERVER_KEY` dengan **Server Key Production** (`Mid-server-xxxx`).

2. **Ganti URL Midtrans di File Backend (`api/create-snap-token.ts` / `.js`)**:
   - Buka repository project backend Anda (`track-it-backend`).
   - Buka file **`api/create-snap-token.ts`** (atau `.js`).
   - Cari baris `axios.post('https://app.sandbox.midtrans.com/snap/v1/transactions', ...)` dan ganti menjadi:
     ```typescript
     // ❌ SANDBOX LAMA:
     // 'https://app.sandbox.midtrans.com/snap/v1/transactions'
     
     // ✅ PRODUCTION BARU:
     'https://app.midtrans.com/snap/v1/transactions'
     ```
     *(Hapus kata `.sandbox` dari URL).*

3. **Deploy Ulang Backend**:
   - Lakukan `git push` ke repository backend Anda atau klik tombol **Redeploy** di Vercel Dashboard agar perubahan URL & Server Key aktif.

> 📱 **Catatan untuk Aplikasi Android (`Track-app`):**  
> Di [`PaymentRepository.kt`](file:///c:/Rivaldi/Track-app/app/src/main/java/com/trackit/app/data/repository/PaymentRepository.kt#L50), `backendUrl` tetap mengarah ke Vercel Anda (`https://track-it-backend-sand.vercel.app/api/create-snap-token`). **Tidak ada kode Android yang perlu diubah.**

#### 4️⃣ Verifikasi Metode Pembayaran yang Aktif (Payment Methods)
1. Buka Dashboard Midtrans Production ➔ **Payment Methods**.
2. Pastikan channel pembayaran yang ingin dibuka sudah berstatus **Active** / **Approved**:
   - ✅ **QRIS (Gopay, ShopeePay, BCA QRIS, dll.)** — *Rekomendasi Utama (Instan & Mudah)*
   - ✅ **Virtual Account (BCA, Mandiri, BRI, BNI, Permata)**
   - ✅ **E-Wallet (GoPay, ShopeePay, Dana, Ovo jika diaktifkan)**

#### 5️⃣ Uji Coba Transaksi Nyata (Live Smoke Test)
1. Buka aplikasi TrackIt versi debug/release.
2. Login dengan akun pribadi untuk uji coba.
3. Pilih salah satu paket (misal Expense Tracker Rp 49.000 atau buat paket uji coba Rp 1.000 jika diperlukan).
4. Lakukan scan QRIS menggunakan mobile banking asli (uang riil akan masuk ke saldo merchant Midtrans kamu).
5. Pastikan:
   - Notifikasi webhook Vercel menerima status `settlement` (HTTP 200).
   - Dokumen user di Firestore langsung terupdate field `accessLevel`-nya.
   - Layar aplikasi langsung otomatis berpindah (*redirect*) ke Dashboard tanpa perlu restart app.

#### 6️⃣ Konfigurasi Payout / Penarikan Dana Otomatis
1. Buka Dashboard Midtrans Production ➔ menu **Payouts / Billing**.
2. Atur jadwal transfer dana dari Midtrans ke rekening bank kamu (bisa harian atau sesuai jadwal otomatis yang diinginkan).

---

## 🎯 Status Integrasi Saat Ini
- [x] Backend Vercel Serverless Function terpasang (`create-snap-token` & `midtrans-webhook`).
- [x] Webhook Signature Security (SHA-512) aktif.
- [x] UI Android Paywall (`PendingVerificationScreen.kt`) terpasang dengan tema Light Mode & Price Anchoring.
- [x] Auto-polling & Realtime Access Listener terhubung ke Firestore.
- [ ] *Menunggu proses approval/ACC akun Midtrans Production (2-3 hari kerja).*
- [ ] *Switch API keys & endpoint ke Production.*
