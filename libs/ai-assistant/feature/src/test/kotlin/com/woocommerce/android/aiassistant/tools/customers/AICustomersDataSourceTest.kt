package com.woocommerce.android.aiassistant.tools.customers

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCCustomerStore

class AICustomersDataSourceTest {
    private val selectedSite: SelectedSite = mock()
    private val customerStore: WCCustomerStore = mock()
    private val dataSource = AICustomersDataSource(selectedSite, customerStore)

    @Test
    fun `given customer store succeeds, when customers are fetched, then store result is returned`() = runTest {
        // given
        val customer = customer(42)
        whenever(selectedSite.getOrNull()).thenReturn(DEFAULT_SITE)
        doReturn(WooResult(listOf(customer)))
            .whenever(customerStore)
            .fetchCustomers(
                site = DEFAULT_SITE,
                search = "jo",
                email = "jane@example.com",
                include = listOf(42),
                orderby = "email",
                order = "asc",
                page = 2,
                perPage = 50,
            )

        // when
        val result = dataSource.fetchCustomers(
            search = "jo",
            email = "jane@example.com",
            include = listOf(42),
            orderby = "email",
            order = "asc",
            page = 2,
            perPage = 50,
        )

        // then
        assertThat(result.getOrNull()).containsExactly(customer)
    }

    @Test
    fun `given customer store fails, when customers are fetched, then OnChangedException is returned`() = runTest {
        // given
        whenever(selectedSite.getOrNull()).thenReturn(DEFAULT_SITE)
        whenever(customerStore.fetchCustomers(DEFAULT_SITE)).thenReturn(WooResult(TEST_ERROR))

        // when
        val result = dataSource.fetchCustomers()

        // then
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(OnChangedException::class.java)
        assertThat((exception as OnChangedException).error).isEqualTo(TEST_ERROR)
    }

    private fun customer(id: Long) = WCCustomerModel(
        localSiteId = LocalId(DEFAULT_SITE.id),
        remoteCustomerId = RemoteId(id),
    )

    private companion object {
        val DEFAULT_SITE = SiteModel().apply { id = 1 }
        val TEST_ERROR = WooError(WooErrorType.INVALID_RESPONSE, NETWORK_ERROR, "Network error")
    }
}
