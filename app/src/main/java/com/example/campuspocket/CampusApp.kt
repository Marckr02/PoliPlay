package com.example.campuspocket

import android.app.Application
import com.example.campuspocket.core.notifications.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CampusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        com.example.campuspocket.feature.settings.security.LockInterceptor.register(this)
    }
}
