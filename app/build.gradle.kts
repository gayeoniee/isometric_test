plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.daengs.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.daengs.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 디버그 서명 키를 저장소에 넣어 공유한다.
    // PC 마다 다른 ~/.android/debug.keystore 로 서명되면 폰에 이미 깔린 앱을
    // 덮어쓰지 못하고 INSTALL_FAILED_UPDATE_INCOMPATIBLE 이 난다.
    // (디버그 전용 키라 공개해도 안전하다. 릴리스 키는 절대 커밋하지 않는다)
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // 도감 WebView 가 앱 안의 파일을 https 오리진으로 읽게 해준다 (WebViewAssetLoader).
    // 서버가 아니라 로더다 — 인터넷 권한은 필요 없다.
    implementation(libs.androidx.webkit)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}