* **Dukungan Multiplatform (Android & iOS)**:
    * Migrasi arsitektur ke **Compose Multiplatform (CMP)** dan **Kotlin Multiplatform (KMP)**.
    * Pembuatan modul `:composeApp` untuk berbagi UI dan *business logic* antara Android dan iOS.
* **Integrasi Target iOS & SwiftUI**:
    * Penambahan wrapper `iosApp` berbasis SwiftUI (`iOSApp.swift` & `ContentView.swift`).
    * Dukungan keamanan **Face ID / Touch ID** via Apple `LocalAuthentication` framework.
* **Modernisasi Network & Dependency Injection**:
    * Penggunaan **Ktor Client** untuk sinkronisasi cloud Firestore REST API lintas platform.
    * Pengenalan **Koin Multiplatform** untuk manajemen Dependency Injection.
* **Dual-Platform CI/CD Pipeline**:
    * Workflow GitHub Actions baru yang mem-build APK Android (`ubuntu-latest`) dan memvalidasi target iOS (`macos-latest`) secara simultan pada setiap rilis.
