package com.woocommerce.android.ui.analytics.hub.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.compose.component.DiscardChangesDialog
import com.woocommerce.android.ui.compose.component.DragAndDropItemsList
import com.woocommerce.android.ui.compose.component.DragAndDropSelectableItem
import com.woocommerce.android.ui.compose.rememberDragDropState
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun AnalyticsHubSettingScreen(viewModel: AnalyticsHubSettingsViewModel) {
    BackHandler(onBack = viewModel::onBackPressed)
    viewModel.viewStateData.liveData.observeAsState().value?.let { state ->
        AnalyticsHubSettingScreen(
            state = state,
            onBackPressed = viewModel::onBackPressed,
            onSaveChanges = viewModel::onSaveChanges,
            onOrderChange = viewModel::onOrderChange,
            onSelectionChange = viewModel::onSelectionChange,
            onDismissDiscardChanges = viewModel::onDismissDiscardChanges,
            onDiscardChanges = viewModel::onDiscardChanges,
            onExplorePlugin = viewModel::onExploreUrl
        )
    }
}

@Composable
fun AnalyticsHubSettingScreen(
    state: AnalyticsHubSettingsViewState,
    onBackPressed: () -> Unit,
    onSaveChanges: () -> Unit,
    onOrderChange: (Int, Int) -> Unit,
    onSelectionChange: (AnalyticCardConfigurationUI.SelectableCardConfigurationUI, Boolean) -> Unit,
    onDismissDiscardChanges: () -> Unit,
    onDiscardChanges: () -> Unit,
    onExplorePlugin: (String) -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(text = stringResource(id = R.string.customize_analytics)) },
            navigationIcon = {
                IconButton(onBackPressed) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.back)
                    )
                }
            },
            backgroundColor = colorResource(id = R.color.color_toolbar),
            actions = {
                TextButton(
                    onClick = onSaveChanges,
                    enabled = state is AnalyticsHubSettingsViewState.CardsConfiguration && state.isSaveButtonEnabled
                ) {
                    Text(
                        text = stringResource(id = R.string.save).uppercase()
                    )
                }
            },
        )
    }) { padding ->
        when (state) {
            is AnalyticsHubSettingsViewState.CardsConfiguration -> {
                DragAndDropItemsList(
                    items = state.cardsConfiguration,
                    onOrderChange = onOrderChange,
                    itemKey = { _, card -> card.card },
                    isItemDraggable = { it is AnalyticCardConfigurationUI.SelectableCardConfigurationUI },
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) { item, dragDropState ->
                    when (item) {
                        is AnalyticCardConfigurationUI.SelectableCardConfigurationUI -> {
                            DragAndDropSelectableItem(
                                item = item,
                                isSelected = item in state.cardsConfiguration.filter { it.isVisible },
                                dragDropState = dragDropState,
                                onSelectionChange = onSelectionChange,
                                itemKey = { it.card },
                                itemFormatter = { title },
                                isEnabled = item.isEnabled
                            )
                        }

                        is AnalyticCardConfigurationUI.ExploreCardConfigurationUI -> {
                            ExplorePluginItem(
                                item = item,
                                onExplorePlugin = onExplorePlugin
                            )
                        }
                    }
                }

                if (state.showDiscardDialog) {
                    DiscardChangesDialog(
                        dismissButton = onDismissDiscardChanges,
                        discardButton = onDiscardChanges
                    )
                }
            }

            is AnalyticsHubSettingsViewState.Loading -> LoadWidgetsConfiguration()
        }
    }
}

@Composable
private fun ExplorePluginItem(
    item: AnalyticCardConfigurationUI.ExploreCardConfigurationUI,
    onExplorePlugin: (url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExplorePlugin(item.url) }
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            item.title,
            Modifier
                .padding(start = 40.dp)
                .align(Alignment.CenterStart)
        )
        Card(
            elevation = 1.dp,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colors.primary)
                    .padding(vertical = 6.dp, horizontal = 12.dp)
            ) {
                Text("Explore!", color = MaterialTheme.colors.onPrimary)
            }
        }
    }
}

@Composable
fun LoadWidgetsConfiguration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colors.primary
        )
    }
}

@Composable
@Preview(name = "Screen", device = Devices.PIXEL_4)
fun AnalyticsHubSettingScreenPreview() {
    val items = listOf(
        AnalyticCardConfigurationUI.SelectableCardConfigurationUI(AnalyticsCards.Revenue, "Revenue", true),
        AnalyticCardConfigurationUI.SelectableCardConfigurationUI(AnalyticsCards.Orders, "Orders", true),
        AnalyticCardConfigurationUI.SelectableCardConfigurationUI(AnalyticsCards.Session, "Session", false),
        AnalyticCardConfigurationUI.ExploreCardConfigurationUI(AnalyticsCards.Bundles, "Bundles", "")
    )
    AnalyticsHubSettingScreen(
        state = AnalyticsHubSettingsViewState.CardsConfiguration(items, false),
        onBackPressed = {},
        onSaveChanges = {},
        onOrderChange = { _, _ -> },
        onSelectionChange = { _, _ -> },
        onDismissDiscardChanges = {},
        onDiscardChanges = {},
        onExplorePlugin = {}
    )
}

@Composable
@Preview
fun AnalyticCardItemPreview() {
    WooThemeWithBackground {
        DragAndDropSelectableItem(
            item = AnalyticCardConfigurationUI.SelectableCardConfigurationUI(AnalyticsCards.Revenue, "Revenue", true),
            isSelected = true,
            onSelectionChange = { _, _ -> },
            dragDropState = rememberDragDropState(lazyListState = rememberLazyListState()) { _, _ -> },
            itemKey = { }
        )
    }
}

@Composable
@Preview(name = "Screen", device = Devices.PIXEL_4)
fun LoadingCardsConfigurationPreview() {
    WooThemeWithBackground {
        LoadWidgetsConfiguration()
    }
}
