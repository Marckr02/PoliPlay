package com.example.campuspocket.core.designsystem.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.campuspocket.core.designsystem.PillShape
import com.example.campuspocket.core.designsystem.extended

enum class CampusButtonStyle { PRIMARY, SECONDARY, DESTRUCTIVE }

/**
 * Botón único de la app. Primario en ámbar con texto verde profundo, secundario
 * en pista y destructivo en danger. Siempre de 54 dp de alto.
 */
@Composable
fun CampusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CampusButtonStyle = CampusButtonStyle.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val ext = MaterialTheme.extended
    val container = when (style) {
        CampusButtonStyle.PRIMARY -> ext.accent
        CampusButtonStyle.SECONDARY -> ext.track
        CampusButtonStyle.DESTRUCTIVE -> ext.danger
    }
    val content = when (style) {
        CampusButtonStyle.PRIMARY -> ext.onAccent
        CampusButtonStyle.SECONDARY -> MaterialTheme.colorScheme.onSurface
        CampusButtonStyle.DESTRUCTIVE -> ext.onDanger
    }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp),
        enabled = enabled && !loading,
        shape = PillShape,
        contentPadding = PaddingValues(horizontal = 24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.45f),
            disabledContentColor = content.copy(alpha = 0.6f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = content
            )
        } else {
            if (leadingIcon != null) {
                androidx.compose.material3.Icon(
                    imageVector = leadingIcon,
                    contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                    modifier = Modifier.size(18.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun CampusButtonRowScope(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CampusButtonStyle = CampusButtonStyle.PRIMARY
) {
    CampusButton(text = text, onClick = onClick, modifier = modifier, style = style)
}

/** Compatibilidad con las pantallas existentes. */
@Composable
fun CampusPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    CampusButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        style = CampusButtonStyle.PRIMARY,
        enabled = enabled,
        loading = loading
    )
}

/** Enlace de acción textual con área táctil mínima de 48 dp. */
@Composable
fun CampusTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
