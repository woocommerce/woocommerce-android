package com.woocommerce.android.ui.orders.list

import androidx.annotation.ColorRes

internal sealed interface OrderListContentState {
    data object InitialLoading : OrderListContentState

    data class Content(
        val isAppending: Boolean = false,
        val contentRevision: Long = 0L,
    ) : OrderListContentState

    data class Empty(
        val state: OrderListEmptyState,
    ) : OrderListContentState
}

internal sealed interface OrderListEmptyState {
    data object NoOrders : OrderListEmptyState
    data object Filtered : OrderListEmptyState
    data class Search(val query: String) : OrderListEmptyState
    data class GuestSearch(val query: String) : OrderListEmptyState
    data object Offline : OrderListEmptyState
    data object NetworkError : OrderListEmptyState
}

internal sealed interface OrderListItemUiModel {
    data class DateSection(
        val title: String,
    ) : OrderListItemUiModel

    data class Order(
        val orderId: Long,
        val number: String,
        val customerName: String,
        val dateCreated: String?,
        val total: String,
        val badges: List<OrderListBadgeUiModel>,
        val isCompleted: Boolean = false,
        val showDivider: Boolean = true,
    ) : OrderListItemUiModel

    data class Loading(
        val orderId: Long,
    ) : OrderListItemUiModel
}

internal data class OrderListBadgeUiModel(
    val text: String,
    @ColorRes val containerColorRes: Int,
    @ColorRes val contentColorRes: Int,
)

internal data class OrderListRowState(
    val bulkSelectedOrderIds: Set<Long> = emptySet(),
    val detailHighlightedOrderId: Long? = null,
) {
    val isBulkSelectionActive: Boolean
        get() = bulkSelectedOrderIds.isNotEmpty()
}
