plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.stravaalt.app"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.stravaalt.app"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
}
