package com.example.campuspocket.feature.finance.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.ContrastUtil
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.feature.finance.domain.model.Category

@Composable
fun CategoriesScreen(
    onAddCategory: () -> Unit,
    onEditCategory: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ext = MaterialTheme.extended

    Column(modifier = modifier.fillMaxSize()) {
        CampusTopBar(
            title = stringResource(R.string.categories_manage),
            onBack = onBack,
            actions = {
                RoundIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.category_new),
                    onClick = onAddCategory,
                    containerColor = ext.accent,
                    contentColor = ext.onAccent
                )
            }
        )

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (state.expense.isNotEmpty()) {
                item(key = "header_expense") {
                    Text(
                        text = stringResource(R.string.category_kind_expense),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(state.expense, key = { it.id ?: 0L }) { category ->
                    CategoryRow(category, onEditCategory)
                }
            }
            if (state.income.isNotEmpty()) {
                item(key = "header_income") {
                    Text(
                        text = stringResource(R.string.category_kind_income),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(state.income, key = { it.id ?: 0L }) { category ->
                    CategoryRow(category, onEditCategory)
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onEditCategory: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val background = Color(category.colorArgb)
    val editLabel = stringResource(R.string.edit)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClickLabel = editLabel) {
                category.id?.let(onEditCategory)
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.name.first().uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = ContrastUtil.onColorFor(background)
            )
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
