package com.example.campuspocket.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.PendingScreen
import com.example.campuspocket.feature.academic.ui.AcademicDestinations
import com.example.campuspocket.feature.academic.ui.coursedetail.CourseDetailScreen
import com.example.campuspocket.feature.academic.ui.courseform.CourseFormScreen
import com.example.campuspocket.feature.academic.ui.courses.MateriasScreen
import com.example.campuspocket.feature.academic.ui.importschedule.ImportScheduleScreen
import com.example.campuspocket.feature.academic.ui.taskform.TaskFormScreen
import com.example.campuspocket.feature.academic.ui.tasks.TasksScreen
import com.example.campuspocket.feature.academic.ui.today.HoyScreen
import com.example.campuspocket.feature.academic.ui.week.SemanaScreen
import com.example.campuspocket.feature.finance.ui.FinanceDestinations
import com.example.campuspocket.feature.finance.ui.accounts.AccountDetailScreen
import com.example.campuspocket.feature.finance.ui.accounts.AccountFormScreen
import com.example.campuspocket.feature.finance.ui.categories.CategoriesScreen
import com.example.campuspocket.feature.finance.ui.categories.CategoryFormScreen
import com.example.campuspocket.feature.finance.ui.home.FinanceHomeScreen
import com.example.campuspocket.feature.finance.ui.transactions.TransactionFormScreen
import com.example.campuspocket.feature.notes.ui.NoteEditorScreen
import com.example.campuspocket.feature.notes.ui.NotesDestinations
import com.example.campuspocket.feature.notes.ui.NotesScreen
import com.example.campuspocket.feature.settings.ui.SettingsScreen

/**
 * Un solo NavHost; cada pestaña de la barra inferior es un grafo anidado
 * (navigation(route = destino)). Así la barra puede saber si la pestaña está
 * activa mirando la jerarquía del destino actual.
 */
@Composable
fun CampusNavHost(
    modifier: Modifier = Modifier,
    pendingTaskId: StateFlow<Long> = remember { MutableStateFlow(-1L) },
    onPendingTaskHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Deep link desde una notificación: abre el detalle (formulario) de la tarea.
    val pendingTask by pendingTaskId.collectAsStateWithLifecycle()
    LaunchedEffect(pendingTask) {
        if (pendingTask != -1L) {
            navController.navigate(AcademicDestinations.taskForm(pendingTask))
            onPendingTaskHandled()
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.start.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            academicGraph(navController)
            financeGraph(navController)
            notesGraph(navController)
            settingsGraph()
        }
    }
}

private fun NavGraphBuilder.academicGraph(navController: NavHostController) {
    navigation(startDestination = AcademicDestinations.HOY, route = TopLevelDestination.ACADEMIC.route) {
        composable(AcademicDestinations.HOY) {
            HoyScreen(
                onShowWeek = {
                    navController.navigate(AcademicDestinations.SEMANA) { launchSingleTop = true }
                },
                onNavigateToCourses = { navController.navigate(AcademicDestinations.MATERIAS) },
                onNavigateToTasks = { navController.navigate(AcademicDestinations.TASKS) },
                onOpenCourse = { courseId ->
                    navController.navigate(AcademicDestinations.courseDetail(courseId))
                }
            )
        }
        composable(AcademicDestinations.SEMANA) {
            SemanaScreen(
                onShowToday = {
                    navController.navigate(AcademicDestinations.HOY) { launchSingleTop = true }
                },
                onNavigateToCourses = { navController.navigate(AcademicDestinations.MATERIAS) },
                onNavigateToTasks = { navController.navigate(AcademicDestinations.TASKS) },
                onOpenCourse = { courseId ->
                    navController.navigate(AcademicDestinations.courseDetail(courseId))
                }
            )
        }
        composable(AcademicDestinations.MATERIAS) {
            MateriasScreen(
                onOpenCourse = { courseId ->
                    navController.navigate(AcademicDestinations.courseDetail(courseId))
                },
                onAddCourse = { navController.navigate(AcademicDestinations.materiaForm()) },
                onImport = { navController.navigate(AcademicDestinations.IMPORT) }
            )
        }
        composable(AcademicDestinations.IMPORT) {
            ImportScheduleScreen(onDone = { navController.popBackStack() })
        }
        composable(
            route = AcademicDestinations.COURSE_DETAIL_ROUTE,
            arguments = listOf(
                navArgument(AcademicDestinations.COURSE_ID_ARG) {
                    type = NavType.LongType
                }
            )
        ) {
            CourseDetailScreen(
                onEditCourse = { courseId ->
                    navController.navigate(AcademicDestinations.materiaForm(courseId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = AcademicDestinations.MATERIA_FORM_ROUTE,
            arguments = listOf(
                navArgument(AcademicDestinations.COURSE_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            CourseFormScreen(onDone = { navController.popBackStack() })
        }
        composable(AcademicDestinations.TASKS) {
            TasksScreen(
                onAddTask = { navController.navigate(AcademicDestinations.taskForm()) },
                onOpenTask = { taskId -> navController.navigate(AcademicDestinations.taskForm(taskId)) }
            )
        }
        composable(
            route = AcademicDestinations.TASK_FORM_ROUTE,
            arguments = listOf(
                navArgument(AcademicDestinations.TASK_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            TaskFormScreen(onDone = { navController.popBackStack() })
        }
    }
}

private fun NavGraphBuilder.financeGraph(navController: NavHostController) {
    navigation(startDestination = FinanceDestinations.HOME, route = TopLevelDestination.FINANCE.route) {
        composable(FinanceDestinations.HOME) {
            FinanceHomeScreen(
                onOpenAccount = { navController.navigate(FinanceDestinations.accountDetail(it)) },
                onAddAccount = { navController.navigate(FinanceDestinations.accountForm()) },
                onManageCategories = { navController.navigate(FinanceDestinations.CATEGORIES) },
                onOpenTransaction = { id, type ->
                    navController.navigate(FinanceDestinations.transactionForm(type.name, id))
                },
                onAddTransaction = { type ->
                    navController.navigate(FinanceDestinations.transactionForm(type.name))
                },
                onAddPayment = { navController.navigate(FinanceDestinations.paymentForm()) },
                onEditPayment = { id -> navController.navigate(FinanceDestinations.paymentForm(id)) }
            )
        }
        composable(
            route = FinanceDestinations.ACCOUNT_DETAIL_ROUTE,
            arguments = listOf(
                navArgument(FinanceDestinations.ACCOUNT_ID_ARG) { type = NavType.LongType }
            )
        ) {
            AccountDetailScreen(
                onEditAccount = { id -> navController.navigate(FinanceDestinations.accountForm(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = FinanceDestinations.ACCOUNT_FORM_ROUTE,
            arguments = listOf(
                navArgument(FinanceDestinations.ACCOUNT_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            AccountFormScreen(onDone = { navController.popBackStack() })
        }
        composable(FinanceDestinations.CATEGORIES) {
            CategoriesScreen(
                onAddCategory = { navController.navigate(FinanceDestinations.categoryForm()) },
                onEditCategory = { id -> navController.navigate(FinanceDestinations.categoryForm(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = FinanceDestinations.CATEGORY_FORM_ROUTE,
            arguments = listOf(
                navArgument(FinanceDestinations.CATEGORY_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            CategoryFormScreen(
                onDone = { navController.popBackStack() },
                onArchived = { navController.popBackStack() }
            )
        }
        composable(
            route = FinanceDestinations.TRANSACTION_FORM_ROUTE,
            arguments = listOf(
                navArgument(FinanceDestinations.TRANSACTION_TYPE_ARG) {
                    type = NavType.StringType
                },
                navArgument(FinanceDestinations.TRANSACTION_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            TransactionFormScreen(onDone = { navController.popBackStack() })
        }
        composable(
            route = FinanceDestinations.PAYMENT_FORM_ROUTE,
            arguments = listOf(
                navArgument(FinanceDestinations.PAYMENT_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            com.example.campuspocket.feature.finance.ui.payments.PaymentFormScreen(
                onDone = { navController.popBackStack() }
            )
        }
    }
}

private fun NavGraphBuilder.notesGraph(navController: NavHostController) {
    navigation(startDestination = "notes/list", route = TopLevelDestination.NOTES.route) {
        composable("notes/list") {
            NotesScreen(
                onOpenNote = { noteId: Long -> navController.navigate(NotesDestinations.editor(noteId)) },
                onNewNote = { navController.navigate(NotesDestinations.editor(null)) }
            )
        }
        composable(
            route = NotesDestinations.NOTE_EDITOR_ROUTE,
            arguments = listOf(navArgument(NotesDestinations.NOTE_ID_ARG) { type = NavType.LongType })
        ) {
            NoteEditorScreen(onDone = { navController.popBackStack() })
        }
    }
}

private fun NavGraphBuilder.settingsGraph() {
    navigation(startDestination = "settings/main", route = TopLevelDestination.SETTINGS.route) {
        composable("settings/main") {
            SettingsScreen()
        }
    }
}
