package com.woocommerce.android.ui.orders.creation.customerlist

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.store.WCCustomerStore
import javax.inject.Inject

class CustomerListRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val customerStore: WCCustomerStore
) {
    /**
     * Submits a fetch request to get the first page of customers matching the passed query
     */
    suspend fun searchCustomerList(
        searchQuery: String,
    ): List<WCCustomerModel>? {
        val result = customerStore.fetchCustomers(
            site = selectedSite.get(),
            searchQuery = searchQuery
        )
        return if (result.isError) {
            null
        } else {
            result.model
        }
    }
}
