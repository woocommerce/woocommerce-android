package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

/**
 * Represents the sort order for bookings. Only two values are allowed by the API: "asc" and "desc".
 *
 * Use [value] when building query parameters.
 */
enum class BookingsOrderOption(val value: String) {
    ASC("asc"),
    DESC("desc");

    override fun toString(): String = value
}
