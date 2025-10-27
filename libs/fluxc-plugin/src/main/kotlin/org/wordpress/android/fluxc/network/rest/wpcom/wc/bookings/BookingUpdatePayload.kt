package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import org.wordpress.android.fluxc.persistence.entity.BookingEntity

data class BookingUpdatePayload(
    val attendanceStatus: BookingEntity.AttendanceStatus? = null,
    val note: String? = null,
    val status: BookingEntity.Status? = null
)
