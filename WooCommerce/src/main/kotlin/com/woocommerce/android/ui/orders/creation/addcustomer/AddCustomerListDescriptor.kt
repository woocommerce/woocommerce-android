package com.woocommerce.android.ui.orders.creation.addcustomer

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerListDescriptor
import org.wordpress.android.fluxc.model.list.ListConfig

private const val PAGE_SIZE = 20

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
            networkPageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            dbPageSize = PAGE_SIZE,
            prefetchDistance = 3
    )
}
