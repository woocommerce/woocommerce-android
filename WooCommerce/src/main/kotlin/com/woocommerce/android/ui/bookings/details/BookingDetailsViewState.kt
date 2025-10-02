package com.woocommerce.android.ui.bookings.details

import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel

data class BookingDetailsViewState(
    val toolbarTitle: String = "",
    val orderId: Long = 0L,
    val bookingSummary: BookingSummaryModel? = null,
    val bookingsAppointmentDetails: BookingAppointmentDetailsModel? = null,
    val bookingCustomerDetails: BookingCustomerDetailsModel? = null,
    val bookingPaymentDetails: BookingPaymentDetailsModel? = null,
    val onCancelBooking: () -> Unit = {},
    val onAttendanceStatusSelected: (BookingAttendanceStatus) -> Unit = { _ -> }
)
