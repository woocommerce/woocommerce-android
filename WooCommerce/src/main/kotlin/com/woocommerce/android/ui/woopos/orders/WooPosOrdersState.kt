package com.woocommerce.android.ui.woopos.orders

import androidx.compose.runtime.Immutable
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.Status
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState

@Immutable
sealed class WooPosOrdersState {
    abstract val pullToRefreshState: WooPosPullToRefreshState
    abstract val searchInputState: WooPosSearchInputState

    @Immutable
    sealed interface OrderAction {
        val orderId: Long

        @Immutable
        data class IssueRefund(override val orderId: Long) : OrderAction

        @Immutable
        data class EmailReceipt(override val orderId: Long) : OrderAction
    }

    @Immutable
    sealed class OrderActionsState {
        @Immutable
        data object Loading : OrderActionsState()

        @Immutable
        data class Loaded(val actions: List<OrderAction>) : OrderActionsState()
    }

    @Immutable
    sealed class OrderDetailsViewState {
        abstract val orderId: Long

        @Immutable
        data class Lazy(
            override val orderId: Long,
            val order: Order,
            val refundResult: RefundsFetchResult
        ) : OrderDetailsViewState()

        @Immutable
        data class Computed(
            override val orderId: Long,
            val details: Details
        ) : OrderDetailsViewState() {
            @Immutable
            data class Details(
                val id: Long,
                val number: String,
                val dateTime: String,
                val customerEmail: String?,
                val status: PosOrderStatus,

                val lineItems: LineItemsState = LineItemsState.Loading,
                val refundedLineItems: LineItemsState = LineItemsState.Loading,
                val breakdown: TotalsBreakdown,
                val total: String,
                val totalPaid: String,
                val paymentMethodTitle: String?,
                val actionsState: OrderActionsState
            ) {
                @Immutable
                sealed interface LineItemsState {
                    @Immutable
                    data object Loading : LineItemsState

                    @Immutable
                    data class Loaded(val items: List<LineItemRow>) : LineItemsState
                }

                @Immutable
                sealed interface BookingInfo {
                    @Immutable
                    data class Loading(val bookingId: Long) : BookingInfo

                    @Immutable
                    data class Loaded(val text: String) : BookingInfo

                    @Immutable
                    data class Error(val text: String) : BookingInfo
                }

                @Immutable
                data class LineItemRow(
                    val id: Long,
                    val name: String,
                    val attributesDescription: String?,
                    val qtyAndUnitPrice: String,
                    val lineTotal: String,
                    val imageUrl: String?,
                    val bookingInfo: BookingInfo? = null,
                )

                @Immutable
                data class RefundRow(
                    val label: String,
                    val amount: String,
                    val date: String,
                    val reason: String?,
                )

                @Immutable
                data class TotalsBreakdown(
                    val products: String,
                    val discount: String?,
                    val discountCode: String?,
                    val taxes: String,
                    val shipping: String?,
                    val refundsState: RefundsState,
                    val netPayment: String?,
                )

                @Immutable
                sealed interface RefundsState {
                    @Immutable
                    data object Loading : RefundsState

                    @Immutable
                    data class Loaded(val refunds: List<RefundRow>) : RefundsState

                    @Immutable
                    data class Error(val message: String) : RefundsState
                }
            }
        }
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
    data class Content(
        val items: Items,
        override val pullToRefreshState: WooPosPullToRefreshState,
        override val searchInputState: WooPosSearchInputState,
        val selectedDetails: OrderDetailsViewState.Computed.Details?,
        val paginationState: WooPosPaginationState,
        val dialogState: DialogState
    ) : WooPosOrdersState() {
        sealed class Items {
            data class Loaded(val items: Map<OrderItemViewState, OrderDetailsViewState>) : Items()
            object Searching : Items()
            data class Error(val title: String, val message: String) : Items()
            data class NothingFound(val title: String, val message: String) : Items()
        }

        sealed class DialogState {
            data object Hidden : DialogState()
            data class IssueRefund(
                val orderId: Long
            ) : DialogState()

            data class RefundDetails(
                val label: String,
                val items: List<OrderDetailsViewState.Computed.Details.LineItemRow>,
                val itemsSubtotalLabel: String,
                val itemsSubtotalAmount: String,
                val tax: String,
                val refundTotal: String,
                val paymentMethodTitle: String?,
            ) : DialogState()
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
    data class Loading(
        override val searchInputState: WooPosSearchInputState
    ) : WooPosOrdersState() {
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
    }

    @Immutable
    data class Empty(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
        override val searchInputState: WooPosSearchInputState
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
