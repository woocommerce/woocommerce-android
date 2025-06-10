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
    private val analyticsMapper: WCCustomerFromAnalyticsMapper = mock()
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
