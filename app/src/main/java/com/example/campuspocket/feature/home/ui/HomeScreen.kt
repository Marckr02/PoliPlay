package com.example.campuspocket.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.ModuleCard
import com.example.campuspocket.core.designsystem.components.SectionHeader
import com.example.campuspocket.core.designsystem.components.descriptionRes
import com.example.campuspocket.core.designsystem.components.iconRes
import com.example.campuspocket.core.designsystem.components.titleRes
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.preferences.CampusModule
import com.example.campuspocket.core.preferences.UserPreferences

/**
 * Inicio: lanzador de los módulos activos. No muestra cifras que no provengan
 * de repositorios reales; su función es llevar rápido a cada módulo.
 */
@Composable
fun HomeScreen(
    preferences: UserPreferences,
    onOpenModule: (CampusModule) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeModules = CampusModule.entries.filter { preferences.isModuleEnabled(it) }
    val displayName = preferences.displayName.ifBlank { null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado: marca, saludo y acceso a ajustes.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.nav_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = displayName?.let { stringResource(R.string.home_greeting_name, it) }
                ?: stringResource(R.string.home_greeting_generic),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        SectionHeader(title = stringResource(R.string.home_title))

        if (activeModules.isEmpty()) {
            EmptyStateCard(
                title = stringResource(R.string.home_no_modules_title),
                message = stringResource(R.string.home_no_modules_message),
                actionText = stringResource(R.string.home_choose_modules),
                onAction = onOpenSettings
            )
        } else {
            // Cuadrícula adaptable: dos columnas en pantallas amplias, una en móviles.
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val twoColumns = maxWidth >= 600.dp
                val modules = activeModules
                if (twoColumns) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        modules.chunked(2).forEach { rowModules ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowModules.forEach { module ->
                                    HomeModuleEntry(
                                        module = module,
                                        onClick = { onOpenModule(module) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowModules.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        modules.forEach { module ->
                            HomeModuleEntry(
                                module = module,
                                onClick = { onOpenModule(module) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.home_open_settings))
        }
    }
}

@Composable
private fun HomeModuleEntry(
    module: CampusModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (container, onContainer) = when (module) {
        CampusModule.ACADEMIC -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        CampusModule.FINANCE -> MaterialTheme.extended.sage to MaterialTheme.extended.onSage
        CampusModule.NOTES -> MaterialTheme.extended.warm to MaterialTheme.extended.onWarm
    }
    ModuleCard(
        iconRes = module.iconRes(),
        title = stringResource(module.titleRes()),
        description = stringResource(module.descriptionRes()),
        onClick = onClick,
        modifier = modifier,
        accentContainer = container,
        onAccentContainer = onContainer
    )
}
