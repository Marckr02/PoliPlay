package com.example.campuspocket.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.campuspocket.core.util.LogUtil
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Tras reiniciar el teléfono las alarmas se pierden: las reprograma desde la base de datos. */
class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootEntryPoint {
        fun reminderScheduler(): ReminderScheduler
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val scheduler = EntryPointAccessors
            .fromApplication(context.applicationContext, BootEntryPoint::class.java)
            .reminderScheduler()

        // goAsync permite trabajar fuera del hilo principal sin que el sistema mate el receiver.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                scheduler.rescheduleAll()
            } catch (e: Exception) {
                LogUtil.e("No se pudieron reprogramar los recordatorios", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
