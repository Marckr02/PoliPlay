package com.example.campuspocket.feature.settings.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.BuildConfig
import com.example.campuspocket.R
import com.example.campuspocket.core.backup.BackupImportIssue
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.CampusSwitch
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.PillSegmentedControl
import com.example.campuspocket.core.designsystem.components.PrimaryPillButton
import com.example.campuspocket.core.designsystem.components.SecondaryPillButton
import com.example.campuspocket.core.designsystem.components.SettingsSectionCard
import com.example.campuspocket.core.designsystem.components.descriptionRes
import com.example.campuspocket.core.designsystem.components.iconRes
import com.example.campuspocket.core.designsystem.components.titleRes
import com.example.campuspocket.core.preferences.CampusModule
import com.example.campuspocket.core.preferences.ThemeMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lastBackupAt by viewModel.lastBackupAt.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemesterName.collectAsStateWithLifecycle()
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showExportWarning by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    var nameSaved by remember { mutableStateOf(false) }

    // Campo de perfil: se sincroniza con el valor guardado y con el borrado.
    var displayNameInput by remember { mutableStateOf(userPreferences.displayName) }
    LaunchedEffect(userPreferences.displayName) {
        displayNameInput = userPreferences.displayName
        nameSaved = false
    }

    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            viewModel.exportTo(uri)
            message = R.string.settings_export_success
        }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.prepareImport(uri)
    }

    LaunchedEffect(state.pendingImport, state.importError, state.importDone) {
        when {
            state.pendingImport != null -> showImportConfirm = true
            state.importError != null -> {
                message = when (state.importError) {
                    BackupImportIssue.WRONG_FORMAT_VERSION,
                    BackupImportIssue.WRONG_DB_VERSION_OLD,
                    BackupImportIssue.WRONG_DB_VERSION_NEW -> R.string.settings_import_unsupported
                    else -> R.string.settings_import_invalid
                }
            }
            state.importDone -> message = R.string.settings_import_success
            else -> {}
        }
    }

    // Confirmaciones: exportar avisa que el archivo no está cifrado; importar
    // reemplaza todos los datos, así que exige confirmación explícita.
    if (showExportWarning) {
        CampusDialog(
            onDismissRequest = { showExportWarning = false },
            title = stringResource(R.string.settings_export),
            message = stringResource(R.string.settings_export_not_encrypted),
            icon = Icons.Filled.Warning,
            confirmText = stringResource(R.string.action_continue),
            onConfirm = {
                showExportWarning = false
                createLauncher.launch(viewModel.suggestedFileName())
            }
        )
    }
    if (showImportConfirm) {
        CampusDialog(
            onDismissRequest = {
                showImportConfirm = false
                viewModel.dismissImport()
            },
            title = stringResource(R.string.settings_import),
            message = stringResource(R.string.settings_import_confirm),
            icon = Icons.Filled.Warning,
            destructive = true,
            confirmText = stringResource(R.string.action_continue),
            onConfirm = {
                showImportConfirm = false
                viewModel.confirmImport()
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        CampusTopBar(
            title = stringResource(R.string.nav_settings),
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Banner de respaldo
            if (viewModel.delegateShouldShowBanner(lastBackupAt, prefs.lastBackupBannerDismissedAt)) {
                BentoCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.settings_backup_reminder),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryPillButton(
                            text = stringResource(R.string.done),
                            onClick = { viewModel.dismissBackupBanner() },
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }
            }

        // ---- Mi perfil ----
        SettingsSectionCard(
            title = stringResource(R.string.settings_profile),
            subtitle = stringResource(R.string.settings_profile_local_note)
        ) {
            CampusTextField(
                value = displayNameInput,
                onValueChange = {
                    displayNameInput = it
                    nameSaved = false
                },
                label = stringResource(R.string.settings_profile_label),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryPillButton(
                    text = stringResource(R.string.save),
                    onClick = {
                        viewModel.setDisplayName(displayNameInput)
                        nameSaved = true
                    },
                    modifier = Modifier.weight(1f)
                )
                if (userPreferences.displayName.isNotBlank()) {
                    SecondaryPillButton(
                        text = stringResource(R.string.settings_profile_clear),
                        onClick = {
                            displayNameInput = ""
                            nameSaved = false
                            viewModel.clearDisplayName()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (nameSaved) {
                Text(
                    text = stringResource(R.string.settings_profile_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ---- Mis módulos ----
        SettingsSectionCard(
            title = stringResource(R.string.settings_modules),
            subtitle = stringResource(R.string.settings_modules_note)
        ) {
            CampusModule.entries.forEach { module ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(module.iconRes()),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(module.titleRes()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(module.descriptionRes()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CampusSwitch(
                        checked = userPreferences.isModuleEnabled(module),
                        onCheckedChange = { viewModel.setModuleEnabled(module, it) },
                        contentDescription = stringResource(module.titleRes())
                    )
                }
            }
        }

        // ---- Apariencia ----
        SettingsSectionCard(title = stringResource(R.string.settings_appearance)) {
            PillSegmentedControl(
                options = ThemeMode.entries,
                selected = userPreferences.themeMode,
                onSelect = viewModel::setThemeMode,
                label = { mode -> stringResource(themeLabelRes(mode)) }
            )
        }

        // ---- Respaldo ----
        SettingsSectionCard(title = stringResource(R.string.settings_backup)) {
            Text(
                text = lastBackupAt?.let {
                    DATE_FMT.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
                } ?: stringResource(R.string.never),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PrimaryPillButton(
                text = stringResource(R.string.settings_export),
                onClick = { showExportWarning = true },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            )
            SecondaryPillButton(
                text = stringResource(R.string.settings_import),
                onClick = { openLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/*")) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            )
            // Botón de debug solo en builds de depuración (el canal real es el aviso diario).
            if (BuildConfig.DEBUG) {
                SecondaryPillButton(
                    text = stringResource(R.string.settings_debug_review_now),
                    onClick = { viewModel.reviewNow(context); message = R.string.loading },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (state.busy) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            message?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Notificaciones ----
        SettingsSectionCard(title = stringResource(R.string.settings_notifications)) {
            val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.app.ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
            val exactAlarmsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
            } else true
            ToggleRow(
                label = stringResource(R.string.settings_notifications_system),
                checked = notifGranted,
                onCheckedChange = {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    })
                }
            )
            ToggleRow(
                label = stringResource(R.string.settings_exact_alarms),
                checked = exactAlarmsOk,
                onCheckedChange = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    })
                },
                detail = if (!exactAlarmsOk) stringResource(R.string.settings_exact_alarms_denied) else null
            )
            Spacer(Modifier.height(4.dp))
            ToggleRow(label = stringResource(R.string.settings_channel_tasks), checked = prefs.channelTasks, onCheckedChange = { viewModel.setChannelEnabled("tasks", it) })
            ToggleRow(label = stringResource(R.string.settings_channel_payments), checked = prefs.channelPayments, onCheckedChange = { viewModel.setChannelEnabled("payments", it) })
            ToggleRow(label = stringResource(R.string.settings_channel_budgets), checked = prefs.channelBudgets, onCheckedChange = { viewModel.setChannelEnabled("budgets", it) })
        }

        // ---- Semestre activo ----
        SettingsSectionCard(title = stringResource(R.string.settings_active_semester)) {
            Text(
                text = activeSemester ?: stringResource(R.string.never),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            semesters.forEach { sem ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CampusSwitch(
                        checked = sem.isActive,
                        onCheckedChange = { if (it) viewModel.setActiveSemester(sem.id) },
                        contentDescription = sem.name
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(sem.name)
                }
            }
        }

        // ---- Seguridad ----
        SettingsSectionCard(title = stringResource(R.string.settings_security)) {
            val canLock = viewModel.lockSupported(context)
            ToggleRow(
                label = stringResource(R.string.settings_biometric_lock),
                checked = prefs.lockEnabled,
                onCheckedChange = { if (it) viewModel.enableLock() else viewModel.disableLock() },
                enabled = canLock
            )
            if (!canLock) {
                Text(stringResource(R.string.settings_lock_unsupported), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if (prefs.lockEnabled) {
                Text(stringResource(R.string.settings_lock_timeout), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PillSegmentedControl(
                    options = listOf(
                        0 to R.string.settings_lock_immediately,
                        60 to R.string.settings_lock_1min,
                        300 to R.string.settings_lock_5min
                    ),
                    selected = listOf(
                        0 to R.string.settings_lock_immediately,
                        60 to R.string.settings_lock_1min,
                        300 to R.string.settings_lock_5min
                    ).first { it.first == prefs.lockTimeoutSeconds },
                    onSelect = { viewModel.setLockTimeout(it.first) },
                    label = { (_, labelRes) -> stringResource(labelRes) }
                )
            }
        }

        // ---- Acerca de ----
        SettingsSectionCard(title = stringResource(R.string.settings_about)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.settings_version) + ": " + BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.settings_last_backup) + ": " +
                    (lastBackupAt?.let { DATE_FMT.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())) } ?: stringResource(R.string.never)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    }
}

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es"))

private fun themeLabelRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    detail: String? = null
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            CampusSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                contentDescription = label,
                enabled = enabled
            )
        }
        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
