@file:Suppress("MagicNumber")

package com.woocommerce.android.ui.orders.list

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.ui.compose.designsystem.component.WooBadgeTone
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground

@PreviewLightDark
@Composable
private fun OrderListPopulatedPreview() {
    OrderListContentPreview(
        items = previewItems,
    )
}

@Preview(name = "Bulk selection and detail highlight", heightDp = 500)
@Composable
private fun OrderListSelectionPreview() {
    OrderListContentPreview(
        items = previewItems,
        rowState = OrderListRowState(
            bulkSelectedOrderIds = setOf(1L),
            detailHighlightedOrderId = 2L,
        ),
    )
}

@Preview(name = "Initial loading")
@Composable
private fun OrderListLoadingPreview() {
    OrderListContentPreview(
        state = OrderListContentState.InitialLoading,
    )
}

@Preview(name = "Append progress", heightDp = 500)
@Composable
private fun OrderListAppendPreview() {
    OrderListContentPreview(
        state = OrderListContentState.Content(isAppending = true),
        items = previewItems,
    )
}

@Preview(name = "Indexed skeletons", heightDp = 400)
@Composable
private fun OrderListSkeletonPreview() {
    OrderListContentPreview(
        items = listOf(
            OrderListItemUiModel.DateSection("Today"),
            OrderListItemUiModel.Loading(orderId = 3L),
            null,
        ),
    )
}

@Preview(name = "Empty")
@Composable
private fun OrderListEmptyPreview() {
    OrderListContentPreview(
        state = OrderListContentState.Empty(OrderListEmptyState.NoOrders),
    )
}

@Preview(name = "Network error")
@Composable
private fun OrderListErrorPreview() {
    OrderListContentPreview(
        state = OrderListContentState.Empty(OrderListEmptyState.NetworkError),
    )
}

@Preview(name = "Narrow large font", widthDp = 280, heightDp = 600, fontScale = 2f)
@Composable
private fun OrderListNarrowLargeFontPreview() {
    OrderListContentPreview(
        items = listOf(
            previewItems.first(),
            previewOrder(
                orderId = 3L,
                number = "#1003",
                customerName = "A customer name that wraps safely on a narrow display",
                total = "\$1,234.56",
            ),
        ),
    )
}

@Preview(name = "RTL", locale = "ar", heightDp = 500)
@Composable
private fun OrderListRtlPreview() {
    OrderListContentPreview(
        items = listOf(
            OrderListItemUiModel.DateSection("اليوم"),
            previewOrder(
                orderId = 4L,
                number = "#1004",
                customerName = "متجر ووكومرس",
                total = "\$42.00",
            ),
        ),
    )
}

@Composable
private fun OrderListContentPreview(
    state: OrderListContentState = OrderListContentState.Content(),
    items: List<OrderListItemUiModel?> = emptyList(),
    rowState: OrderListRowState = OrderListRowState(),
) {
    WooDesignSystemThemeWithBackground {
        OrderListContent(
            state = state,
            rowState = rowState,
            itemCount = items.size,
            itemKey = { index -> previewItemKey(index, items[index]) },
            itemAt = items::get,
            onOrderTapped = {},
            onOrderLongPressed = {},
            onOrderSelectionToggled = { true },
            onLearnMoreClicked = {},
            onShowGuestOrdersClicked = {},
            onRetryClicked = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun previewItemKey(
    index: Int,
    item: OrderListItemUiModel?,
): Any = when (item) {
    is OrderListItemUiModel.DateSection -> "section-${item.title}"
    is OrderListItemUiModel.Order -> "order-${item.orderId}"
    is OrderListItemUiModel.Loading -> "loading-${item.orderId}"
    null -> "placeholder-$index"
}

private val previewItems = listOf(
    OrderListItemUiModel.DateSection("Today"),
    previewOrder(
        orderId = 1L,
        number = "#1001",
        customerName = "Ada Lovelace",
        total = "\$48.00",
    ),
    previewOrder(
        orderId = 2L,
        number = "#1002",
        customerName = "Grace Hopper",
        total = "\$86.50",
        showDivider = false,
    ),
)

private fun previewOrder(
    orderId: Long,
    number: String,
    customerName: String,
    total: String,
    showDivider: Boolean = true,
) = OrderListItemUiModel.Order(
    orderId = orderId,
    number = number,
    customerName = customerName,
    dateCreated = "Jul 24, 2026 10:30",
    total = total,
    badges = listOf(
        OrderListBadgeUiModel("Processing", WooBadgeTone.Neutral),
        OrderListBadgeUiModel("POS", WooBadgeTone.Neutral),
    ),
    showDivider = showDivider,
)
