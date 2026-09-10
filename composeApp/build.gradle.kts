plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("com.android.library")
}


kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            
            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            
            // Koin Dependency Injection
            implementation("io.insert-koin:koin-core:3.5.3")
            implementation("io.insert-koin:koin-compose:1.1.2")
            
            // Ktor Client for REST API / Cloud Sync
            implementation("io.ktor:ktor-client-core:2.3.8")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.8")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")
            
            // DataStore KMP
            implementation("androidx.datastore:datastore-preferences-core:1.1.0-beta01")
        }

        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:2.3.8")
            implementation("io.insert-koin:koin-android:3.5.3")
            implementation("androidx.core:core-ktx:1.12.0")
            implementation("androidx.biometric:biometric:1.1.0")
        }

        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.8")
        }
    }
}

android {
    namespace = "com.trackit.app.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
