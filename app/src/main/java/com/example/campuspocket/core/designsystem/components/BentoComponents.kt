package com.example.campuspocket.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusNumeralSmall
import com.example.campuspocket.core.designsystem.CampusOverline
import com.example.campuspocket.core.designsystem.PillShape
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.feature.notes.data.NoteEntity

/** Entrada de la barra flotante; se mapea desde TopLevelDestination en navegación. */
data class DockDestination(
    val route: String,
    val label: String,
    @DrawableRes val outlineRes: Int,
    @DrawableRes val filledRes: Int
)

/**
 * Barra flotante v4: cuatro celdas iguales con icono sobre etiqueta siempre
 * visible. El activo usa icono relleno e indicador honey; los inactivos,
 * contorno en inkMuted. La acción contextual vive fuera de esta barra.
 */
@Composable
fun FloatingDock(
    destinations: List<DockDestination>,
    currentRoute: String?,
    onNavigate: (DockDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val dockShape = PillShape
    val elevationModifier = if (isDark) {
        Modifier.border(1.dp, ext.inkLine, dockShape)
    } else {
        Modifier.shadow(
            elevation = 8.dp,
            shape = dockShape,
            clip = false,
            ambientColor = Color(0x4704252E),
            spotColor = Color(0x4704252E)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 68.dp)
            .then(elevationModifier)
            .clip(dockShape)
            .background(ext.dock)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        destinations.forEach { destination ->
            val selected = destination.route == currentRoute
            val indicatorColor by animateColorAsState(
                targetValue = if (selected) ext.lime else Color.Transparent,
                animationSpec = tween(180),
                label = "dockIndicator"
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .semantics(mergeDescendants = true) { this.selected = selected }
                    .combinedClickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigate(destination)
                        },
                        onLongClick = null,
                        role = Role.Tab
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(indicatorColor),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = selected,
                        animationSpec = tween(120),
                        label = "dockIcon"
                    ) { active ->
                        Icon(
                            painter = painterResource(
                                if (active) destination.filledRes else destination.outlineRes
                            ),
                            contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                            tint = if (active) ext.onLime else ext.inkMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = destination.label,
                    style = CampusOverline.copy(
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                    ),
                    color = if (selected) ext.onDock else ext.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Contenedor base del rediseño: color y radio configurables, sin borde ni
 * elevación. Toda la jerarquía viene del color y del tamaño.
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier.fillMaxWidth()
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Botón circular de 44 dp (acciones de cabecera) o 56 dp (acciones rápidas). */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    role: Role = Role.Button,
    stateDescription: String? = null,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics {
                this.contentDescription = contentDescription
                if (stateDescription != null) this.stateDescription = stateDescription
                if (!enabled) disabled()
            }
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = null,
                role = role
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                tint = contentColor,
                modifier = Modifier.size(if (size >= 52.dp) 24.dp else 22.dp)
            )
        }
    }
}

/** Botón circular que usa un drawable existente (iconos de la app). */
@Composable
fun RoundIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    role: Role = Role.Button,
    stateDescription: String? = null,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics {
                this.contentDescription = contentDescription
                if (stateDescription != null) this.stateDescription = stateDescription
                if (!enabled) disabled()
            }
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = null,
                role = role
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                tint = contentColor,
                modifier = Modifier.size(if (size >= 52.dp) 24.dp else 22.dp)
            )
        }
    }
}

/** Chip/píldora del rediseño. Seleccionado en tinta, inactivo en pista. */
@Composable
fun CampusChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    role: Role = Role.Tab,
    semanticLabel: String? = null
) {
    val ext = MaterialTheme.extended
    val container by animateColorAsState(
        targetValue = if (selected) ext.selected else ext.track,
        animationSpec = tween(150),
        label = "chipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) ext.onSelected else ext.onTrack,
        animationSpec = tween(150),
        label = "chipContent"
    )
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(PillShape)
            .background(container)
            .semantics {
                this.selected = selected
                if (semanticLabel != null) contentDescription = semanticLabel
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel,
                role = role
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                tint = content,
                modifier = Modifier.size(16.dp)
            )
        }
        leading?.invoke()
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1
        )
    }
}

/** Pista de segmentos tipo píldora para pestañas y filtros. */
@Composable
fun <T> PillSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    // Keep all labels whole when the user enlarges text; at default scale the
    // segments stay evenly distributed across the track.
    val scrollable = options.size >= 3 && fontScale > 1.2f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(PillShape)
            .background(ext.track)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        if (scrollable) {
            val listState = rememberLazyListState()
            LaunchedEffect(selected) {
                val index = options.indexOf(selected)
                if (index >= 0) listState.animateScrollToItem(index)
            }
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                itemsIndexed(options) { _, option ->
                    PillSegmentOption(
                        option = option,
                        isSelected = option == selected,
                        label = label,
                        onSelect = onSelect,
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentPadding = 12.dp
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                options.forEach { option ->
                    PillSegmentOption(
                        option = option,
                        isSelected = option == selected,
                        label = label,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        contentPadding = 0.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> PillSegmentOption(
    option: T,
    isSelected: Boolean,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier,
    contentPadding: Dp
) {
    val ext = MaterialTheme.extended
    val container by animateColorAsState(
        targetValue = if (isSelected) ext.selected else Color.Transparent,
        animationSpec = tween(150),
        label = "segmentContainer"
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) ext.onSelected else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150),
        label = "segmentContent"
    )
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(container)
            .semantics { this.selected = isSelected }
            .combinedClickable(
                onClick = { onSelect(option) },
                onLongClick = null,
                role = Role.Tab
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                    tint = content,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label(option),
                style = MaterialTheme.typography.labelLarge,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Fila de 7 días (L M X J V S D) con el día seleccionado en tinta. */
@Composable
fun DayStrip(
    days: List<DayStripDay>,
    selectedDate: java.time.LocalDate,
    onSelect: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEach { day ->
            val selected = day.date == selectedDate
            val container = if (selected) ext.selected else ext.track
            val content = if (selected) ext.onSelected else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .height(66.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(23.dp))
                    .background(container)
                    .semantics { this.selected = selected }
                    .combinedClickable(
                        onClick = { onSelect(day.date) },
                        onLongClick = null,
                        role = Role.Tab
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(day.letterRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = content
                )
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = content
                )
                if (selected) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(ext.accent)
                    )
                }
            }
        }
    }
}

/** Datos mínimos para pintar un día del DayStrip. */
data class DayStripDay(
    val date: java.time.LocalDate,
    @StringRes val letterRes: Int,
    val dayOfMonth: Int
)

/**
 * Fila de línea de tiempo: hora a la izquierda, tarjeta de clase a la derecha.
 * La clase actual usa tinta y barra lima; las demás se tiñen con el color de la materia.
 */
@Composable
fun TimelineRow(
    startTime: String,
    endTime: String,
    courseName: String,
    room: String?,
    courseColor: Color,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    trailingLabel: String? = null
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.width(44.dp).padding(top = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = startTime,
                style = MaterialTheme.typography.labelLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = endTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val container = if (isCurrent) {
            ext.ink
        } else {
            courseColor.copy(alpha = 0.14f).compositeOver(MaterialTheme.colorScheme.surface)
        }
        val content = if (isCurrent) ext.onInk else MaterialTheme.colorScheme.onSurface
        val muted = if (isCurrent) ext.onInkMuted else MaterialTheme.colorScheme.onSurfaceVariant

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(container)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = null,
                    role = Role.Button
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCurrent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(ext.lime))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.timeline_now),
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.lime
                        )
                    }
                } else {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(courseColor))
                }
                trailingLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = muted
                    )
                }
            }
            Text(
                text = courseName,
                style = MaterialTheme.typography.titleMedium,
                color = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(room?.takeIf { it.isNotBlank() }, trailingLabel).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (progress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(PillShape)
                        .background(if (isCurrent) ext.onInk.copy(alpha = 0.2f) else ext.track)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(PillShape)
                            .background(if (isCurrent) ext.lime else courseColor)
                    )
                }
            }
        }
    }
}

/** Tarjeta de cuenta 230 × 104 dp, alternando tinta y acento. */
@Composable
fun WalletCard(
    name: String,
    typeLabel: String,
    amount: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(230.dp).heightIn(min = 104.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Column {
                Text(
                    text = amount,
                    style = CampusNumeralSmall,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/** Fila de movimiento: icono tintado, nombre, metadatos e importe a la derecha. */
@Composable
fun TransactionRow(
    title: String,
    meta: String,
    amount: String,
    iconText: String,
    iconColor: Color,
    onIconColor: Color,
    amountColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = null,
                role = Role.Button
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconText.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = onIconColor
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall,
            color = amountColor,
            maxLines = 1
        )
    }
}

/**
 * Tarjeta de nota pastel. Las acciones (fijar, mover, borrar) viven en un menú
 * contextual de pulsación larga, no en una fila de iconos.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PastelNoteCard(
    note: NoteEntity,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true },
                    role = Role.Button
                ),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = note.title.ifBlank { stringResource(R.string.notes_empty_title) },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = contentColor,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.pinned) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = stringResource(R.string.note_pinned),
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content.take(140).replace('\n', ' '),
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.78f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (note.pinned) R.string.note_unpin else R.string.note_pin
                        )
                    )
                },
                onClick = {
                    menuExpanded = false
                    onPinToggle(!note.pinned)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.note_move_to_folder)) },
                onClick = {
                    menuExpanded = false
                    onMove()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}

/** Buscador en píldora blanca, sin label flotante ni contorno. */
@Composable
fun SearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val interaction = remember { MutableInteractionSource() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interaction,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
            ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    )
}

/** Campo de formulario: fondo pista, radio 16 y sin línea. */
@Composable
fun CampusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    prefix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    readOnly: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null
) {
    val ext = MaterialTheme.extended
    val shape = MaterialTheme.shapes.small
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 52.dp else 96.dp)
                .clip(shape)
                .background(ext.track)
                .then(
                    if (isError) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.error, shape)
                    } else {
                        Modifier
                    }
                ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else minLines.coerceAtLeast(2),
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = interaction,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
                ) {
                    prefix?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                    trailingIcon?.invoke()
                }
            }
        )
        supportingText?.invoke()
    }
}

/** Botón primario en píldora de 52 dp. */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val ext = MaterialTheme.extended
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = PillShape,
        contentPadding = PaddingValues(horizontal = 24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ext.accent,
            contentColor = ext.onAccent,
            disabledContainerColor = ext.accent.copy(alpha = 0.45f),
            disabledContentColor = ext.onAccent.copy(alpha = 0.6f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = ext.onAccent
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Botón secundario en píldora: relleno suave, sin borde. */
@Composable
fun SecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val ext = MaterialTheme.extended
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = PillShape,
        contentPadding = PaddingValues(horizontal = 24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ext.track,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = ext.track.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Botón destructivo en píldora, sin borde. */
@Composable
fun DangerPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val ext = MaterialTheme.extended
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = PillShape,
        contentPadding = PaddingValues(horizontal = 24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ext.danger,
            contentColor = ext.onDanger
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Cabecera de sección con acción opcional a la derecha. */
@Composable
fun CampusSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(PillShape)
                    .combinedClickable(
                        onClick = onAction,
                        onLongClick = null,
                        role = Role.Button
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            )
        }
    }
}
