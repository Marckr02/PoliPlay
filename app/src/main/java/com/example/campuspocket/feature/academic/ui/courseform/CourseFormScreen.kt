package com.example.campuspocket.feature.academic.ui.courseform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.feature.academic.domain.model.SchoolDays
import com.example.campuspocket.feature.academic.ui.TimeField
import com.example.campuspocket.feature.academic.ui.TimeText
import com.example.campuspocket.feature.academic.ui.shortLabelRes

@Composable
fun CourseFormScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    CourseFormContent(
        state = state,
        onBack = onDone,
        onSave = viewModel::save,
        onCodeChange = viewModel::onCodeChange,
        onNameChange = viewModel::onNameChange,
        onSectionChange = viewModel::onSectionChange,
        onTeacherChange = viewModel::onTeacherChange,
        onAddSession = viewModel::addSession,
        onRemoveSession = viewModel::removeSession,
        onSessionChange = viewModel::updateSession,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseFormContent(
    state: CourseFormUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onCodeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onSectionChange: (String) -> Unit,
    onTeacherChange: (String) -> Unit,
    onAddSession: () -> Unit,
    onRemoveSession: (Int) -> Unit,
    onSessionChange: (Int, SessionFormState) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.course_form_edit else R.string.course_form_new
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = !state.isLoading && !state.isSaving && !state.saved
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormField(
                value = state.code,
                onValueChange = onCodeChange,
                labelRes = R.string.course_code,
                errorRes = state.codeError
            )
            FormField(
                value = state.name,
                onValueChange = onNameChange,
                labelRes = R.string.course_name,
                errorRes = state.nameError
            )
            FormField(
                value = state.section,
                onValueChange = onSectionChange,
                labelRes = R.string.course_section,
                errorRes = state.sectionError
            )
            FormField(
                value = state.teacher,
                onValueChange = onTeacherChange,
                labelRes = R.string.course_teacher,
                errorRes = null
            )

            Text(
                text = stringResource(R.string.course_sessions_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
            state.sessionsError?.let { errorRes ->
                Text(
                    text = stringResource(errorRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            state.sessions.forEachIndexed { index, session ->
                SessionEditor(
                    session = session,
                    onChange = { onSessionChange(index, it) },
                    onRemove = { onRemoveSession(index) }
                )
            }

            TextButton(onClick = onAddSession) {
                Text(stringResource(R.string.course_add_session))
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    errorRes: Int?,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        isError = errorRes != null,
        supportingText = errorRes?.let { { Text(stringResource(it)) } },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionEditor(
    session: SessionFormState,
    onChange: (SessionFormState) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.session_day),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.delete)
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Días lectivos: lunes a sábado (aquí no hay clases los domingos).
                SchoolDays.withSaturday.forEach { day ->
                    FilterChip(
                        selected = session.day == day,
                        onClick = { onChange(session.copy(day = day)) },
                        label = { Text(stringResource(day.shortLabelRes())) }
                    )
                }
            }
            session.dayError?.let { errorRes ->
                Text(
                    text = stringResource(errorRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField(
                    labelRes = R.string.session_start,
                    value = session.start,
                    errorRes = session.startError,
                    onTimeSelected = { time -> onChange(session.copy(start = TimeText.format(time))) },
                    modifier = Modifier.weight(1f)
                )
                TimeField(
                    labelRes = R.string.session_end,
                    value = session.end,
                    errorRes = session.endError,
                    onTimeSelected = { time -> onChange(session.copy(end = TimeText.format(time))) },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = session.room,
                onValueChange = { onChange(session.copy(room = it)) },
                label = { Text(stringResource(R.string.session_room)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Campo de hora como botón con icono de reloj: toca y abre un TimePicker (formato 24 h),
 * en vez de un teclado donde no cabe el separador ":".
 */

