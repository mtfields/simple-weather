plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android { namespace = "info.mfields.weather"; compileSdk = 35
 defaultConfig { applicationId = "info.mfields.weather"; minSdk = 29; targetSdk = 35; versionCode = 1; versionName = "1.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 buildTypes { release { isMinifyEnabled = false; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro") } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 buildFeatures { compose = true }
}

dependencies {
 val composeBom = platform("androidx.compose:compose-bom:2025.01.01")
 implementation(composeBom); androidTestImplementation(composeBom)
 implementation("androidx.core:core-ktx:1.15.0")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
 implementation("androidx.activity:activity-compose:1.10.0")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 debugImplementation("androidx.compose.ui:ui-tooling")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 implementation("androidx.datastore:datastore:1.1.1")
 implementation("androidx.work:work-runtime-ktx:2.10.0")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 implementation("com.google.android.gms:play-services-location:21.3.0")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
 testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
