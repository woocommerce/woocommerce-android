package com.woocommerce.android.ui.orders.creation.customerlist

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.customer.WCCustomerFromAnalytics
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCCustomerStore
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

class CustomerListRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val customerStore: WCCustomerStore,
    private val dataStore: WCDataStore,
    private val dispatchers: CoroutineDispatchers,
) {
    private var countries: List<Location> = emptyList()

    /**
     * Ensure country/state data has been fetched
     */
    suspend fun loadCountries() = withContext(dispatchers.io) {
        countries = dataStore.getCountries().map { it.toAppModel() }
        if (countries.isEmpty()) {
            dataStore.fetchCountriesAndStates(selectedSite.get()).model?.let {
                countries = it.map { it.toAppModel() }
            }
        }
    }

    fun getCountry(countryCode: String) =
        countries.find { it.code == countryCode }
            ?: Location.EMPTY

    fun getState(countryCode: String, stateCode: String) =
        dataStore.getStates(countryCode)
            .find { it.code == stateCode }?.toAppModel()
            ?: Location.EMPTY

    suspend fun searchCustomerListWithEmail(
        searchQuery: String,
        searchBy: String,
        pageSize: Int,
        page: Int,
    ): Result<List<WCCustomerModel>> = withContext(dispatchers.io) {
        val result = customerStore.fetchCustomersFromAnalytics(
            site = selectedSite.get(),
            searchQuery = searchQuery,
            searchBy = searchBy,
            pageSize = pageSize,
            page = page,
            filterEmpty = listOf(FILTER_EMPTY_PARAM)
        )

        if (result.isError) {
            Result.failure(WooException(result.error))
        } else if (result.model == null) {
            Result.failure(IllegalStateException("empty model returned"))
        } else {
            val cacheResult = page == 1 && searchQuery.isEmpty()
            if (cacheResult) {
                customerStore.deleteCustomersForSite(selectedSite.get())
                customerStore.saveCustomers(result.model!!)
            }
            Result.success(result.model!!)
        }
    }

    suspend fun getCustomerList(count: Int): List<WCCustomerModel> =
        withContext(dispatchers.io) {
            val cachedCustomers = customerStore.getCustomersForSite(selectedSite.get())
            cachedCustomers
                .subList(0, count.coerceAtMost(cachedCustomers.size))
        }

    suspend fun fetchCustomerByRemoteId(remoteId: Long): WooResult<WCCustomerModel> = withContext(dispatchers.io) {
        customerStore.fetchSingleCustomer(selectedSite.get(), remoteId)
    }

    suspend fun fetchCustomerFromAnalyticsByUserId(remoteId: Long): WooResult<WCCustomerFromAnalytics> =
        withContext(dispatchers.io) {
            customerStore.fetchSingleCustomerFromAnalyticsByUserId(selectedSite.get(), remoteId)
        }

    suspend fun fetchCustomerFromAnalyticsByAnalyticsCustomerId(
        analyticsCustomerId: Long
    ): WooResult<WCCustomerFromAnalytics> = withContext(dispatchers.io) {
        customerStore.fetchSingleCustomerFromAnalyticsByAnalyticsCustomerId(selectedSite.get(), analyticsCustomerId)
    }

    suspend fun getCustomerByRemoteId(remoteId: Long) = withContext(dispatchers.io) {
        customerStore.getCustomerByRemoteId(selectedSite.get(), remoteId)
    }

    suspend fun getCustomerByAnalyticsCustomerId(customerId: Long) = withContext(dispatchers.io) {
        customerStore.getCustomerFromAnalyticsByAnalyticsId(selectedSite.get(), customerId)
    }

    private companion object {
        const val FILTER_EMPTY_PARAM = "email"
    }
}
