package com.woocommerce.android.ui.orders.list

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

    val headerMode: OrderListHeaderMode
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

    val shouldShowBrowsingControls: Boolean
        get() = headerMode == OrderListHeaderMode.Browsing

    val shouldShowCreateOrderFab: Boolean
        get() = showCreateOrderFab && headerMode != OrderListHeaderMode.Selection
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
