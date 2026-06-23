plugins {
    // Applies the core Android application plugin to build an APK
    id("com.android.application")
    // Applies the Kotlin Android plugin for Kotlin language support
    id("org.jetbrains.kotlin.android")
    // Applies Google Services plugin required for Firebase integration
    id("com.google.gms.google-services")
    // Applies Firebase Crashlytics plugin for crash reporting
    id("com.google.firebase.crashlytics")
}

android {
    // The unique application identifier/package name
    namespace = "com.example.mapmytasks"
    // The API level used to compile the project
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mapmytasks"
        // Minimum API level required to run the app (Android 8.0)
        minSdk = 26
        // The API level the app is designed and tested against
        targetSdk = 35
        // Internal version number for app updates
        versionCode = 1
        // Publicly visible version string
        versionName = "1.0"

        // Specifies the test runner for Android instrumented tests
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        // Enables ViewBinding to safely interact with UI components
        viewBinding = true
        // Enables generation of the BuildConfig class for environment variables
        buildConfig = true
    }

    packaging {
        // Excludes specific META-INF files to prevent build conflicts from third-party libraries
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
        }
    }

    buildTypes {
        release {
            // Disables code shrinking and obfuscation for the release build
            isMinifyEnabled = false
            // Specifies the ProGuard rules files
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Sets Java 11 as the target compatibility for Java compilation
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        // Sets Java 11 as the target for Kotlin compilation
        jvmTarget = "11"
    }
}

dependencies {
    // AndroidX & UI: Core libraries for lifecycle, backward compatibility, and modern UI components
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.activity:activity-ktx:1.9.3")

    // Firebase (Using BoM): Bill of Materials ensures all Firebase dependencies use compatible versions
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    // Firebase App Check: Protects API resources from abuse; debug provider allows testing on emulators
    implementation("com.google.firebase:firebase-appcheck-ktx")
    implementation("com.google.firebase:firebase-appcheck-debug")

    // Networking: Retrofit and OkHttp for making REST API calls and logging network traffic
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Google Services: APIs for Places autocomplete, Maps rendering, and location tracking
    implementation("com.google.android.libraries.places:places:4.1.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Charts: MPAndroidChart library for rendering statistical graphs and bar charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Google Calendar API: Libraries for integrating and syncing with Google Calendar
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0")

    // Testing: Frameworks for local unit tests and UI instrumented tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // WorkManager: Schedules and executes deferrable, guaranteed background work
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Google Generative AI: SDK for integrating Gemini AI features into the app
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
}