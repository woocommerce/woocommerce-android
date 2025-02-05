package org.wordpress.android.fluxc.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerFromAnalyticsMapper
import org.wordpress.android.fluxc.model.customer.WCCustomerMapper
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerDTO
import org.wordpress.android.fluxc.persistence.CustomerSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.persistence.dao.CustomerFromAnalyticsDao
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCCustomerStoreTest {
    val error = WooError(INVALID_RESPONSE, NETWORK_ERROR, "Invalid site ID")

    private val restClient: CustomerRestClient = mock()
    private val mapper: WCCustomerMapper = mock()
    private val analyticsMapper: WCCustomerFromAnalyticsMapper= mock()
    private val customerFromAnalyticsDao: CustomerFromAnalyticsDao = mock()

    private lateinit var store: WCCustomerStore

    @Before
    fun setUp() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val config = SingleStoreWellSqlConfigForTests(
            appContext,
            listOf(WCCustomerModel::class.java),
            WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        store = WCCustomerStore(
            restClient,
            initCoroutineEngine(),
            mapper,
            customerFromAnalyticsDao,
            analyticsMapper
        )
    }

    @Test
    fun `fetch single customer with success returns success`() = test {
        // given
        val siteModelId = 1
        val remoteCustomerId = 2L
        val siteModel = SiteModel().apply { id = siteModelId }

        val response: CustomerDTO = mock()
        whenever(restClient.fetchSingleCustomer(siteModel, remoteCustomerId))
            .thenReturn(WooPayload(response))
        val model: WCCustomerModel = mock()
        whenever(mapper.mapToModel(siteModel, response)).thenReturn(model)

        // when
        val result = store.fetchSingleCustomer(siteModel, remoteCustomerId)

        // then
        assertFalse(result.isError)
        assertEquals(model, result.model)
    }

    @Test
    fun `fetch single customer with error returns error`() = test {
        // given
        val siteModelId = 1
        val remoteCustomerId = 2L
        val siteModel = SiteModel().apply { id = siteModelId }

        whenever(restClient.fetchSingleCustomer(siteModel, remoteCustomerId)).thenReturn(
            WooPayload(
                error
            )
        )

        // when
        val result = store.fetchSingleCustomer(siteModel, remoteCustomerId)

        // then
        assertTrue(result.isError)
        assertEquals(error, result.error)
    }

    @Test
    fun `fetch customers with success returns success and cache`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }

        val customerOne: CustomerDTO = mock()
        val customerTwo: CustomerDTO = mock()
        val response = arrayOf(customerOne, customerTwo)
        whenever(restClient.fetchCustomers(siteModel, 25))
            .thenReturn(WooPayload(response))
        val modelOne = WCCustomerModel().apply {
            remoteCustomerId = 1L
            localSiteId = siteModelId
        }
        val modelTwo = WCCustomerModel().apply {
            remoteCustomerId = 2L
            localSiteId = siteModelId
        }
        whenever(mapper.mapToModel(siteModel, customerOne)).thenReturn(modelOne)
        whenever(mapper.mapToModel(siteModel, customerTwo)).thenReturn(modelTwo)

        // when
        val result = store.fetchCustomers(siteModel, 25)

        // then
        assertFalse(result.isError)
        assertEquals(listOf(modelOne, modelTwo), result.model)
        assertEquals(modelOne, CustomerSqlUtils.getCustomersForSite(siteModel)[0])
        assertEquals(modelTwo, CustomerSqlUtils.getCustomersForSite(siteModel)[1])
    }

    @Test
    fun `fetch customers with and search success returns success and does not cache`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }
        val searchQuery = "searchQuery"

        val customerOne: CustomerDTO = mock()
        val customerTwo: CustomerDTO = mock()
        val response = arrayOf(customerOne, customerTwo)
        whenever(restClient.fetchCustomers(siteModel, 25, searchQuery = searchQuery))
            .thenReturn(WooPayload(response))
        val modelOne: WCCustomerModel = mock()
        val modelTwo: WCCustomerModel = mock()
        whenever(mapper.mapToModel(siteModel, customerOne)).thenReturn(modelOne)
        whenever(mapper.mapToModel(siteModel, customerTwo)).thenReturn(modelTwo)

        // when
        val result = store.fetchCustomers(siteModel, 25, searchQuery = searchQuery)

        // then
        assertFalse(result.isError)
        assertEquals(listOf(modelOne, modelTwo), result.model)
        assertTrue(CustomerSqlUtils.getCustomersForSite(siteModel).isEmpty())
    }

    @Test
    fun `fetch customers with and email success returns success and does not cache`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }
        val email = "email"

        val customerOne: CustomerDTO = mock()
        val customerTwo: CustomerDTO = mock()
        val response = arrayOf(customerOne, customerTwo)
        whenever(restClient.fetchCustomers(siteModel, 25, email = email))
            .thenReturn(WooPayload(response))
        val modelOne: WCCustomerModel = mock()
        val modelTwo: WCCustomerModel = mock()
        whenever(mapper.mapToModel(siteModel, customerOne)).thenReturn(modelOne)
        whenever(mapper.mapToModel(siteModel, customerTwo)).thenReturn(modelTwo)

        // when
        val result = store.fetchCustomers(siteModel, 25, email = email)

        // then
        assertFalse(result.isError)
        assertEquals(listOf(modelOne, modelTwo), result.model)
        assertTrue(CustomerSqlUtils.getCustomersForSite(siteModel).isEmpty())
    }

    @Test
    fun `fetch customers with and role success returns success and does not cache`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }
        val role = "role"

        val customerOne: CustomerDTO = mock()
        val customerTwo: CustomerDTO = mock()
        val response = arrayOf(customerOne, customerTwo)
        whenever(restClient.fetchCustomers(siteModel, 25, role = role))
            .thenReturn(WooPayload(response))
        val modelOne: WCCustomerModel = mock()
        val modelTwo: WCCustomerModel = mock()
        whenever(mapper.mapToModel(siteModel, customerOne)).thenReturn(modelOne)
        whenever(mapper.mapToModel(siteModel, customerTwo)).thenReturn(modelTwo)

        // when
        val result = store.fetchCustomers(siteModel, 25, role = role)

        // then
        assertFalse(result.isError)
        assertEquals(listOf(modelOne, modelTwo), result.model)
        assertTrue(CustomerSqlUtils.getCustomersForSite(siteModel).isEmpty())
    }

    @Test
    fun `fetch customers with and remote ids success returns success and does not cache`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }
        val remoteCustomerIds = listOf(1L)

        val customerOne: CustomerDTO = mock()
        val customerTwo: CustomerDTO = mock()
        val response = arrayOf(customerOne, customerTwo)
        whenever(restClient.fetchCustomers(siteModel, 25, remoteCustomerIds = remoteCustomerIds))
            .thenReturn(WooPayload(response))
        val modelOne: WCCustomerModel = mock()
        val modelTwo: WCCustomerModel = mock()
        whenever(mapper.mapToModel(siteModel, customerOne)).thenReturn(modelOne)
        whenever(mapper.mapToModel(siteModel, customerTwo)).thenReturn(modelTwo)

        // when
        val result = store.fetchCustomers(siteModel, 25, remoteCustomerIds = remoteCustomerIds)

        // then
        assertFalse(result.isError)
        assertEquals(listOf(modelOne, modelTwo), result.model)
        assertTrue(CustomerSqlUtils.getCustomersForSite(siteModel).isEmpty())
    }

    @Test
    fun `fetch customers with and excluded ids success returns success and does not cache`() =
        test {
            // given
            val siteModelId = 1
            val siteModel = SiteModel().apply { id = siteModelId }
            val excludedCustomerIds = listOf(1L)

            val customerOne: CustomerDTO = mock()
            val customerTwo: CustomerDTO = mock()
            val response = arrayOf(customerOne, customerTwo)
            whenever(
                restClient.fetchCustomers(
                    siteModel,
                    25,
                    excludedCustomerIds = excludedCustomerIds
                )
            )
                .thenReturn(WooPayload(response))
            val modelOne: WCCustomerModel = mock()
            val modelTwo: WCCustomerModel = mock()
            whenever(mapper.mapToModel(siteModel, customerOne)).thenReturn(modelOne)
            whenever(mapper.mapToModel(siteModel, customerTwo)).thenReturn(modelTwo)

            // when
            val result = store.fetchCustomers(
                siteModel,
                25,
                excludedCustomerIds = excludedCustomerIds
            )

            // then
            assertFalse(result.isError)
            assertEquals(listOf(modelOne, modelTwo), result.model)
            assertTrue(CustomerSqlUtils.getCustomersForSite(siteModel).isEmpty())
        }

    @Test
    fun `fetch customers with error returns error and not cache`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }

        whenever(restClient.fetchCustomers(siteModel, 25)).thenReturn(WooPayload(error))

        // when
        val result = store.fetchCustomers(siteModel, 25)

        // then
        assertTrue(result.isError)
        assertTrue(CustomerSqlUtils.getCustomersForSite(siteModel).isEmpty())
    }

    @Test
    fun `fetch customers by id with one error and one success returns error`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }

        whenever(restClient.fetchCustomers(siteModel, 2, remoteCustomerIds = listOf(1, 2)))
            .thenReturn(WooPayload(error))

        val model = WCCustomerModel().apply {
            remoteCustomerId = 1L
            localSiteId = siteModelId
        }
        val customer: CustomerDTO = mock()
        whenever(mapper.mapToModel(siteModel, customer)).thenReturn(model)
        whenever(restClient.fetchCustomers(siteModel, 2, remoteCustomerIds = listOf(3, 4)))
            .thenReturn(WooPayload(arrayOf(customer)))

        // when
        val result = store.fetchCustomersByIdsAndCache(siteModel, 2, listOf(1, 2, 3, 4))

        // then
        assertTrue(result.isError)
        assertEquals(CustomerSqlUtils.getCustomersForSite(siteModel).size, 1)
    }

    @Test
    fun `fetch customers by id with two success returns success`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }

        val modelOne = WCCustomerModel().apply {
            remoteCustomerId = 1L
            localSiteId = siteModelId
        }
        val customerOne: CustomerDTO = mock()
        whenever(mapper.mapToModel(siteModel, customerOne)).thenReturn(modelOne)

        val modelTwo = WCCustomerModel().apply {
            remoteCustomerId = 2L
            localSiteId = siteModelId
        }
        val customerTwo: CustomerDTO = mock()
        whenever(mapper.mapToModel(siteModel, customerTwo)).thenReturn(modelTwo)

        whenever(restClient.fetchCustomers(siteModel, 2, remoteCustomerIds = listOf(1, 2)))
            .thenReturn(WooPayload(arrayOf(customerOne)))

        whenever(restClient.fetchCustomers(siteModel, 2, remoteCustomerIds = listOf(3, 4)))
            .thenReturn(WooPayload(arrayOf(customerTwo)))

        // when
        val result = store.fetchCustomersByIdsAndCache(siteModel, 2, listOf(1, 2, 3, 4))

        // then
        assertFalse(result.isError)
        val customersForSite = CustomerSqlUtils.getCustomersForSite(siteModel)
        assertEquals(customersForSite.size, 2)
        assertTrue(customersForSite.contains(modelOne))
        assertTrue(customersForSite.contains(modelTwo))
    }

    @Test
    fun `create customer with error returns error`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }
        val customerDto: CustomerDTO = mock()
        val customerModel: WCCustomerModel = mock()

        whenever(mapper.mapToDTO(customerModel)).thenReturn(customerDto)
        whenever(restClient.createCustomer(siteModel, customerDto)).thenReturn(WooPayload(error))

        // when
        val result = store.createCustomer(siteModel, customerModel)

        // then
        assertTrue(result.isError)
    }

    @Test
    fun `create customer with success returns dto`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }
        val customerDto: CustomerDTO = mock()
        val customerDtoResponse: CustomerDTO = mock()
        val customerModel: WCCustomerModel = mock()

        whenever(mapper.mapToDTO(customerModel)).thenReturn(customerDto)
        whenever(mapper.mapToModel(siteModel, customerDtoResponse)).thenReturn(customerModel)
        whenever(restClient.createCustomer(siteModel, customerDto)).thenReturn(
            WooPayload(
                customerDtoResponse
            )
        )

        // when
        val result = store.createCustomer(siteModel, customerModel)

        // then
        assertFalse(result.isError)
        assertEquals(customerModel, result.model)
    }

    @Test
    fun `given error, when fetchCustomersFromAnalytics, then nothing is stored and error`() =
        test {
            // given
            val siteModelId = 1
            val siteModel = SiteModel().apply { id = siteModelId }
            whenever(
                restClient.fetchCustomersFromAnalytics(
                    siteModel,
                    page = 1,
                    pageSize = 25
                )
            ).thenReturn(WooPayload(error))

            // when
            val result = store.fetchCustomersFromAnalytics(siteModel, 1)

            // then
            assertThat(result.isError).isTrue
            assertThat(CustomerSqlUtils.getCustomersForSite(siteModel)).isEmpty()
        }
}
