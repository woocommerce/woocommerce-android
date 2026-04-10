package com.woocommerce.android.ui.bookings.details

import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetailsUiModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import com.woocommerce.android.ui.compose.DialogState

sealed class BookingDetailsViewState(
    open val toolbarTitle: String = "",
) {
    data object Empty : BookingDetailsViewState()

    data class ShowBooking(
        override val toolbarTitle: String = "",
        val bookingUiState: BookingUiState? = null,
        val loadingState: BookingDetailsLoadingState = BookingDetailsLoadingState.Idle,
        val dialogState: DialogState? = null,
        val onRefresh: () -> Unit = {},
    ) : BookingDetailsViewState() {

        val shouldShowSkeleton: Boolean =
            bookingUiState == null && loadingState == BookingDetailsLoadingState.Loading
    }
}

data class BookingUiState(
    val orderId: Long,
    val bookingSummary: BookingSummaryModel,
    val bookingsAppointmentDetails: BookingAppointmentDetailsModel,
    val bookingCustomerDetails: BookingCustomerDetailsUiModel,
    val bookingPaymentDetails: BookingPaymentDetailsModel?,
    val note: String,
    val isAttendanceStatusEditable: Boolean,
    val onCancelBooking: () -> Unit = {},
    val onRescheduleBooking: () -> Unit = {},
    val onAttendanceToggle: () -> Unit = {},
    val onNoteClicked: () -> Unit = {},
    val onViewOrderClicked: () -> Unit = {},
    val onIssueRefundClicked: (() -> Unit)? = null,
)

sealed interface BookingDetailsLoadingState {
    data object Idle : BookingDetailsLoadingState
    data object Loading : BookingDetailsLoadingState
    data object Refreshing : BookingDetailsLoadingState
}

sealed interface CancelStatus {
    data object Idle : CancelStatus
    data object InProgress : CancelStatus
}

sealed interface AttendanceUpdateStatus {
    data object Idle : AttendanceUpdateStatus
    data object InProgress : AttendanceUpdateStatus
}
