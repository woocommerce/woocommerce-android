package com.woocommerce.android.ui.analytics.hub.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
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
import com.woocommerce.android.ui.compose.component.AlertDialog
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.configuration.SelectionCheck

@Composable
fun AnalyticsHubSettingScreen(viewModel: AnalyticsHubSettingsViewModel) {
    BackHandler(onBack = viewModel::onBackPressed)
    viewModel.viewStateData.liveData.observeAsState().value?.let { state ->
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.manage_analytics)) },
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
                    AnalyticsHubSettingScreen(
                        cards = state.cardsConfiguration,
                        onSelectionChange = viewModel::onSelectionChange,
                        modifier = Modifier.padding(padding)
                    )

                    if (state.showDismissDialog) {
                        DiscardChangesDialog(
                            dismissButton = viewModel::onDismissDiscardChanges,
                            discardButton = viewModel::onDiscardChanges
                        )
                    }
                }

                is AnalyticsHubSettingsViewState.Loading -> LoadingCardsConfiguration()
            }
        }
    }
}

@Composable
fun AnalyticsHubSettingScreen(
    cards: List<AnalyticCardConfiguration>,
    onSelectionChange: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        itemsIndexed(items = cards, key = { _, card -> card.id }) { i, card ->
            AnalyticCardItem(
                showTopDivider = i == 0,
                id = card.id,
                title = card.title,
                isSelected = card.isVisible,
                onSelectionChange = onSelectionChange
            )
        }
    }
}

@Composable
fun AnalyticCardItem(
    id: Int,
    title: String,
    isSelected: Boolean,
    onSelectionChange: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showTopDivider: Boolean = false
) {
    Column {
        if (showTopDivider) Divider()
        Row(
            modifier = modifier
                .clickable { onSelectionChange(id, !isSelected) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionCheck(
                isSelected = isSelected,
                onSelectionChange = { onSelectionChange(id, !isSelected) }
            )
            Text(
                text = title,
                modifier
                    .weight(2f)
                    .padding(horizontal = 16.dp)
            )
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = stringResource(id = R.string.drag_handle)
            )
        }
        Divider()
    }
}

@Composable
fun DiscardChangesDialog(
    discardButton: () -> Unit,
    dismissButton: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        text = {
            Text(text = stringResource(id = R.string.discard_message))
        },
        confirmButton = {
            TextButton(onClick = dismissButton) {
                Text(stringResource(id = R.string.keep_editing).uppercase())
            }
        },
        dismissButton = {
            TextButton(onClick = discardButton) {
                Text(stringResource(id = R.string.discard).uppercase())
            }
        },
        neutralButton = {}
    )
}

@Composable
fun LoadingCardsConfiguration(modifier: Modifier = Modifier) {
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
    AnalyticsHubSettingScreen(
        listOf(
            AnalyticCardConfiguration(1, "Revenue", true),
            AnalyticCardConfiguration(2, "Orders", true),
            AnalyticCardConfiguration(3, "Stats", false)
        ),
        onSelectionChange = { _, _ -> }
    )
}

@Composable
@Preview
fun AnalyticCardItemPreview() {
    WooThemeWithBackground {
        AnalyticCardItem(id = 1, title = "Revenue", isSelected = true, onSelectionChange = { _, _ -> })
    }
}

@Composable
@Preview(name = "Screen", device = Devices.PIXEL_4)
fun LoadingCardsConfigurationPreview() {
    WooThemeWithBackground {
        LoadingCardsConfiguration()
    }
}
