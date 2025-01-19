plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}

buildscript {
    dependencies {
        // Add the AGP (Android Gradle Plugin) classpath here
        classpath("com.android.tools.build:gradle:8.5.1") // AGP version that supports SDK 35
    }
}

allprojects {
    // Remove repository declarations here since they are now handled in settings.gradle.kts
}
