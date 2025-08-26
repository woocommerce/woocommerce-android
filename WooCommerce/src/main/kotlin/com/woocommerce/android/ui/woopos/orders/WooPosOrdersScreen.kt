package com.woocommerce.android.ui.woopos.orders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
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

    BackHandler { onNavigationEvent(WooPosNavigationEvent.GoBack) }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            OrdersToolbar(
                titleText = stringResource(R.string.woopos_orders_title)
            )

            WooPosOrdersListPaneScreen(
                orders = state.orders,
                selectedOrderId = state.selectedOrderId,
                onOrderSelected = viewModel::onOrderSelected,
                modifier = Modifier.fillMaxSize()
            )
        }

        WooPosOrdersDetailPaneScreen(
            order = state.orders.firstOrNull { it.id == state.selectedOrderId },
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }
}

@Composable
private fun OrdersToolbar(
    titleText: String
) {
    WooPosText(
        text = titleText,
        style = WooPosTypography.Heading,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = WooPosSpacing.Medium.value,
                vertical = WooPosSpacing.Medium.value
            )
    )
}

@Composable
fun WooPosOrdersListPaneScreen(
    orders: List<WooPosOrder>,
    selectedOrderId: Long?,
    onOrderSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = WooPosSpacing.XSmall.value)
    ) {
        items(orders, key = { it.id }) { order ->
            val isSelected = order.id == selectedOrderId
            val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WooPosSpacing.Small.value)
                    .clip(MaterialTheme.shapes.medium)
                    .background(bg)
                    .clickable { onOrderSelected(order.id) }
                    .semantics { selected = isSelected }
                    .padding(
                        horizontal = WooPosSpacing.Medium.value,
                        vertical = WooPosSpacing.Medium.value
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WooPosText(
                    text = order.title,
                    style = WooPosTypography.BodyMedium,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = fg,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = fg
                )
            }
        }
    }
}

@Composable
fun WooPosOrdersDetailPaneScreen(
    order: WooPosOrder?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        WooPosToolbar(
            modifier = Modifier
                .padding(
                    top = WooPosSpacing.Medium.value,
                    start = WooPosSpacing.Medium.value,
                    end = WooPosSpacing.Medium.value
                ),
            titleText = order?.title ?: stringResource(R.string.woopos_orders_title),
            onBackClicked = null,
            titleStyle = WooPosTypography.BodyLarge,
            titleFontWeight = FontWeight.Normal
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
