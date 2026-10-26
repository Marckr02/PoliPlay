package com.example.campuspocket

import android.content.Intent
import android.net.Uri
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
import com.example.campuspocket.core.designsystem.MotionKeyHolder
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

    /** Ruta directa enviada desde un App Shortcut o desde un Intent ACTION_SEND. */
    private val pendingRoute = MutableStateFlow<String?>(null)

    /** PDF elegido en la bienvenida: se importa al entrar en la app. */
    private val pendingImportUri = MutableStateFlow<Uri?>(null)

    /** Único repositorio de preferencias: perfil local, módulos y tema. */
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val motionKeyHolder = MotionKeyHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Revisión diaria de pagos programados a las 8:00.
        DailyPaymentReviewWorker.enqueueOnce(applicationContext)
        consumeIntents(intent)

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
            CampusTheme(darkTheme = darkTheme, motionKeyHolder = motionKeyHolder) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val prefs = preferences
                    when {
                        prefs == null -> Box(Modifier.fillMaxSize())
                        !prefs.onboardingCompleted -> OnboardingScreen(
                            onFinished = { pendingImportUri.value = it }
                        )
                        else -> CompositionLocalProvider(LocalPrivacyMode provides prefs.hideAmounts) {
                            CampusNavHost(
                                preferences = prefs,
                                pendingTaskId = pendingTaskId,
                                onPendingTaskHandled = { pendingTaskId.value = -1L },
                                pendingRoute = pendingRoute,
                                onPendingRouteHandled = { pendingRoute.value = null },
                                pendingImportUri = pendingImportUri,
                                onPendingImportHandled = { pendingImportUri.value = null },
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
        consumeIntents(intent)
    }

    override fun onResume() {
        super.onResume()
        motionKeyHolder.refresh()
    }

    private fun consumeIntents(intent: Intent?) {
        if (intent == null) return
        val taskId = intent.getLongExtra(ReminderReceiver.EXTRA_TASK_ID, -1L)
        if (taskId != -1L) {
            pendingTaskId.value = taskId
            intent.removeExtra(ReminderReceiver.EXTRA_TASK_ID)
        }

        val routeExtra = intent.getStringExtra("route")
        if (!routeExtra.isNullOrBlank()) {
            pendingRoute.value = routeExtra
            intent.removeExtra("route")
        } else if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                pendingRoute.value = "notes/editor"
                intent.action = null
            }
        }
    }
}