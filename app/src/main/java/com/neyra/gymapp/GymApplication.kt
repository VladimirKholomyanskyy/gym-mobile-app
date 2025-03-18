package com.neyra.gymapp

import android.app.Application
import com.neyra.gymapp.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GymApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        // Set the base URL dynamically based on build flavor or environment.
        System.setProperty("com.neyra.gymapp.openapi.baseUrl", "http://10.0.2.2:8080")

    }
}