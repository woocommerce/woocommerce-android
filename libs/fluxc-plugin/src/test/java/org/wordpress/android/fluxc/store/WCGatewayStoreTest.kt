package org.wordpress.android.fluxc.store

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.gateways.GatewayMapper
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.persistence.dao.GatewaysDao
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.wc.gateways.GatewayTestFixtures.gatewaysEntities
import org.wordpress.android.fluxc.wc.gateways.GatewayTestFixtures.gatewaysResponse
import org.wordpress.android.fluxc.wc.gateways.GatewayTestFixtures.stubSite

class WCGatewayStoreTest {
    private val restClient = mock<GatewayRestClient>()
    private val errorSite = SiteModel().apply { id = 123 }
    private val mapper = GatewayMapper(Gson())
    private lateinit var store: WCGatewayStore
    private val gatewaysDao: GatewaysDao = mock()

    private val gatewayId = gatewaysResponse.first().gatewayId
    private val error = WooError(WooErrorType.INVALID_ID, BaseRequest.GenericErrorType.NOT_FOUND, "Invalid gateway ID")

    @Before
    fun setUp() {
        store = WCGatewayStore(
            restClient,
            gatewaysDao,
            mapper,
            initCoroutineEngine(),
        )
    }

    @Test
    fun `when fetch all gateways, then works as expected`() = test {
        val result = fetchAllTestGateways()

        assertThat(result.model?.size).isEqualTo(gatewaysResponse.size)
        assertThat(result.model?.first()).isEqualTo(mapper.toModel(gatewaysResponse.first()))

        whenever(restClient.fetchAllGateways(errorSite)).thenReturn(WooPayload(error))
        val invalidRequestResult = store.fetchAllGateways(errorSite)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `when update gateway, then works as expected`() = test {
        fetchAllTestGateways()
        whenever(gatewaysDao.getGateway(stubSite.localId(), gatewayId))
            .thenReturn(gatewaysEntities.first())
        val gateway = store.getGateway(stubSite, gatewayId)
        assertThat(gateway).isEqualTo(mapper.toModel(gatewaysResponse.first()))
        val gatewayIdCod = GatewayRestClient.GatewayId.CASH_ON_DELIVERY
        val updatedGateway = gatewaysResponse.first().copy(enabled = true)
        whenever(restClient.updateGateway(stubSite, gatewayIdCod, true))
            .thenReturn(WooPayload(updatedGateway))

        val model = store.updateGateway(
            site = stubSite,
            gatewayId = gatewayIdCod,
            enabled = true
        )

        verify(gatewaysDao).upsertGateway(mapper.toEntity(stubSite.localId(), updatedGateway))
        assertThat(model).isEqualTo(WooResult(mapper.toModel(updatedGateway)))
    }

    @Test
    fun `when get gateway, then works as expected`() = test {
        whenever(gatewaysDao.getGateway(stubSite.localId(), gatewayId))
            .thenReturn(gatewaysEntities.first())
        whenever(gatewaysDao.getGateway(errorSite.localId(), gatewayId))
            .thenReturn(null)

        val gateway = store.getGateway(stubSite, gatewayId)

        assertThat(gateway).isEqualTo(mapper.toModel(gatewaysResponse.first()))
        val invalidRequestResult = store.getGateway(errorSite, gatewayId)
        assertThat(invalidRequestResult).isNull()
    }

    @Test
    fun `when get all gateways, then works as expected`() = test {
        whenever(gatewaysDao.getGatewaysForSite(stubSite.localId()))
            .thenReturn(gatewaysEntities)
        whenever(gatewaysDao.getGatewaysForSite(errorSite.localId()))
            .thenReturn(emptyList())

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
