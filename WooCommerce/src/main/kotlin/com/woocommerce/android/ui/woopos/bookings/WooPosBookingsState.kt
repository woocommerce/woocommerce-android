package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.runtime.Immutable
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.Status
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult

@Immutable
sealed class WooPosBookingsState {
    abstract val pullToRefreshState: WooPosPullToRefreshState
    abstract val searchInputState: WooPosSearchInputState

    @Immutable
    sealed interface BookingAction {
        val orderId: Long

        @Immutable
        data class IssueRefund(override val orderId: Long) : BookingAction

        @Immutable
        data class EmailReceipt(override val orderId: Long) : BookingAction
    }

    @Immutable
    sealed class BookingActionsState {
        @Immutable
        data object Loading : BookingActionsState()

        @Immutable
        data class Loaded(val actions: List<BookingAction>) : BookingActionsState()
    }

    @Immutable
    sealed class BookingDetailsViewState {
        abstract val orderId: Long

        @Immutable
        data class Lazy(
            override val orderId: Long,
            val order: Order,
            val refundResult: RefundsFetchResult
        ) : BookingDetailsViewState()

        @Immutable
        data class Computed(
            override val orderId: Long,
            val details: Details
        ) : BookingDetailsViewState() {
            @Immutable
            data class Details(
                val id: Long,
                val number: String,
                val dateTime: String,
                val customerEmail: String?,
                val status: PosBookingStatus,

                val lineItems: List<LineItemRow>,
                val breakdown: TotalsBreakdown,
                val total: String,
                val totalPaid: String,
                val paymentMethodTitle: String?,
                val actionsState: BookingActionsState
            ) {
                @Immutable
                data class LineItemRow(
                    val id: Long,
                    val name: String,
                    val attributesDescription: String?,
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
        }
    }

    @Immutable
    data class BookingItemViewState(
        val id: Long,
        val title: String,
        val date: String,
        val total: String,
        val customerEmail: String?,
        val isSelected: Boolean,
        val status: PosBookingStatus,
        val statusSlug: String,
        val createdAtMillis: Long
    )

    @Immutable
    data class Content(
        val items: Items,
        override val pullToRefreshState: WooPosPullToRefreshState,
        override val searchInputState: WooPosSearchInputState,
        val selectedDetails: BookingDetailsViewState.Computed.Details?,
        val paginationState: WooPosPaginationState,
        val dialogState: DialogState
    ) : WooPosBookingsState() {
        sealed class Items {
            data class Loaded(val items: Map<BookingItemViewState, BookingDetailsViewState>) : Items()
            object Searching : Items()
            data class Error(val title: String, val message: String) : Items()
            data class NothingFound(val title: String, val message: String) : Items()
        }

        sealed class DialogState {
            data object Hidden : DialogState()
            data class IssueRefund(
                val orderId: Long
            ) : DialogState()
        }
    }

    @Immutable
    data class Error(
        val message: String,
        override val searchInputState: WooPosSearchInputState
    ) : WooPosBookingsState() {
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
    }

    @Immutable
    data class Loading(
        override val searchInputState: WooPosSearchInputState
    ) : WooPosBookingsState() {
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
    }

    @Immutable
    data class Empty(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
        override val searchInputState: WooPosSearchInputState
    ) : WooPosBookingsState()
}

enum class BookingStatusColorKey {
    COMPLETED,
    FAILED,
    PROCESSING,
    ON_HOLD,
    OTHER;

    companion object {
        fun fromStatus(status: Status): BookingStatusColorKey = when (status) {
            Status.Completed -> COMPLETED
            Status.Failed -> FAILED
            Status.Processing -> PROCESSING
            Status.OnHold -> ON_HOLD
            else -> OTHER
        }
    }
}

data class PosBookingStatus(
    val text: String,
    val colorKey: BookingStatusColorKey
)
