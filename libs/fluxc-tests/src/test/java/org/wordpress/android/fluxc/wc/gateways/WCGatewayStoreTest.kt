package org.wordpress.android.fluxc.wc.gateways

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.gateways.GatewayMapper
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_ID
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.persistence.WCGatewaySqlUtils.GatewaysTable
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCGatewayStoreTest {
    private val restClient = mock<GatewayRestClient>()
    private val site = SiteModel().apply { id = 321 }
    private val errorSite = SiteModel().apply { id = 123 }
    private val mapper = GatewayMapper()
    private lateinit var store: WCGatewayStore

    private val gatewayId = GATEWAYS_RESPONSE.first().gatewayId
    private val error = WooError(INVALID_ID, NOT_FOUND, "Invalid gateway ID")

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(GatewaysTable::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        store = WCGatewayStore(
                restClient,
                initCoroutineEngine(),
                mapper
        )
    }

    @Test
    fun `fetch all gateways`() = test {
        val result = fetchAllTestGateways()

        assertThat(result.model?.size).isEqualTo(GATEWAYS_RESPONSE.size)
        assertThat(result.model?.first()).isEqualTo(mapper.map(GATEWAYS_RESPONSE.first()))

        whenever(restClient.fetchAllGateways(errorSite)).thenReturn(WooPayload(error))
        val invalidRequestResult = store.fetchAllGateways(errorSite)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `get all gateways`() = test {
        fetchAllTestGateways()

        val gateways = store.getAllGateways(site)

        assertThat(gateways.size).isEqualTo(2)
        assertThat(gateways.first()).isEqualTo(mapper.map(GATEWAYS_RESPONSE.first()))

        val invalidRequestResult = store.getAllGateways(errorSite)
        assertThat(invalidRequestResult.size).isEqualTo(0)
    }

    @Test
    fun `fetch specific gateway`() = test {
        val gateway = fetchSpecificTestGateway()

        assertThat(gateway.model).isEqualTo(mapper.map(GATEWAYS_RESPONSE.first()))

        whenever(restClient.fetchGateway(errorSite, gatewayId)).thenReturn(WooPayload(error))
    }

    @Test
    fun `get specific gateway`() = test {
        fetchSpecificTestGateway()
        val gateway = store.getGateway(site, gatewayId)

        assertThat(gateway).isEqualTo(mapper.map(GATEWAYS_RESPONSE.first()))
        val invalidRequestResult = store.getGateway(errorSite, gatewayId)
        assertThat(invalidRequestResult).isNull()
    }

    private suspend fun fetchSpecificTestGateway(): WooResult<WCGatewayModel> {
        val fetchGatewaysPayload = WooPayload(GATEWAYS_RESPONSE.first())
        whenever(restClient.fetchGateway(site, gatewayId)).thenReturn(fetchGatewaysPayload)
        return store.fetchGateway(site, gatewayId)
    }

    private suspend fun fetchAllTestGateways(): WooResult<List<WCGatewayModel>> {
        val fetchGatewaysPayload = WooPayload(GATEWAYS_RESPONSE.toTypedArray())
        whenever(restClient.fetchAllGateways(site)).thenReturn(fetchGatewaysPayload)
        return store.fetchAllGateways(site)
    }
}
