plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "cat.gestorhort.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "cat.gestorhort.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

// Automatically sync latest web assets into assets folder before building
val syncWebAssetsTask = tasks.register<Copy>("syncWebAssets") {
    val rootDir = file("../..")
    from(rootDir) {
        include("*.html")
        include("*.json")
        include("*.js")
        include("*.png")
    }
    into(file("src/main/assets"))
}

tasks.named("preBuild") {
    dependsOn(syncWebAssetsTask)
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.webkit:webkit:1.12.0")
}
