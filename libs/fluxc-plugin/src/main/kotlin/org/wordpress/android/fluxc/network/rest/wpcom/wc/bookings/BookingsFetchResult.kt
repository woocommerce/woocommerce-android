package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import org.wordpress.android.fluxc.persistence.entity.BookingEntity

data class BookingsFetchResult(
    val bookings: List<BookingEntity>,
    val hasMorePages: Boolean
)
