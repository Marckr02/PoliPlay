package com.example.campuspocket.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.campuspocket.core.designsystem.LocalMotion
import com.example.campuspocket.core.designsystem.MotionTokens
import com.example.campuspocket.core.designsystem.extended

/**
 * Interruptor propio de 52 × 32 dp con área táctil de 48 dp. En oscuro el
 * carril encendido se invierte (claro) para que el estado no dependa solo del
 * color.
 */
@Composable
fun CampusSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val ext = MaterialTheme.extended
    val motion = LocalMotion.current
    val track by animateColorAsState(
        targetValue = if (checked) ext.selected else ext.track,
        animationSpec = motion.tween(MotionTokens.Short),
        label = "switchTrack"
    )
    val thumb by animateColorAsState(
        targetValue = if (checked) ext.onSelected else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motion.tween(MotionTokens.Short),
        label = "switchThumb"
    )
    val offset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = motion.tween(MotionTokens.Short),
        label = "switchOffset"
    )
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 48.dp)
            .semantics { this.contentDescription = contentDescription }
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .size(width = 52.dp, height = 32.dp)
                .clip(CircleShape)
                .background(track)
                .padding(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = offset)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(thumb)
            )
        }
    }
}