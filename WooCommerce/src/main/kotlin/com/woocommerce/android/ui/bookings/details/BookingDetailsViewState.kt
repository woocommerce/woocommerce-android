package com.woocommerce.android.ui.bookings.details

import com.woocommerce.android.ui.bookings.compose.AttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummary

data class BookingDetailsViewState(
    val toolbarTitle: String = "",
    val bookingSummary: BookingSummary = BookingSummary(
        date = "05/07/2025, 11:00 AM",
        name = "Women’s Haircut",
        customerName = "Margarita Nikolaevna",
        attendanceStatus = AttendanceStatus.CHECKED_IN,
        paymentStatus = BookingPaymentStatus.PAID
    ),
    val bookingsAppointmentDetails: BookingAppointmentDetailsModel = BookingAppointmentDetailsModel(
        date = "Monday, 05 July 2025",
        time = "11:00 am - 12:00 pm",
        staff = "Marianne Renoir",
        location = "238 Willow Creek Drive, Montgomery AL 36109",
        duration = "60 min",
        price = "$55.00"
    )
)
