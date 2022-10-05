package com.woocommerce.android.ui.appwidgets.stats

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WCStatsStore

@OptIn(ExperimentalCoroutinesApi::class)
class GetWidgetStatsTest : BaseUnitTest() {

    private val accountStore: AccountStore = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val statsRepository: StatsRepository = mock()
    private val networkStatus: NetworkStatus = mock()

    private val defaultGranularity = WCStatsStore.StatsGranularity.DAYS
    private val defaultSiteModel = SiteModel().apply { siteId = 1 }
    private val defaultError = WooError(
        type = WooErrorType.GENERIC_ERROR,
        original = BaseRequest.GenericErrorType.UNKNOWN,
        message = "Error fetching site stats for site ${defaultSiteModel.siteId}"
    )
    private val defaultResponse = StatsRepository.SiteStats(
        visitors = mapOf(
            "2020-10-01" to 1,
            "2020-11-01" to 3,
            "2020-12-01" to 4
        ),
        revenue = WCRevenueStatsModel(),
        currencyCode = "USD"
    )

    private val sut = GetWidgetStats(
        accountStore = accountStore,
        appPrefsWrapper = appPrefsWrapper,
        statsRepository = statsRepository,
        networkStatus = networkStatus,
        coroutineDispatchers = coroutinesTestRule.testDispatchers
    )

    @Test
    fun `when the user is NOT logged in then get stats respond with WidgetStatsAuthFailure `() = testBlocking {
        // Given the user is NOT logged in
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        // When GetWidgetStats is invoked
        val result = sut.invoke(defaultGranularity, defaultSiteModel)

        // Then the result is WidgetStatsAuthFailure
        assertThat(result).isEqualTo(GetWidgetStats.WidgetStatsResult.WidgetStatsAuthFailure)
    }

    @Test
    fun `when the v4 stats is NOT supported then get stats respond with WidgetStatsAPINotSupportedFailure `() =
        testBlocking {
            // Given the user is logged in and v4 stats is not supported
            whenever(accountStore.hasAccessToken()).thenReturn(true)
            whenever(appPrefsWrapper.isV4StatsSupported()).thenReturn(false)

            // When GetWidgetStats is invoked
            val result = sut.invoke(defaultGranularity, defaultSiteModel)

            // Then the result is WidgetStatsAPINotSupportedFailure
            assertThat(result).isEqualTo(GetWidgetStats.WidgetStatsResult.WidgetStatsAPINotSupportedFailure)
        }

    @Test
    fun `when there is no connection then get stats respond with WidgetStatsNetworkFailure `() =
        testBlocking {
            // Given the user is logged, v4 stats is supported and there is no connection
            whenever(accountStore.hasAccessToken()).thenReturn(true)
            whenever(appPrefsWrapper.isV4StatsSupported()).thenReturn(true)
            whenever(networkStatus.isConnected()).thenReturn(false)

            // When GetWidgetStats is invoked
            val result = sut.invoke(defaultGranularity, defaultSiteModel)

            // Then the result is WidgetStatsAPINotSupportedFailure
            assertThat(result).isEqualTo(GetWidgetStats.WidgetStatsResult.WidgetStatsNetworkFailure)
        }

    @Test
    fun `when we don't have information about siteModel then get stats respond with WidgetStatsFailure`() =
        testBlocking {
            // Given the user is logged, v4 stats is supported and network is working fine
            whenever(accountStore.hasAccessToken()).thenReturn(true)
            whenever(appPrefsWrapper.isV4StatsSupported()).thenReturn(true)
            whenever(networkStatus.isConnected()).thenReturn(true)

            // When GetWidgetStats is invoked with a null siteModel
            val result = sut.invoke(defaultGranularity, null)
            val expected = GetWidgetStats.WidgetStatsResult.WidgetStatsFailure("No site selected")

            // Then the result is WidgetStatsFailure
            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `when fetchStats fails then get stats respond with WidgetStatsFailure`() =
        testBlocking {
            // Given the user is logged, v4 stats is supported and network is working fine
            whenever(accountStore.hasAccessToken()).thenReturn(true)
            whenever(appPrefsWrapper.isV4StatsSupported()).thenReturn(true)
            whenever(networkStatus.isConnected()).thenReturn(true)

            // Given fetching the stats fails
            whenever(statsRepository.fetchStats(defaultGranularity, true, defaultSiteModel))
                .thenReturn(WooResult(defaultError))

            // When GetWidgetStats is invoked
            val result = sut.invoke(defaultGranularity, defaultSiteModel)
            val expected = GetWidgetStats.WidgetStatsResult.WidgetStatsFailure(defaultError.message)

            // Then the result is WidgetStatsFailure
            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `when fetchStats succeed then get stats respond with WidgetStatsFailure`() =
        testBlocking {
            // Given the user is logged, v4 stats is supported and network is working fine
            whenever(accountStore.hasAccessToken()).thenReturn(true)
            whenever(appPrefsWrapper.isV4StatsSupported()).thenReturn(true)
            whenever(networkStatus.isConnected()).thenReturn(true)

            // Given fetching the stats succeed
            whenever(statsRepository.fetchStats(defaultGranularity, true, defaultSiteModel))
                .thenReturn(WooResult(defaultResponse))

            // When GetWidgetStats is invoked
            val result = sut.invoke(defaultGranularity, defaultSiteModel)
            val expected = GetWidgetStats.WidgetStatsResult.WidgetStats(defaultResponse)

            // Then the result is WidgetStatsFailure
            assertThat(result).isEqualTo(expected)
        }
}
