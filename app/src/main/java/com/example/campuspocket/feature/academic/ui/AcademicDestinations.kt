package com.example.campuspocket.feature.academic.ui

/** Rutas del grafo académico (se usan en CampusNavHost y en el SavedStateHandle del formulario). */
object AcademicDestinations {
    const val HOY = "academic/hoy"
    const val SEMANA = "academic/semana"
    const val MATERIAS = "academic/materias"
    const val TASKS = "academic/tasks"
    const val IMPORT = "academic/import"

    const val COURSE_ID_ARG = "courseId"
    const val COURSE_DETAIL_ROUTE = "academic/course/{$COURSE_ID_ARG}"
    const val MATERIA_FORM_ROUTE = "academic/materia_form?$COURSE_ID_ARG={$COURSE_ID_ARG}"

    const val TASK_ID_ARG = "taskId"
    const val TASK_FORM_ROUTE = "academic/task_form?$TASK_ID_ARG={$TASK_ID_ARG}"

    fun courseDetail(courseId: Long): String = "academic/course/$courseId"

    fun materiaForm(courseId: Long? = null): String =
        if (courseId == null) "academic/materia_form" else "academic/materia_form?$COURSE_ID_ARG=$courseId"

    fun taskForm(taskId: Long? = null): String =
        if (taskId == null) "academic/task_form" else "academic/task_form?$TASK_ID_ARG=$taskId"
}
