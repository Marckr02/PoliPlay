package com.example.campuspocket.feature.onboarding.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.preferences.CampusModule
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.PrimaryPillButton
import com.example.campuspocket.core.designsystem.components.SecondaryPillButton
import com.example.campuspocket.core.designsystem.components.SelectableModuleCard
import com.example.campuspocket.core.designsystem.components.descriptionRes
import com.example.campuspocket.core.designsystem.components.iconRes
import com.example.campuspocket.core.designsystem.components.titleRes

/**
 * Onboarding de primer uso, tres pasos: nombre opcional, selección de módulos y horario/avisos.
 * Se muestra una sola vez (marca `onboarding_completed` en DataStore).
 */
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val academicEnabled = state.academicSelected
    val totalSteps = state.totalSteps(academicEnabled)

    // Launchers para permisos y selección de PDF
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: androidx.activity.result.ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { viewModel.handlePdfResult(it) }
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            viewModel.setTaskReminders(false)
            viewModel.setPermissionErrorRes(R.string.onboarding_permission_denied)
        }
    }

    BackHandler(enabled = state.step > 0) {
        when (state.step) {
            1 -> viewModel.goBackToName()
            2 -> viewModel.goBackToModules()
            else -> {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Indicador de pasos dinámico
        StepIndicator(
            currentStep = state.step + 1,
            totalSteps = totalSteps
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.extended.track),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(88.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        when (state.step) {
            0 -> NameStep(
                name = state.name,
                onNameChange = viewModel::onNameChange,
                onContinue = viewModel::goToModules
            )
            1 -> ModulesStep(
                state = state,
                onToggle = viewModel::toggleModule,
                onBack = viewModel::goBackToName,
                onContinue = viewModel::goToSchedule
            )
            2 -> ScheduleStep(
                state = state,
                onImportClick = { viewModel.launchPdfImport(pdfLauncher) },
                onTaskRemindersChange = viewModel::setTaskReminders,
                onExactAlarmsChange = viewModel::setExactAlarms,
                onExactAlarmsClick = { viewModel.requestExactAlarmPermission(context) },
                onNotificationPermissionClick = { viewModel.requestNotificationPermission(notificationPermissionLauncher) },
                onBack = viewModel::goBackToModules,
                onFinish = viewModel::finish
            )
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        (1..totalSteps).forEach { step ->
            val isActive = step == currentStep
            Box(
                modifier = Modifier
                    .size(if (isActive) 28.dp else 8.dp, 8.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
            if (step < totalSteps) Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun NameStep(
    name: String,
    onNameChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_title_name),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = stringResource(R.string.onboarding_message_name),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    CampusTextField(
        value = name,
        onValueChange = onNameChange,
        label = stringResource(R.string.onboarding_name_label),
        supportingText = { Text(stringResource(R.string.onboarding_name_support)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    PrimaryPillButton(
        text = stringResource(R.string.onboarding_continue),
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth()
    )
    SecondaryPillButton(
        text = stringResource(R.string.onboarding_later),
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ModulesStep(
    state: OnboardingUiState,
    onToggle: (CampusModule, Boolean) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_title_modules),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = stringResource(R.string.onboarding_message_modules),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    CampusModule.entries.forEach { module ->
        SelectableModuleCard(
            iconRes = module.iconRes(),
            title = stringResource(module.titleRes()),
            description = stringResource(module.descriptionRes()),
            selected = state.isSelected(module),
            onSelectedChange = { onToggle(module, it) },
            enabled = !state.saving
        )
    }
    Spacer(Modifier.height(4.dp))
    PrimaryPillButton(
        text = stringResource(R.string.onboarding_continue),
        onClick = onContinue,
        enabled = !state.saving,
        loading = state.saving,
        modifier = Modifier.fillMaxWidth()
    )
    SecondaryPillButton(
        text = stringResource(R.string.back),
        onClick = onBack,
        enabled = !state.saving,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ScheduleStep(
    state: OnboardingUiState,
    onImportClick: () -> Unit,
    onTaskRemindersChange: (Boolean) -> Unit,
    onExactAlarmsChange: (Boolean) -> Unit,
    onExactAlarmsClick: () -> Unit,
    onNotificationPermissionClick: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_title_schedule),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = stringResource(R.string.onboarding_message_schedule),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))

    // Tarjeta importar horario
    val ext = MaterialTheme.extended
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = ext.cream
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ext.onCream.copy(alpha = 0.12f))
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_folder),
                        contentDescription = null,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(ext.onCream),
                        modifier = Modifier
                            .size(24.dp)
                            .padding(8.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_import_schedule),
                        style = MaterialTheme.typography.titleMedium,
                        color = ext.onCream
                    )
                    Text(
                        text = stringResource(R.string.onboarding_import_schedule_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.onCream.copy(alpha = 0.72f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            PrimaryPillButton(
                text = stringResource(R.string.onboarding_import_schedule),
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth()
            )
            SecondaryPillButton(
                text = stringResource(R.string.onboarding_import_later),
                onClick = { /* No-op, just skip */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    // Sección avisos
    Text(
        text = stringResource(R.string.onboarding_section_notices),
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))

    // Switch: Recordatorios de tareas
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_notice_task_reminders),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.onboarding_notice_task_reminders_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.taskRemindersEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        onNotificationPermissionClick()
                    } else {
                        onTaskRemindersChange(false)
                    }
                },
                enabled = !state.saving,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    }

    // Switch: Alarmas exactas
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_notice_exact_alarms),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.onboarding_notice_exact_alarms_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.exactAlarmsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        onExactAlarmsClick()
                    } else {
                        onExactAlarmsChange(false)
                    }
                },
                enabled = !state.saving,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    }

    // Error de permiso si aplica
    if (state.permissionErrorRes != 0) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(state.permissionErrorRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    Spacer(Modifier.height(16.dp))

    // Botones finales
    PrimaryPillButton(
        text = stringResource(R.string.onboarding_start),
        onClick = onFinish,
        enabled = !state.saving,
        loading = state.saving,
        modifier = Modifier.fillMaxWidth()
    )
    SecondaryPillButton(
        text = stringResource(R.string.back),
        onClick = onBack,
        enabled = !state.saving,
        modifier = Modifier.fillMaxWidth()
    )
}