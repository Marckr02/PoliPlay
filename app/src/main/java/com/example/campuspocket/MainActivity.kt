package com.example.campuspocket

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.navigation.CampusNavHost
import com.example.campuspocket.core.notifications.ReminderReceiver
import com.example.campuspocket.feature.finance.notifications.DailyPaymentReviewWorker
import com.example.campuspocket.feature.settings.security.LockChecker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    /** Tarea a abrir cuando se entra desde una notificación (-1 = ninguna pendiente). */
    private val pendingTaskId = MutableStateFlow(-1L)

    /** Ajuste de tema leído en DataStore ("system" por defecto). */
    private val themePreferences = MutableStateFlow("system")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Revisión diaria de pagos programados a las 8:00.
        DailyPaymentReviewWorker.enqueueOnce(applicationContext)
        consumeTaskExtra(intent)
        themePreferences.value = com.example.campuspocket.feature.settings.security.LockChecker.theme(applicationContext)

        setContent {
            val prefs by themePreferences.collectAsState("system")
            CampusTheme(darkTheme = when (prefs) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CampusNavHost(
                        pendingTaskId = pendingTaskId,
                        onPendingTaskHandled = { pendingTaskId.value = -1L }
                    )
                }
            }
        }

        // Bloqueo opcional al entrar: si el usuario lo activó en Ajustes, el prompt se
        // muestra aquí; la política real vive en LockInterceptor, registrada en CampusApp.
        if (com.example.campuspocket.feature.settings.security.LockChecker.enabled(applicationContext)) {
            com.example.campuspocket.feature.settings.security.LockInterceptor.showPrompt(this)
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
