package org.wordpress.android.fluxc.network.rest.wpcom.wc

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient.GatewayId.CASH_ON_DELIVERY

class GatewayRestClientTest {
    private val wooNetwork: WooNetwork = mock()
    private lateinit var gatewayRestClient: GatewayRestClient

    @Before
    fun setup() {
        gatewayRestClient = GatewayRestClient(wooNetwork)
    }

    @Test
    fun `given success response, when fetch all gateways, then return success`() {
        runBlocking {
            whenever(
                wooNetwork.executeGetGsonRequest(
                    site = any(),
                    path = any(),
                    clazz = any<Class<Array<GatewayRestClient.GatewayResponse>>>(),
                    params = any(),
                    enableCaching = any(),
                    cacheTimeToLive = any(),
                    forced = any(),
                    requestTimeout = any(),
                    retries = any()
                )
            ).thenReturn(
                WPAPIResponse.Success(arrayOf(mock()))
            )

            val actualResponse = gatewayRestClient.fetchAllGateways(SiteModel())

            Assertions.assertThat(actualResponse.isError).isFalse
            Assertions.assertThat(actualResponse.result).isNotNull
        }
    }

    @Test
    fun `given error response, when fetch all gateways, then return error`() {
        runBlocking {
            val expectedError = mock<WPAPINetworkError>().apply {
                type = mock()
            }
            whenever(
                wooNetwork.executeGetGsonRequest(
                    site = any(),
                    path = any(),
                    clazz = any<Class<GatewayRestClient.GatewayResponse>>(),
                    params = any(),
                    enableCaching = any(),
                    cacheTimeToLive = any(),
                    forced = any(),
                    requestTimeout = any(),
                    retries = any()
                )
            ).thenReturn(
                WPAPIResponse.Error(expectedError)
            )

            val actualResponse = gatewayRestClient.fetchAllGateways(SiteModel())

            Assertions.assertThat(actualResponse.isError).isTrue
            Assertions.assertThat(actualResponse.error).isNotNull
        }
    }

    @Test
    fun `given success response, when update gateway, then return success`() {
        runBlocking {
            whenever(
                wooNetwork.executePostGsonRequest(
                    any(),
                    any(),
                    any<Class<GatewayRestClient.GatewayResponse>>(),
                    any()
                )
            ).thenReturn(
                WPAPIResponse.Success(mock())
            )

            val actualResponse = gatewayRestClient.updateGateway(
                SiteModel(),
                CASH_ON_DELIVERY,
                enabled = false,
                title = "title"
            )

            Assertions.assertThat(actualResponse.isError).isFalse
            Assertions.assertThat(actualResponse.result).isNotNull
        }
    }

    @Test
    fun `given error response, when update gateway, then return error`() {
        runBlocking {
            val expectedError = mock<WPAPINetworkError>().apply {
                type = mock()
            }
            whenever(
                wooNetwork.executePostGsonRequest(
                    any(),
                    any(),
                    any<Class<GatewayRestClient.GatewayResponse>>(),
                    any()
                )
            ).thenReturn(
                WPAPIResponse.Error(expectedError)
            )

            val actualResponse = gatewayRestClient.updateGateway(
                SiteModel(),
                CASH_ON_DELIVERY,
                enabled = true,
                title = "title"
            )

            Assertions.assertThat(actualResponse.isError).isTrue
            Assertions.assertThat(actualResponse.error).isNotNull
        }
    }
}
