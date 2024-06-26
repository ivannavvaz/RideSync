import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
    kotlin("kapt")
}

android {
    namespace = "com.inavarro.ridesync"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.inavarro.ridesync"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load local properties
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())

        buildConfigField("String", "MAPS_API_KEY", properties.getProperty("MAPS_API_KEY"))
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    buildFeatures.dataBinding = true
}

dependencies {
    implementation("androidx.activity:activity:1.8.0")

    val firebaseVersion = "32.8.0"
    val firebaseAuthGoogleVersion = "20.7.0"
    val firebaseUIVersion = "8.0.2"
    val navVersion = "2.7.7"
    val glideVersion = "4.13.2"
    val playServicesMapVersion = "18.0.1"
    val playServicesLocationVersion = "19.0.1"

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Firebase bom
    implementation(platform("com.google.firebase:firebase-bom:$firebaseVersion"))

    // Firebase Auth
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Firebase Database
    implementation("com.google.firebase:firebase-database-ktx")

    // Firebase Storage
    implementation("com.google.firebase:firebase-storage-ktx")

    // Firebase Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Firebase Auth with Google+
    implementation("com.google.android.gms:play-services-auth:$firebaseAuthGoogleVersion")

    // Firebase UI Library
    implementation("com.firebaseui:firebase-ui-auth:$firebaseUIVersion")
    implementation("com.firebaseui:firebase-ui-database:$firebaseUIVersion")
    implementation("com.firebaseui:firebase-ui-firestore:$firebaseUIVersion")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    // Glide
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    kapt("com.github.bumptech.glide:compiler:$glideVersion")

    // MapFragment
    implementation("com.google.android.gms:play-services-maps:$playServicesMapVersion")
    implementation("com.google.android.gms:play-services-location:$playServicesLocationVersion")
}