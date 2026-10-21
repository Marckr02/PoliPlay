package com.example.campuspocket.feature.onboarding.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.preferences.CampusModule
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.PrimaryPillButton
import com.example.campuspocket.core.designsystem.components.SecondaryPillButton
import com.example.campuspocket.core.designsystem.components.SelectableModuleCard
import com.example.campuspocket.core.designsystem.components.descriptionRes
import com.example.campuspocket.core.designsystem.components.iconRes
import com.example.campuspocket.core.designsystem.components.titleRes

/**
 * Onboarding de primer uso, dos pasos: nombre opcional y selección de módulos.
 * Se muestra una sola vez (marca `onboarding_completed` en DataStore).
 */
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.step > 0) { viewModel.goBackToName() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.extended.track),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(88.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.onboarding_step, state.step + 1, 2),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        when (state.step) {
            0 -> NameStep(
                name = state.name,
                onNameChange = viewModel::onNameChange,
                onContinue = viewModel::goToModules
            )
            else -> ModulesStep(
                state = state,
                onToggle = viewModel::toggleModule,
                onBack = viewModel::goBackToName,
                onFinish = viewModel::finish
            )
        }
    }
}

@Composable
private fun NameStep(
    name: String,
    onNameChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_title_name),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = stringResource(R.string.onboarding_message_name),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    CampusTextField(
        value = name,
        onValueChange = onNameChange,
        label = stringResource(R.string.onboarding_name_label),
        supportingText = { Text(stringResource(R.string.onboarding_name_support)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    PrimaryPillButton(
        text = stringResource(R.string.onboarding_continue),
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth()
    )
    SecondaryPillButton(
        text = stringResource(R.string.onboarding_later),
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ModulesStep(
    state: OnboardingUiState,
    onToggle: (CampusModule, Boolean) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_title_modules),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = stringResource(R.string.onboarding_message_modules),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    CampusModule.entries.forEach { module ->
        SelectableModuleCard(
            iconRes = module.iconRes(),
            title = stringResource(module.titleRes()),
            description = stringResource(module.descriptionRes()),
            selected = state.isSelected(module),
            onSelectedChange = { onToggle(module, it) },
            enabled = !state.saving
        )
    }
    Spacer(Modifier.height(4.dp))
    PrimaryPillButton(
        text = stringResource(R.string.onboarding_start),
        onClick = onFinish,
        enabled = !state.saving,
        loading = state.saving,
        modifier = Modifier.fillMaxWidth()
    )
    SecondaryPillButton(
        text = stringResource(R.string.back),
        onClick = onBack,
        enabled = !state.saving,
        modifier = Modifier.fillMaxWidth()
    )
}
