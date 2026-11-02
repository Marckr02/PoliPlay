package com.example.campuspocket.feature.settings.security

import android.content.Context
import com.example.campuspocket.core.preferences.PreferenceKeys
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/**
 * Lectura del DataStore para decidir el bloqueo al entrar.
 * (La preferencia de tema la consume MainActivity reactivamente vía UserPreferencesRepository.)
 */
object LockChecker {

    suspend fun enabled(context: Context): Boolean =
        EntryPointAccessors.fromApplication(context.applicationContext, LockInterceptor.DsEntryPoint::class.java)
            .dataStore().data.first()[PreferenceKeys.LOCK_ENABLED] == true
}
