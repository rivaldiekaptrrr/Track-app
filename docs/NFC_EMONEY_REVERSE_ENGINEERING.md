# Dokumentasi Teknis: Reverse Engineering NFC E-Money & Arsitektur Resolusi Lokasi

Dokumen ini mencatat temuan teknis dari hasil *reverse engineering* kartu smartcard e-money Indonesia (BCA Flazz Gen 1/Gen 2, Mandiri e-Money, BNI TapCash, BRI Brizzi) serta arsitektur integrasi API untuk resolusi nama lokasi/terminal transaksi.

---

## 1. Ikhtisar Smartcard E-Money Indonesia

Kartu e-money di Indonesia menggunakan protokol contactless smartcard berbasis **ISO/IEC 14443-4 (Type A)** dengan komunikasi APDU (*Application Protocol Data Unit*):

| Kartu | Protokol | AID (Application Identifier) | Log Transaksi |
|---|---|---|---|
| **BCA Flazz (Gen 1 & 2)** | ISO-DEP (Type A) | `D3 60 00 00 01 00 00 00` | Tersedia (Gen 1: blok 120 hex, Gen 2: blok 64 hex) |
| **Mandiri e-Money (G1 & G2)** | ISO-DEP (Type A) | `A0 00 00 00 03 00 00 00` / `00 01` | Tersedia (blok 48 hex per transaksi) |
| **BNI TapCash** | ISO-DEP (Type A) | `90 5A 00 00 03 01 00 00` | Tersedia pada sektor riwayat |
| **BRI Brizzi** | ISO-DEP (Type A) | `A0 00 00 00 03 00 00 00` | Tersedia pada file log |

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

## 3. Resolusi Nama Lokasi & Terminal ID (Reverse Engineering API)

### 3.1 Identifikasi Endpoint & Kredensial
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

### 3.2 Format Payload & Respons API

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

### 3.3 Standarisasi Prefix Terminal ID (Database 27.958 Lokasi)

Database remote memuat lebih dari **27.000 lokasi** di seluruh Indonesia dengan format kode:

| Prefix | Kategori | Contoh ID | Contoh Nama Lokasi |
|---|---|---|---|
| **`EPT...`** | Electronic Payment Train / KRL | `EPT74560`, `EPT74976`, `EPT74135` | *Stasiun KRL Tangerang*, *Stasiun KRL Juanda*, *Stasiun KRL Duri* |
| **`EPJ...`** | Electronic Payment Jasa Marga (Tol) | `EPJ15966`, `EPJ13835`, `EPJ17068` | *DUKUH 2*, *GT Tomang*, *GT Meruya* |
| **`EPP...`** | Electronic Payment Parkir | `EPP28996`, `EPP67625`, `EPP18576` | *Parkir Grand Indonesia*, *Mall Gandaria*, *Soekarno Hatta T1* |
| **`EPB...`** | Electronic Payment Bus | `EPB54945`, `EPB54246` | *Transjakarta 8M*, *Buperta Cibubur* |
| **Angka Hex** | Mandiri / Flazz Topup EDC | `21010400`, `EMO00003` | *GT Banyumanik*, *Topup Saldo Flazz* |

---

### 3.4 Penanganan Varian Terminal ID (`EP` vs `EPT`)
Pada beberapa kartu atau pembacaan terminal, kode gerbang yang ditangkap dapat berupa 7 karakter (misal `EP74560`) karena karakter 'T' tidak terbaca atau format terminal yang berbeda.

Aplikasi mengimplementasikan **Smart Variant Resolution**:
* Jika terminal ID adalah `EP74560` (7 karakter diawali `EP`), aplikasi otomatis menyertakan `EPT74560` (8 karakter) dalam array query.
* Sebaliknya jika terminal ID adalah `EPT74560` (8 karakter diawali `EPT`), aplikasi juga mencari `EP74560`.

---

## 4. Arsitektur Caching Lokal (Offline-First)

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

## 5. Script Pengujian Mandiri (Developer Tooling)

### 5.1 Python Script (`test_location_api.py`)
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

