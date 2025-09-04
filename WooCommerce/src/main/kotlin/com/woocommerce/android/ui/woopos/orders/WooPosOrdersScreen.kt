package com.woocommerce.android.ui.woopos.orders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosOrdersScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    val viewModel: WooPosOrdersViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    val onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) }
    BackHandler { onNavigationEvent(WooPosNavigationEvent.GoBack) }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            WooPosToolbar(
                titleText = stringResource(R.string.woopos_orders_title),
                onBackClicked = onBackClicked,
            )

            when (val s = state) {
                is WooPosOrdersState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WooPosText(
                            text = stringResource(R.string.loading),
                            style = WooPosTypography.BodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(WooPosSpacing.Large.value)
                        )
                    }
                }

                is WooPosOrdersState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WooPosText(
                            text = s.message,
                            style = WooPosTypography.BodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(WooPosSpacing.Large.value)
                        )
                    }
                }

                is WooPosOrdersState.Empty -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WooPosText(
                            text = "No Orders Found",
                            style = WooPosTypography.BodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(WooPosSpacing.Large.value)
                        )
                    }
                }

                is WooPosOrdersState.Content -> {
                    WooPosOrdersListPaneScreen(
                        items = s.items,
                        selectedOrderId = s.selectedOrderId,
                        onOrderSelected = viewModel::onOrderSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Right pane
        val selectedItem: OrderItemViewState? = when (val s = state) {
            is WooPosOrdersState.Content -> s.items.firstOrNull { it.id == s.selectedOrderId }
            else -> null
        }

        WooPosOrdersDetailPaneScreen(
            selected = selectedItem,
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }
}

@Composable
fun WooPosOrdersListPaneScreen(
    items: List<OrderItemViewState>,
    selectedOrderId: Long?,
    onOrderSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = WooPosSpacing.XSmall.value)
    ) {
        items(items, key = { it.id }) { item ->
            val isSelected = item.id == selectedOrderId
            val background = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }

            val foreground = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(background)
                    .clickable { onOrderSelected(item.id) }
                    .semantics { selected = isSelected }
                    .padding(
                        horizontal = WooPosSpacing.Medium.value,
                        vertical = WooPosSpacing.Medium.value
                    ),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value)
                ) {
                    WooPosText(
                        text = item.title,
                        style = WooPosTypography.BodyMedium,
                        color = foreground
                    )
                    WooPosText(
                        text = item.date,
                        style = WooPosTypography.BodySmall,
                        color = foreground
                    )
                }

                Spacer(Modifier.weight(1f))

                WooPosText(
                    text = item.total,
                    style = WooPosTypography.BodyMedium,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
    }
}

@Composable
fun WooPosOrdersDetailPaneScreen(
    selected: OrderItemViewState?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        WooPosToolbar(
            modifier = Modifier.fillMaxWidth(),
            titleText = selected?.title ?: "--",
            titleFontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = WooPosSpacing.Large.value,
                    end = WooPosSpacing.Large.value,
                )
        ) {
            WooPosText(
                text = "Orders details will be displayed here",
                style = WooPosTypography.BodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosOrdersScreenPreview() {
    WooPosTheme {
        WooPosOrdersScreen(
            onNavigationEvent = {}
        )
    }
}
