package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
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
        page: Int
    ): BookingsFetchedPayload<List<BookingDto>> {
        val endpoint = WOOCOMMERCE.bookings.pathV2Bookings

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = endpoint,
            clazz = Array<BookingDto>::class.java,
            params = mapOf(
                "per_page" to perPage.toString(),
                "page" to page.toString()
            )
        )
        return when (response) {
            is Success -> BookingsFetchedPayload(response.data?.toList() ?: emptyList())
            is Error -> BookingsFetchedPayload(response.error.toBookingsError())
        }
    }
}
