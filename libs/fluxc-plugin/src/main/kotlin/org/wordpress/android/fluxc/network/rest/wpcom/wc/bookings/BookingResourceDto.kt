package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import com.google.gson.annotations.SerializedName

data class BookingResourceDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("qty") val qty: Int,
    @SerializedName("role") val role: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("image_id") val imageId: Long,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("product_booking_ids") val productBookingIds: List<Long>?,
)
