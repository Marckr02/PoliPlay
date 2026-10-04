package com.example.campuspocket.feature.finance.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.campuspocket.core.notifications.NotificationChannels
import com.example.campuspocket.feature.finance.domain.PaymentDomain.paymentsToNotify
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.LocalTime

/**
 * Revisión diaria a las 8:00 locales: notifica los pagos que vencen hoy o dentro
 * de "recordar N días antes". Una sola notificación routine por pago/día.
 */
class DailyPaymentReviewWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FinanceEntryPoint {
        fun financeRepository(): FinanceRepository
    }

    override suspend fun doWork(): Result {
        val repo = EntryPointAccessors.fromApplication(applicationContext, FinanceEntryPoint::class.java)
            .financeRepository()

        val today = LocalDate.now()
        val allDue = paymentsToNotify(repo.getPaymentsDueUntil(today.plusYears(1)), today)
        allDue.forEach { payment ->
            // Una vez al día por pago: guardo la fecha de notificación en el propio registro.
            // El repo no guarda "lastNotified"; el canal se controla en la capa de notificación.
            notifyPayment(applicationContext, payment)
        }
        return Result.success()
    }

    private fun notifyPayment(context: Context, payment: com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val channelId = NotificationChannels.PAYMENTS
        NotificationChannels.ensureCreated(context)

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.campuspocket.R.drawable.ic_notification)
            .setContentTitle(payment.name)
            .setContentText(
                context.getString(
                    com.example.campuspocket.R.string.notification_payment_due,
                    payment.nextDueDate.toString()
                )
            )
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(("pay_" + payment.id).hashCode(), notification)
    }

    companion object {
        const val WORK_NAME = "daily_payment_review_8am"

        /** Retraso en milisegundos hasta la próxima 8:00 locales. */
        fun delayToNext8AM(now: LocalTime = LocalTime.now(), today: LocalDate = LocalDate.now()): Long {
            val target = LocalTime.of(8, 0)
            val base = today.atTime(target)
            val candidate = if (now.isBefore(target)) base else base.plusDays(1)
            return java.time.Duration.between(today.atTime(now), candidate).toMillis()
        }

        fun enqueueOnce(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<DailyPaymentReviewWorker>(java.time.Duration.ofDays(1))
                    .setInitialDelay(java.time.Duration.ofMillis(delayToNext8AM()))
                    .build()
            )
        }

        /** Botón "Revisar ahora" (solo debug). */
        fun enqueueNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME + "_manual",
                androidx.work.ExistingWorkPolicy.REPLACE,
                androidx.work.OneTimeWorkRequestBuilder<DailyPaymentReviewWorker>().build()
            )
        }
    }
}
