package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.runtime.Immutable
import com.woocommerce.android.model.Order.Status
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState

@Immutable
sealed class WooPosBookingsState {
    abstract val pullToRefreshState: WooPosPullToRefreshState

    @Immutable
    sealed interface BookingAction {
        val orderId: Long

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
    enum class AttendanceState {
        ATTENDED,
        UNATTENDED,
    }

    @Immutable
    data class CustomerSection(
        val name: String?,
        val email: String?,
        val phone: String?,
        val billingAddress: String?,
        val note: String?,
    )

    @Immutable
    data class AttendanceSection(
        val isAttendedSelected: Boolean,
        val isUnattendedSelected: Boolean,
    )

    @Immutable
    data class PaymentSection(
        val serviceAmount: String,
        val taxAmount: String,
        val discountAmount: String,
        val totalAmount: String,
        val paidWithLabel: String?,
        val showPayButtons: Boolean,
    )

    @Immutable
    data class BookingDetailsViewState(
        val id: Long,
        val number: String,
        val status: WooPosBookingStatus,
        val actionsState: BookingActionsState,
        val headerTitle: String,
        val headerSubtitle: String,
        val attendanceBadge: AttendanceState?,
        val bookingName: String,
        val appointmentDate: String,
        val appointmentTime: String,
        val duration: String,
        val teamMember: String?,
        val location: String?,
        val customerSection: CustomerSection?,
        val attendanceSection: AttendanceSection?,
        val paymentSection: PaymentSection,
        val bookingNote: String?,
    )

    @Immutable
    data class BookingItemViewState(
        val id: Long,
        val title: String,
        val date: String,
        val total: String,
        val customerEmail: String?,
        val isSelected: Boolean,
        val status: WooPosBookingStatus,
        val statusSlug: String,
        val createdAtMillis: Long
    )

    @Immutable
    data class Content(
        val items: Items,
        override val pullToRefreshState: WooPosPullToRefreshState,
        val selectedDetails: BookingDetailsViewState?,
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
    ) : WooPosBookingsState() {
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
    }

    @Immutable
    data object Loading : WooPosBookingsState() {
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
    }

    @Immutable
    data class Empty(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosBookingsState()
}

enum class WooPosBookingStatusColorKey {
    COMPLETED,
    FAILED,
    PROCESSING,
    ON_HOLD,
    OTHER;

    companion object {
        fun fromStatus(status: Status): WooPosBookingStatusColorKey = when (status) {
            Status.Completed -> COMPLETED
            Status.Failed -> FAILED
            Status.Processing -> PROCESSING
            Status.OnHold -> ON_HOLD
            else -> OTHER
        }
    }
}

data class WooPosBookingStatus(
    val text: String,
    val colorKey: WooPosBookingStatusColorKey
)
