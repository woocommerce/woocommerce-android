package com.woocommerce.android.ui.bookings.details

import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel

sealed interface BookingDetailsLoadingState {
    data object Idle : BookingDetailsLoadingState
    data object Loading : BookingDetailsLoadingState
    data object Refreshing : BookingDetailsLoadingState
}

data class BookingDetailsViewState(
    val toolbarTitle: String = "",
    val bookingUiState: BookingUiState? = null,
    val loadingState: BookingDetailsLoadingState = BookingDetailsLoadingState.Idle,
    val onCancelBooking: () -> Unit = {},
    val onAttendanceStatusSelected: (BookingAttendanceStatus) -> Unit = { _ -> },
    val showCancelBookingDialog: Boolean = false,
    val cancelDialogMessage: String = "",
    val onDismissCancelDialog: () -> Unit = {},
    val onConfirmCancelBooking: () -> Unit = {},
    val onRefresh: () -> Unit = {},
) {
    val shouldShowSkeleton: Boolean = bookingUiState == null && loadingState == BookingDetailsLoadingState.Refreshing
}

data class BookingUiState(
    val orderId: Long,
    val bookingSummary: BookingSummaryModel,
    val bookingsAppointmentDetails: BookingAppointmentDetailsModel,
    val bookingCustomerDetails: BookingCustomerDetailsModel,
    val bookingPaymentDetails: BookingPaymentDetailsModel?,
)

sealed interface CancelState {
    data object Idle : CancelState
    data object InProgress : CancelState
}
