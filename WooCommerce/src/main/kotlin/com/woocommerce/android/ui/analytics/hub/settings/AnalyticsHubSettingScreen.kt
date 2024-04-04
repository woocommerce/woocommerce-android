package com.woocommerce.android.ui.analytics.hub.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.woocommerce.android.ui.compose.component.DragAndDropItem
import com.woocommerce.android.ui.compose.component.DragAndDropSelectableItemsList
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun AnalyticsHubSettingScreen(viewModel: AnalyticsHubSettingsViewModel) {
    BackHandler(onBack = viewModel::onBackPressed)
    viewModel.viewStateData.liveData.observeAsState().value?.let { state ->
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.customize_analytics)) },
                navigationIcon = {
                    IconButton(viewModel::onBackPressed) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                backgroundColor = colorResource(id = R.color.color_toolbar),
                actions = {
                    TextButton(
                        onClick = viewModel::onSaveChanges,
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
                    DragAndDropSelectableItemsList(
                        items = state.cardsConfiguration,
                        selectedItems = state.cardsConfiguration.filter { it.isVisible },
                        onSelectionChange = viewModel::onSelectionChange,
                        onOrderChange = viewModel::onOrderChange,
                        itemFormatter = { title },
                        itemKey = { _, card -> card.card },
                        modifier = Modifier.padding(padding)
                    )

                    if (state.showDiscardDialog) {
                        DiscardChangesDialog(
                            dismissButton = viewModel::onDismissDiscardChanges,
                            discardButton = viewModel::onDiscardChanges
                        )
                    }
                }

                is AnalyticsHubSettingsViewState.Loading -> LoadWidgetsConfiguration()
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
        AnalyticCardConfigurationUI(AnalyticsCards.Revenue, "Revenue", true),
        AnalyticCardConfigurationUI(AnalyticsCards.Orders, "Orders", true),
        AnalyticCardConfigurationUI(AnalyticsCards.Session, "Session", false)
    )
    DragAndDropSelectableItemsList(
        items = items,
        selectedItems = items.filter { it.isVisible },
        onSelectionChange = { _, _ -> },
        onOrderChange = { _, _ -> },
        itemFormatter = { title },
        itemKey = { _, card -> card.card }
    )
}

@Composable
@Preview
fun AnalyticCardItemPreview() {
    WooThemeWithBackground {
        DragAndDropItem(
            item = AnalyticCardConfigurationUI(AnalyticsCards.Revenue, "Revenue", true),
            title = "Revenue",
            isSelected = true,
            onSelectionChange = { _, _ -> }
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
