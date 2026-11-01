package com.example.campuspocket.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.campuspocket.core.designsystem.LocalMotion
import com.example.campuspocket.core.designsystem.MotionTokens
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
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
import com.example.campuspocket.core.designsystem.ShapeChip
import com.example.campuspocket.core.designsystem.ShapeControl
import com.example.campuspocket.core.designsystem.ShapeDock
import com.example.campuspocket.core.designsystem.ShapeRoundButton
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.designsystem.ShapeBadge
import com.example.campuspocket.core.designsystem.ShapeCard
import com.example.campuspocket.core.designsystem.ShapeTaskCheckbox
import com.example.campuspocket.feature.notes.data.NoteEntity

/** Entrada de la barra flotante; se mapea desde TopLevelDestination en navegación. */
data class DockDestination(
    val route: String,
    val label: String,
    @DrawableRes val outlineRes: Int,
    @DrawableRes val filledRes: Int
)

/**
 * Dock v8: superficie sólida con borde 1 dp `InkIndicator`, radio 18, alto 72 y
 * sombra suave. Cuatro celdas iguales con divisor `Line` entre ellas; la activa
 * usa icono relleno, etiqueta en peso 800 y un subrayado de 3 dp `InkIndicator`
 * bajo el icono (desviación obligatoria del prototipo, cuyo subrayado en `Accent`
 * da solo 1,55:1). La acción contextual vive fuera de esta barra.
 */
@Composable
fun FloatingDock(
    destinations: List<DockDestination>,
    currentRoute: String?,
    onNavigate: (DockDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val haptic = LocalHapticFeedback.current
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    // Tinte de la sombra suave del dock: no es contenido ni indicador.
    val shadowTint = ext.ink.copy(alpha = 0.12f)

    Row(
        modifier = modifier
            .testTag("floating_dock")
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .heightIn(min = 72.dp)
            .height(IntrinsicSize.Min)
            .shadow(
                elevation = 6.dp,
                shape = ShapeDock,
                clip = false,
                ambientColor = shadowTint,
                spotColor = shadowTint
            )
            .border(1.dp, ext.inkIndicator, ShapeDock)
            .clip(ShapeDock)
            .background(ext.dock)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        destinations.forEachIndexed { index, destination ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(lineColor)
                )
            }
            val selected = destination.route == currentRoute
            DockCell(
                destination = destination,
                selected = selected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigate(destination)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DockCell(
    destination: DockDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val motion = LocalMotion.current
    val contentColor by animateColorAsState(
        targetValue = if (selected) ext.inkContent else ext.inkMuted,
        animationSpec = tween(motion.ms(180)),
        label = "dockContent"
    )
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(ShapeChip)
            .semantics(mergeDescendants = true) { this.selected = selected }
            .combinedClickable(
                onClick = onClick,
                onLongClick = null,
                role = Role.Tab
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.width(42.dp).height(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = selected,
                animationSpec = tween(motion.ms(120)),
                label = "dockIcon"
            ) { active ->
                Icon(
                    painter = painterResource(
                        if (active) destination.filledRes else destination.outlineRes
                    ),
                    contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (selected) {
                // Subrayado de 3 dp bajo el icono, en `InkIndicator`.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(ext.inkIndicator)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Contenedor base del rediseño editorial v8: superficie sólida con borde fino,
 * radio 12 y sin sombra. El héroe (tinta) recibe un borde blanco al 14 %.
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = ShapeCard,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = MaterialTheme.extended
    val isHero = containerColor == ext.ink || containerColor == ext.heroContainer
    val resolvedBorder = borderColor ?: if (isHero) {
        ext.onInk.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val cardModifier = modifier.fillMaxWidth()
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            border = BorderStroke(1.dp, resolvedBorder),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = cardModifier,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            border = BorderStroke(1.dp, resolvedBorder),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/**
 * Botón de acción redondo v8: 44 dp visuales (radio 8) sobre área táctil de 48,
 * sin relleno y con borde `Line`; con contenedor tintado el borde desaparece.
 */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    role: Role = Role.Button,
    stateDescription: String? = null,
    enabled: Boolean = true
) {
    val borderColor = if (containerColor == MaterialTheme.extended.lime) {
        MaterialTheme.extended.ink
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
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
                .clip(ShapeRoundButton)
                .background(containerColor)
                .border(1.dp, borderColor, ShapeRoundButton),
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

/** Botón redondo que usa un drawable existente (iconos de la app). */
@Composable
fun RoundIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    role: Role = Role.Button,
    stateDescription: String? = null,
    enabled: Boolean = true
) {
    val borderColor = if (containerColor == MaterialTheme.extended.lime) {
        MaterialTheme.extended.ink
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
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
                .clip(ShapeRoundButton)
                .background(containerColor)
                .border(1.dp, borderColor, ShapeRoundButton),
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

/**
 * Chip de filtro v8: 40 dp visuales sobre área táctil de 48, radio 10 y borde
 * `ControlLine`. El activo usa `Lime` con borde `InkIndicator` y texto `OnLime`;
 * el check y el `stateDescription` evitan que el estado dependa solo del color.
 *
 * Con [onClick] nulo el chip es solo de estado (no accionable): sirve para
 * mostrar "Activo"/"Pausado" sin sumar un tercer control a una tarjeta.
 */
@Composable
fun CampusChip(
    label: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    role: Role = Role.Tab,
    semanticLabel: String? = null
) {
    val ext = MaterialTheme.extended
    val motion = LocalMotion.current
    val selectedState = stringResource(R.string.chip_selected_state)
    val unselectedState = stringResource(R.string.chip_unselected_state)
    val container by animateColorAsState(
        targetValue = if (selected) ext.lime else Color.Transparent,
        animationSpec = tween(motion.ms(150)),
        label = "chipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) ext.onLime else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(motion.ms(150)),
        label = "chipContent"
    )
    val border by animateColorAsState(
        targetValue = if (selected) ext.inkIndicator else ext.controlLine,
        animationSpec = tween(motion.ms(150)),
        label = "chipBorder"
    )
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(ShapeChip)
            .semantics {
                this.selected = selected
                stateDescription = if (selected) selectedState else unselectedState
                if (semanticLabel != null) contentDescription = semanticLabel
            }
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                        onLongClickLabel = onLongClickLabel,
                        role = role
                    )
                } else {
                    // Chip de solo estado: sin acción ni rol de control.
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .clip(ShapeChip)
                .background(container)
                .border(1.dp, border, ShapeChip)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                    tint = content,
                    modifier = Modifier.size(14.dp)
                )
            }
            leading?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = content,
                maxLines = 1
            )
        }
    }
}

/**
 * Selector de formulario v8: fila de chips de 40 dp (área táctil 48) con
 * borde `ControlLine`; el activo en `Lime` con borde `InkIndicator` y texto
 * `OnLime`. No es una pestaña de navegación: para eso existe [UnderlinedTabRow].
 */
@Deprecated("Replaced by CampusChip selector groups", ReplaceWith(""))
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
    // Con fuente grande los chips se desplazan para no cortar las etiquetas.
    val scrollable = options.size >= 3 && fontScale > 1.2f
    if (scrollable) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                FormSegment(
                    isSelected = option == selected,
                    label = label(option),
                    onSelect = { onSelect(option) },
                    modifier = Modifier.widthIn(min = 96.dp)
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                FormSegment(
                    isSelected = option == selected,
                    label = label(option),
                    onSelect = { onSelect(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FormSegment(
    isSelected: Boolean,
    label: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val motion = LocalMotion.current
    val container by animateColorAsState(
        targetValue = if (isSelected) ext.lime else Color.Transparent,
        animationSpec = tween(motion.ms(150)),
        label = "segmentContainer"
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) ext.onLime else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(motion.ms(150)),
        label = "segmentContent"
    )
    val border by animateColorAsState(
        targetValue = if (isSelected) ext.inkIndicator else ext.controlLine,
        animationSpec = tween(motion.ms(150)),
        label = "segmentBorder"
    )
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(ShapeChip)
            .selectable(
                selected = isSelected,
                onClick = onSelect,
                role = Role.RadioButton
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .clip(ShapeChip)
                .background(container)
                .border(1.dp, border, ShapeChip)
                .padding(horizontal = 10.dp, vertical = 8.dp),
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
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Pestañas subrayadas de navegación (v8 §6.4): 45 dp visuales con área táctil
 * de 48, fondo transparente, línea inferior `Line` y subrayado de 3 dp
 * `InkIndicator` en la activa. El texto activo usa `InkContent`. Usa `Role.Tab`
 * y `selectableGroup`.
 */
@Composable
fun <T> UnderlinedTabRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier
) {
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    val scrollable = options.size >= 3 && fontScale > 1.2f
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val rowModifier = Modifier
        .fillMaxWidth()
        .drawBehind {
            val stroke = 1.dp.toPx()
            drawLine(
                color = dividerColor,
                start = Offset(0f, size.height - stroke / 2f),
                end = Offset(size.width, size.height - stroke / 2f),
                strokeWidth = stroke
            )
        }
        .selectableGroup()
    if (scrollable) {
        Row(
            modifier = modifier
                .then(rowModifier)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEach { option ->
                UnderlinedTab(
                    isSelected = option == selected,
                    label = label(option),
                    onSelect = { onSelect(option) },
                    modifier = Modifier.widthIn(min = 96.dp)
                )
            }
        }
    } else {
        Row(modifier = modifier.then(rowModifier)) {
            options.forEach { option ->
                UnderlinedTab(
                    isSelected = option == selected,
                    label = label(option),
                    onSelect = { onSelect(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun UnderlinedTab(
    isSelected: Boolean,
    label: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val motion = LocalMotion.current
    val content by animateColorAsState(
        targetValue = if (isSelected) ext.inkContent else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(motion.ms(150)),
        label = "tabContent"
    )
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .selectable(
                selected = isSelected,
                onClick = onSelect,
                role = Role.Tab
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (isSelected) ext.inkIndicator else Color.Transparent)
        )
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
    val cellShape = ShapeTaskCheckbox
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeControl)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeControl)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEachIndexed { index, day ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(55.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            val selected = day.date == selectedDate
            val content = if (selected) ext.onInk else MaterialTheme.colorScheme.onSurface
            val muted = if (selected) ext.lime else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(63.dp)
                    .clip(cellShape)
                    // Relleno del día seleccionado (no es texto ni indicador):
                    // se queda `Ink` y el contenido usa `OnInk`.
                    .background(if (selected) ext.ink else Color.Transparent)
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
                    color = muted
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
                            .background(ext.lime)
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
            MaterialTheme.colorScheme.surface
        }
        val content = if (isCurrent) ext.onInk else MaterialTheme.colorScheme.onSurface
        val muted = if (isCurrent) ext.onInkMuted else MaterialTheme.colorScheme.onSurfaceVariant
        val cardShape = MaterialTheme.shapes.medium

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(cardShape)
                .background(container)
                .then(
                    if (isCurrent) {
                        Modifier
                    } else {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)
                    }
                )
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
                .clip(ShapeBadge)
                .background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconText.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = onIconColor,
                modifier = Modifier.clearAndSetSemantics {}
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
 * Tarjeta de nota pastel v8: Surface + borde Line 1 dp + **borde superior 5 dp** en el color de la nota
 * (lavanda #8D79AD, lima #9AAB3E, durazno #BF7D55, cielo #6692A0), alto mínimo 154 dp.
 * No usar fondos pastel completos.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PastelNoteCard(
    note: NoteEntity,
    noteColor: Color,
    onClick: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val ext = MaterialTheme.extended
    val topBorderColor = when {
        noteColor == ext.lavender -> ext.noteLavenderBorder
        noteColor == ext.limeSoft -> ext.noteLimeSoftBorder
        noteColor == ext.peach -> ext.notePeachBorder
        noteColor == ext.sky -> ext.noteSkyBorder
        else -> ext.inkIndicator
    }
    val isDark = ext.isDark
    val contentColor = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 154.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true },
                    role = Role.Button
                ),
            shape = ShapeCard,
            color = MaterialTheme.colorScheme.surface,
            contentColor = contentColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Borde superior 5 dp en el color de la nota
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(topBorderColor)
                )
                Column(
                    modifier = Modifier.padding(14.dp),
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
                                tint = ext.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (note.content.isNotBlank()) {
                        Text(
                            text = note.content.take(140).replace('\n', ' '),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.72f),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        CampusMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            CampusMenuItem(
                text = stringResource(
                    if (note.pinned) R.string.note_unpin else R.string.note_pin
                ),
                onClick = {
                    menuExpanded = false
                    onPinToggle(!note.pinned)
                }
            )
            CampusMenuItem(
                text = stringResource(R.string.note_move_to_folder),
                onClick = {
                    menuExpanded = false
                    onMove()
                }
            )
            CampusMenuDivider()
            CampusMenuItem(
                text = stringResource(R.string.delete),
                destructive = true,
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}

/** Buscador en píldora v8: radio 28, borde ControlLine, foco con anillo Lime + borde InkIndicator. */
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
    val focused by interaction.collectIsFocusedAsState()
    val ext = MaterialTheme.extended
    val borderColor = if (focused) ext.inkIndicator else ext.controlLine
    val boxColor = MaterialTheme.colorScheme.surface
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(ext.lime),
        interactionSource = interaction,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
            ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(PillShape)
                    .background(boxColor)
                    .border(1.dp, borderColor, PillShape)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null, // decorativo: el campo ya anuncia su función
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
        }
    )
}

/**
 * Campo de formulario v8 (§6.6): superficie `Surface`, borde `ControlLine` de
 * 1 dp, radio 7, alto mínimo 50 y etiqueta encima. Al enfocar se añade un anillo
 * `Lime` de 2 dp y el borde pasa a `InkIndicator`; en error el borde es `Danger`
 * y el mensaje se anuncia en una región viva junto a un icono.
 */
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
    errorDescription: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null
) {
    val ext = MaterialTheme.extended
    val shape = ShapeControl
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val errorColor = MaterialTheme.colorScheme.error
    val borderColor = if (isError) errorColor else ext.controlLine
    // FIX-30: la etiqueta viaja en semantics para que TalkBack la anuncie al enfocar.
    val a11yLabel = label.ifBlank { placeholder.orEmpty() }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 50.dp else 96.dp)
                .background(MaterialTheme.colorScheme.surface, shape)
                .drawWithCache {
                    val corner = 7.dp.toPx()
                    val stroke = 1.dp.toPx()
                    val ring = 2.dp.toPx()
                    onDrawWithContent {
                        drawContent()
                        if (focused && !isError) {
                            drawRoundRect(
                                color = ext.lime,
                                topLeft = Offset(ring / 2f, ring / 2f),
                                size = Size(size.width - ring, size.height - ring),
                                cornerRadius = CornerRadius(corner - ring / 2f),
                                style = Stroke(width = ring)
                            )
                            drawRoundRect(
                                color = ext.inkIndicator,
                                topLeft = Offset(ring, ring),
                                size = Size(
                                    (size.width - ring * 2f).coerceAtLeast(0f),
                                    (size.height - ring * 2f).coerceAtLeast(0f)
                                ),
                                cornerRadius = CornerRadius((corner - ring).coerceAtLeast(0f)),
                                style = Stroke(width = stroke)
                            )
                        } else {
                            drawRoundRect(
                                color = borderColor,
                                topLeft = Offset(stroke / 2f, stroke / 2f),
                                size = Size(size.width - stroke, size.height - stroke),
                                cornerRadius = CornerRadius(corner - stroke / 2f),
                                style = Stroke(width = stroke)
                            )
                        }
                    }
                }
                .semantics {
                    contentDescription = a11yLabel
                    if (isError) {
                        error(errorDescription ?: a11yLabel)
                    }
                },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
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
                        .padding(horizontal = 16.dp, vertical = if (singleLine) 14.dp else 12.dp),
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
        supportingText?.let { supporting ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { if (isError) liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isError) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null, // decorativo: el mensaje contiguo describe el error
                        tint = errorColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Box(Modifier.weight(1f)) { supporting() }
            }
        }
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
