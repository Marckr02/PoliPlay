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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.BuildConfig
import com.example.campuspocket.R
import com.example.campuspocket.core.backup.BackupImportIssue
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lastBackupAt by viewModel.lastBackupAt.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemesterName.collectAsStateWithLifecycle()
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showExportWarning by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }

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

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.titleLarge)

        // Banner de respaldo
        if (viewModel.delegateShouldShowBanner(lastBackupAt, prefs.lastBackupBannerDismissedAt)) {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_backup_reminder), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.dismissBackupBanner() }) { Text(stringResource(R.string.done)) }
                }
            }
        }

        // ---- Respaldo ----
        SettingsCard(title = stringResource(R.string.settings_backup)) {
            Text(
                text = lastBackupAt?.let {
                    DATE_FMT.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
                } ?: stringResource(R.string.never),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = { showExportWarning = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_export))
            }
            OutlinedButton(
                onClick = { openLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/*")) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_import)) }
            // Botón de debug solo en builds de depuración (Fase 7 compila fuera en release).
            if (BuildConfig.DEBUG) {
                TextButton(
                    onClick = { viewModel.reviewNow(context); message = R.string.loading },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Revisar ahora (debug)") }
            }
            if (state.busy) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        // ---- Notificaciones ----
        SettingsCard(title = stringResource(R.string.settings_notifications)) {
            val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.app.ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
            val exactAlarmsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
            } else true
            ToggleRow(
                label = stringResource(R.string.settings_notifications) + " (sistema)",
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
            HorizontalDivider()
            ToggleRow(label = "Tareas", checked = prefs.channelTasks, onCheckedChange = { viewModel.setChannelEnabled("tasks", it) })
            ToggleRow(label = "Pagos", checked = prefs.channelPayments, onCheckedChange = { viewModel.setChannelEnabled("payments", it) })
            ToggleRow(label = "Presupuestos", checked = prefs.channelBudgets, onCheckedChange = { viewModel.setChannelEnabled("budgets", it) })
        }

        // ---- Apariencia ----
        SettingsCard(title = stringResource(R.string.settings_theme)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(
                    "system" to R.string.settings_theme_system,
                    "light" to R.string.settings_theme_light,
                    "dark" to R.string.settings_theme_dark
                ).forEachIndexed { index, (value, labelRes) ->
                    SegmentedButton(
                        selected = prefs.theme == value,
                        onClick = { viewModel.setTheme(value) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                    ) { Text(stringResource(labelRes)) }
                }
            }
        }

        // ---- Semestre activo ----
        SettingsCard(title = stringResource(R.string.settings_active_semester)) {
            Text(activeSemester ?: stringResource(R.string.never), style = MaterialTheme.typography.bodyMedium)
            semesters.forEach { sem ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.setActiveSemester(sem.id) }
                ) {
                    Switch(checked = sem.isActive, onCheckedChange = null)
                    Text(sem.name, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        // ---- Seguridad ----
        SettingsCard(title = stringResource(R.string.settings_security)) {
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
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(
                        0 to R.string.settings_lock_immediately,
                        60 to R.string.settings_lock_1min,
                        300 to R.string.settings_lock_5min
                    ).forEachIndexed { index, (seconds, labelRes) ->
                        SegmentedButton(
                            selected = prefs.lockTimeoutSeconds == seconds,
                            onClick = { viewModel.setLockTimeout(seconds) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                        ) { Text(stringResource(labelRes)) }
                    }
                }
            }
        }

        // ---- Acerca de ----
        SettingsCard(title = stringResource(R.string.settings_about)) {
            Text("CampusPocket", style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_version) + ": " + BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = stringResource(R.string.settings_last_backup) + ": " +
                    (lastBackupAt?.let { DATE_FMT.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())) } ?: stringResource(R.string.never)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // ---- Diálogos ----
    if (showExportWarning) {
        AlertDialog(
            onDismissRequest = { showExportWarning = false },
            title = { Text(stringResource(R.string.settings_export)) },
            text = { Text(stringResource(R.string.settings_export_not_encrypted)) },
            confirmButton = {
                TextButton(onClick = { showExportWarning = false; createLauncher.launch(viewModel.suggestedFileName()) }) {
                    Text(stringResource(R.string.settings_export))
                }
            },
            dismissButton = { TextButton(onClick = { showExportWarning = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (showImportConfirm && state.pendingImport != null) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false; viewModel.dismissImport() },
            title = { Text(stringResource(R.string.settings_import)) },
            text = { Text(stringResource(R.string.settings_import_confirm)) },
            confirmButton = {
                TextButton(onClick = { showImportConfirm = false; viewModel.confirmImport() }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { showImportConfirm = false; viewModel.dismissImport() }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es"))

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
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
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}


