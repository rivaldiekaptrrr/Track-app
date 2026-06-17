# Panduan Setup Google Drive Backup — TrackIt

Dokumen ini adalah panduan **khusus Developer** untuk mengonfigurasi Google Drive API agar fitur Backup & Restore ke Google Drive bisa berjalan pada aplikasi TrackIt.

---

## Prasyarat

- Akun Google yang terhubung ke Google Cloud Console
- Android Studio dengan proyek TrackIt
- SHA-1 fingerprint dari keystore kamu (Debug dan/atau Release)

---

## Langkah 1: Buat Project di Google Cloud Console

1. Buka [https://console.cloud.google.com](https://console.cloud.google.com)
2. Klik **"Select a project"** → **"New Project"**
3. Beri nama proyek: `TrackIt`
4. Klik **"Create"**

---

## Langkah 2: Aktifkan Google Drive API

1. Di dalam proyek yang baru dibuat, buka **"APIs & Services"** → **"Library"**
2. Cari **"Google Drive API"**
3. Klik hasil pertama → Klik **"Enable"**

---

## Langkah 3: Konfigurasi OAuth Consent Screen

1. Buka **"APIs & Services"** → **"OAuth consent screen"**
2. Pilih **"External"** → Klik **"Create"**
3. Isi formulir:
   - **App name**: `TrackIt`
   - **User support email**: *(email kamu)*
   - **Developer contact information**: *(email kamu)*
4. Klik **"Save and Continue"** (skip Scopes, skip Test Users)
5. Klik **"Back to Dashboard"**

---

## Langkah 4: Buat OAuth 2.0 Client ID (untuk Android)

1. Buka **"APIs & Services"** → **"Credentials"**
2. Klik **"+ Create Credentials"** → **"OAuth client ID"**
3. Pilih **Application type: Android**
4. Isi formulir:
   - **Name**: `TrackIt Android`
   - **Package name**: `com.trackit.app`
   - **SHA-1 certificate fingerprint**: *(lihat cara mendapatkannya di bawah)*
5. Klik **"Create"**

> ⚠️ **Penting:** Kamu perlu membuat **2 Client ID terpisah** — satu untuk *Debug Keystore* dan satu lagi untuk *Release Keystore* (Production).

---

## Cara Mendapatkan SHA-1 Fingerprint

### Debug Keystore (otomatis oleh Android Studio)
Jalankan perintah berikut di Terminal:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```
Salin nilai **SHA1** dari output.

### Release Keystore (Keystore kamu sendiri)
```bash
keytool -list -v -keystore /path/to/your/release.keystore -alias your-key-alias
```

---

## Langkah 5: Tambahkan Dependency ke `build.gradle.kts`

Buka file [build.gradle.kts](file:///d:\Project\Track-app\app\build.gradle.kts) dan pastikan dependency berikut sudah ada di blok `dependencies`:

```kotlin
// Google Sign-In
implementation("com.google.android.gms:play-services-auth:21.2.0")

// Google API Client
implementation("com.google.api-client:google-api-client-android:2.2.0") {
    exclude(group = "org.apache.httpcomponents")
}

// Google Drive API v3
implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0") {
    exclude(group = "org.apache.httpcomponents")
}
```

---

## Langkah 6: Tambahkan Client ID ke `strings.xml`

Buka atau buat `app/src/main/res/values/strings.xml` dan tambahkan:

```xml
<string name="google_oauth_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
```

> Client ID untuk Android tidak perlu dimasukkan ke strings.xml. Hanya **Web Client ID** (yang digenerate otomatis saat kamu membuat Android Client ID) yang digunakan untuk `GoogleSignInOptions.requestIdToken()`.

---

## Langkah 7: Verifikasi

Setelah semua konfigurasi selesai:
1. Jalankan aplikasi di *Debug build*
2. Buka **Pengaturan** → **Backup Google Drive**
3. Ketuk **"Login dengan Google"**
4. Pilih akun Google kamu → Izinkan akses
5. Ketuk **"Backup Sekarang"** dan cek apakah file muncul di Google Drive folder `TrackIt Backups`

---

## Catatan Penting

| Lingkungan | SHA-1 | Client ID |
|---|---|---|
| Debug | Debug Keystore SHA-1 | Debug Android Client |
| Release/Play Store | Release Keystore SHA-1 | Production Android Client |

> Jika menggunakan Play App Signing, SHA-1 *Release* harus diambil dari **Google Play Console → Setup → App Signing**.

---

*Panduan ini dibuat untuk tim developer TrackIt. Jangan bagikan Client ID atau SHA-1 ke publik.*
