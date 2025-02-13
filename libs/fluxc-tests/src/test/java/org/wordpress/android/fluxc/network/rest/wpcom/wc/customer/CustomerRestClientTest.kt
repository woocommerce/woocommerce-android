package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.NAME_ASC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerFromAnalyticsDTO
import org.wordpress.android.fluxc.test

class CustomerRestClientTest {
    private val wooNetwork: WooNetwork = mock()
    private val client = CustomerRestClient(wooNetwork)

    @Test
    fun `given searchQuery searchBy filter empty, when fetchCustomersFromAnalytics, then that passed to API`() {
        test {
            // GIVEN
            val searchQuery = "searchQuery"
            val searchBy = "searchBy"
            val pageSize = 50
            val page = 1
            val sortType = NAME_ASC
            val site = SiteModel().apply { siteId = 1234 }

            whenever(
                wooNetwork.executeGetGsonRequest(
                    site = site,
                    path = "/wc-analytics/reports/customers/",
                    params = mapOf(
                        "page" to page.toString(),
                        "per_page" to pageSize.toString(),
                        "order" to "asc",
                        "orderby" to "name",
                        "search" to searchQuery,
                        "searchby" to searchBy,
                        "filter_empty" to "email,city,state,country"
                    ),
                    clazz = Array<CustomerFromAnalyticsDTO>::class.java
                )
            ).thenReturn(
                WPAPIResponse.Success(
                    arrayOf(mock())
                )
            )

            // WHEN
            client.fetchCustomersFromAnalytics(
                site = site,
                pageSize = pageSize,
                page = page,
                sortType = sortType,
                searchQuery = searchQuery,
                searchBy = searchBy,
                filterEmpty = listOf("email", "city", "state", "country")
            )

            // THEN
            verify(wooNetwork).executeGetGsonRequest(
                site = site,
                path = "/wc-analytics/reports/customers/",
                params = mapOf(
                    "page" to page.toString(),
                    "per_page" to pageSize.toString(),
                    "order" to "asc",
                    "orderby" to "name",
                    "search" to searchQuery,
                    "searchby" to searchBy,
                    "filter_empty" to "email,city,state,country"
                ),
                clazz = Array<CustomerFromAnalyticsDTO>::class.java
            )
        }
    }
}
