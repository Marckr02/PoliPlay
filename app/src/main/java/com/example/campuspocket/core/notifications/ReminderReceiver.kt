package com.example.campuspocket.core.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.campuspocket.MainActivity
import com.example.campuspocket.R
import com.example.campuspocket.core.util.LogUtil

/** Muestra la notificación cuando se dispara la alarma de un recordatorio de tarea. */
class ReminderReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission") // el permiso se comprueba explícitamente abajo
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE)
            ?: context.getString(R.string.notification_task_reminder)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            LogUtil.w("Sin permiso de notificaciones; no se muestra el recordatorio $reminderId")
            return
        }

        NotificationChannels.ensureCreated(context)

        // Al tocar la notificación se abre la app (la navegación a la tarea llega en la Fase 3).
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.TASKS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notification_task_reminder))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(reminderId.toInt(), notification)
    }

    companion object {
        const val ACTION_REMINDER = "com.example.campuspocket.REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TITLE = "title"
    }
}
