package com.example.campuspocket.core.calendar

import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.model.Task
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Resultado de generar el calendario; [eventCount] permite avisar si no había nada. */
data class CalendarExport(
    val content: String,
    val eventCount: Int
)

/**
 * Generador `.ics` sin dependencias de Android. Las clases se exportan como
 * eventos semanales hasta el fin del semestre activo y las entregas pendientes
 * de hoy en adelante como bloques de 30 minutos. El horario se escribe como
 * hora local flotante para que cada calendario lo interprete en su zona.
 */
object IcsCalendar {

    private const val FALLBACK_WEEKS = 16L
    private const val MAX_LINE_BYTES = 75
    private const val TASK_DURATION_MINUTES = 30L

    private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val UTC_STAMP: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

    fun build(
        courses: List<Course>,
        tasks: List<Task>,
        semesterEnd: LocalDate?,
        today: LocalDate,
        zone: ZoneId,
        now: Instant
    ): CalendarExport {
        val events = buildList {
            courses.forEach { course ->
                addAll(classEvents(course, today, semesterEnd))
            }
            addAll(taskEvents(courses, tasks, today, zone))
        }.sortedWith(compareBy({ it.start }, { it.summary }))

        val lines = buildList {
            add("BEGIN:VCALENDAR")
            add("VERSION:2.0")
            add("PRODID:-//CampusPocket//PoliPlay//ES")
            add("CALSCALE:GREGORIAN")
            add("METHOD:PUBLISH")
            events.forEach { event ->
                add("BEGIN:VEVENT")
                add("UID:${escape(event.uid)}")
                add("DTSTAMP:${UTC_STAMP.format(now)}")
                add("DTSTART:${DATE_TIME.format(event.start)}")
                add("DTEND:${DATE_TIME.format(event.end)}")
                add("SUMMARY:${escape(event.summary)}")
                event.description?.let { add("DESCRIPTION:${escape(it)}") }
                event.location?.let { add("LOCATION:${escape(it)}") }
                event.rrule?.let { add("RRULE:$it") }
                add("END:VEVENT")
            }
            add("END:VCALENDAR")
        }
        val content = lines.joinToString("\r\n") { foldLine(it) } + "\r\n"
        return CalendarExport(content = content, eventCount = events.size)
    }

    private fun classEvents(
        course: Course,
        today: LocalDate,
        semesterEnd: LocalDate?
    ): List<Event> = course.sessions.mapNotNull { session ->
        val first = firstDateOnOrAfter(today, session.dayOfWeek)
        val count = if (semesterEnd == null) {
            FALLBACK_WEEKS
        } else {
            if (first.isAfter(semesterEnd)) return@mapNotNull null
            ChronoUnit.WEEKS.between(first, semesterEnd) + 1
        }
        val courseKey = course.id?.toString() ?: course.code.hashCode().toString()
        val sessionKey = session.id?.toString()
            ?: "${session.dayOfWeek.value}-${session.startTime}"
        val details = listOfNotNull(
            course.code.takeIf { it.isNotBlank() }?.let { "Código: $it" },
            course.section.takeIf { it.isNotBlank() }?.let { "Sección: $it" },
            course.teacher?.takeIf { it.isNotBlank() }?.let { "Docente: $it" }
        ).joinToString("\n")
        Event(
            uid = "class-$courseKey-$sessionKey@campuspocket",
            start = first.atTime(session.startTime),
            end = first.atTime(session.endTime),
            summary = course.name,
            description = details.ifBlank { null },
            location = session.room?.takeIf { it.isNotBlank() },
            rrule = "FREQ=WEEKLY;COUNT=$count"
        )
    }

    private fun taskEvents(
        courses: List<Course>,
        tasks: List<Task>,
        today: LocalDate,
        zone: ZoneId
    ): List<Event> {
        val courseNames = courses.mapNotNull { course ->
            course.id?.let { it to course.name }
        }.toMap()
        return tasks.mapNotNull { task ->
            if (task.completedAt != null) return@mapNotNull null
            val due = task.dueAt.atZone(zone)
            if (due.toLocalDate().isBefore(today)) return@mapNotNull null
            val details = listOfNotNull(
                task.description?.takeIf { it.isNotBlank() },
                task.courseId?.let(courseNames::get)?.let { "Materia: $it" }
            ).joinToString("\n")
            Event(
                uid = "task-${task.id ?: "${task.title.hashCode()}-${task.dueAt}"}@campuspocket",
                start = due.toLocalDateTime(),
                end = due.plusMinutes(TASK_DURATION_MINUTES).toLocalDateTime(),
                summary = task.title,
                description = details.ifBlank { null },
                location = null,
                rrule = null
            )
        }
    }

    private fun firstDateOnOrAfter(today: LocalDate, day: DayOfWeek): LocalDate {
        val days = (day.value - today.dayOfWeek.value + 7) % 7
        return today.plusDays(days.toLong())
    }

    /**
     * RFC 5545: ninguna línea debe superar 75 octetos. Las continuaciones
     * empiezan con un espacio y se cuentan dentro del límite.
     */
    private fun foldLine(line: String): String {
        if (line.toByteArray(Charsets.UTF_8).size <= MAX_LINE_BYTES) return line
        val output = StringBuilder()
        val current = StringBuilder()
        var currentBytes = 0
        var firstLine = true
        var index = 0
        while (index < line.length) {
            val codePoint = line.codePointAt(index)
            val chars = Character.charCount(codePoint)
            val piece = line.substring(index, index + chars)
            val pieceBytes = piece.toByteArray(Charsets.UTF_8).size
            val limit = if (firstLine) MAX_LINE_BYTES else MAX_LINE_BYTES - 1
            if (currentBytes + pieceBytes > limit && current.isNotEmpty()) {
                output.append(current).append("\r\n ")
                current.setLength(0)
                currentBytes = 0
                firstLine = false
            }
            current.append(piece)
            currentBytes += pieceBytes
            index += chars
        }
        output.append(current)
        return output.toString()
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n")

    private data class Event(
        val uid: String,
        val start: LocalDateTime,
        val end: LocalDateTime,
        val summary: String,
        val description: String?,
        val location: String?,
        val rrule: String?
    )
}
