buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.1")
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.0.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false

    // The google-services plugin is required to parse the google-services.json file
    id("com.google.gms.google-services") version "4.3.15" apply false

    // The Safe Args Gradle plugin generates simple object and builder classes for type-safe access to arguments specified for destinations in your navigation graph.
    id("androidx.navigation.safeargs.kotlin") version "2.7.1" apply false
}