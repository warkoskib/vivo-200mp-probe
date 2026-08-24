plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.vivo200mpprobe"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.vivo200mpprobe"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(
        "androidx.core:core-ktx:1.17.0"
    )

    implementation(
        "androidx.appcompat:appcompat:1.7.1"
    )

    implementation(
        "com.google.android.material:material:1.13.0"
    )

    implementation(
        "androidx.activity:activity-ktx:1.11.0"
    )

    implementation(
        "androidx.constraintlayout:constraintlayout:2.2.1"
    )

    // Shizuku API
    implementation(
        "dev.rikka.shizuku:api:13.1.5"
    )

    // Shizuku provider support
    implementation(
        "dev.rikka.shizuku:provider:13.1.5"
    )

    testImplementation(
        "junit:junit:4.13.2"
    )

    androidTestImplementation(
        "androidx.test.ext:junit:1.3.0"
    )

    androidTestImplementation(
        "androidx.test.espresso:espresso-core:3.7.0"
    )
}
