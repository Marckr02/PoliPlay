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
import com.example.campuspocket.feature.academic.data.TaskReminderDao
import com.example.campuspocket.feature.academic.domain.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Muestra el recordatorio de una tarea y permite completarla o posponerla una
 * hora desde la propia notificación (v5 §3.6).
 */
class ReminderReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderEntryPoint {
        fun taskRepository(): TaskRepository
        fun taskReminderDao(): TaskReminderDao
        fun reminderScheduler(): ReminderScheduler
    }

    @SuppressLint("MissingPermission") // El permiso se comprueba explícitamente al mostrar.
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)

        when (intent.action) {
            ACTION_COMPLETE -> {
                if (reminderId <= 0 || taskId <= 0) return
                handleComplete(context, reminderId, taskId)
            }

            ACTION_SNOOZE -> {
                if (reminderId <= 0) return
                handleSnooze(context, reminderId)
            }

            ACTION_REMINDER -> {
                if (reminderId <= 0 || taskId <= 0) return
                showReminder(context, intent, reminderId, taskId)
            }
        }
    }

    private fun showReminder(
        context: Context,
        intent: Intent,
        reminderId: Long,
        taskId: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            LogUtil.w("Sin permiso de notificaciones; no se muestra el recordatorio $reminderId")
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE)
            ?: context.getString(R.string.notification_task_reminder)

        NotificationChannels.ensureCreated(context)

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
            .addAction(
                0,
                context.getString(R.string.notification_complete_task),
                actionIntent(context, ACTION_COMPLETE, reminderId, taskId)
            )
            .addAction(
                0,
                context.getString(R.string.notification_snooze_task),
                actionIntent(context, ACTION_SNOOZE, reminderId, taskId)
            )
            .build()

        NotificationManagerCompat.from(context).notify(reminderId.toInt(), notification)
    }

    private fun handleComplete(context: Context, reminderId: Long, taskId: Long) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val entryPoint = entryPoint(context)
                entryPoint.taskRepository().getById(taskId)?.let { task ->
                    entryPoint.taskRepository().complete(task, Instant.now())
                }
                NotificationManagerCompat.from(context).cancel(reminderId.toInt())
            } catch (e: Exception) {
                LogUtil.e("No se pudo completar la tarea desde la notificación", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSnooze(context: Context, reminderId: Long) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val entryPoint = entryPoint(context)
                val reminder = entryPoint.taskReminderDao().getById(reminderId)
                if (reminder != null) {
                    val newTime = System.currentTimeMillis() + SNOOZE_MILLIS
                    entryPoint.taskReminderDao().update(reminder.copy(remindAt = newTime))
                    val task = entryPoint.taskRepository().getById(reminder.taskId)
                    if (task != null && task.completedAt == null) {
                        entryPoint.reminderScheduler().schedule(
                            reminderId = reminderId,
                            taskId = reminder.taskId,
                            remindAtMillis = newTime,
                            title = task.title
                        )
                    }
                }
                NotificationManagerCompat.from(context).cancel(reminderId.toInt())
            } catch (e: Exception) {
                LogUtil.e("No se pudo posponer el recordatorio", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun actionIntent(
        context: Context,
        action: String,
        reminderId: Long,
        taskId: Long
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            (reminderId.hashCode() * 31) + action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun entryPoint(context: Context): ReminderEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReminderEntryPoint::class.java
        )

    companion object {
        const val ACTION_REMINDER = "com.example.campuspocket.REMINDER"
        const val ACTION_COMPLETE = "com.example.campuspocket.REMINDER_COMPLETE"
        const val ACTION_SNOOZE = "com.example.campuspocket.REMINDER_SNOOZE"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TITLE = "title"

        private const val SNOOZE_MILLIS = 60 * 60 * 1000L
    }
}
