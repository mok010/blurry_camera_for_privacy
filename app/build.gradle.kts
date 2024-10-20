plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "org.techtown.new_camera"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.techtown.new_camera"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // RenderScript 설정 추가 (Kotlin 스크립트 방식)
        renderscriptTargetApi = 18
        renderscriptSupportModeEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation ("me.relex:circleindicator:VERSION")
    implementation ("androidx.viewpager2:viewpager2:1.0.0")
    implementation ("me.relex:circleindicator:2.1.6")
    implementation ("com.google.mlkit:face-detection:16.1.5")
    implementation ("com.google.mlkit:pose-detection:18.0.0-beta5")
    implementation ("com.google.mlkit:pose-detection-accurate:18.0.0-beta5")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.vision.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}