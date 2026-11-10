# Dokumentasi Teknis: Reverse Engineering NFC E-Money & Arsitektur Resolusi Lokasi

Dokumen ini mencatat temuan teknis dari hasil *reverse engineering* kartu smartcard e-money Indonesia (BCA Flazz Gen 1/Gen 2, Mandiri e-Money, BNI TapCash, BRI Brizzi) serta arsitektur integrasi API untuk resolusi nama lokasi/terminal transaksi.

---

## 1. Ikhtisar Smartcard E-Money Indonesia

Kartu e-money di Indonesia menggunakan protokol contactless smartcard berbasis **ISO/IEC 14443-4 (Type A)** dengan komunikasi APDU (*Application Protocol Data Unit*), serta **Sony FeliCa (NFC-F / JIS X 6319-4)** khusus untuk Kartu Multi Trip (KMT) KAI Commuter:

| Kartu | Protokol | AID / Service Code | Log Transaksi |
|---|---|---|---|
| **BCA Flazz (Gen 1 & 2)** | ISO-DEP (Type A) | `D3 60 00 00 01 00 00 00` | Tersedia (Gen 1: blok 120 hex, Gen 2: blok 64 hex) |
| **Mandiri e-Money (G1 & G2)** | ISO-DEP (Type A) | `A0 00 00 00 03 00 00 00` / `00 01` | Tersedia (blok 48 hex per transaksi) |
| **BNI TapCash** | ISO-DEP (Type A) | `90 5A 00 00 03 01 00 00` | Tersedia pada sektor riwayat |
| **BRI Brizzi** | ISO-DEP (Type A) | `A0 00 00 00 03 00 00 00` | Tersedia pada file log |
| **Bank DKI JakCard** | ISO-DEP (Type A) | ISO 14443-4 APDU | Saldo & Nomor Kartu |
| **Bank Mega MegaCash** | ISO-DEP (Type A) | ISO 14443-4 APDU | Saldo & Nomor Kartu |
| **KAI Commuter (KMT)** | **Sony FeliCa (NFC-F)** | Service Code: `0x90B7` (37047) | Tersedia (Blok 2..N, nominal, stasiun gate, waktu) |

---

## 2. Bedah Teknis BCA Flazz Gen 2

### 2.1 Alur Transceive APDU Riwayat
Untuk membaca riwayat transaksi BCA Flazz Gen 2, urutan perintah APDU adalah sebagai berikut:

1. **Select AID BCA Flazz**:
   ```
   00 A4 04 00 08 D3 60 00 00 01 00 00 00
   ```
   Status Word sukses: `90 00`

2. **Minta Tantangan (*Get Challenge*)**:
   ```
   00 84 00 00 08
   ```
   Mengembalikan 8 byte random challenge. Status Word: `90 00`.

3. **Inisialisasi Sesi Log**:
   ```
   90 32 03 00 0A 08 01 00 00 00 00 00 00 00 00 29
   ```

4. **Loop Pembacaan Blok Riwayat**:
   Loop index `i = 0..255`:
   ```
   90 32 03 00 01 <index_hex> 20
   ```
   Hentikan loop jika kartu merespons bukan `90 00` (atau `c(bArr) != -28672`).

### 2.2 Struktur Data Blok Riwayat Gen 2 (64 Karakter Hex / 32 Byte)
Setiap blok 64 karakter hex mewakili 1 transaksi dengan rincian offset:

| Offset Karakter Hex | Panjang | Keterangan | Rumus / Cara Parsing |
|---|---|---|---|
| `00..02` | 2 hex (1 byte) | Tipe Transaksi | `04` = Debit/Payment (Belanja/Tol/KRL), selain `04` = Top Up |
| `02..08` | 6 hex (3 byte) | Nominal Transaksi (24-bit) | Jika Debit (`04`): `16777216 - rawAmount` (Two's Complement). Jika Top Up: `rawAmount`. |
| `08..16` | 8 hex (4 byte) | Waktu Transaksi (Detik) | Detik dihitung sejak **1 Januari 1980 00:00:00 UTC** |
| `28..44` | 16 hex (8 byte) | **Terminal / Location ID** | 16 karakter hex dikonversi menjadi **8 karakter ASCII** |
| `44..50` | 6 hex (3 byte) | Saldo Setelah Transaksi | 24-bit integer saldo akhir kartu |

> **PENTING (Two's Complement Nominal)**: Pada Flazz Gen 2, nominal transaksi pengeluaran (debit) disimpan dalam bentuk komplement dua 24-bit. Nilai mentah di hex harus dikurangi dari `16777216` (`0x1000000`). Contoh: hex `FFFF00` $\rightarrow$ raw `16776960` $\rightarrow$ `16777216 - 16776960 = 256`.

---

## 3. Bedah Teknis KAI Commuter Kartu Multi Trip (KMT)

### 3.1 Protokol Sony FeliCa (NFC-F)
Berbeda dengan kartu perbankan yang menggunakan ISO-DEP (ISO 14443-4 APDU), Kartu Multi Trip (KMT) KRL Commuter Line menggunakan chip **Sony FeliCa (JIS X 6319-4 / ISO/IEC 18092)** yang diakses melalui teknologi Android `android.nfc.tech.NfcF`.

* **Polling Tag**: Wildcard System Code `0xFFFF` atau `0x00FE` / `0x88B4`
* **Layanan (Service Code)**: `0x90B7` (37047 dalam desimal)
* **Perintah Baca**: `Read Without Encryption` (Kode Perintah: `0x06`)

### 3.2 Struktur Paket Perintah Read Without Encryption
Untuk membaca 1 blok data dari kartu KMT:
```
[Len (1B)] [CMD (1B)] [IDm (8B)] [NumServices (1B)] [ServiceCode (2B Little-Endian)] [NumBlocks (1B)] [BlockElement (2B)]
```
* **Contoh Blok 0**:
  `10 06 <IDm_8_BYTES> 01 B7 90 01 80 00`
  - `0x10` = 16 bytes total panjang paket
  - `0x06` = Perintah Read Without Encryption
  - `0x01` = 1 Service
  - `0xB7, 0x90` = Service `0x90B7` dalam format Little-Endian
  - `0x01` = 1 Block
  - `0x80, 0x00` = Blok nomor 0 (service index 0, 2-byte block list element)

### 3.3 Struktur Data Blok KMT (16 Byte per Blok)
Kartu KMT menyimpan data dalam blok berukuran 16 byte:

| Nomor Blok | Keterangan | Format / Rumus Parsing |
|---|---|---|
| **Blok 0** | **Nomor Kartu KMT** | 16 byte string ASCII (contoh: `"1001567890123456"`) |
| **Blok 1** | **Saldo Kartu** | 4 byte integer **Little-Endian** pada byte `[0..3]`: `b[0] + (b[1]<<8) + (b[2]<<16) + (b[3]<<24)` |
| **Blok 2 s/d 9** | **Log Riwayat Transaksi** | Blok riwayat transaksi perjalanan dan top-up (maksimal 8 transaksi terakhir) |

#### Detail Struktur Blok Riwayat (Blok 2..N):
* **Byte `[12]` (Tipe Transaksi)**:
  - `0` = Top Up Saldo
  - `1` = Payment / Tap Gate Masuk/Keluar Stasiun
* **Byte `[4..7]` (Nominal)**:
  - 4 byte integer **Big-Endian**: `((b[4]<<24) | (b[5]<<16) | (b[6]<<8) | b[7])`
* **Byte `[0..3]` (Waktu Transaksi)**:
  - 4 byte integer **Big-Endian** menyatakan jumlah detik sejak **1 Januari 2000 07:00:00 WIB** (`946684800000L` ms):
    $$\text{Timestamp} = 946684800000 + (\text{seconds} \times 1000)$$
* **Byte `[8..9]` (Kode Stasiun)**:
  - 2 byte integer **Big-Endian**: `((b[8]<<8) | b[9])`
  - Dipetakan langsung ke nama stasiun KRL Commuter Line:
    - `1` / `329`: Stasiun KRL Tanah Abang
    - `257`: Stasiun KRL Bogor
    - `279`: Stasiun KRL Manggarai
    - `282`: Stasiun KRL Juanda
    - `286`: Stasiun KRL Jakarta Kota
    - `287`: Stasiun KRL Bekasi
    - `306` / `313`: Stasiun KRL Tangerang
    - `376`: Stasiun KRL Cikarang
    - *(Dan seluruh 54 kode stasiun Jabodetabek lainnya)*

---

## 4. Resolusi Nama Lokasi & Terminal ID (Reverse Engineering API)

### 4.1 Identifikasi Endpoint & Kredensial
Dari pembongkaran pustaka native C/C++ `libcard-lib.so`:
* **Base URL**: `https://robycohen.pythonanywhere.com/`
* **Query Endpoint**: `POST https://robycohen.pythonanywhere.com/api/location/list`
* **Insert Endpoint**: `POST https://robycohen.pythonanywhere.com/api/insert-location`
* **Kredensial Autentikasi**:
  * Username: `wakwaw`
  * Password: `wakwaw123`
  * Header HTTP: `Authorization: Basic d2Frd2F3Ondha3dhdzEyMw==`
* **User-Agent**: `okhttp/4.9.0`

> ⚠️ **Catatan**: Jika header `Authorization` tidak disertakan, server akan selalu mengembalikan respons `HTTP 401 Unauthorized`.

---

### 4.2 Format Payload & Respons API

#### A. Query Lokasi (`/api/location/list`)
* **Method**: `POST`
* **Header**:
  ```http
  Content-Type: application/json
  Authorization: Basic d2Frd2F3Ondha3dhdzEyMw==
  User-Agent: okhttp/4.9.0
  ```
* **Request Body**:
  ```json
  {
    "location_id": ["EPT74560", "EPJ15966"]
  }
  ```
* **Response Body** (`HTTP 200 OK`):
  ```json
  {
    "result": [
      {
        "id": 37542,
        "location_id": "EPT74560",
        "location_name": "Stasiun KRL Tangerang"
      },
      {
        "id": 15,
        "location_id": "EPJ15966",
        "location_name": "DUKUH 2"
      }
    ]
  }
  ```

#### B. Kontribusi Lokasi Baru (`/api/insert-location`)
* **Method**: `POST`
* **Request Body**:
  ```json
  {
    "location_id": "EPT74560",
    "location_name": "Stasiun KRL Tangerang"
  }
  ```
* **Response Body**:
  ```json
  {
    "result": "Operation successfully"
  }
  ```

---

### 4.3 Standarisasi Prefix Terminal ID (Database 27.958 Lokasi)

Database remote memuat lebih dari **27.000 lokasi** di seluruh Indonesia dengan format kode:

| Prefix | Kategori | Contoh ID | Contoh Nama Lokasi |
|---|---|---|---|
| **`EPT...`** | Electronic Payment Train / KRL | `EPT74560`, `EPT74976`, `EPT74135` | *Stasiun KRL Tangerang*, *Stasiun KRL Juanda*, *Stasiun KRL Duri* |
| **`EPJ...`** | Electronic Payment Jasa Marga (Tol) | `EPJ15966`, `EPJ13835`, `EPJ17068` | *DUKUH 2*, *GT Tomang*, *GT Meruya* |
| **`EPP...`** | Electronic Payment Parkir | `EPP28996`, `EPP67625`, `EPP18576` | *Parkir Grand Indonesia*, *Mall Gandaria*, *Soekarno Hatta T1* |
| **`EPB...`** | Electronic Payment Bus | `EPB54945`, `EPB54246` | *Transjakarta 8M*, *Buperta Cibubur* |
| **Angka Hex** | Mandiri / Flazz Topup EDC | `21010400`, `EMO00003` | *GT Banyumanik*, *Topup Saldo Flazz* |

---

### 4.4 Penanganan Varian Terminal ID (`EP` vs `EPT`)
Pada beberapa kartu atau pembacaan terminal, kode gerbang yang ditangkap dapat berupa 7 karakter (misal `EP74560`) karena karakter 'T' tidak terbaca atau format terminal yang berbeda.

Aplikasi mengimplementasikan **Smart Variant Resolution**:
* Jika terminal ID adalah `EP74560` (7 karakter diawali `EP`), aplikasi otomatis menyertakan `EPT74560` (8 karakter) dalam array query.
* Sebaliknya jika terminal ID adalah `EPT74560` (8 karakter diawali `EPT`), aplikasi juga mencari `EP74560`.

---

## 5. Arsitektur Caching Lokal (Offline-First)

Untuk meminimalkan latensi dan menghemat kuota internet pengguna, TrackIt menerapkan arsitektur caching berjenjang:

```
[Tap Kartu NFC]
       │
       ▼
[Ekstraksi Terminal ID (ASCII/Hex)]
       │
       ▼
[1. Cek Room DB (nfc_locations)] ──(Ditemukan)──► Tampilkan Nama Instan (Offline)
       │
   (Tidak Ada)
       ▼
[2. Panggil API Cloud (Basic Auth)]
       │
  (Berhasil 200)
       ├──► 3. Simpan ke Room DB (Auto-cache)
       └──► Tampilkan Nama Lokasi
       │
   (Gagal / Offline)
       ▼
Fallback ke format "Terminal <ID>"
```

### Schema Tabel Room: `nfc_locations`
```sql
CREATE TABLE IF NOT EXISTS nfc_locations (
    location_id TEXT PRIMARY KEY NOT NULL,
    location_name TEXT NOT NULL,
    is_user_custom INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL
);
```

---

## 6. Script Pengujian Mandiri (Developer Tooling)

### 6.1 Python Script (`test_location_api.py`)
```python
import json
import urllib.request

def check_location(location_id: str):
    url = "https://robycohen.pythonanywhere.com/api/location/list"
    headers = {
        "Content-Type": "application/json",
        "Authorization": "Basic d2Frd2F3Ondha3dhdzEyMw==",
        "User-Agent": "okhttp/4.9.0"
    }
    payload = json.dumps({"location_id": [location_id]}).encode("utf-8")
    
    req = urllib.request.Request(url, data=payload, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as response:
            print(f"Status: {response.status}")
            data = json.loads(response.read().decode("utf-8"))
            for item in data.get("result", []):
                print(f"[{item.get('location_id')}] -> {item.get('location_name')}")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    check_location("EPT74560")
```

### 5.2 cURL Command
```bash
curl -X POST "https://robycohen.pythonanywhere.com/api/location/list" \
     -H "Content-Type: application/json" \
     -H "Authorization: Basic d2Frd2F3Ondha3dhdzEyMw==" \
     -H "User-Agent: okhttp/4.9.0" \
     -d '{"location_id": ["EPT74560"]}'
```

