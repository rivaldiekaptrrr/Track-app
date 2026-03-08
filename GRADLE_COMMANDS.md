# Gradle Wrapper Commands

Dokumentasi perintah Gradle yang umum digunakan untuk project TrackIt.

## Persiapan

Buka terminal di folder project (`D:\App\Track_it`), lalu jalankan perintah di bawah ini.

> **Catatan:** Untuk Windows, gunakan `.gradlew.bat`  
> Contoh: `./gradlew.bat assembleDebug`

---

## Perintah Dasar

| Perintah | Fungsi |
|----------|--------|
| `./gradlew -v` | Cek versi Gradle & Java |
| `./gradlew tasks` | Lihat semua task yang tersedia |
| `./gradlew help` | Menampilkan bantuan |

---

## Build Commands

| Perintah | Fungsi |
|----------|--------|
| `./gradlew assembleDebug` | Build debug APK |
| `./gradlew assembleRelease` | Build release APK (perlu signing config) |
| `./gradlew assemble` | Build semua variant (debug + release) |

---

## Run & Install

| Perintah | Fungsi |
|----------|--------|
| `./gradlew installDebug` | Install debug APK ke device/emulator yang terhubung |
| `./gradlew installRelease` | Install release APK ke device/emulator |

---

## Development

| Perintah | Fungsi |
|----------|--------|
| `./gradlew clean` | Hapus folder build (bersihkan cache) |
| `./gradlew build` | Full build: clean + assemble |
| `./gradlew --recompile-scripts` |强制重新编译 build scripts |

---

## Debugging

| Perintah | Fungsi |
|----------|--------|
| `./gradlew assembleDebug --stacktrace` | Build dengan menampilkan stack trace error |
| `./gradlew assembleDebug --info` | Build dengan verbose output (detail) |
| `./gradlew assembleDebug --scan` | Build dengan Gradle Build Scan (buka di browser) |

---

## Dependency Management

| Perintah | Fungsi |
|----------|--------|
| `./gradlew dependencies` | Tampilkan semua dependency tree |
| `./gradlew app:dependencies` | Tampilkan dependency untuk module app saja |
| `./gradlew buildEnvironment` | Tampilkan environment classpath |

---

## Testing (jika ada)

| Perintah | Fungsi |
|----------|--------|
| `./gradlew test` | Run unit tests |
| `./gradlew connectedAndroidTest` | Run instrumented tests (di emulator/device) |

---

## Maintenance

| Perintah | Fungsi |
|----------|--------|
| `./gradlew --stop` | Hentikan Gradle daemon yang sedang berjalan |
| `./gradlew --status` | Cek status Gradle daemon |
| `./gradlew wrapper --gradle-version=X.X` | Upgrade/downgrade wrapper ke versi tertentu |

---

## Contoh Penggunaan

### Pertama kali setup
```bash
# Clean lalu build debug
./gradlew clean assembleDebug
```

### Install ke emulator
```bash
# Pastikan emulator/device terhubung, lalu:
./gradlew installDebug
```

### Cek error detail
```bash
# Kalau build gagal, jalankan ini untuk lihat error lengkap:
./gradlew assembleDebug --stacktrace
```

---

## Troubleshooting

| Masalah | Solusi |
|---------|--------|
| Build lambat | Tambah RAM untuk Gradle di `gradle.properties`: `org.gradle.jvmargs=-Xmx4g` |
| Daemon error | Jalankan `./gradlew --stop` lalu build ulang |
| Cache corrupt | Jalankan `./gradlew clean` lalu build ulang |

---

## Catatan

- Semua perintah di atas menggunakan **Gradle Wrapper** (`./gradlew`)
- Tidak perlu install Gradle secara manual di komputer
- Wrapper akan otomatis download Gradle versi yang sesuai (`gradle-wrapper.properties`)
