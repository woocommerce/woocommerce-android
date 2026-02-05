package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.runtime.Immutable
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState

@Immutable
sealed class WooPosBookingsState {
    data object Loading : WooPosBookingsState()

    @Immutable
    data class Error(val message: String) : WooPosBookingsState()

    data object Empty : WooPosBookingsState()

    @Immutable
    data class Content(
        val selectedTab: BookingTab,
        val items: List<BookingListItem>,
        val selectedDetail: BookingDetail?,
        val paginationState: WooPosPaginationState,
        val pullToRefreshState: WooPosPullToRefreshState,
        val dialogState: DialogState,
        val isLoadingList: Boolean = false,
    ) : WooPosBookingsState()
}

@Immutable
data class BookingListItem(
    val id: Long,
    val orderId: Long,
    val customerName: String,
    val serviceName: String,
    val startTime: String,
    val amount: String,
    val bookingStatus: BookingStatusUi,
    val attendanceStatus: AttendanceStatusUi?,
    val isSelected: Boolean,
)

@Immutable
data class BookingDetail(
    val id: Long,
    val orderId: Long,
    val customerName: String,
    val serviceName: String,
    val startDate: String,
    val startTime: String,
    val endTime: String,
    val amount: String,
    val currency: String,
    val bookingStatus: BookingStatusUi,
    val attendanceStatus: AttendanceStatusUi?,
    val isCancellable: Boolean,
    val isAttendanceEditable: Boolean,
    val hasLinkedOrder: Boolean,
    val isPayable: Boolean,
    val attendanceUpdateInProgress: Boolean,
    val cancelInProgress: Boolean,
    val paymentUpdateInProgress: Boolean,
    val orderTotals: BookingOrderTotals? = null,
    val actions: List<BookingAction> = emptyList(),
)

@Immutable
data class BookingOrderTotals(
    val subtotalText: String,
    val taxText: String,
    val totalText: String,
)

enum class BookingStatusUi(val label: String) {
    Unpaid("Unpaid"),
    PendingConfirmation("Pending"),
    Confirmed("Confirmed"),
    Paid("Paid"),
    Cancelled("Cancelled"),
    Complete("Complete"),
    InCart("In Cart"),
    Unknown("Unknown"),
}

enum class AttendanceStatusUi(val label: String) {
    Attended("Attended"),
    Unattended("Unattended"),
    Cancelled("Cancelled"),
}

@Immutable
sealed interface BookingAction {
    data object ViewOrder : BookingAction
    data object CancelBooking : BookingAction
}

sealed class DialogState {
    data object Hidden : DialogState()
    data class CancelConfirmation(val bookingId: Long) : DialogState()
}
