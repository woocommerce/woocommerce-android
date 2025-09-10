package com.woocommerce.android.ui.woopos.orders

import androidx.compose.runtime.Immutable
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState

@Immutable
data class OrderItemViewState(
    val id: Long,
    val title: String,
    val date: String,
    val total: String,
    val isSelected: Boolean
)

@Immutable
sealed class WooPosOrdersState {
    abstract val pullToRefreshState: WooPosPullToRefreshState

    @Immutable
    data class Content(
        val items: List<OrderItemViewState>,
        override val pullToRefreshState: WooPosPullToRefreshState,
        val paginationState: WooPosPaginationState,
        val selectedOrderId: Long?
    ) : WooPosOrdersState()

    @Immutable
    data class Error(
        val message: String,
    ) : WooPosOrdersState() {
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
    }

    @Immutable
    data object Loading : WooPosOrdersState() {
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
    }

    @Immutable
    data class Empty(
        override val pullToRefreshState: WooPosPullToRefreshState =
            WooPosPullToRefreshState.Enabled
    ) : WooPosOrdersState()
}
