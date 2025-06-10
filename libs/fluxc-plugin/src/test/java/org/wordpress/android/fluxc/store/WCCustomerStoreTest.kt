package org.wordpress.android.fluxc.store

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerFromAnalyticsMapper
import org.wordpress.android.fluxc.model.customer.WCCustomerMapper
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerDTO
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.CustomerDao
import org.wordpress.android.fluxc.persistence.dao.CustomerFromAnalyticsDao
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCCustomerStoreTest {
    val error = WooError(
        WooErrorType.INVALID_RESPONSE,
        BaseRequest.GenericErrorType.NETWORK_ERROR,
        "Invalid site ID"
    )

    private val restClient: CustomerRestClient = mock()
    private val mapper: WCCustomerMapper = mock()
    private val analyticsMapper: WCCustomerFromAnalyticsMapper = mock()
    private val customerFromAnalyticsDao: CustomerFromAnalyticsDao = mock()

    private lateinit var roomDb: WCAndroidDatabase
    private lateinit var customerDao: CustomerDao

    private lateinit var store: WCCustomerStore

    @Before
    fun setUp() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        roomDb = Room.inMemoryDatabaseBuilder(appContext, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        customerDao = roomDb.customerDao

        store = WCCustomerStore(
            restClient,
            initCoroutineEngine(),
            mapper,
            customerDao,
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
        val model: WCCustomerModel = WCCustomerModel()
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
        runTest {
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
            Assertions.assertThat(result.isError).isTrue
            assertThat(customerDao.getCustomersForSite(siteModel.localId())).isEmpty()
        }
}
