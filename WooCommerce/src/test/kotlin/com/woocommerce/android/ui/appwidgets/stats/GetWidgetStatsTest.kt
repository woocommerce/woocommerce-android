package com.woocommerce.android.ui.appwidgets.stats

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.appwidgets.IsDeviceBatterySaverActive
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class GetWidgetStatsTest : BaseUnitTest() {

    private val accountRepository: AccountRepository = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val statsRepository: StatsRepository = mock()
    private val networkStatus: NetworkStatus = mock()
    private val isDeviceBatterySaverActive: IsDeviceBatterySaverActive = mock()

    private val defaultRange = StatsTimeRangeSelection.build(
        selectionType = SelectionType.TODAY,
        referenceDate = Date(),
        calendar = Calendar.getInstance(),
        locale = Locale.getDefault()
    )
    private val defaultSiteModel = SiteModel().apply {
        siteId = 1
        origin = SiteModel.ORIGIN_WPCOM_REST
        setIsJetpackConnected(true)
    }
    private val defaultErrorMessage = "Error fetching site stats for site ${defaultSiteModel.siteId}"
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
        accountRepository = accountRepository,
        appPrefsWrapper = appPrefsWrapper,
        statsRepository = statsRepository,
        networkStatus = networkStatus,
        coroutineDispatchers = coroutinesTestRule.testDispatchers,
        isDeviceBatterySaverActive = isDeviceBatterySaverActive
    )

    @Test
    fun `when the user is NOT logged in then get stats respond with WidgetStatsAuthFailure `() = testBlocking {
        // Given the user is NOT logged in
        whenever(accountRepository.isUserLoggedIn()).thenReturn(false)

        // When GetWidgetStats is invoked
        val result = sut.invoke(defaultRange, defaultSiteModel)

        // Then the result is WidgetStatsAuthFailure
        assertThat(result).isEqualTo(GetWidgetStats.WidgetStatsResult.WidgetStatsAuthFailure)
    }

    @Test
    fun `when the v4 stats is NOT supported then get stats respond with WidgetStatsAPINotSupportedFailure `() =
        testBlocking {
            // Given the user is logged in and v4 stats is not supported
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(appPrefsWrapper.isV4StatsSupported()).thenReturn(false)

            // When GetWidgetStats is invoked
            val result = sut.invoke(defaultRange, defaultSiteModel)

            // Then the result is WidgetStatsAPINotSupportedFailure
            assertThat(result).isEqualTo(GetWidgetStats.WidgetStatsResult.WidgetStatsAPINotSupportedFailure)
        }

    @Test
    fun `when there is no connection then get stats respond with WidgetStatsNetworkFailure `() =
        testBlocking {
            // Given the user is logged, v4 stats is supported and there is no connection
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(appPrefsWrapper.isV4StatsSupported()).thenReturn(true)
            whenever(networkStatus.isConnected()).thenReturn(false)

            // When GetWidgetStats is invoked
            val result = sut.invoke(defaultRange, defaultSiteModel)

            // Then the result is WidgetStatsAPINotSupportedFailure
            assertThat(result).isEqualTo(GetWidgetStats.WidgetStatsResult.WidgetStatsNetworkFailure)
        }

    @Test
    fun `when we don't have information about siteModel then get stats respond with WidgetStatsFailure`() =
        testBlocking {
            // Given the user is logged, v4 stats is supported and network is working fine
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(appPrefsWrapper.isV4StatsSupported()).thenReturn(true)
            whenever(networkStatus.isConnected()).thenReturn(true)

            // When GetWidgetStats is invoked with a null siteModel
            val result = sut.invoke(defaultRange, null)
            val expected = GetWidgetStats.WidgetStatsResult.WidgetStatsFailure("No site selected")

            // Then the result is WidgetStatsFailure
            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `when fetchStats fails then get stats respond with WidgetStatsFailure`() =
        testBlocking {
            // Given the user is logged, v4 stats is supported and network is working fine
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(appPrefsWrapper.isV4StatsSupported()).thenReturn(true)
            whenever(networkStatus.isConnected()).thenReturn(true)

            // Given fetching the stats fails
            whenever(statsRepository.fetchStats(any(), any(), any(), eq(true), eq(true), eq(defaultSiteModel)))
                .thenReturn(Result.failure(Exception(defaultErrorMessage)))

            // When GetWidgetStats is invoked
            val result = sut.invoke(defaultRange, defaultSiteModel)
            val expected = GetWidgetStats.WidgetStatsResult.WidgetStatsFailure(defaultErrorMessage)

            // Then the result is WidgetStatsFailure
            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `when fetchStats succeed then get stats respond with WidgetStats`() =
        testBlocking {
            // Given the user is logged, v4 stats is supported and network is working fine
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(appPrefsWrapper.isV4StatsSupported()).thenReturn(true)
            whenever(networkStatus.isConnected()).thenReturn(true)

            // Given fetching the stats succeed
            whenever(statsRepository.fetchStats(any(), any(), any(), eq(true), eq(true), eq(defaultSiteModel)))
                .thenReturn(Result.success(defaultResponse))

            // When GetWidgetStats is invoked
            val result = sut.invoke(defaultRange, defaultSiteModel)
            val expected = GetWidgetStats.WidgetStatsResult.WidgetStats(defaultResponse)

            // Then the result is WidgetStatsFailure
            assertThat(result).isEqualTo(expected)
        }
}
