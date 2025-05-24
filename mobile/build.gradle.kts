plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.iot.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.iot.myapplication"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.firebase.messaging.ktx)
    val awsSdkVersion = "1.4.86"
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.wearable)

    implementation(libs.androidx.lifecycle.service)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play)

    // AWS IOT CORE 통신용
    implementation("software.amazon.awssdk.iotdevicesdk:aws-iot-device-sdk-android:1.25.0")



    // FCM 알람 수신용
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Retrofit (웹 통신용)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp (Retrofit이 내부적으로 사용, 명시적으로 추가하여 버전 관리 가능)
    implementation("com.squareup.okhttp3:okhttp:4.10.0") // 최신 버전 확인
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    wearApp(project(":wear"))
}