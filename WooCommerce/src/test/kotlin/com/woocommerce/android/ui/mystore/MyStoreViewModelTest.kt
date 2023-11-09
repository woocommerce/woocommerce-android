package com.woocommerce.android.ui.mystore

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.offsetInHours
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.mystore.domain.GetStats
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.TopPerformerProduct
import com.woocommerce.android.ui.mystore.domain.ObserveLastUpdate
import com.woocommerce.android.ui.prefs.privacy.banner.domain.ShouldShowPrivacyBanner
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.TimezoneProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.TimeZone
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class MyStoreViewModelTest : BaseUnitTest() {
    private val savedState = SavedStateHandle()
    private val networkStatus: NetworkStatus = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val getStats: GetStats = mock()
    private val getTopPerformers: GetTopPerformers = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val selectedSite: SelectedSite = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val usageTracksEventEmitter: MyStoreStatsUsageTracksEventEmitter = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val myStoreTransactionLauncher: MyStoreTransactionLauncher = mock()
    private val localNotificationScheduler: LocalNotificationScheduler = mock()
    private val shouldShowPrivacyBanner: ShouldShowPrivacyBanner = mock {
        onBlocking { invoke() } doReturn true
    }
    private val timezoneProvider: TimezoneProvider = mock()
    private val observeLastUpdate: ObserveLastUpdate = mock {
        onBlocking { invoke(any(), anyList()) } doReturn flowOf(DEFAULT_LAST_UPDATE)
    }

    private lateinit var sut: MyStoreViewModel

    @Before
    fun setup() = testBlocking {
        givenStatsLoadingResult(GetStats.LoadStatsResult.VisitorsStatsError)
        givenFetchTopPerformersResult(Result.failure(WooException(WOO_GENERIC_ERROR)))
    }

    @Test
    fun `given there is network connectivity, when view model is created, stats are fetched`() =
        testBlocking {
            givenNetworkConnectivity(connected = true)
            givenObserveTopPerformersEmits(emptyList())
            whenViewModelIsCreated()

            verify(getStats).invoke(refresh = false, DEFAULT_STATS_GRANULARITY)
            verify(getTopPerformers).fetchTopPerformers(
                granularity = DEFAULT_STATS_GRANULARITY,
                refresh = false,
                topPerformersCount = ANY_TOP_PERFORMERS_COUNT
            )
        }

    @Test
    fun `given there is no network, when view model is created, stats are not fetched from API`() =
        testBlocking {
            givenNetworkConnectivity(connected = false)
            givenObserveTopPerformersEmits(emptyList())
            whenViewModelIsCreated()

            verify(getStats, never()).invoke(any(), any())
            verify(getTopPerformers, never()).fetchTopPerformers(any(), any(), any())
        }

    @Test
    fun `given there is no network, when granularity changed, stats are not fetched from API`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = false)
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            verify(getStats, never()).invoke(any(), any())
            verify(getTopPerformers, never()).fetchTopPerformers(any(), any(), any())
        }

    @Test
    fun `given cached stats, when stats granularity changes, then load stats for given granularity from cache`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            verify(getStats).invoke(refresh = false, ANY_SELECTED_STATS_GRANULARITY)
            verify(getTopPerformers).fetchTopPerformers(
                ANY_SELECTED_STATS_GRANULARITY,
                refresh = false,
                ANY_TOP_PERFORMERS_COUNT
            )
        }

    @Test
    fun `given network connection, when on swipe to refresh, then stats are refreshed for selected granularity`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            whenViewModelIsCreated()

            sut.onPullToRefresh()

            verify(getStats).invoke(refresh = true, DEFAULT_STATS_GRANULARITY)
            verify(getTopPerformers).fetchTopPerformers(
                DEFAULT_STATS_GRANULARITY,
                refresh = true,
                ANY_TOP_PERFORMERS_COUNT
            )
        }

    @Test
    fun `given network connection, when on swipe to refresh, then analytics is tracked`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            whenViewModelIsCreated()

            sut.onPullToRefresh()

            verify(analyticsTrackerWrapper).track(AnalyticsEvent.DASHBOARD_PULLED_TO_REFRESH)
        }

    @Test
    fun `given success loading revenue, when stats granularity changes, then UI is updated with revenue stats`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.RevenueStatsSuccess(null))
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.revenueStatsState.value).isEqualTo(
                MyStoreViewModel.RevenueStatsViewState.Content(
                    null,
                    ANY_SELECTED_STATS_GRANULARITY
                )
            )
        }

    @Test
    fun `given success loading revenue, when stats granularity changes, then analytics is tracked`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.RevenueStatsSuccess(null))
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.DASHBOARD_MAIN_STATS_LOADED,
                mapOf(AnalyticsTracker.KEY_RANGE to "weeks")
            )
        }

    @Test
    fun `given stats loaded, when stats granularity changes, then selected option is saved into prefs`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.RevenueStatsSuccess(null))
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            verify(appPrefsWrapper).setActiveStatsGranularity(
                ANY_SELECTED_STATS_GRANULARITY.name
            )
        }

    @Test
    fun `given error loading revenue, when stats granularity changes, then UI is updated with error`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.RevenueStatsError)
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.revenueStatsState.value).isEqualTo(
                MyStoreViewModel.RevenueStatsViewState.GenericError
            )
        }

    @Test
    fun `given jetpack plugin not active, when stats granularity changes, then UI is updated with jetpack error`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.PluginNotActive)
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.revenueStatsState.value).isEqualTo(
                MyStoreViewModel.RevenueStatsViewState.PluginNotActiveError
            )
        }

    @Test
    fun `given success loading visitor stats, when stats granularity changes, then UI is updated with visitor stats`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.VisitorsStatsSuccess(emptyMap()))
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.visitorStatsState.value).isEqualTo(
                MyStoreViewModel.VisitorStatsViewState.Content(emptyMap())
            )
        }

    @Test
    fun `given error loading visitor stats, when stats granularity changes, then UI is updated with error`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.VisitorsStatsError)
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.visitorStatsState.value).isEqualTo(
                MyStoreViewModel.VisitorStatsViewState.Error
            )
        }

    @Test
    fun `given jetpack CP connected, when stats granularity changes, then show jetpack CP connected state`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(
                GetStats.LoadStatsResult.VisitorStatUnavailable(
                    connectionType = SiteConnectionType.JetpackConnectionPackage
                )
            )
            whenever(selectedSite.observe()).thenReturn(flowOf(SiteModel()))
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.visitorStatsState.value)
                .isInstanceOf(MyStoreViewModel.VisitorStatsViewState.Unavailable::class.java)
        }

    @Test
    fun `given store has orders, when stats granularity changes, then UI is updated with has orders state`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.HasOrders(hasOrder = true))
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.hasOrders.value).isEqualTo(
                MyStoreViewModel.OrderState.AtLeastOne
            )
        }

    @Test
    fun `given store has no orders, when stats granularity changes, then UI is updated with no orders state`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.HasOrders(hasOrder = false))
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.hasOrders.value).isEqualTo(
                MyStoreViewModel.OrderState.Empty
            )
        }

    @Test
    fun `given top performers load success, when clicked, then analytics is tracked`() =
        testBlocking {
            givenCurrencyFormatter(TOP_PERFORMER_PRODUCT.total, TOP_PERFORMER_PRODUCT.currency)
            givenResourceProvider()
            givenNetworkConnectivity(connected = true)
            givenFetchTopPerformersResult(Result.success(Unit))
            givenObserveTopPerformersEmits(listOf(TOP_PERFORMER_PRODUCT))

            whenViewModelIsCreated()
            sut.topPerformersState.value!!.topPerformers[0].onClick.invoke(1L)

            verify(analyticsTrackerWrapper).track(AnalyticsEvent.TOP_EARNER_PRODUCT_TAPPED)
        }

    @Test
    fun `given top performers load success, when stats granularity changes, then analytics is tracked`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenFetchTopPerformersResult(Result.success(Unit))
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.DASHBOARD_TOP_PERFORMERS_LOADED,
                mapOf(AnalyticsTracker.KEY_RANGE to "weeks")
            )
        }

    @Test
    fun `given top performers error, when stats granularity changes, then UI is updated with top performers error`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenFetchTopPerformersResult(Result.failure(WooException(WOO_GENERIC_ERROR)))
            whenViewModelIsCreated()

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertTrue(sut.topPerformersState.value!!.isError)
        }

    @Test
    fun `given the viewModel started, when device and store timezones are different, then trigger expected analytics event`() =
        testBlocking {
            // Given
            val testSite = SiteModel().apply {
                timezone = "-3"
            }

            val deviceTimezone = mock<TimeZone> {
                on { rawOffset } doReturn 0
            }

            whenever(selectedSite.getIfExists()) doReturn testSite
            whenever(timezoneProvider.deviceTimezone) doReturn deviceTimezone
            whenever(
                appPrefsWrapper.isTimezoneTrackEventNeverTriggeredFor(any(), any(), any())
            ) doReturn true
            givenObserveTopPerformersEmits(emptyList())

            // When
            whenViewModelIsCreated()

            // Then
            verify(analyticsTrackerWrapper).track(
                stat = AnalyticsEvent.DASHBOARD_STORE_TIMEZONE_DIFFER_FROM_DEVICE,
                properties = mapOf(
                    AnalyticsTracker.KEY_STORE_TIMEZONE to testSite.timezone,
                    AnalyticsTracker.KEY_LOCAL_TIMEZONE to deviceTimezone.offsetInHours.toString()
                )
            )
        }

    @Test
    fun `given the viewModel started, when device and store timezones are the same, then do nothing`() = testBlocking {
        // Given
        val testSite = SiteModel().apply {
            timezone = "0"
        }

        val deviceTimezone = mock<TimeZone> {
            on { rawOffset } doReturn 0
        }

        whenever(selectedSite.getIfExists()) doReturn testSite
        whenever(timezoneProvider.deviceTimezone) doReturn deviceTimezone
        whenever(
            appPrefsWrapper.isTimezoneTrackEventNeverTriggeredFor(any(), any(), any())
        ) doReturn true
        givenObserveTopPerformersEmits(emptyList())

        // When
        whenViewModelIsCreated()

        // Then
        verify(analyticsTrackerWrapper, never()).track(
            stat = AnalyticsEvent.DASHBOARD_STORE_TIMEZONE_DIFFER_FROM_DEVICE,
            properties = mapOf(
                AnalyticsTracker.KEY_STORE_TIMEZONE to testSite.timezone,
                AnalyticsTracker.KEY_LOCAL_TIMEZONE to deviceTimezone.offsetInHours.toString()
            )
        )
    }

    @Test
    fun `given the viewModel started, when timezone track was NOT triggered before, then trigger expected analytics event`() =
        testBlocking {
            // Given
            val testSite = SiteModel().apply {
                timezone = "-3"
                siteId = 7777777
            }

            val deviceTimezone = mock<TimeZone> {
                on { rawOffset } doReturn 0
            }

            whenever(selectedSite.getIfExists()) doReturn testSite
            whenever(timezoneProvider.deviceTimezone) doReturn deviceTimezone
            whenever(
                appPrefsWrapper.isTimezoneTrackEventNeverTriggeredFor(
                    siteId = 7777777,
                    localTimezone = "0",
                    storeTimezone = "-3"
                )
            ) doReturn true
            givenObserveTopPerformersEmits(emptyList())

            // When
            whenViewModelIsCreated()

            // Then
            verify(analyticsTrackerWrapper).track(
                stat = AnalyticsEvent.DASHBOARD_STORE_TIMEZONE_DIFFER_FROM_DEVICE,
                properties = mapOf(
                    AnalyticsTracker.KEY_STORE_TIMEZONE to testSite.timezone,
                    AnalyticsTracker.KEY_LOCAL_TIMEZONE to deviceTimezone.offsetInHours.toString()
                )
            )
        }

    @Test
    fun `given the viewModel started, when timezone track is triggered, then set appPrefs flag`() = testBlocking {
        // Given
        val testSite = SiteModel().apply {
            timezone = "-3"
            siteId = 7777777
        }

        val deviceTimezone = mock<TimeZone> {
            on { rawOffset } doReturn 0
        }

        whenever(selectedSite.getIfExists()) doReturn testSite
        whenever(timezoneProvider.deviceTimezone) doReturn deviceTimezone
        whenever(
            appPrefsWrapper.isTimezoneTrackEventNeverTriggeredFor(
                siteId = 7777777,
                localTimezone = "0",
                storeTimezone = "-3"
            )
        ) doReturn true
        givenObserveTopPerformersEmits(emptyList())

        // When
        whenViewModelIsCreated()

        // Then
        verify(appPrefsWrapper).setTimezoneTrackEventTriggeredFor(
            siteId = 7777777,
            localTimezone = "0",
            storeTimezone = "-3"
        )
    }

    @Test
    fun `given the viewModel started, when timezone track was triggered before, then do nothing`() = testBlocking {
        // Given
        val testSite = SiteModel().apply {
            timezone = "-3"
            siteId = 7777777
        }

        val deviceTimezone = mock<TimeZone> {
            on { rawOffset } doReturn 0
        }

        whenever(selectedSite.getIfExists()) doReturn testSite
        whenever(timezoneProvider.deviceTimezone) doReturn deviceTimezone
        whenever(
            appPrefsWrapper.isTimezoneTrackEventNeverTriggeredFor(
                siteId = 7777777,
                localTimezone = "0",
                storeTimezone = "-3"
            )
        ) doReturn false
        givenObserveTopPerformersEmits(emptyList())

        // When
        whenViewModelIsCreated()

        // Then
        verify(analyticsTrackerWrapper, never()).track(
            stat = AnalyticsEvent.DASHBOARD_STORE_TIMEZONE_DIFFER_FROM_DEVICE,
            properties = mapOf(
                AnalyticsTracker.KEY_STORE_TIMEZONE to testSite.timezone,
                AnalyticsTracker.KEY_LOCAL_TIMEZONE to deviceTimezone.offsetInHours.toString()
            )
        )
    }

    @Test
    fun `given the viewModel started, when the store is null, then do nothing`() = testBlocking {
        // Given
        val testSite = SiteModel().apply {
            timezone = "0"
        }

        val deviceTimezone = mock<TimeZone> {
            on { rawOffset } doReturn 0
        }

        whenever(selectedSite.getIfExists()) doReturn null
        givenObserveTopPerformersEmits(emptyList())

        // When
        whenViewModelIsCreated()

        // Then
        verify(analyticsTrackerWrapper, never()).track(
            stat = AnalyticsEvent.DASHBOARD_STORE_TIMEZONE_DIFFER_FROM_DEVICE,
            properties = mapOf(
                AnalyticsTracker.KEY_STORE_TIMEZONE to testSite.timezone,
                AnalyticsTracker.KEY_LOCAL_TIMEZONE to deviceTimezone.offsetInHours.toString()
            )
        )
    }

    @Test
    fun `given the viewModel started, when the store timezone is null, then do nothing`() = testBlocking {
        // Given
        val testSite = SiteModel().apply {
            timezone = null
        }

        val deviceTimezone = mock<TimeZone> {
            on { rawOffset } doReturn 0
        }

        whenever(selectedSite.getIfExists()) doReturn testSite

        givenObserveTopPerformersEmits(emptyList())

        // When
        whenViewModelIsCreated()

        // Then
        verify(analyticsTrackerWrapper, never()).track(
            stat = AnalyticsEvent.DASHBOARD_STORE_TIMEZONE_DIFFER_FROM_DEVICE,
            properties = mapOf(
                AnalyticsTracker.KEY_STORE_TIMEZONE to testSite.timezone,
                AnalyticsTracker.KEY_LOCAL_TIMEZONE to deviceTimezone.offsetInHours.toString()
            )
        )
    }

    @Test
    fun `test refresh behavior`() = testBlocking {
        // Given
        givenNetworkConnectivity(connected = true)
        givenObserveTopPerformersEmits(emptyList())
        whenViewModelIsCreated()

        // When ViewModel starts refresh is false
        verify(getStats).invoke(refresh = false, DEFAULT_STATS_GRANULARITY)

        sut.onPullToRefresh()

        // When pull-to-refresh refresh is true
        verify(getStats).invoke(refresh = true, DEFAULT_STATS_GRANULARITY)

        sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

        // When granularity changes refresh is false
        verify(getStats).invoke(refresh = false, ANY_SELECTED_STATS_GRANULARITY)
    }

    private suspend fun givenStatsLoadingResult(result: GetStats.LoadStatsResult) {
        whenever(getStats.invoke(any(), any())).thenReturn(flow { emit(result) })
    }

    private suspend fun givenFetchTopPerformersResult(result: Result<Unit>) {
        whenever(
            getTopPerformers.fetchTopPerformers(
                any(),
                anyBoolean(),
                anyInt()
            )
        ).thenReturn(result)
    }

    private fun givenCurrencyFormatter(amount: Double, currency: String) {
        whenever(currencyFormatter.formatCurrency(amount.toBigDecimal(), currency)).thenReturn("1.00")
    }

    private fun givenResourceProvider() {
        whenever(resourceProvider.getString(any(), any())).thenReturn("")
    }

    private fun givenObserveTopPerformersEmits(topPerformers: List<TopPerformerProduct>) {
        whenever(getTopPerformers.observeTopPerformers(any()))
            .thenReturn(
                flow { emit(topPerformers) }
            )
    }

    private fun whenViewModelIsCreated() {
        sut = MyStoreViewModel(
            savedState,
            networkStatus,
            resourceProvider,
            wooCommerceStore,
            getStats,
            getTopPerformers,
            currencyFormatter,
            selectedSite,
            appPrefsWrapper,
            usageTracksEventEmitter,
            analyticsTrackerWrapper,
            myStoreTransactionLauncher,
            timezoneProvider,
            observeLastUpdate,
            localNotificationScheduler,
            shouldShowPrivacyBanner
        )
    }

    private fun givenNetworkConnectivity(connected: Boolean) {
        whenever(networkStatus.isConnected()).thenReturn(connected)
    }

    private companion object {
        val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS
        val ANY_SELECTED_STATS_GRANULARITY = StatsGranularity.WEEKS
        const val ANY_TOP_PERFORMERS_COUNT = 5
        val WOO_GENERIC_ERROR = WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN)
        val TOP_PERFORMER_PRODUCT = TopPerformerProduct(
            productId = 123,
            name = "name",
            quantity = 1,
            currency = "USD",
            total = 1.5,
            imageUrl = null
        )
        const val DEFAULT_LAST_UPDATE = 1690382344865L
    }
}
