package com.example.campuspocket.feature.academic.ui.courseform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.ContrastUtil
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.CampusButton
import com.example.campuspocket.core.designsystem.components.CampusChip
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.SecondaryPillButton
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
        onColorChange = viewModel::onColorChange,
        onAddSession = viewModel::addSession,
        onRemoveSession = viewModel::removeSession,
        onSessionChange = viewModel::updateSession,
        modifier = modifier
    )
}

@Composable
private fun CourseFormContent(
    state: CourseFormUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onCodeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onSectionChange: (String) -> Unit,
    onTeacherChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onAddSession: () -> Unit,
    onRemoveSession: (Int) -> Unit,
    onSessionChange: (Int, SessionFormState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        CampusTopBar(
            title = stringResource(
                if (state.isEditing) R.string.course_form_edit else R.string.course_form_new
            ),
            onBack = onBack,
            actions = {
                CampusButton(
                    text = stringResource(R.string.save),
                    onClick = onSave,
                    enabled = !state.isLoading && !state.isSaving && !state.saved
                )
            }
        )
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
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

            // Selector de color para la materia
            val paletteColors = listOf(
                0xFF4C8DFF.toInt(),
                0xFF22C55E.toInt(),
                0xFFEAB308.toInt(),
                0xFFEF4444.toInt(),
                0xFFA855F7.toInt(),
                0xFFEC4899.toInt(),
                0xFF06B6D4.toInt(),
                0xFFF97316.toInt()
            )
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = stringResource(R.string.course_color),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    paletteColors.forEach { colorArgb ->
                        val isSelected = state.colorArgb == colorArgb
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { onColorChange(colorArgb) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorArgb))
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = ContrastUtil.onColorFor(Color(colorArgb)),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

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

            SecondaryPillButton(
                text = stringResource(R.string.course_add_session),
                onClick = onAddSession,
                modifier = Modifier.fillMaxWidth()
            )
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
    CampusTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(labelRes),
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
    BentoCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                SchoolDays.allDays.forEach { day ->
                    CampusChip(
                        label = stringResource(day.shortLabelRes()),
                        selected = session.day == day,
                        onClick = { onChange(session.copy(day = day)) }
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
            CampusTextField(
                value = session.room,
                onValueChange = { onChange(session.copy(room = it)) },
                label = stringResource(R.string.session_room),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}