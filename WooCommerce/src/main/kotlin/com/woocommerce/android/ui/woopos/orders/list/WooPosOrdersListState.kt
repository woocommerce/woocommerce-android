package com.woocommerce.android.ui.woopos.orders.list

import androidx.compose.runtime.Immutable
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderItemViewState

@Immutable
sealed class WooPosOrdersListState {
    abstract val pullToRefreshState: WooPosPullToRefreshState
    abstract val searchInputState: WooPosSearchInputState

    abstract val showToolbar: Boolean

    @Immutable
    data class Content(
        val items: Items,
        override val pullToRefreshState: WooPosPullToRefreshState,
        override val searchInputState: WooPosSearchInputState,
        val paginationState: WooPosPaginationState,
        override val showToolbar: Boolean,
    ) : WooPosOrdersListState() {
        sealed class Items {
            data class Loaded(val items: List<OrderItemViewState>) : Items()
            data object Searching : Items()
            data class Error(val title: String, val message: String) : Items()
            data class NothingFound(val title: String, val message: String) : Items()
        }
    }

    @Immutable
    data class Error(
        val message: String,
        override val searchInputState: WooPosSearchInputState,
        override val showToolbar: Boolean,
    ) : WooPosOrdersListState() {
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
    }

    @Immutable
    data class Loading(
        override val searchInputState: WooPosSearchInputState,
        override val showToolbar: Boolean,
    ) : WooPosOrdersListState() {
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
    }

    @Immutable
    data class Empty(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
        override val searchInputState: WooPosSearchInputState,
        override val showToolbar: Boolean,
    ) : WooPosOrdersListState()
}
