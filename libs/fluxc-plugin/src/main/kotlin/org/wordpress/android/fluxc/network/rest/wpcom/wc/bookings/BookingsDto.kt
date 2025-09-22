package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.store.Store.OnChangedError

data class BookingsFetchedPayload<T>(
    val response: T? = null
) : Payload<BookingsError>() {
    constructor(error: BookingsError) : this() {
        this.error = error
    }
}

class BookingsError @JvmOverloads constructor(
    val type: BookingsErrorType,
    val message: String? = null
) : OnChangedError

enum class BookingsErrorType {
    GENERIC_ERROR,
    AUTHORIZATION_REQUIRED,
    INVALID_RESPONSE,
    API_ERROR,
    TIMEOUT
}

fun WPAPINetworkError.toBookingsError(): BookingsError {
    val type = when (type) {
        GenericErrorType.TIMEOUT -> BookingsErrorType.TIMEOUT
        GenericErrorType.HTTP_AUTH_ERROR,
        GenericErrorType.AUTHORIZATION_REQUIRED,
        GenericErrorType.NOT_AUTHENTICATED -> BookingsErrorType.AUTHORIZATION_REQUIRED
        GenericErrorType.PARSE_ERROR,
        GenericErrorType.NOT_FOUND,
        GenericErrorType.CENSORED,
        GenericErrorType.INVALID_RESPONSE -> BookingsErrorType.INVALID_RESPONSE
        GenericErrorType.NO_CONNECTION,
        GenericErrorType.SERVER_ERROR,
        GenericErrorType.INVALID_SSL_CERTIFICATE,
        GenericErrorType.NETWORK_ERROR -> BookingsErrorType.API_ERROR
        GenericErrorType.UNKNOWN,
        null -> BookingsErrorType.GENERIC_ERROR
    }
    return BookingsError(type, message)
}

// Minimal response model for bookings based on the provided JSON

data class BookingDto(
    @SerializedName("id") val id: Long,
    @SerializedName("start") val start: Long,
    @SerializedName("end") val end: Long,
    @SerializedName("all_day") val allDay: Boolean,
    @SerializedName("status") val status: String,
    @SerializedName("cost") val cost: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("customer_id") val customerId: Long,
    @SerializedName("product_id") val productId: Long,
    @SerializedName("resource_id") val resourceId: Long,
    @SerializedName("date_created") val dateCreated: Long,
    @SerializedName("date_modified") val dateModified: Long,
    @SerializedName("google_calendar_event_id") val googleCalendarEventId: String,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("order_item_id") val orderItemId: Long,
    @SerializedName("parent_id") val parentId: Long,
    @SerializedName("person_counts") val personCounts: List<Int>?,
    @SerializedName("local_timezone") val localTimezone: String
)
