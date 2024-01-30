package com.woocommerce.android.ui.blaze.creation.targets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetSelectionViewModel.TargetItem
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetSelectionViewModel.ViewState
import com.woocommerce.android.ui.compose.component.MultiSelectAllItemsButton
import com.woocommerce.android.ui.compose.component.MultiSelectList
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews

@Composable
fun BlazeCampaignTargetSelectionScreen(viewModel: BlazeCampaignTargetSelectionViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        TargetSelectionScreen(
            state = state,
            onBackPressed = viewModel::onBackPressed,
            onSaveTapped = viewModel::onSaveTapped,
            onItemTapped = viewModel::onItemTapped,
            onAllButtonTapped = viewModel::onAllButtonTapped
        )
    }
}

@Composable
private fun TargetSelectionScreen(
    state: ViewState,
    onBackPressed: () -> Unit,
    onSaveTapped: () -> Unit,
    onItemTapped: (TargetItem) -> Unit,
    onAllButtonTapped: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = state.title,
                onNavigationButtonClick = onBackPressed,
                navigationIcon = Filled.ArrowBack,
                actions = {
                    TextButton(onClick = onSaveTapped, enabled = state.selectedItems.isNotEmpty()) {
                        Text(stringResource(id = string.save).uppercase())
                    }
                }
            )
        },
        modifier = Modifier.background(MaterialTheme.colors.surface)
    ) { paddingValues ->
        MultiSelectList(
            items = state.items,
            selectedItems = state.selectedItems,
            itemFormatter = { value },
            onItemToggled = onItemTapped,
            allItemsButton = MultiSelectAllItemsButton(
                text = stringResource(id = string.blaze_campaign_preview_target_default_value),
                onClicked = onAllButtonTapped
            ),
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
        )
    }
}

@LightDarkThemePreviews
@Composable
fun PreviewTargetSelectionScreen() {
    TargetSelectionScreen(
        state = ViewState(
            items = listOf(
                TargetItem("1", "Item 1"),
                TargetItem("2", "Item 2"),
                TargetItem("3", "Item 3"),
                TargetItem("4", "Item 4"),
                TargetItem("5", "Item 5"),
                TargetItem("6", "Item 6"),
                TargetItem("7", "Item 7"),
                TargetItem("8", "Item 8"),
                TargetItem("9", "Item 9")
            ),
            selectedItems = listOf(
                TargetItem("4", "Item 4"),
                TargetItem("5", "Item 5"),
                TargetItem("8", "Item 8"),
                TargetItem("9", "Item 9")
            ),
            title = "Title"
        ),
        onBackPressed = { /*TODO*/ },
        onSaveTapped = { /*TODO*/ },
        onItemTapped = {},
        onAllButtonTapped = {}
    )
}
