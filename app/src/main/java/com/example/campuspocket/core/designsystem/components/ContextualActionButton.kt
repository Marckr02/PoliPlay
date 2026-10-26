package com.example.campuspocket.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.campuspocket.core.designsystem.LocalMotion
import com.example.campuspocket.core.designsystem.MotionTokens
import com.example.campuspocket.core.designsystem.PillShape
import com.example.campuspocket.core.designsystem.extended

/**
 * Acción contextual de la pestaña raíz visible. [labelRes] null pinta el botón
 * como círculo (Inicio); si no, se extiende con icono + etiqueta.
 */
data class DockAction(
    @StringRes val labelRes: Int?,
    @DrawableRes val iconRes: Int,
    @StringRes val descriptionRes: Int,
    val onClick: () -> Unit
)

/**
 * Botón flotante de acción contextual de v4. Se extiende al entrar o al subir
 * el scroll y se colapsa al bajarlo; Inicio siempre es un círculo.
 */
@Composable
fun ContextualActionButton(
    action: DockAction,
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val fontScale = LocalDensity.current.fontScale
    val motion = LocalMotion.current
    val showLabel = expanded && action.labelRes != null && fontScale <= 1.5f
    val duration = motion.ms(180)
    val collapsedDescription = stringResource(action.descriptionRes)

    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(PillShape)
            .background(ext.accent)
            .animateContentSize(tween(duration))
            .combinedClickable(
                onClick = action.onClick,
                onLongClick = null,
                role = Role.Button
            )
            .semantics {
                if (!showLabel) {
                    contentDescription = collapsedDescription
                }
            }
            .padding(start = 16.dp, end = if (showLabel) 20.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = action.iconRes,
            transitionSpec = {
                fadeIn(tween(motion.ms(160))) togetherWith
                    fadeOut(tween(motion.ms(100)))
            },
            label = "contextualActionIcon"
        ) { iconRes ->
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                tint = ext.onAccent,
                modifier = Modifier.size(24.dp)
            )
        }
        AnimatedVisibility(
            visible = showLabel,
            enter = fadeIn(tween(motion.ms(160))) +
                expandHorizontally(tween(motion.ms(160))),
            exit = fadeOut(tween(motion.ms(100))) +
                shrinkHorizontally(tween(motion.ms(100)))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(action.labelRes ?: action.descriptionRes),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                    ),
                    color = ext.onAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}