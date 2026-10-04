package com.example.campuspocket.feature.academic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.campuspocket.R
import com.example.campuspocket.feature.academic.domain.model.ClassSession

/** Conmutador Hoy/Semana que va arriba de ambas pantallas. */
@Composable
fun TodayWeekSwitch(
    isTodaySelected: Boolean,
    onShowToday: () -> Unit,
    onShowWeek: () -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = isTodaySelected,
            onClick = onShowToday,
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) {
            Text(stringResource(R.string.academic_today))
        }
        SegmentedButton(
            selected = !isTodaySelected,
            onClick = onShowWeek,
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) {
            Text(stringResource(R.string.academic_week))
        }
    }
}

/** Una clase del horario: barra de color, rango de hora, nombre de la materia y aula. */
@Composable
fun SessionRow(
    courseName: String,
    courseColorArgb: Int,
    session: ClassSession,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .width(4.dp)
                .height(48.dp)
                .background(Color(courseColorArgb))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = courseName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(
                    R.string.session_time_range,
                    TimeText.format(session.startTime),
                    TimeText.format(session.endTime)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        session.room?.takeIf { it.isNotBlank() }?.let { room ->
            Text(
                text = room,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
