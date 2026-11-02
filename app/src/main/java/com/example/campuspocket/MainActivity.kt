package com.example.campuspocket

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.LocalPrivacyMode
import com.example.campuspocket.core.designsystem.MotionKeyHolder
import com.example.campuspocket.core.designsystem.components.CampusButton
import com.example.campuspocket.core.navigation.CampusNavHost
import com.example.campuspocket.core.notifications.ReminderReceiver
import com.example.campuspocket.core.preferences.ThemeMode
import com.example.campuspocket.core.preferences.UserPreferencesRepository
import com.example.campuspocket.core.shortcuts.ShortcutPublisher
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

    /** Atajos dinámicos del ícono (repetir gasto y nota rápida). */
    @Inject
    lateinit var shortcutPublisher: ShortcutPublisher

    private val motionKeyHolder = MotionKeyHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Revisión diaria de pagos programados a las 8:00.
        DailyPaymentReviewWorker.enqueueOnce(applicationContext)
        consumeIntents(intent)

        // Los atajos se recargan al volver a primer plano para reflejar gastos
        // creados mientras la app estaba en segundo plano.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                shortcutPublisher.keepPublished()
            }
        }

        setContent {
            val locked by LockInterceptor.locked.collectAsStateWithLifecycle()
            // null = aún no llegó la primera lectura de DataStore (evita parpadeos).
            val preferences by userPreferencesRepository.preferences
                .collectAsStateWithLifecycle(initialValue = null)
            val themeMode = preferences?.themeMode ?: ThemeMode.SYSTEM
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // Los iconos de la barra de estado siguen el tema elegido en la app, no el del sistema.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkTheme }
                )
                onDispose { }
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
                        else -> if (locked) {
                            // Bloqueado: no se compone el NavHost; solo el botón de desbloqueo.
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(Modifier.fillMaxSize().weight(1f))
                                CampusButton(
                                    text = stringResource(R.string.lock_unlock),
                                    onClick = { LockInterceptor.showPrompt(this@MainActivity) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                )
                            }
                        } else {
                            CompositionLocalProvider(LocalPrivacyMode provides prefs.hideAmounts) {
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
        }

        // Bloqueo opcional al entrar: si el usuario lo activó en Ajustes, el prompt se
        // muestra aquí; la política real vive en LockInterceptor, registrada en CampusApp.
        lifecycleScope.launch {
            if (LockChecker.enabled(applicationContext)) LockInterceptor.showPrompt(this@MainActivity)
        }
    }

    override fun onStart() {
        super.onStart()
        LockInterceptor.maybeLock(this)
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

        val routeExtra = intent.getStringExtra(ShortcutPublisher.EXTRA_ROUTE)
        if (!routeExtra.isNullOrBlank()) {
            pendingRoute.value = routeExtra
            intent.removeExtra(ShortcutPublisher.EXTRA_ROUTE)
        } else if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                pendingRoute.value = "notes/editor/-1"
                intent.action = null
            }
        }
    }
}