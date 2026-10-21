package com.example.campuspocket.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.DockDestination
import com.example.campuspocket.core.designsystem.components.FloatingDock
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.preferences.UserPreferences
import com.example.campuspocket.feature.academic.ui.AcademicDestinations
import com.example.campuspocket.feature.academic.ui.coursedetail.CourseDetailScreen
import com.example.campuspocket.feature.academic.ui.courseform.CourseFormScreen
import com.example.campuspocket.feature.academic.ui.courses.MateriasScreen
import com.example.campuspocket.feature.academic.ui.importschedule.ImportScheduleScreen
import com.example.campuspocket.feature.academic.ui.taskform.TaskFormScreen
import com.example.campuspocket.feature.academic.ui.tasks.TasksScreen
import com.example.campuspocket.feature.academic.ui.today.HoyRoute
import com.example.campuspocket.feature.academic.ui.week.SemanaScreen
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.ui.FinanceDestinations
import com.example.campuspocket.feature.finance.ui.accounts.AccountDetailScreen
import com.example.campuspocket.feature.finance.ui.accounts.AccountFormScreen
import com.example.campuspocket.feature.finance.ui.categories.CategoriesScreen
import com.example.campuspocket.feature.finance.ui.categories.CategoryFormScreen
import com.example.campuspocket.feature.finance.ui.home.FinanceHomeScreen
import com.example.campuspocket.feature.finance.ui.transactions.TransactionFormScreen
import com.example.campuspocket.feature.finance.ui.withdrawal.WithdrawalScreen
import com.example.campuspocket.feature.home.ui.HomeQuickAdd
import com.example.campuspocket.feature.home.ui.HomeRoute
import com.example.campuspocket.feature.notes.ui.NoteEditorScreen
import com.example.campuspocket.feature.notes.ui.NotesDestinations
import com.example.campuspocket.feature.notes.ui.NotesScreen
import com.example.campuspocket.feature.settings.ui.SettingsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Un solo NavHost; cada pestaña de la barra inferior es un grafo anidado
 * (navigation(route = destino)). Así la barra puede saber si la pestaña está
 * activa mirando la jerarquía del destino actual.
 *
 * La barra y los accesos se construyen a partir de los módulos activos
 * (UserPreferences); si un módulo se desactiva mientras está abierto, se
 * vuelve a Inicio sin borrar datos.
 */
@Composable
fun CampusNavHost(
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
    pendingTaskId: StateFlow<Long> = remember { MutableStateFlow(-1L) },
    onPendingTaskHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentTopLevel: TopLevelDestination? = currentDestination
        ?.hierarchy
        ?.mapNotNull { it.route }
        ?.firstNotNullOfOrNull { route ->
            TopLevelDestination.entries.firstOrNull { it.route == route }
        }

    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Deep link desde una notificación: abre el detalle (formulario) de la tarea.
    val pendingTask by pendingTaskId.collectAsStateWithLifecycle()
    LaunchedEffect(pendingTask) {
        if (pendingTask != -1L) {
            if (preferences.academicEnabled) {
                navController.navigate(AcademicDestinations.taskForm(pendingTask))
            }
            onPendingTaskHandled()
        }
    }

    // Si el módulo abierto se desactiva en Ajustes, volver a Inicio de forma segura.
    LaunchedEffect(currentTopLevel, preferences) {
        val module = currentTopLevel?.module
        if (module != null && !preferences.isModuleEnabled(module)) {
            navController.navigate(TopLevelDestination.HOME.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    var quickAddTarget by remember { mutableStateOf<TopLevelDestination?>(null) }
    val dockDestinations = TopLevelDestination.visibleFor(preferences).map { destination ->
        DockDestination(
            route = destination.route,
            label = stringResource(destination.labelRes),
            iconRes = destination.iconRes
        )
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            FloatingDock(
                destinations = dockDestinations,
                currentRoute = currentTopLevel?.route,
                onNavigate = { destination -> navigateToTopLevel(destination.route) },
                onAddClick = {
                    when (currentTopLevel) {
                        TopLevelDestination.HOME -> quickAddTarget = TopLevelDestination.HOME
                        TopLevelDestination.FINANCE -> quickAddTarget = TopLevelDestination.FINANCE
                        TopLevelDestination.ACADEMIC -> navController.navigate(AcademicDestinations.taskForm()) {
                            launchSingleTop = true
                        }
                        TopLevelDestination.NOTES -> navController.navigate(NotesDestinations.editor(null)) {
                            launchSingleTop = true
                        }
                        else -> Unit
                    }
                },
                addContentDescription = stringResource(R.string.dock_add),
                modifier = Modifier.navigationBarsPadding().padding(bottom = 4.dp)
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.start.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeRoute(
                    preferences = preferences,
                    onOpenModule = { module ->
                        val route = TopLevelDestination.entries
                            .first { it.module == module }
                            .route
                        navigateToTopLevel(route)
                    },
                    onOpenSettings = { navigateToTopLevel(TopLevelDestination.SETTINGS.route) },
                    onOpenCourse = { courseId ->
                        if (preferences.academicEnabled) {
                            navController.navigate(AcademicDestinations.courseDetail(courseId))
                        }
                    },
                    onOpenNotes = { navigateToTopLevel(TopLevelDestination.NOTES.route) },
                    onQuickAdd = { action ->
                        when (action) {
                            HomeQuickAdd.EXPENSE -> navController.navigate(
                                FinanceDestinations.transactionForm(TransactionType.EXPENSE.name)
                            )
                            HomeQuickAdd.TASK -> navController.navigate(AcademicDestinations.taskForm())
                            HomeQuickAdd.NOTE -> navController.navigate(NotesDestinations.editor(null))
                        }
                    }
                )
            }
            academicGraph(navController)
            financeGraph(navController)
            notesGraph(navController)
            settingsGraph(navController)
        }
    }

    quickAddTarget?.let { target ->
        QuickAddSheet(
            target = target,
            preferences = preferences,
            onDismiss = { quickAddTarget = null },
            onNavigate = { route ->
                quickAddTarget = null
                navController.navigate(route) { launchSingleTop = true }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(
    target: TopLevelDestination,
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val ext = MaterialTheme.extended
    val options = when (target) {
        TopLevelDestination.HOME -> buildList {
            if (preferences.financeEnabled) {
                add(
                    QuickAddOption(
                        labelRes = R.string.finance_expense,
                        descriptionRes = R.string.quick_add_expense_desc,
                        iconRes = R.drawable.ic_wallet,
                        containerColor = ext.peach,
                        contentColor = ext.onPeach,
                        onClick = {
                            onNavigate(
                                FinanceDestinations.transactionForm(TransactionType.EXPENSE.name)
                            )
                        }
                    )
                )
            }
            if (preferences.academicEnabled) {
                add(
                    QuickAddOption(
                        labelRes = R.string.task_new,
                        descriptionRes = R.string.quick_add_task_desc,
                        iconRes = R.drawable.ic_schedule,
                        containerColor = ext.lavender,
                        contentColor = ext.onLavender,
                        onClick = { onNavigate(AcademicDestinations.taskForm()) }
                    )
                )
            }
            if (preferences.notesEnabled) {
                add(
                    QuickAddOption(
                        labelRes = R.string.note_title,
                        descriptionRes = R.string.quick_add_note_desc,
                        iconRes = R.drawable.ic_notes,
                        containerColor = ext.limeSoft,
                        contentColor = ext.onLimeSoft,
                        onClick = { onNavigate(NotesDestinations.editor(null)) }
                    )
                )
            }
        }

        TopLevelDestination.FINANCE -> listOf(
            QuickAddOption(
                labelRes = R.string.finance_expense,
                descriptionRes = R.string.quick_add_expense_desc,
                iconRes = R.drawable.ic_wallet,
                containerColor = ext.peach,
                contentColor = ext.onPeach,
                onClick = {
                    onNavigate(FinanceDestinations.transactionForm(TransactionType.EXPENSE.name))
                }
            ),
            QuickAddOption(
                labelRes = R.string.finance_income,
                descriptionRes = R.string.quick_add_income_desc,
                iconRes = R.drawable.ic_wallet,
                containerColor = ext.mint,
                contentColor = ext.onMint,
                onClick = {
                    onNavigate(FinanceDestinations.transactionForm(TransactionType.INCOME.name))
                }
            ),
            QuickAddOption(
                labelRes = R.string.finance_quick_transfer,
                descriptionRes = R.string.quick_add_transfer_desc,
                iconRes = R.drawable.ic_wallet,
                containerColor = ext.sky,
                contentColor = ext.onSky,
                onClick = {
                    onNavigate(FinanceDestinations.transactionForm(TransactionType.TRANSFER.name))
                }
            ),
            QuickAddOption(
                labelRes = R.string.finance_withdrawal,
                descriptionRes = R.string.quick_add_withdrawal_desc,
                iconRes = R.drawable.ic_wallet,
                containerColor = ext.lavender,
                contentColor = ext.onLavender,
                onClick = { onNavigate(FinanceDestinations.WITHDRAWAL) }
            ),
            QuickAddOption(
                labelRes = R.string.payment_new,
                descriptionRes = R.string.quick_add_payment_desc,
                iconRes = R.drawable.ic_schedule,
                containerColor = ext.limeSoft,
                contentColor = ext.onLimeSoft,
                onClick = { onNavigate(FinanceDestinations.paymentForm()) }
            )
        )

        else -> emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.quick_add_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            option.onClick()
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(option.containerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(option.iconRes),
                            contentDescription = null,
                            tint = option.contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(option.labelRes),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(option.descriptionRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private data class QuickAddOption(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val containerColor: Color,
    val contentColor: Color,
    val onClick: () -> Unit
)

private fun NavGraphBuilder.academicGraph(navController: NavHostController) {
    navigation(startDestination = AcademicDestinations.HOY, route = TopLevelDestination.ACADEMIC.route) {
        composable(AcademicDestinations.HOY) {
            HoyRoute(
                onShowWeek = {
                    navController.navigate(AcademicDestinations.SEMANA) { launchSingleTop = true }
                },
                onNavigateToCourses = { navController.navigate(AcademicDestinations.MATERIAS) },
                onNavigateToTasks = { navController.navigate(AcademicDestinations.TASKS) },
                onOpenCourse = { courseId ->
                    navController.navigate(AcademicDestinations.courseDetail(courseId))
                },
                onOpenTask = { taskId ->
                    navController.navigate(AcademicDestinations.taskForm(taskId))
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
                onEditPayment = { id -> navController.navigate(FinanceDestinations.paymentForm(id)) },
                onWithdrawal = { navController.navigate(FinanceDestinations.WITHDRAWAL) },
                onOpenCalendar = { navController.navigate(FinanceDestinations.CALENDAR) }
            )
        }
        composable(FinanceDestinations.CALENDAR) {
            com.example.campuspocket.feature.finance.ui.calendar.CalendarScreen(
                onBack = { navController.popBackStack() },
                onDaySelected = { navController.popBackStack() }
            )
        }
        composable(FinanceDestinations.WITHDRAWAL) {
            WithdrawalScreen(
                onDone = { navController.popBackStack() },
                onCreateAccount = { navController.navigate(FinanceDestinations.accountForm()) }
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

private fun NavGraphBuilder.settingsGraph(navController: NavHostController) {
    navigation(startDestination = "settings/main", route = TopLevelDestination.SETTINGS.route) {
        composable("settings/main") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
