package com.example.campuspocket.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.campuspocket.R

/** Canales de notificación (minSdk 26: los canales siempre existen). */
object NotificationChannels {
    const val TASKS = "task_reminders"
    const val PAYMENTS = "payments"
    const val BUDGET = "budget_alerts"
    const val CLASSES = "class_reminders"

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    TASKS,
                    context.getString(R.string.notification_channel_tasks),
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    PAYMENTS,
                    context.getString(R.string.notification_channel_payments),
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    BUDGET,
                    context.getString(R.string.notification_channel_budget),
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                // Desactivado por defecto en la app: importancia baja hasta que el usuario lo active.
                NotificationChannel(
                    CLASSES,
                    context.getString(R.string.notification_channel_classes),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        )
    }
}
