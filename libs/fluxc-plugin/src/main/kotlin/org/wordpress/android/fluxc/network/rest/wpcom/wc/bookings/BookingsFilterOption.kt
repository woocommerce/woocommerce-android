package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import java.time.Instant

sealed interface BookingsFilterOption {
    data class DateRange(
        val before: Instant?,
        val after: Instant?
    ) : BookingsFilterOption
}
