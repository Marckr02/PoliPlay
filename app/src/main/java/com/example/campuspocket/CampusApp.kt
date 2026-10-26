package com.example.campuspocket

import android.app.Application
import com.example.campuspocket.core.notifications.NotificationChannels
import com.example.campuspocket.feature.settings.security.LockInterceptor
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class CampusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val esLocale = Locale.forLanguageTag("es")
        Locale.setDefault(esLocale)
        NotificationChannels.ensureCreated(this)
        LockInterceptor.register(this)
    }
}