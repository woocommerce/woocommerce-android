package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.utils.extensions.filterNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingsRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {
    companion object {
        const val DEFAULT_PER_PAGE = 25 // Number of items to fetch in a single request
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

    @Suppress("LongParameterList")
    suspend fun fetchBookings(
        site: SiteModel,
        perPage: Int,
        page: Int,
        query: String?,
        filters: List<BookingsFilterOption>,
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
            ).filterNotNull() + filters.toQueryParams()
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

    private fun List<BookingsFilterOption>.toQueryParams(): Map<String, String> = buildMap {
        this@toQueryParams.forEach { filter ->
            when (filter) {
                BookingsFilterOption.TeamMember -> TODO()
                BookingsFilterOption.AttendanceStatus -> TODO()
                BookingsFilterOption.PaymentStatus -> TODO()
                BookingsFilterOption.BookingType -> TODO()
                is BookingsFilterOption.Customer -> set("customer", filter.customerId.toString())

                BookingsFilterOption.Location -> TODO()
                is BookingsFilterOption.DateRange -> {
                    filter.before?.let { set("start_date_before", it.toString()) }
                    filter.after?.let { set("start_date_after", it.toString()) }
                }

                BookingsFilterOption.ServiceEvent -> TODO()
            }
        }
    }
}
