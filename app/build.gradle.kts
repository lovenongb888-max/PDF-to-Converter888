plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// เวอร์ชันถูกกำหนดจาก GitHub Actions (tag v1.2.3 -> versionName 1.2.3)
val appVersionName = (findProperty("appVersion") as String?) ?: "1.0.0"
val appVersionCode = (findProperty("appCode") as String?)?.toIntOrNull() ?: 1

// ถ้ามี keystore (จาก GitHub Secrets) จะเซ็นด้วย keystore ของคุณ ไม่งั้นใช้ debug key
val keystorePath: String? = System.getenv("KEYSTORE_PATH")

android {
    namespace = "com.chujai888.pdftoconverter888"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.chujai888.pdftoconverter888"
        minSdk = 29
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (keystorePath != null && File(keystorePath).exists()) {
            create("release") {
                storeFile = File(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ใช้ปลดรหัสผ่านไฟล์ PDF (ทำงานในเครื่อง ไม่ส่งข้อมูลออกไปไหน)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}
