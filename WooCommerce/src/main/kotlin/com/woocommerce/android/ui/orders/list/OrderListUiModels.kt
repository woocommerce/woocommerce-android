package com.woocommerce.android.ui.orders.list

import androidx.annotation.ColorRes

internal data class OrderListScreenState(
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val filterCount: Int = 0,
    val lastUpdate: String? = null,
    val rowState: OrderListRowState = OrderListRowState(),
    val troubleshooting: OrderListTroubleshootingPresentation? = null,
    val showCreateOrderFab: Boolean = true,
) {
    val isSelecting: Boolean
        get() = rowState.isBulkSelectionActive

    val selectedOrderCount: Int
        get() = rowState.bulkSelectedOrderIds.size

    private val headerMode: OrderListHeaderMode
        get() = when {
            isSelecting -> OrderListHeaderMode.Selection
            isSearchActive -> OrderListHeaderMode.Search
            else -> OrderListHeaderMode.Browsing
        }

    val headerContent: OrderListHeaderContent
        get() = OrderListHeaderContent(
            mode = headerMode,
            selectedOrderCount = selectedOrderCount,
        )

    val shouldShowCreateOrderFab: Boolean
        get() = showCreateOrderFab && headerMode == OrderListHeaderMode.Browsing
}

internal data class OrderListHeaderContent(
    val mode: OrderListHeaderMode,
    val selectedOrderCount: Int,
)

internal enum class OrderListHeaderMode {
    Selection,
    Search,
    Browsing,
}

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

internal data class OrderListTroubleshootingPresentation(
    val type: OrderListTroubleshootingType,
    val isExpanded: Boolean = true,
)

internal enum class OrderListTroubleshootingType {
    ParsingError,
    Timeout,
}
