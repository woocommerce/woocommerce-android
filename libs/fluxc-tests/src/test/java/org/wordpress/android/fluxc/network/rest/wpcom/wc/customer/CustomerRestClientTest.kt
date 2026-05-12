package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.NAME_ASC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerDTO
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerFromAnalyticsDTO
import org.wordpress.android.fluxc.test

class CustomerRestClientTest {
    private val wooNetwork: WooNetwork = mock()
    private val client = CustomerRestClient(wooNetwork)

    @Test
    fun `given default customer list args, when fetchCustomers, then wc customers endpoint is used`() {
        test {
            // GIVEN
            val site = SiteModel().apply { siteId = 1234 }

            whenever(
                wooNetwork.executeGetGsonRequest(
                    site = site,
                    path = "/wc/v3/customers/",
                    params = mapOf(
                        "per_page" to "20",
                        "orderby" to "registered_date",
                        "order" to "desc",
                    ),
                    clazz = Array<CustomerDTO>::class.java
                )
            ).thenReturn(WPAPIResponse.Success(emptyArray(), emptyList()))

            // WHEN
            client.fetchCustomers(site = site)

            // THEN
            verify(wooNetwork).executeGetGsonRequest(
                site = site,
                path = "/wc/v3/customers/",
                params = mapOf(
                    "per_page" to "20",
                    "orderby" to "registered_date",
                    "order" to "desc",
                ),
                clazz = Array<CustomerDTO>::class.java
            )
        }
    }

    @Test
    fun `given optional customer list args, when fetchCustomers, then supported params are passed to API`() {
        test {
            // GIVEN
            val site = SiteModel().apply { siteId = 1234 }

            whenever(
                wooNetwork.executeGetGsonRequest(
                    site = site,
                    path = "/wc/v3/customers/",
                    params = mapOf(
                        "per_page" to "50",
                        "orderby" to "email",
                        "order" to "asc",
                        "search" to "jo",
                        "email" to "jane@example.com",
                        "include" to "42,73",
                        "page" to "3",
                    ),
                    clazz = Array<CustomerDTO>::class.java
                )
            ).thenReturn(WPAPIResponse.Success(emptyArray(), emptyList()))

            // WHEN
            client.fetchCustomers(
                site = site,
                search = "jo",
                email = "jane@example.com",
                include = listOf(42, 73),
                orderby = "email",
                order = "asc",
                page = 3,
                perPage = 50,
            )

            // THEN
            verify(wooNetwork).executeGetGsonRequest(
                site = site,
                path = "/wc/v3/customers/",
                params = mapOf(
                    "per_page" to "50",
                    "orderby" to "email",
                    "order" to "asc",
                    "search" to "jo",
                    "email" to "jane@example.com",
                    "include" to "42,73",
                    "page" to "3",
                ),
                clazz = Array<CustomerDTO>::class.java
            )
        }
    }

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
                    arrayOf(mock()), emptyList()
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
