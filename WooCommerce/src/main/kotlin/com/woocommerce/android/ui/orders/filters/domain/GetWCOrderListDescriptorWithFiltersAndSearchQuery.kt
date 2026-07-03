package com.woocommerce.android.ui.orders.filters.domain

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import javax.inject.Inject

class GetWCOrderListDescriptorWithFiltersAndSearchQuery @Inject constructor(
    private val getWCOrderListDescriptorWithFilters: GetWCOrderListDescriptorWithFilters,
    private val resourceProvider: ResourceProvider,
) {
    operator fun invoke(searchQuery: String): WCOrderListDescriptor {
        val listDescriptorWithFilters = getWCOrderListDescriptorWithFilters.invoke()

        return if (searchQuery.isGuestSearch() && listDescriptorWithFilters.canFilterByGuestCustomer()) {
            listDescriptorWithFilters.copy(
                searchQuery = null,
                customerId = GUEST_CUSTOMER_ID
            )
        } else {
            listDescriptorWithFilters.copy(searchQuery = searchQuery)
        }
    }

    private fun String.isGuestSearch(): Boolean =
        trim().equals(resourceProvider.getString(R.string.orderdetail_customer_name_default), ignoreCase = true)

    private fun WCOrderListDescriptor.canFilterByGuestCustomer(): Boolean =
        customerId == null || customerId == GUEST_CUSTOMER_ID

    private companion object {
        const val GUEST_CUSTOMER_ID = 0L
    }
}
