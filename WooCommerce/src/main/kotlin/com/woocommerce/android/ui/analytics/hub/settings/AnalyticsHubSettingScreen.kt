package com.woocommerce.android.ui.analytics.hub.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.compose.DraggableItem
import com.woocommerce.android.ui.compose.component.AlertDialog
import com.woocommerce.android.ui.compose.dragContainerForDragHandle
import com.woocommerce.android.ui.compose.rememberDragDropState
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.configuration.SelectionCheck

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
                    AnalyticsHubSettingScreen(
                        cards = state.cardsConfiguration,
                        onSelectionChange = viewModel::onSelectionChange,
                        onOrderChange = viewModel::onOrderChange,
                        modifier = Modifier.padding(padding)
                    )

                    if (state.showDiscardDialog) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnalyticsHubSettingScreen(
    cards: List<AnalyticCardConfigurationUI>,
    onSelectionChange: (AnalyticsCards, Boolean) -> Unit,
    onOrderChange: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex -> onOrderChange(fromIndex, toIndex) }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        itemsIndexed(items = cards, key = { _, card -> card.card }) { i, card ->
            DraggableItem(dragDropState, i) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 4.dp else 1.dp, label = "card_elevation")
                val showDividers = when {
                    isDragging -> ShowDividers.None
                    i == 0 -> ShowDividers.All
                    else -> ShowDividers.Bottom
                }
                AnalyticCardItem(
                    showDividers = showDividers,
                    card = card.card,
                    title = card.title,
                    isSelected = card.isVisible,
                    isEnabled = card.isEnabled,
                    onSelectionChange = onSelectionChange,
                    elevation = elevation
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = stringResource(id = R.string.drag_handle),
                        modifier = Modifier
                            .dragContainerForDragHandle(
                                dragDropState = dragDropState,
                                key = card.card
                            )
                            .clickable(
                                onClick = {},
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                    )
                }
            }
        }
    }
}

enum class ShowDividers { Bottom, All, None }

@Composable
fun AnalyticCardItem(
    card: AnalyticsCards,
    title: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onSelectionChange: (AnalyticsCards, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDividers: ShowDividers = ShowDividers.Bottom,
    elevation: Dp = 1.dp,
    dragHandle: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier,
        elevation = elevation,
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            if (showDividers == ShowDividers.All) Divider()
            val rowModifier = if (isEnabled) {
                Modifier
                    .clickable { onSelectionChange(card, !isSelected) }
                    .padding(16.dp)
            } else {
                Modifier.padding(16.dp)
            }
            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionCheck(
                    isSelected = isSelected,
                    isEnabled = isEnabled,
                    onSelectionChange = { onSelectionChange(card, !isSelected) }
                )
                Text(
                    text = title,
                    modifier
                        .weight(2f)
                        .padding(horizontal = 16.dp)
                )
                dragHandle?.let { dragHandle -> dragHandle() }
            }
            if (showDividers != ShowDividers.None) Divider()
        }
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
            AnalyticCardConfigurationUI(AnalyticsCards.Revenue, "Revenue", true),
            AnalyticCardConfigurationUI(AnalyticsCards.Orders, "Orders", true),
            AnalyticCardConfigurationUI(AnalyticsCards.Session, "Session", false)
        ),
        onSelectionChange = { _, _ -> },
        onOrderChange = { _, _ -> }
    )
}

@Composable
@Preview
fun AnalyticCardItemPreview() {
    WooThemeWithBackground {
        AnalyticCardItem(
            card = AnalyticsCards.Revenue,
            title = "Revenue",
            isSelected = true,
            isEnabled = true,
            onSelectionChange = { _, _ -> }
        )
    }
}

@Composable
@Preview(name = "Screen", device = Devices.PIXEL_4)
fun LoadingCardsConfigurationPreview() {
    WooThemeWithBackground {
        LoadingCardsConfiguration()
    }
}
