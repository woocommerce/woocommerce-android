package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import com.google.gson.annotations.SerializedName

data class BookingDto(
    @SerializedName("id") val id: Long,
    @SerializedName("start") val start: Long,
    @SerializedName("end") val end: Long,
    @SerializedName("all_day") val allDay: Boolean,
    @SerializedName("status") val status: String,
    @SerializedName("cost") val cost: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("customer_id") val customerId: Long,
    @SerializedName("user_id") val userId: Long = 0,
    @SerializedName("product_id") val productId: Long,
    @SerializedName("resource_id") val resourceId: Long,
    @SerializedName("date_created") val dateCreated: Long,
    @SerializedName("date_modified") val dateModified: Long,
    @SerializedName("google_calendar_event_id") val googleCalendarEventId: String,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("order_item_id") val orderItemId: Long,
    @SerializedName("parent_id") val parentId: Long,
    @SerializedName("person_counts") val personCounts: List<Int>?,
    @SerializedName("local_timezone") val localTimezone: String,
    @SerializedName("attendance_status") val attendanceStatus: String?,
    @SerializedName("note") val note: String? = null,
)
