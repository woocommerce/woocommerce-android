package com.woocommerce.android.ui.woopos.bookings

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class WooPosBooking(
    val id: Long,
    val allDay: Boolean,
    val cost: String,
    val customerId: Int,
    val dateCreated: Long,
    val dateModified: Long,
    val start: Long,
    val end: Long,
    val googleCalendarEventId: String?,
    val orderId: Int,
    val orderItemId: Int,
    val parentId: Int,
    val personCounts: List<Int>,
    val productId: Int,
    val resourceId: Int,
    val status: String,
    val localTimezone: String,
    val customerName: String? = null
) {
    val startDateTime: LocalDateTime
        get() = Instant.ofEpochSecond(start).atZone(ZoneId.systemDefault()).toLocalDateTime()
    
    val endDateTime: LocalDateTime
        get() = Instant.ofEpochSecond(end).atZone(ZoneId.systemDefault()).toLocalDateTime()
    
    val costAsBigDecimal: BigDecimal
        get() = try { BigDecimal(cost) } catch (e: Exception) { BigDecimal.ZERO }
    
    val isPaid: Boolean
        get() = status.equals("paid", ignoreCase = true)
    
    val bookingStatus: BookingStatus
        get() = when (status.lowercase()) {
            "paid" -> BookingStatus.CONFIRMED
            "unpaid" -> BookingStatus.PENDING
            "pending-confirmation" -> BookingStatus.PENDING
            "complete", "completed" -> BookingStatus.COMPLETED
            "cancelled", "canceled" -> BookingStatus.CANCELLED
            else -> BookingStatus.PENDING
        }
    
    val displayCustomerName: String
        get() = customerName ?: "Customer #$customerId"
    
    val serviceName: String
        get() = "Service #$productId"
}

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
