package com.woocommerce.android.ui.bookings.details

import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel

data class BookingDetailsViewState(
    val toolbarTitle: String = "",
    val bookingUiState: BookingUiState? = null,
    val onCancelBooking: () -> Unit = {},
    val onAttendanceStatusSelected: (BookingAttendanceStatus) -> Unit = { _ -> },
    val showCancelBookingDialog: Boolean = false,
    val cancelDialogMessage: String = "",
    val onDismissCancelDialog: () -> Unit = {},
    val onConfirmCancelBooking: () -> Unit = {},
) {

    val shouldShowSkeleton: Boolean = bookingUiState == null
}

data class BookingUiState(
    val orderId: Long,
    val bookingSummary: BookingSummaryModel,
    val bookingsAppointmentDetails: BookingAppointmentDetailsModel,
    val bookingCustomerDetails: BookingCustomerDetailsModel,
    val bookingPaymentDetails: BookingPaymentDetailsModel?,
)
