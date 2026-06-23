plugins {
    // Android Gradle Plugin (AGP) version, updated to ensure compatibility with all project dependencies.
    id("com.android.application") version "8.9.0" apply false

    // Kotlin Android plugin version, matched for full compatibility with the Firebase SDK.
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false

    // Google Services plugin, required to process the google-services.json file and connect the app to Firebase.
    id("com.google.gms.google-services") version "4.4.1" apply false

    // Firebase Crashlytics plugin, used for tracking, analyzing, and reporting app crashes in real-time.
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}