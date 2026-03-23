plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.dndsync"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.dndsync"
        minSdk = 30
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        ndk {
            abiFilters.add("armeabi-v7a")
        }
        resConfigs("zh")

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
}

dependencies {
}