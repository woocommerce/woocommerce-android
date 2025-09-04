package com.woocommerce.android.ui.woopos.bookings

import java.math.BigDecimal
import java.time.LocalDateTime

data class WooPosBooking(
    val id: Long,
    val customerName: String,
    val serviceName: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val status: BookingStatus,
    val price: BigDecimal,
    val isPaid: Boolean
)

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
