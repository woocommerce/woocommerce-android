package com.woocommerce.android.ui.woopos.orders

import androidx.compose.runtime.Immutable
import com.woocommerce.android.model.Order.Status
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState

@Immutable
data class OrderItemViewState(
    val id: Long,
    val title: String,
    val date: String,
    val total: String,
    val customerEmail: String?,
    val isSelected: Boolean,
    val status: PosOrderStatus
)

@Immutable
sealed class WooPosOrdersState {
    abstract val pullToRefreshState: WooPosPullToRefreshState
    abstract val searchInputState: WooPosSearchInputState

    @Immutable
    data class Content(
        val items: List<OrderItemViewState>,
        override val pullToRefreshState: WooPosPullToRefreshState,
        override val searchInputState: WooPosSearchInputState,
        val paginationState: WooPosPaginationState,
        val selectedOrderId: Long?
    ) : WooPosOrdersState()

    @Immutable
    data class Error(
        val message: String,
        override val searchInputState: WooPosSearchInputState
    ) : WooPosOrdersState() {
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
    }

    @Immutable
    data class Loading(override val searchInputState: WooPosSearchInputState) : WooPosOrdersState() {
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
    }

    @Immutable
    data class Empty(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
        override val searchInputState: WooPosSearchInputState,
    ) : WooPosOrdersState()
}

enum class OrderStatusColorKey {
    COMPLETED,
    FAILED,
    PROCESSING,
    ON_HOLD,
    OTHER;

    companion object {
        fun fromStatus(status: Status): OrderStatusColorKey = when (status) {
            Status.Completed -> COMPLETED
            Status.Failed -> FAILED
            Status.Processing -> PROCESSING
            Status.OnHold -> ON_HOLD
            else -> OTHER
        }
    }
}

data class PosOrderStatus(
    val text: String,
    val colorKey: OrderStatusColorKey
)
