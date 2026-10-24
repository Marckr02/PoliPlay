package com.example.campuspocket

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.LocalPrivacyMode
import com.example.campuspocket.core.navigation.CampusNavHost
import com.example.campuspocket.core.notifications.ReminderReceiver
import com.example.campuspocket.core.preferences.ThemeMode
import com.example.campuspocket.core.preferences.UserPreferencesRepository
import com.example.campuspocket.feature.finance.notifications.DailyPaymentReviewWorker
import com.example.campuspocket.feature.onboarding.ui.OnboardingScreen
import com.example.campuspocket.feature.settings.security.LockChecker
import com.example.campuspocket.feature.settings.security.LockInterceptor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    /** Tarea a abrir cuando se entra desde una notificación (-1 = ninguna pendiente). */
    private val pendingTaskId = MutableStateFlow(-1L)

    /** Único repositorio de preferencias: perfil local, módulos y tema. */
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Revisión diaria de pagos programados a las 8:00.
        DailyPaymentReviewWorker.enqueueOnce(applicationContext)
        consumeTaskExtra(intent)

        setContent {
            // null = aún no llegó la primera lectura de DataStore (evita parpadeos).
            val preferences by userPreferencesRepository.preferences
                .collectAsStateWithLifecycle(initialValue = null)
            val themeMode = preferences?.themeMode ?: ThemeMode.SYSTEM
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            CampusTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val prefs = preferences
                    when {
                        prefs == null -> Box(Modifier.fillMaxSize())
                        !prefs.onboardingCompleted -> OnboardingScreen()
                        else -> CompositionLocalProvider(LocalPrivacyMode provides prefs.hideAmounts) {
                            CampusNavHost(
                                preferences = prefs,
                                pendingTaskId = pendingTaskId,
                                onPendingTaskHandled = { pendingTaskId.value = -1L },
                                onToggleAmountsPrivacy = {
                                    lifecycleScope.launch {
                                        userPreferencesRepository.setHideAmounts(!prefs.hideAmounts)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bloqueo opcional al entrar: si el usuario lo activó en Ajustes, el prompt se
        // muestra aquí; la política real vive en LockInterceptor, registrada en CampusApp.
        if (LockChecker.enabled(applicationContext)) {
            LockInterceptor.showPrompt(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeTaskExtra(intent)
    }

    private fun consumeTaskExtra(intent: Intent?) {
        val taskId = intent?.getLongExtra(ReminderReceiver.EXTRA_TASK_ID, -1L) ?: -1L
        if (taskId != -1L) {
            pendingTaskId.value = taskId
            intent?.removeExtra(ReminderReceiver.EXTRA_TASK_ID)
        }
    }
}
