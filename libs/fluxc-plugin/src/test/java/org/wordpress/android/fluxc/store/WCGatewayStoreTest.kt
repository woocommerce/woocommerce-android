package org.wordpress.android.fluxc.store

import com.google.gson.Gson
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
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.persistence.WCGatewaySqlUtils.GatewaysTable
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.wc.gateways.GatewayTestFixtures.gatewaysResponse
import org.wordpress.android.fluxc.wc.gateways.GatewayTestFixtures.stubSite

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCGatewayStoreTest {
    private val restClient = mock<GatewayRestClient>()
    private val errorSite = SiteModel().apply { id = 123 }
    private val mapper = GatewayMapper(Gson())
    private lateinit var store: WCGatewayStore
    private val gatewayId = gatewaysResponse.first().gatewayId
    private val error = WooError(WooErrorType.INVALID_ID, BaseRequest.GenericErrorType.NOT_FOUND, "Invalid gateway ID")

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
            appContext,
            listOf(GatewaysTable::class.java),
            WellSqlConfig.Companion.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        store = WCGatewayStore(
            restClient,
            mapper,
            initCoroutineEngine()
        )
    }

    @Test
    fun `fetch all gateways`() = test {
        val result = fetchAllTestGateways()

        assertThat(result.model?.size).isEqualTo(gatewaysResponse.size)
        assertThat(result.model?.first()).isEqualTo(mapper.toModel(gatewaysResponse.first()))

        whenever(restClient.fetchAllGateways(errorSite)).thenReturn(WooPayload(error))
        val invalidRequestResult = store.fetchAllGateways(errorSite)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `update gateway`() = test {
        fetchAllTestGateways()
        val gateway = store.getGateway(stubSite, gatewayId)
        assertThat(gateway).isEqualTo(mapper.toModel(gatewaysResponse.first()))
        val gatewayIdCod = GatewayRestClient.GatewayId.CASH_ON_DELIVERY
        val updatedGateway = gatewaysResponse.first().copy(enabled = true)
        whenever(restClient.updateGateway(stubSite, gatewayIdCod, true))
            .thenReturn(WooPayload(updatedGateway))

        store.updateGateway(
            site = stubSite,
            gatewayId = gatewayIdCod,
            enabled = true
        )

        assertThat(store.getGateway(stubSite, gatewayId)).isEqualTo(mapper.toModel(updatedGateway))
    }

    @Test
    fun `get gateway`() = test {
        fetchAllTestGateways()

        val gateway = store.getGateway(stubSite, gatewayId)

        assertThat(gateway).isEqualTo(mapper.toModel(gatewaysResponse.first()))
    }

    @Test
    fun `get all gateways`() = test {
        fetchAllTestGateways()

        val gateways = store.getAllGateways(stubSite)

        assertThat(gateways.size).isEqualTo(2)
        assertThat(gateways.first()).isEqualTo(mapper.toModel(gatewaysResponse.first()))

        val invalidRequestResult = store.getAllGateways(errorSite)
        assertThat(invalidRequestResult.size).isEqualTo(0)
    }

    private suspend fun fetchAllTestGateways(): WooResult<List<WCGatewayModel>> {
        val fetchGatewaysPayload = WooPayload(gatewaysResponse.toTypedArray())
        whenever(restClient.fetchAllGateways(stubSite)).thenReturn(fetchGatewaysPayload)
        return store.fetchAllGateways(stubSite)
    }
}
