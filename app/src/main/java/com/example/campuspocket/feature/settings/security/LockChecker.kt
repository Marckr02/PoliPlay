package com.example.campuspocket.feature.settings.security

import android.content.Context
import com.example.campuspocket.core.preferences.PreferenceKeys
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Lectura síncrona del DataStore para decidir el bloqueo al entrar.
 * (La preferencia de tema la consume MainActivity reactivamente vía UserPreferencesRepository.)
 */
object LockChecker {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DataStoreEntryPoint {
        fun dataStore(): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    }

    fun enabled(context: Context): Boolean = runBlocking {
        EntryPointAccessors.fromApplication(context.applicationContext, LockInterceptor.DsEntryPoint::class.java)
            .dataStore().data.first()[PreferenceKeys.LOCK_ENABLED] == true
    }
}
