package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import com.google.gson.annotations.SerializedName

/**
 * DTO for the response from `GET wc-bookings/v2/products/{id}/availability`.
 *
 * The `availability` field contains slots grouped as: month → day → time slot → capacity count.
 */
data class BookingAvailabilityDto(
    @SerializedName("product_id")
    val productId: Long,
    @SerializedName("resource_id")
    val resourceId: Long,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String,
    @SerializedName("timezone_offset")
    val timezoneOffset: Int,
    @SerializedName("availability")
    val availability: Map<String, Map<String, Map<String, Int>>>,
)
