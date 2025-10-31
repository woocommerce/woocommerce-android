package com.woocommerce.android.ui.woopos.orders

import androidx.compose.runtime.Immutable
import com.woocommerce.android.model.Order.Status
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState

@Immutable
data class OrderDetailsViewState(
    val id: Long,
    val number: String,
    val dateTime: String,
    val customerEmail: String?,
    val status: PosOrderStatus,

    val lineItems: List<LineItemRow>,
    val breakdown: TotalsBreakdown,
    val total: String,
    val totalPaid: String,
    val paymentMethodTitle: String?,
) {
    @Immutable
    data class LineItemRow(
        val id: Long,
        val name: String,
        val qtyAndUnitPrice: String,
        val lineTotal: String,
        val imageUrl: String?,
    )

    @Immutable
    data class TotalsBreakdown(
        val products: String,
        val discount: String?,
        val discountCode: String?,
        val taxes: String,
        val shipping: String?,
        val refunds: List<String>,
        val netPayment: String?
    )
}

@Immutable
data class OrderItemViewState(
    val id: Long,
    val title: String,
    val date: String,
    val total: String,
    val customerEmail: String?,
    val isSelected: Boolean,
    val status: PosOrderStatus,
    val statusSlug: String,
    val createdAtMillis: Long
)

@Immutable
sealed class WooPosOrdersState {
    abstract val pullToRefreshState: WooPosPullToRefreshState
    abstract val searchInputState: WooPosSearchInputState

    @Immutable
    data class Content(
        val items: Items,
        override val pullToRefreshState: WooPosPullToRefreshState,
        override val searchInputState: WooPosSearchInputState,
        val selectedDetails: OrderDetailsViewState,
        val paginationState: WooPosPaginationState
    ) : WooPosOrdersState() {
        sealed class Items {
            data class Loaded(val items: Map<OrderItemViewState, OrderDetailsViewState>) : Items()
            object Searching : Items()
            data class Error(val title: String, val message: String) : Items()
            data class NothingFound(val title: String, val message: String) : Items()
        }
    }

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
