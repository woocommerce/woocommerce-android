package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.DATE_LAST_ACTIVE_ASC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.DATE_LAST_ACTIVE_DESC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.INCLUDE_ASC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.INCLUDE_DESC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.NAME_ASC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.NAME_DESC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.REGISTERED_DATE_ASC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.REGISTERED_DATE_DESC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerDTO
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerFromAnalyticsDTO
import org.wordpress.android.fluxc.network.utils.toMap
import org.wordpress.android.fluxc.utils.putIfNotEmpty
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class CustomerRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    /**
     * Makes a GET call to `/wc/v3/customers/[remoteCustomerId]` to fetch a single customer
     *
     * @param [remoteCustomerId] Unique server id of the customer to fetch
     */
    suspend fun fetchSingleCustomer(
        site: SiteModel,
        remoteCustomerId: Long
    ): WooPayload<CustomerDTO> {
        val url = WOOCOMMERCE.customers.id(remoteCustomerId).pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = CustomerDTO::class.java
        )

        return response.toWooPayload()
    }

    /**
     * Makes a GET call to `/wc/v3/customers/` to fetch customers
     *
     */
    suspend fun fetchCustomers(
        site: SiteModel,
        pageSize: Int,
        sortType: CustomerSorting = NAME_ASC,
        offset: Long = 0,
        searchQuery: String? = null,
        email: String? = null,
        role: String? = null,
        context: String? = null,
        remoteCustomerIds: List<Long>? = null,
        excludedCustomerIds: List<Long>? = null
    ): WooPayload<Array<CustomerDTO>> {
        val url = WOOCOMMERCE.customers.pathV3

        val orderBy = when (sortType) {
            NAME_ASC, NAME_DESC -> "name"
            INCLUDE_ASC, INCLUDE_DESC -> "include"
            REGISTERED_DATE_ASC, REGISTERED_DATE_DESC -> "registered_date"
            DATE_LAST_ACTIVE_ASC, DATE_LAST_ACTIVE_DESC -> "date_last_active"
        }
        val sortOrder = when (sortType) {
            NAME_ASC, INCLUDE_ASC, REGISTERED_DATE_ASC, DATE_LAST_ACTIVE_ASC -> "asc"
            INCLUDE_DESC, NAME_DESC, REGISTERED_DATE_DESC, DATE_LAST_ACTIVE_DESC -> "desc"
        }

        val params = mutableMapOf(
            "per_page" to pageSize.toString(),
            "orderby" to orderBy,
            "order" to sortOrder,
            "offset" to offset.toString()
        ).run {
            putIfNotEmpty("search" to searchQuery)
            putIfNotEmpty("email" to email)
            putIfNotEmpty("role" to role)
            putIfNotEmpty("context" to context)
        }

        if (!remoteCustomerIds.isNullOrEmpty()) {
            params["include"] = remoteCustomerIds.map { it }.joinToString()
        }

        if (!excludedCustomerIds.isNullOrEmpty()) {
            params["exclude"] = excludedCustomerIds.map { it }.joinToString()
        }

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<CustomerDTO>::class.java
        )

        return response.toWooPayload()
    }

    /**
     * Makes a POST call to `/wc/v3/customers/` to create a customer
     */
    suspend fun createCustomer(site: SiteModel, customer: CustomerDTO): WooPayload<CustomerDTO> {
        val url = WOOCOMMERCE.customers.pathV3

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = customer.toMap(),
            clazz = CustomerDTO::class.java
        )

        return response.toWooPayload()
    }

    /**
     * Makes a GET call to `wc-analytics/reports/customers` to fetch customers
     *
     */
    suspend fun fetchCustomersFromAnalytics(
        site: SiteModel,
        pageSize: Int,
        page: Int,
        sortType: CustomerSorting = NAME_ASC,
        searchQuery: String? = null,
        searchBy: String? = null,
        filterEmpty: List<String>? = null,
    ): WooPayload<Array<CustomerFromAnalyticsDTO>> {
        val url = WOOCOMMERCE.reports.customers.pathV4Analytics

        val orderBy = when (sortType) {
            NAME_ASC, NAME_DESC -> "name"
            INCLUDE_ASC, INCLUDE_DESC -> "include"
            REGISTERED_DATE_ASC, REGISTERED_DATE_DESC -> "registered_date"
            DATE_LAST_ACTIVE_ASC, DATE_LAST_ACTIVE_DESC -> "date_last_active"
        }
        val sortOrder = when (sortType) {
            NAME_ASC, INCLUDE_ASC, REGISTERED_DATE_ASC, DATE_LAST_ACTIVE_ASC -> "asc"
            INCLUDE_DESC, NAME_DESC, REGISTERED_DATE_DESC, DATE_LAST_ACTIVE_DESC -> "desc"
        }

        val params = mutableMapOf(
            "per_page" to pageSize.toString(),
            "orderby" to orderBy,
            "order" to sortOrder,
            "page" to page.toString(),
        ).run {
            putIfNotEmpty("search" to searchQuery)
            putIfNotEmpty("searchby" to searchBy)
        }

        if (!filterEmpty.isNullOrEmpty()) {
            params["filter_empty"] = filterEmpty.joinToString(",")
        }

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<CustomerFromAnalyticsDTO>::class.java
        )

        return response.toWooPayload()
    }

    suspend fun fetchSingleCustomerFromAnalyticsByUserId(
        site: SiteModel,
        remoteCustomerId: Long
    ): WooPayload<CustomerFromAnalyticsDTO?> {
        val url = WOOCOMMERCE.reports.customers.pathV4Analytics

        val params = mapOf(
            "filter" to "single_customer",
            "users" to remoteCustomerId.toString()
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<CustomerFromAnalyticsDTO>::class.java
        )
        return response.toWooPayload { items -> items.firstOrNull() }
    }

    suspend fun fetchSingleCustomerFromAnalyticsByCustomerId(
        site: SiteModel,
        analyticsCustomerId: Long
    ): WooPayload<CustomerFromAnalyticsDTO?> {
        val url = WOOCOMMERCE.reports.customers.pathV4Analytics

        val params = mapOf(
            "filter" to "single_customer",
            "customers" to analyticsCustomerId.toString()
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<CustomerFromAnalyticsDTO>::class.java
        )
        return response.toWooPayload { items -> items.firstOrNull() }
    }
}
