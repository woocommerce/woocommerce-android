package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.utils.extensions.filterNotNull
import org.wordpress.android.fluxc.utils.extensions.putIfNotNull
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingsRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {
    companion object {
        const val DEFAULT_PER_PAGE = 25 // Number of items to fetch in a single request
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
    }

    suspend fun fetchBooking(
        site: SiteModel,
        bookingId: Long
    ): WooPayload<BookingDto> {
        val endpoint = WOOCOMMERCE.bookings.id(bookingId).pathV2Bookings
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = endpoint,
            clazz = BookingDto::class.java,
            params = emptyMap()
        )
        return when (response) {
            is Success -> WooPayload(response.data)
            is Error -> WooPayload(response.error.toWooError())
        }
    }

    suspend fun updateBooking(
        site: SiteModel,
        bookingId: Long,
        payload: BookingUpdatePayload,
    ): WooPayload<BookingDto> {
        val endpoint = WOOCOMMERCE.bookings.id(bookingId).pathV2Bookings
        val body = payload.asMap
        val response = wooNetwork.executePutGsonRequest(
            site = site,
            path = endpoint,
            clazz = BookingDto::class.java,
            body = body,
        )
        return when (response) {
            is Success -> WooPayload(response.data)
            is Error -> WooPayload(response.error.toWooError())
        }
    }

    @Suppress("LongParameterList")
    suspend fun fetchBookings(
        site: SiteModel,
        perPage: Int,
        page: Int,
        query: String?,
        filters: BookingFilters?,
        order: BookingsOrderOption
    ): WooPayload<Array<BookingDto>> {
        val endpoint = WOOCOMMERCE.bookings.pathV2Bookings

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = endpoint,
            clazz = Array<BookingDto>::class.java,
            params = mapOf(
                "orderby" to "start_date",
                "order" to order.value,
                "per_page" to perPage.toString(),
                "page" to page.toString(),
                "search" to query
            ).filterNotNull() + filters?.toQueryParams().orEmpty()
        )
        return when (response) {
            is Success -> WooPayload(response.data)
            is Error -> WooPayload(response.error.toWooError())
        }
    }

    suspend fun fetchResources(site: SiteModel): WooPayload<Array<BookingResourceDto>> {
        val endpoint = WOOCOMMERCE.resources.team_members.pathV2Bookings

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = endpoint,
            clazz = Array<BookingResourceDto>::class.java,
            params = emptyMap()
        )
        return when (response) {
            is Success -> WooPayload(response.data)
            is Error -> WooPayload(response.error.toWooError())
        }
    }

    suspend fun fetchProductBookingLocation(
        site: SiteModel,
        productId: Long
    ): WooPayload<ProductBookingLocationDto> {
        val url = WOOCOMMERCE.products.id(productId).pathV3
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = ProductBookingLocationDto::class.java,
            params = mapOf("_fields" to "id,booking_location")
        )
        return when (response) {
            is Success -> WooPayload(response.data)
            is Error -> WooPayload(response.error.toWooError())
        }
    }

    suspend fun fetchResource(
        site: SiteModel,
        resourceId: Long
    ): WooPayload<BookingResourceDto> {
        val endpoint = WOOCOMMERCE.resources.team_members.id(resourceId).pathV2Bookings

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = endpoint,
            clazz = BookingResourceDto::class.java,
            params = emptyMap()
        )
        return when (response) {
            is Success -> WooPayload(response.data)
            is Error -> WooPayload(response.error.toWooError())
        }
    }

    suspend fun fetchProductAvailability(
        site: SiteModel,
        productId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        resourceId: Long,
    ): WooPayload<BookingAvailabilityDto> {
        val endpoint = WOOCOMMERCE.products.id(productId).availability.pathV2Bookings
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = endpoint,
            clazz = BookingAvailabilityDto::class.java,
            params = mapOf(
                "start_date" to startDate.format(DATE_TIME_FORMATTER),
                "end_date" to endDate.format(DATE_TIME_FORMATTER),
                "resource_id" to resourceId.toString(),
            )
        )
        return when (response) {
            is Success -> WooPayload(response.data)
            is Error -> WooPayload(response.error.toWooError())
        }
    }

    private fun BookingFilters.toQueryParams(): Map<String, String> = buildMap {
        if (teamMembers != BookingsFilterOption.TeamMembers.DEFAULT) {
            teamMembers.values.forEachIndexed { index, resource ->
                set("resource[$index]", resource.value.toString())
            }
        }
        if (bookingType != null) TODO()
        if (serviceEvents != BookingsFilterOption.ServiceEvents.DEFAULT) {
            serviceEvents.values.forEachIndexed { index, event ->
                set("product[$index]", event.productId.toString())
            }
        }
        attendanceStatus.value?.let { set("attendance_status", it.key) }
        if (excludedBookingStatuses != BookingsFilterOption.ExcludedBookingStatuses.DEFAULT) {
            excludedBookingStatuses.values.forEachIndexed { index, status ->
                set("booking_status_exclude[$index]", status.key)
            }
        }
        if (paymentStatus != null) TODO()
        if (customer != null) set("user", customer.userId.toString())
        if (dateRange != BookingsFilterOption.DateRange.DEFAULT) {
            dateRange.before?.let {
                set("start_date_before", it.toString())
            }
            dateRange.after?.let {
                set("start_date_after", it.toString())
            }
        }
        if (location != null) TODO()
    }
}

data class ProductBookingLocationDto(
    val id: Long? = null,
    @SerializedName("booking_location")
    val bookingLocation: String? = null,
)

private val BookingUpdatePayload.asMap: Map<String, Any>
    get() = mutableMapOf<String, Any>().putIfNotNull(
        "attendance_status" to attendanceStatus?.key,
        "note" to note,
        "status" to status?.key
    )
