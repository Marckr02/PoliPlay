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
import androidx.lifecycle.lifecycleScope
import com.example.campuspocket.core.preferences.PreferenceKeys
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    /** `true` mientras la app está bloqueada: el contenido no debe mostrarse. */
    val locked = kotlinx.coroutines.flow.MutableStateFlow(false)

    fun register(@Suppress("UNUSED_PARAMETER") context: Context) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) { wentBackgroundAt = System.currentTimeMillis() }
        })
    }

    /** Da 60 s de gracia tras un desbloqueo correcto. */
    fun unlockedNow() { unlockedUntil = System.currentTimeMillis() + 60_000 }

    /** Se llama desde MainActivity.onStart: pide autenticación si venimos de segundo plano. */
    fun maybeLock(activity: FragmentActivity) {
        if (wentBackgroundAt == 0L) return
        val now = System.currentTimeMillis()
        if (unlockedUntil >= now) return
        activity.lifecycleScope.launch {
            val prefs = EntryPointAccessors
                .fromApplication(activity.applicationContext, DsEntryPoint::class.java)
                .dataStore().data.first()
            if (prefs[PreferenceKeys.LOCK_ENABLED] != true) return@launch
            val timeoutSeconds = prefs[PreferenceKeys.LOCK_TIMEOUT_SECONDS] ?: 0
            if (now >= wentBackgroundAt + timeoutSeconds * 1000L) showPrompt(activity)
        }
    }

    fun showPrompt(activity: FragmentActivity) {
        locked.value = true
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                locked.value = false
                unlockedNow()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { /* sigue bloqueado */ }
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
