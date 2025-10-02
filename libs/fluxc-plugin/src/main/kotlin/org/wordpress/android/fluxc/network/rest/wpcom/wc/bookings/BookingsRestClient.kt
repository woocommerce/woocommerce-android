package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingsRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {
    companion object {
        const val DEFAULT_PER_PAGE = 25 // Number of items to fetch in a single request
    }

    suspend fun fetchBookings(
        site: SiteModel,
        perPage: Int,
        page: Int,
        filters: List<BookingsFilterOption>
    ): WooPayload<Array<BookingDto>> {
        val endpoint = WOOCOMMERCE.bookings.pathV2Bookings

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = endpoint,
            clazz = Array<BookingDto>::class.java,
            params = mapOf(
                "per_page" to perPage.toString(),
                "page" to page.toString()
            ) + filters.toQueryParams()
        )
        return when (response) {
            is Success -> WooPayload(response.data)
            is Error -> WooPayload(response.error.toWooError())
        }
    }

    private fun List<BookingsFilterOption>.toQueryParams(): Map<String, String> {
        return buildMap {
            this@toQueryParams.forEach { filter ->
                when (filter) {
                    is BookingsFilterOption.DateRange -> {
                        filter.before?.let { set("start_date_before", it.toString()) }
                        filter.after?.let { set("start_date_after", it.toString()) }
                    }
                    is BookingsFilterOption.Customer -> set("customer", filter.customerId.toString())
                }
            }
        }
    }
}
