package com.example.campuspocket.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.LocalPrivacyMode
import com.example.campuspocket.core.designsystem.extended

/**
 * Ojo de privacidad (v5 §2.4): oculta o muestra los montos en Inicio y
 * Finanzas. Activo en color cream y con estado anunciado a TalkBack.
 */
@Composable
fun PrivacyToggleButton(
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hidden = LocalPrivacyMode.current
    val ext = MaterialTheme.extended
    RoundIconButton(
        iconRes = if (hidden) R.drawable.ic_visibility_off else R.drawable.ic_visibility,
        contentDescription = stringResource(
            if (hidden) R.string.privacy_show_amounts else R.string.privacy_hide_amounts
        ),
        stateDescription = stringResource(
            if (hidden) R.string.privacy_amounts_hidden else R.string.privacy_amounts_visible
        ),
        onClick = onToggle,
        modifier = modifier,
        size = 48.dp,
        containerColor = if (hidden) ext.cream else ext.track,
        contentColor = if (hidden) ext.onCream else MaterialTheme.colorScheme.onSurface,
        role = Role.Switch
    )
}
