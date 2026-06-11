package com.woocommerce.android.ui.woopos.orders

import androidx.compose.runtime.Immutable
import com.woocommerce.android.model.Order.Status

object WooPosOrdersState {

    @Immutable
    sealed interface OrderAction {
        val orderId: Long

        @Immutable
        data class IssueRefund(override val orderId: Long) : OrderAction

        @Immutable
        data class EmailReceipt(override val orderId: Long) : OrderAction
    }

    @Immutable
    sealed class OrderDetailsViewState {
        abstract val orderId: Long

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
                val actions: List<OrderAction> = emptyList()
            ) {
                @Immutable
                sealed interface LineItemsState {
                    @Immutable
                    data object Loading : LineItemsState

                    @Immutable
                    data class Loaded(val items: List<LineItemRow>) : LineItemsState
                }

                @Immutable
                data class LineItemRow(
                    val id: Long,
                    val name: String,
                    val attributesDescription: String?,
                    val qtyAndUnitPrice: String,
                    val lineTotal: String,
                    val imageUrl: String?,
                    val isLumpSum: Boolean = false,
                    val includesTax: Boolean = false,
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
