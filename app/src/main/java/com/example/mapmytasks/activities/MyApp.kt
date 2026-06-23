package com.example.mapmytasks.activities

import android.app.Application
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.example.mapmytasks.BuildConfig

class MyApp : Application() {

    // Initializes global Firebase configurations, logging, and offline persistence when the app starts.
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        FirebaseFirestore.setLoggingEnabled(true)

        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        FirebaseAuth.getInstance()

        // Installs a debug provider for Firebase App Check to allow testing without physical device attestation.
        if (BuildConfig.DEBUG) {
            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        }
    }
}