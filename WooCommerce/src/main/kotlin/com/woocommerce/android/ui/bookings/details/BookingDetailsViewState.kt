package com.woocommerce.android.ui.bookings.details

import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel

data class BookingDetailsViewState(
    val toolbarTitle: String = "",
    val orderId: Long = 0L,
    val bookingSummary: BookingSummaryModel = BookingSummaryModel(
        date = "05/07/2025, 11:00 AM",
        name = "Women’s Haircut",
        customerName = "Margarita Nikolaevna",
        attendanceStatus = BookingAttendanceStatus.CHECKED_IN,
        status = BookingStatus.Paid
    ),
    val bookingsAppointmentDetails: BookingAppointmentDetailsModel = BookingAppointmentDetailsModel(
        date = "Monday, 05 July 2025",
        time = "11:00 am - 12:00 pm",
        staff = "Marianne Renoir",
        location = "238 Willow Creek Drive, Montgomery AL 36109",
        duration = "60 min",
        price = "$55.00"
    ),
    val bookingCustomerDetails: BookingCustomerDetailsModel = BookingCustomerDetailsModel(
        name = "Margarita Nikolaevna",
        email = "margarita@example.com",
        phone = "+1 555-123-4567",
        billingAddressLines = listOf(
            "238 Willow Creek Drive",
            "Montgomery AL 36109",
            "United States"
        )
    ),
    val bookingPaymentDetails: BookingPaymentDetailsModel = BookingPaymentDetailsModel(
        service = "$55.00",
        tax = "$4.50",
        discount = "-",
        total = "$59.50"
    ),
    val onMarkAsPaid: () -> Unit = {},
    val onMarkAsRefunded: () -> Unit = {},
)
