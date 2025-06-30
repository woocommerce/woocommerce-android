package org.wordpress.android.fluxc.wc.leaderboards

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsRestClient
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleLeaderboardsApiResponse
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.stubSite

class LeaderboardsRestClientTest {
    private lateinit var restClientUnderTest: LeaderboardsRestClient
    private lateinit var wpApiSuccessResponse: WPAPIResponse.Success<Array<LeaderboardsApiResponse>>
    private lateinit var wpApiErrorResponse: WPAPIResponse.Error<Array<LeaderboardsApiResponse>>

    private val wooNetwork: WooNetwork = mock()

    @Before
    fun setUp() {
        wpApiSuccessResponse = mock()
        wpApiErrorResponse = mock()
        restClientUnderTest = LeaderboardsRestClient(wooNetwork)
    }

    @Test
    fun `fetch leaderboards should call correct request with correct parameters and return expected response`() = test {
        val expectedResult = generateSampleLeaderboardsApiResponse()
        configureSuccessRequest(expectedResult!!)
        val response = restClientUnderTest.fetchLeaderboards(
            site = stubSite,
            startDate = "10-10-2022",
            endDate = "22-10-2022",
            quantity = 5,
            forceRefresh = false,
            interval = "day"
        )

        verify(wooNetwork).executeGetGsonRequest(
            site = stubSite,
            path = WOOCOMMERCE.leaderboards.pathV4Analytics,
            params = mapOf(
                "before" to "22-10-2022",
                "after" to "10-10-2022",
                "per_page" to "5",
                "interval" to "day",
                "force_cache_refresh" to "false",
            ),
            clazz = Array<LeaderboardsApiResponse>::class.java
        )
        assertThat(response).isNotNull
        assertThat(response.result).isNotNull
        assertThat(response.error).isNull()
        assertThat(response.result).isEqualTo(expectedResult)
    }

    @Test
    fun `fetch leaderboards should correctly return failure as WooError`() = test {
        configureErrorRequest()
        val response = restClientUnderTest.fetchLeaderboards(
            site = stubSite,
            startDate = "10-10-2022",
            endDate = "22-10-2022",
            forceRefresh = false,
            quantity = 5,
            interval = "day"
        )

        assertThat(response).isNotNull
        assertThat(response.result).isNull()
        assertThat(response.error).isNotNull
        assertThat(response.error).isExactlyInstanceOf(WooError::class.java)
    }

    private suspend fun configureSuccessRequest(expectedResult: Array<LeaderboardsApiResponse>) {
        whenever(wpApiSuccessResponse.data).thenReturn(expectedResult)
        whenever(
            wooNetwork.executeGetGsonRequest(
                site = stubSite,
                path = WOOCOMMERCE.leaderboards.pathV4Analytics,
                params = mapOf(
                    "after" to "10-10-2022",
                    "before" to "22-10-2022",
                    "per_page" to "5",
                    "interval" to "day",
                    "force_cache_refresh" to "false",
                ),
                clazz = Array<LeaderboardsApiResponse>::class.java
            )
        ).thenReturn(wpApiSuccessResponse)
    }

    private suspend fun configureErrorRequest() {
        whenever(wpApiErrorResponse.error).thenReturn(WPAPINetworkError(BaseNetworkError(NETWORK_ERROR)))
        whenever(
            wooNetwork.executeGetGsonRequest(
                site = stubSite,
                path = WOOCOMMERCE.leaderboards.pathV4Analytics,
                params = mapOf(
                    "after" to "10-10-2022",
                    "before" to "22-10-2022",
                    "per_page" to "5",
                    "interval" to "day",
                    "force_cache_refresh" to "false",
                ),
                clazz = Array<LeaderboardsApiResponse>::class.java
            )
        ).thenReturn(wpApiErrorResponse)
    }
}
