package com.example.campuspocket.core.notifications

/** Programa y cancela los recordatorios de tareas. Un recordatorio = una alarma. */
interface ReminderScheduler {
    fun schedule(reminderId: Long, taskId: Long, remindAtMillis: Long, title: String)
    fun cancel(reminderId: Long)

    /** Reprograma desde la base de datos todos los recordatorios futuros (p. ej. tras reiniciar el teléfono). */
    suspend fun rescheduleAll()
}
