package com.woocommerce.android.ui.bookings.details

import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import com.woocommerce.android.ui.compose.DialogState

data class BookingDetailsViewState(
    val toolbarTitle: String = "",
    val bookingUiState: BookingUiState? = null,
    val loadingState: BookingDetailsLoadingState = BookingDetailsLoadingState.Idle,
    val onCancelBooking: () -> Unit = {},
    val onAttendanceStatusSelected: (BookingAttendanceStatus) -> Unit = { _ -> },
    val onMarkAsPaid: () -> Unit = {},
    val paymentUpdateStatus: PaymentUpdateStatus = PaymentUpdateStatus.Idle,
    val dialogState: DialogState? = null,
    val onRefresh: () -> Unit = {},
) {
    val shouldShowSkeleton: Boolean = bookingUiState == null && loadingState == BookingDetailsLoadingState.Loading
    val shouldShowEmptyState: Boolean = bookingUiState == null && loadingState == BookingDetailsLoadingState.Idle
}

data class BookingUiState(
    val orderId: Long,
    val bookingSummary: BookingSummaryModel,
    val bookingsAppointmentDetails: BookingAppointmentDetailsModel,
    val bookingCustomerDetails: BookingCustomerDetailsModel,
    val bookingPaymentDetails: BookingPaymentDetailsModel?,
    val note: String,
    val isAttendanceStatusEditable: Boolean
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

sealed interface PaymentUpdateStatus {
    data object Idle : PaymentUpdateStatus
    data object InProgress : PaymentUpdateStatus
}
