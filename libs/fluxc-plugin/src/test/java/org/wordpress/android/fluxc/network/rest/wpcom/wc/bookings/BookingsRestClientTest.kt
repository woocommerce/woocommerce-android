package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork

class BookingsRestClientTest {

    private val wooNetwork: WooNetwork = mock()
    private lateinit var sut: BookingsRestClient

    @Before
    fun setUp() {
        sut = BookingsRestClient(wooNetwork)
    }

    @Test
    fun `when teamMembers filter has values, then resource params use bracket notation`(): Unit = runBlocking {
        // GIVEN
        val site = SiteModel()
        val filters = BookingFilters(
            teamMembers = BookingsFilterOption.TeamMembers(setOf(RemoteId(239L), RemoteId(13L)))
        )
        val paramsCaptor = argumentCaptor<Map<String, String>>()
        whenever(
            wooNetwork.executeGetGsonRequest(
                site = any(),
                path = any(),
                clazz = any<Class<Array<BookingDto>>>(),
                params = paramsCaptor.capture(),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
            )
        ).thenReturn(WPAPIResponse.Success(emptyArray(), emptyList()))

        // WHEN
        sut.fetchBookings(
            site = site,
            perPage = 25,
            page = 1,
            query = null,
            filters = filters,
            order = BookingsOrderOption.DESC
        )

        // THEN
        val params = paramsCaptor.firstValue
        assertThat(params).doesNotContainKey("resource")
        assertThat(params.keys.filter { it.startsWith("resource[") }).hasSize(2)
        assertThat(params).containsEntry("resource[0]", "239")
        assertThat(params).containsEntry("resource[1]", "13")
    }

    @Test
    fun `when serviceEvents filter has values, then product params use bracket notation`(): Unit = runBlocking {
        // GIVEN
        val site = SiteModel()
        val filters = BookingFilters(
            serviceEvents = BookingsFilterOption.ServiceEvents(
                setOf(
                    BookingsFilterOption.ProductInfo(47L, "Product A"),
                    BookingsFilterOption.ProductInfo(49L, "Product B")
                )
            )
        )
        val paramsCaptor = argumentCaptor<Map<String, String>>()
        whenever(
            wooNetwork.executeGetGsonRequest(
                site = any(),
                path = any(),
                clazz = any<Class<Array<BookingDto>>>(),
                params = paramsCaptor.capture(),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
            )
        ).thenReturn(WPAPIResponse.Success(emptyArray(), emptyList()))

        // WHEN
        sut.fetchBookings(
            site = site,
            perPage = 25,
            page = 1,
            query = null,
            filters = filters,
            order = BookingsOrderOption.DESC
        )

        // THEN
        val params = paramsCaptor.firstValue
        assertThat(params).doesNotContainKey("product")
        assertThat(params.keys.filter { it.startsWith("product[") }).hasSize(2)
        assertThat(params).containsEntry("product[0]", "47")
        assertThat(params).containsEntry("product[1]", "49")
    }
}
