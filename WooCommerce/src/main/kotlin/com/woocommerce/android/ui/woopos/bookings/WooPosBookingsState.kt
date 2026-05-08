package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.runtime.Immutable
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState

@Immutable
data class DateSelectorState(
    val formattedDate: String,
    val selectedDateMillis: Long,
)

@Immutable
sealed class WooPosBookingsState {
    abstract val pullToRefreshState: WooPosPullToRefreshState
    abstract val dateSelectorState: DateSelectorState?

    @Immutable
    sealed interface BookingAction {
        val orderId: Long

        @Immutable
        data class EmailReceipt(override val orderId: Long) : BookingAction

        @Immutable
        data class IssueRefund(override val orderId: Long) : BookingAction

        @Immutable
        data class CancelBooking(val bookingId: Long, override val orderId: Long) : BookingAction

        @Immutable
        data class ViewOrder(override val orderId: Long) : BookingAction
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
        val isGuest: Boolean,
    )

    @Immutable
    sealed class AttendanceSection {
        @Immutable
        data class Visible(val selection: AttendanceState) : AttendanceSection()

        @Immutable
        data object Hidden : AttendanceSection()
    }

    @Immutable
    data class PaymentSection(
        val serviceAmount: String,
        val taxAmount: String,
        val discountAmount: String,
        val totalAmount: String,
        val paidWithLabel: String?,
        val collectPaymentLabel: String?,
    )

    @Immutable
    data class BookingDetailsViewState(
        val id: Long,
        val orderId: Long,
        val number: String,
        val paymentStatus: PaymentStatus,
        val isCancelled: Boolean,
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
        val attendanceSection: AttendanceSection,
        val paymentSection: PaymentSection,
        val bookingNote: String?,
    )

    @Immutable
    data class BookingItemViewState(
        val id: Long,
        val timeRange: String,
        val subtitle: String,
        val isSelected: Boolean,
        val paymentStatus: PaymentStatus,
        val isCancelled: Boolean,
        val attendanceBadge: AttendanceState? = null,
        val teamMember: TeamMember? = null,
    ) {
        @Immutable
        data class TeamMember(
            val initials: String,
            val avatarUrl: String?,
        )
    }

    @Immutable
    data class Content(
        val items: Items,
        override val pullToRefreshState: WooPosPullToRefreshState,
        override val dateSelectorState: DateSelectorState?,
        val selectedDetails: BookingDetailsViewState?,
        val paginationState: WooPosPaginationState,
        val dialogState: DialogState
    ) : WooPosBookingsState() {
        sealed class Items {
            data class Loaded(val items: Map<BookingItemViewState, BookingDetailsViewState>) : Items()
            object Loading : Items()
            data class Error(val title: String, val message: String) : Items()
            data class NothingFound(val title: String, val message: String) : Items()
        }

        sealed class DialogState {
            data object Hidden : DialogState()

            sealed class CancelBooking : DialogState() {
                abstract val bookingId: Long
                abstract val message: String

                data class PendingConfirmation(
                    override val bookingId: Long,
                    override val message: String,
                ) : CancelBooking()

                data class Processing(
                    override val bookingId: Long,
                    override val message: String,
                ) : CancelBooking()

                data class Error(
                    override val bookingId: Long,
                    override val message: String,
                    val errorMessage: String,
                ) : CancelBooking()
            }
        }
    }

    @Immutable
    data class Error(
        val message: String,
    ) : WooPosBookingsState() {
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
        override val dateSelectorState: DateSelectorState? = null
    }

    @Immutable
    data class Loading(
        override val dateSelectorState: DateSelectorState,
    ) : WooPosBookingsState() {
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
    }
}

typealias PaymentStatus = com.woocommerce.android.ui.bookings.PaymentStatus
