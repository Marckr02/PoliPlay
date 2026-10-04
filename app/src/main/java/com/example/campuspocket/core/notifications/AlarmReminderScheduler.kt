package com.example.campuspocket.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.campuspocket.core.util.LogUtil
import com.example.campuspocket.feature.academic.data.TaskDao
import com.example.campuspocket.feature.academic.data.TaskReminderDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val reminderDao: TaskReminderDao
) : ReminderScheduler {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(reminderId: Long, taskId: Long, remindAtMillis: Long, title: String) {
        val intent = baseIntent().apply {
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
        }
        // Un PendingIntent distinto por recordatorio (requestCode = id del recordatorio).
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canUseExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAtMillis, pendingIntent)
                LogUtil.w("Alarma exacta no permitida; se usa alarma inexacta (recordatorio $reminderId)")
            }
        } catch (e: SecurityException) {
            // El permiso se pudo revocar entre la comprobación y la llamada.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAtMillis, pendingIntent)
            LogUtil.w("Sin permiso de alarma exacta; alarma inexacta (recordatorio $reminderId)")
        }
    }

    override fun cancel(reminderId: Long) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            baseIntent(),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    override suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        val upcoming = reminderDao.getUpcoming(now)
        upcoming.forEach { reminder ->
            val task = taskDao.getById(reminder.taskId) ?: return@forEach
            if (task.completedAt != null) return@forEach
            schedule(reminder.id, task.id, reminder.remindAt, task.title)
        }
        LogUtil.d("Reprogramados ${upcoming.size} recordatorios")
    }

    /** Intent sin extras: sirve para identificar el PendingIntent (los extras no cuentan para la igualdad). */
    private fun baseIntent(): Intent =
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMINDER)
}
