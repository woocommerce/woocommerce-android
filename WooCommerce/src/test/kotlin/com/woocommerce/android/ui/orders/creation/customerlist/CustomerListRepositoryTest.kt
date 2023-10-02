package com.woocommerce.android.ui.orders.creation.customerlist

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCCustomerStore
import org.wordpress.android.fluxc.store.WCDataStore

@ExperimentalCoroutinesApi
class CustomerListRepositoryTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val customerStore: WCCustomerStore = mock()
    private val dataStore: WCDataStore = mock()

    private val repo = CustomerListRepository(
        selectedSite,
        customerStore,
        dataStore,
        coroutinesTestRule.testDispatchers,
    )

    @Test
    fun `given page one and empty search query, when searchCustomerListWithEmail, then return success and cache`() =
        testBlocking {
            // GIVEN
            val searchQuery = ""
            val searchBy = "searchBy"
            val pageSize = 10
            val page = 1

            val networkResult = listOf(WCCustomerModel())

            whenever(
                customerStore.fetchCustomersFromAnalytics(
                    site = selectedSite.get(),
                    searchQuery = searchQuery,
                    searchBy = searchBy,
                    pageSize = pageSize,
                    page = page,
                    filterEmpty = listOf("email")
                )
            ).thenReturn(WooResult(networkResult))

            // WHEN
            val result = repo.searchCustomerListWithEmail(
                searchQuery,
                searchBy,
                pageSize,
                page,
            )

            // THEN
            assertThat(result.isSuccess).isTrue()
            verify(customerStore).deleteCustomersForSite(selectedSite.get())
            verify(customerStore).saveCustomers(networkResult)
        }

    @Test
    fun `given page one and non search query, when searchCustomerListWithEmail, then return success and not cache`() =
        testBlocking {
            // GIVEN
            val searchQuery = "searchQuery"
            val searchBy = "searchBy"
            val pageSize = 10
            val page = 1

            val networkResult = listOf(WCCustomerModel())

            whenever(
                customerStore.fetchCustomersFromAnalytics(
                    site = selectedSite.get(),
                    searchQuery = searchQuery,
                    searchBy = searchBy,
                    pageSize = pageSize,
                    page = page,
                    filterEmpty = listOf("email")
                )
            ).thenReturn(WooResult(networkResult))

            // WHEN
            val result = repo.searchCustomerListWithEmail(
                searchQuery,
                searchBy,
                pageSize,
                page,
            )

            // THEN
            assertThat(result.isSuccess).isTrue()
            verify(customerStore, never()).deleteCustomersForSite(selectedSite.get())
            verify(customerStore, never()).saveCustomers(networkResult)
        }

    @Test
    fun `given page two and empty search query, when searchCustomerListWithEmail, then return success and not cache`() =
        testBlocking {
            // GIVEN
            val searchQuery = ""
            val searchBy = "searchBy"
            val pageSize = 10
            val page = 2

            val networkResult = listOf(WCCustomerModel())

            whenever(
                customerStore.fetchCustomersFromAnalytics(
                    site = selectedSite.get(),
                    searchQuery = searchQuery,
                    searchBy = searchBy,
                    pageSize = pageSize,
                    page = page,
                    filterEmpty = listOf("email")
                )
            ).thenReturn(WooResult(networkResult))

            // WHEN
            val result = repo.searchCustomerListWithEmail(
                searchQuery,
                searchBy,
                pageSize,
                page,
            )

            // THEN
            assertThat(result.isSuccess).isTrue()
            verify(customerStore, never()).deleteCustomersForSite(selectedSite.get())
            verify(customerStore, never()).saveCustomers(networkResult)
        }
}
