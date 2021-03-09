package com.woocommerce.android.ui.orders.creation.addcustomer

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerListDescriptor
import org.wordpress.android.fluxc.model.list.ListConfig
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier

const val PAGE_SIZE = 20

private const val DB_PAGE_SIZE = PAGE_SIZE
private const val INITIAL_LOAD_SIZE = PAGE_SIZE
private const val NETWORK_PAGE_SIZE = PAGE_SIZE
private const val PRE_FETCH_DISTANCE = 10

class AddCustomerListDescriptor(
    customerSite: SiteModel,
    customerSearchQuery: String? = null,
    customerEmail: String? = null,
    customerRole: String? = null,
    customerRemoteCustomerIds: List<Long>? = null,
    customerExcludedCustomerIds: List<Long>? = null
) : WCCustomerListDescriptor(
    customerSite,
    customerSearchQuery,
    customerEmail,
    customerRole,
    customerRemoteCustomerIds,
    customerExcludedCustomerIds
) {
    override val config = ListConfig(
        networkPageSize = NETWORK_PAGE_SIZE,
        initialLoadSize = INITIAL_LOAD_SIZE,
        dbPageSize = DB_PAGE_SIZE,
        prefetchDistance = PRE_FETCH_DISTANCE
    )

    companion object {
        @JvmStatic
        fun calculateTypeIdentifier(localSiteId: Int) =
            ListDescriptorTypeIdentifier(calculateListTypeHash(localSiteId))

        private fun calculateListTypeHash(localSiteId: Int) = "woo-site-customer-list$localSiteId".hashCode()
    }
}
