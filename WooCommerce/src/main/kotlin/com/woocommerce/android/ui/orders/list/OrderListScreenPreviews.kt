@file:Suppress("MagicNumber")

package com.woocommerce.android.ui.orders.list

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBanner
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBannerTone
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground

@PreviewLightDark
@Composable
private fun OrderListBrowsingPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            lastUpdate = "Last updated Jul 28, 10:42 AM",
        ),
    )
}

@Preview(name = "Filtered with JITM", heightDp = 780)
@Composable
private fun OrderListFilteredWithJitmPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            filterCount = 3,
            lastUpdate = "Last updated Jul 28, 10:42 AM",
        ),
        jitmContent = {
            WooNoticeBanner(
                title = "Grow your business with Woo",
                description = "A just-in-time message supplied by the Orders host.",
                tone = WooNoticeBannerTone.Info,
                modifier = Modifier.padding(
                    horizontal = WooTheme.padding.padding5,
                    vertical = WooTheme.padding.padding3,
                ),
            )
        },
    )
}

@Preview(name = "Search", heightDp = 700)
@Composable
private fun OrderListSearchPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            isSearchActive = true,
            searchQuery = "#1002",
            filterCount = 2,
        ),
        items = previewOrderListItems.takeLast(1),
    )
}

@Preview(name = "Selection over search", heightDp = 700)
@Composable
private fun OrderListSelectionPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            isSearchActive = true,
            searchQuery = "Ada",
            rowState = OrderListRowState(
                bulkSelectedOrderIds = setOf(1L),
                detailHighlightedOrderId = 2L,
            ),
        ),
    )
}

@PreviewLightDark
@Composable
private fun OrderListTroubleshootingPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            troubleshooting = OrderListTroubleshootingPresentation(
                type = OrderListTroubleshootingType.Timeout,
            ),
        ),
    )
}

@Preview(name = "Troubleshooting collapsed", heightDp = 700)
@Composable
private fun OrderListTroubleshootingCollapsedPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            troubleshooting = OrderListTroubleshootingPresentation(
                type = OrderListTroubleshootingType.ParsingError,
                isExpanded = false,
            ),
        ),
    )
}

@Preview(name = "Narrow large font", widthDp = 320, heightDp = 700, fontScale = 2f)
@Composable
private fun OrderListLargeFontPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            filterCount = 12,
            lastUpdate = "Last updated Jul 28, 10:42 AM",
        ),
    )
}

@Composable
private fun OrderListScreenPreview(
    state: OrderListScreenState,
    items: List<OrderListItemUiModel?> = previewOrderListItems,
    jitmContent: (@Composable () -> Unit)? = null,
) {
    WooDesignSystemThemeWithBackground {
        OrderListScreen(
            state = state,
            orderListContent = { modifier ->
                OrderListContent(
                    state = OrderListContentState.Content(),
                    rowState = state.rowState,
                    itemCount = items.size,
                    itemKey = { index -> previewOrderListItemKey(index, items[index]) },
                    itemAt = items::get,
                    onOrderActivated = {},
                    onOrderLongPressed = {},
                    onOrderSelectionToggled = { true },
                    onMarkOrderCompleted = {},
                    onLearnMoreClicked = {},
                    onShowGuestOrdersClicked = {},
                    onRetryClicked = {},
                    modifier = modifier,
                )
            },
            onSearchClicked = {},
            onSearchQueryChanged = {},
            onSearchSubmitted = {},
            onSearchClosed = {},
            onBarcodeClicked = {},
            onFiltersClicked = {},
            onCreateOrderClicked = {},
            onSelectionCloseClicked = {},
            onSelectionUpdateStatusClicked = {},
            onTroubleshootingExpandedChanged = {},
            onTroubleshootingClicked = {},
            onContactSupportClicked = {},
            jitmContent = jitmContent,
        )
    }
}

private fun previewOrderListItemKey(
    index: Int,
    item: OrderListItemUiModel?,
): Any = when (item) {
    is OrderListItemUiModel.DateSection -> "section-${item.title}"
    is OrderListItemUiModel.Order -> "order-${item.orderId}"
    is OrderListItemUiModel.Loading -> "loading-${item.orderId}"
    null -> "placeholder-$index"
}

private val previewOrderListItems = listOf(
    OrderListItemUiModel.DateSection("Today"),
    previewOrderListOrder(
        orderId = 1L,
        number = "#1001",
        customerName = "Ada Lovelace",
        total = "\$48.00",
    ),
    previewOrderListOrder(
        orderId = 2L,
        number = "#1002",
        customerName = "Grace Hopper",
        total = "\$86.50",
        showDivider = false,
    ),
)

private fun previewOrderListOrder(
    orderId: Long,
    number: String,
    customerName: String,
    total: String,
    showDivider: Boolean = true,
) = OrderListItemUiModel.Order(
    orderId = orderId,
    number = number,
    customerName = customerName,
    dateCreated = "Jul 28, 2026 10:30",
    total = total,
    badges = listOf(
        OrderListBadgeUiModel(
            text = "Processing",
            containerColorRes = R.color.tag_bg_processing,
            contentColorRes = R.color.tagView_text,
        ),
        OrderListBadgeUiModel(
            text = "POS",
            containerColorRes = R.color.tag_bg_pos,
            contentColorRes = R.color.tag_text_pos,
        ),
    ),
    showDivider = showDivider,
)
