package com.woocommerce.android.ui.orders.filters.domain

import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import javax.inject.Inject

class GetWCOrderListDescriptorWithFiltersAndSearchQuery @Inject constructor(
    private val getWCOrderListDescriptorWithFilters: GetWCOrderListDescriptorWithFilters,
) {
    operator fun invoke(searchQuery: String, searchGuestOrders: Boolean = false): WCOrderListDescriptor {
        val listDescriptorWithFilters = getWCOrderListDescriptorWithFilters.invoke()

        return if (searchGuestOrders) {
            listDescriptorWithFilters.copy(
                searchQuery = null,
                customerId = GUEST_CUSTOMER_ID
            )
        } else {
            listDescriptorWithFilters.copy(searchQuery = searchQuery)
        }
    }

    private companion object {
        const val GUEST_CUSTOMER_ID = 0L
    }
}
