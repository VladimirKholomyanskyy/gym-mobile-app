package com.neyra.gymapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class GymApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        // Set the base URL dynamically based on build flavor or environment.
        System.setProperty("com.neyra.gymapp.openapi.baseUrl", "http://10.0.2.2:8080")
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // For release builds, you might want a custom tree that reports crashes
            // Timber.plant(ReleaseTree())
        }
    }
}