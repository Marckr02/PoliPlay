package com.example.campuspocket.feature.settings.security

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.campuspocket.feature.settings.ui.SettingsViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Bloqueo al pasar a segundo plano (credencial del dispositivo o biometría fuerte).
 * La política es la misma en JVM y en el dispositivo; solo si el ajuste está activo.
 * Si el dispositivo NO tiene credencial ni huella registrada, nunca se activa.
 */
object LockInterceptor {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DsEntryPoint { fun dataStore(): DataStore<Preferences> }

    @Volatile private var wentBackgroundAt: Long = 0
    @Volatile private var unlockedUntil: Long = 0

    fun register(context: Context) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) { wentBackgroundAt = System.currentTimeMillis() }
            override fun onStart(owner: LifecycleOwner) { maybeLock(context) }
        })
    }

    /** Expira: Sistema busca alfeizar si app pasó a background y volvió. */
    fun unlockedNow() { unlockedUntil = System.currentTimeMillis() + 60_000 }

    private fun maybeLock(context: Context) {
        if (wentBackgroundAt == 0L) return
        val now = System.currentTimeMillis()
        if (unlockedUntil >= now) return
        runBlocking {
            val prefs = EntryPointAccessors
                .fromApplication(context.applicationContext, DsEntryPoint::class.java)
                .dataStore().data.first()
            if (prefs[SettingsViewModel.KEY_LOCK_ENABLED] != true) return@runBlocking
            val timeoutSeconds = prefs[SettingsViewModel.KEY_LOCK_TIMEOUT] ?: 0
            if (now >= wentBackgroundAt + timeoutSeconds * 1000L && context is FragmentActivity) {
                showPrompt(context)
            }
        }
    }

    fun showPrompt(activity: FragmentActivity) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { unlockedNow() }
        })
        val builder = BiometricPrompt.PromptInfo.Builder().setTitle("CampusPocket").setSubtitle("Desbloquear")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            builder.setDeviceCredentialAllowed(true)
        }
        prompt.authenticate(builder.build())
    }
}
