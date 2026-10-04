package com.example.campuspocket.feature.settings.security

import android.content.Context
import com.example.campuspocket.feature.settings.ui.SettingsViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Lectura síncrona del DataStore para decidir bloqueo (segura: no guarda estados de sesión). */
object LockChecker {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DataStoreEntryPoint {
        fun dataStore(): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    }

    fun theme(context: Context): String = runBlocking {
        EntryPointAccessors.fromApplication(context.applicationContext, LockInterceptor.DsEntryPoint::class.java)
            .dataStore().data.first()[SettingsViewModel.KEY_THEME] ?: "system"
    }

    fun enabled(context: Context): Boolean = runBlocking {
        EntryPointAccessors.fromApplication(context.applicationContext, LockInterceptor.DsEntryPoint::class.java)
            .dataStore().data.first()[SettingsViewModel.KEY_LOCK_ENABLED] == true
    }
}

typealias Preferences = androidx.datastore.preferences.core.Preferences
