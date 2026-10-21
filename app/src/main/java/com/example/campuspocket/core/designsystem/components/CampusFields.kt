package com.example.campuspocket.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.campuspocket.core.designsystem.extended

/**
 * Selector de formulario con el mismo aspecto que CampusTextField. El menú se
 * abre al tocar el campo y se cierra al elegir una opción.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusSelectField(
    value: String,
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        CampusTextField(
            value = value,
            onValueChange = {},
            label = label,
            placeholder = placeholder,
            readOnly = true,
            isError = isError,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            supportingText = supportingText?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            menuContent()
        }
    }
}

/** Elemento de menú para CampusSelectField, con altura táctil de 48 dp. */
@Composable
fun CampusSelectItem(
    text: String,
    onClick: () -> Unit,
    selected: Boolean = false
) {
    androidx.compose.material3.DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        onClick = onClick,
        trailingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.extended.selected
                )
            }
        } else {
            null
        }
    )
}

/**
 * Checkbox circular de 26 dp con área táctil de 48 dp. El check usa el color
 * de contenido de la selección para mantenerse visible en claro y oscuro.
 */
@Composable
fun CampusCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val ext = MaterialTheme.extended
    Box(
        modifier = modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription }
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .then(
                    if (checked) {
                        Modifier.background(ext.selected)
                    } else {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = ext.onSelected,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** Etiqueta de prioridad con texto y color; nunca solo color. */
@Composable
fun PriorityTag(
    priority: com.example.campuspocket.feature.academic.domain.model.Priority,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val (backgroundColor, textColor) = when (priority) {
        com.example.campuspocket.feature.academic.domain.model.Priority.HIGH ->
            Pair(ext.peach, ext.onPeach)
        com.example.campuspocket.feature.academic.domain.model.Priority.MEDIUM ->
            Pair(ext.lavender, ext.onLavender)
        com.example.campuspocket.feature.academic.domain.model.Priority.LOW ->
            Pair(ext.mint, ext.onMint)
    }
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(priority.resourceString),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
