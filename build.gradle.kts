plugins {
    // עדכון לגרסת AGP התואמת לתלויות
    id("com.android.application") version "8.9.0" apply false

    // Kotlin בגרסה תואמת ל-Firebase
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false

    // Google Services plugin
    id("com.google.gms.google-services") version "4.4.1" apply false

    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}
