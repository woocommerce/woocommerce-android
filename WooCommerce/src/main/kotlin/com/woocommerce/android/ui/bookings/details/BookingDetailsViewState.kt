package com.woocommerce.android.ui.bookings.details

import com.woocommerce.android.ui.bookings.compose.AttendanceStatus
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
)
