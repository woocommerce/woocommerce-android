package com.woocommerce.android.ui.mystore

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.offsetInHours
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.mystore.MyStoreViewModel.RevenueStatsViewState.Content
import com.woocommerce.android.ui.mystore.MyStoreViewModel.RevenueStatsViewState.GenericError
import com.woocommerce.android.ui.mystore.MyStoreViewModel.RevenueStatsViewState.PluginNotActiveError
import com.woocommerce.android.ui.mystore.data.CustomDateRangeDataStore
import com.woocommerce.android.ui.mystore.domain.GetStats
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.TopPerformerProduct
import com.woocommerce.android.ui.mystore.domain.ObserveLastUpdate
import com.woocommerce.android.ui.prefs.privacy.banner.domain.ShouldShowPrivacyBanner
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.TimezoneProvider
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
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
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        on { this.getActiveStatsTab() } doReturn DEFAULT_SELECTION_TYPE.identifier
    }
    private val usageTracksEventEmitter: MyStoreStatsUsageTracksEventEmitter = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val myStoreTransactionLauncher: MyStoreTransactionLauncher = mock()
    private val customDateRangeDataStore: CustomDateRangeDataStore = mock()
    private val shouldShowPrivacyBanner: ShouldShowPrivacyBanner = mock {
        onBlocking { invoke() } doReturn true
    }
    private val timezoneProvider: TimezoneProvider = mock()
    private val observeLastUpdate: ObserveLastUpdate = mock {
        onBlocking { invoke(any(), anyList()) } doReturn flowOf(DEFAULT_LAST_UPDATE)
        onBlocking { invoke(any(), any<AnalyticsUpdateDataStore.AnalyticData>()) } doReturn flowOf(DEFAULT_LAST_UPDATE)
    }
    private val dateUtils: DateUtils = mock()

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

            verify(getStats).invoke(refresh = eq(false), selectedRange = any())
            verify(getTopPerformers).fetchTopPerformers(
                selectedRange = any(),
                refresh = eq(false),
                topPerformersCount = any()
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
    fun `given there is no network, when tab changed, stats are not fetched from API`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = false)
            whenViewModelIsCreated()

            sut.onTabSelected(ANY_SELECTION_TYPE)

            verify(getStats, never()).invoke(any(), any())
            verify(getTopPerformers, never()).fetchTopPerformers(any(), any(), any())
        }

    @Test
    fun `given cached stats, when tab changes, then load stats for given tab from cache`() =
        testBlocking {
            val getStatsArgumentCaptor = argumentCaptor<StatsTimeRangeSelection>()
            val topPerformersArgumentCaptor = argumentCaptor<StatsTimeRangeSelection>()
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            whenViewModelIsCreated()

            sut.onTabSelected(ANY_SELECTION_TYPE)

            verify(getStats, times(2)).invoke(
                refresh = eq(false),
                selectedRange = getStatsArgumentCaptor.capture()
            )
            assertThat(getStatsArgumentCaptor.firstValue.selectionType).isEqualTo(DEFAULT_SELECTION_TYPE)
            assertThat(getStatsArgumentCaptor.secondValue.selectionType).isEqualTo(ANY_SELECTION_TYPE)
            verify(getTopPerformers, times(2)).fetchTopPerformers(
                selectedRange = topPerformersArgumentCaptor.capture(),
                refresh = eq(false),
                topPerformersCount = any()
            )
            assertThat(topPerformersArgumentCaptor.firstValue.selectionType).isEqualTo(DEFAULT_SELECTION_TYPE)
            assertThat(topPerformersArgumentCaptor.secondValue.selectionType).isEqualTo(ANY_SELECTION_TYPE)
        }

    @Test
    fun `given network connection, when on swipe to refresh, then stats are refreshed for selected range`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            whenViewModelIsCreated()

            sut.onPullToRefresh()

            verify(getStats).invoke(refresh = eq(true), selectedRange = any())
            verify(getTopPerformers).fetchTopPerformers(
                selectedRange = any(),
                refresh = eq(true),
                eq(ANY_TOP_PERFORMERS_COUNT)
            )
            assertThat(sut.selectedDateRange.getOrAwaitValue().selectionType)
                .isEqualTo(DEFAULT_STATS_RANGE_SELECTION.selectionType)
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
    fun `given success loading revenue, when stats granularity changes, then UI is updated for new selection type`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.RevenueStatsSuccess(null))
            whenViewModelIsCreated()

            sut.onTabSelected(ANY_SELECTION_TYPE)

            assertThat((sut.revenueStatsState.value as Content).statsRangeSelection.selectionType).isEqualTo(
                ANY_SELECTION_TYPE
            )
        }

    @Test
    fun `given success loading revenue, when stats granularity changes, then analytics is tracked`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.RevenueStatsSuccess(null))
            whenViewModelIsCreated()

            sut.onTabSelected(ANY_SELECTION_TYPE)

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.DASHBOARD_MAIN_STATS_LOADED,
                mapOf(AnalyticsTracker.KEY_RANGE to ANY_SELECTION_TYPE.identifier)
            )
        }

    @Test
    fun `given stats loaded, when stats granularity changes, then selected option is saved into prefs`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.RevenueStatsSuccess(null))
            whenViewModelIsCreated()

            sut.onTabSelected(ANY_SELECTION_TYPE)

            verify(appPrefsWrapper).setActiveStatsTab(
                ANY_SELECTION_TYPE.name
            )
        }

    @Test
    fun `given error loading revenue, when stats granularity changes, then UI is updated with error`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.RevenueStatsError)
            whenViewModelIsCreated()

            sut.onTabSelected(ANY_SELECTION_TYPE)

            assertThat(sut.revenueStatsState.value).isEqualTo(
                GenericError
            )
        }

    @Test
    fun `given jetpack plugin not active, when stats granularity changes, then UI is updated with jetpack error`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.PluginNotActive)
            whenViewModelIsCreated()

            sut.onTabSelected(ANY_SELECTION_TYPE)

            assertThat(sut.revenueStatsState.value).isEqualTo(
                PluginNotActiveError
            )
        }

    @Test
    fun `given success loading visitor stats, when stats granularity changes, then UI is updated with visitor stats`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.VisitorsStatsSuccess(emptyMap()))
            whenViewModelIsCreated()

            sut.onTabSelected(ANY_SELECTION_TYPE)

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

            sut.onTabSelected(ANY_SELECTION_TYPE)

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

            sut.onTabSelected(ANY_SELECTION_TYPE)

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

            sut.onTabSelected(ANY_SELECTION_TYPE)

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

            sut.onTabSelected(ANY_SELECTION_TYPE)

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

            sut.onTabSelected(ANY_SELECTION_TYPE)

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.DASHBOARD_TOP_PERFORMERS_LOADED,
                mapOf(AnalyticsTracker.KEY_RANGE to ANY_SELECTION_TYPE.identifier)
            )
        }

    @Test
    fun `given top performers error, when stats granularity changes, then UI is updated with top performers error`() =
        testBlocking {
            givenObserveTopPerformersEmits(emptyList())
            givenNetworkConnectivity(connected = true)
            givenFetchTopPerformersResult(Result.failure(WooException(WOO_GENERIC_ERROR)))
            whenViewModelIsCreated()

            sut.onTabSelected(ANY_SELECTION_TYPE)

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
        verify(getStats).invoke(refresh = eq(false), any())

        sut.onPullToRefresh()

        // When pull-to-refresh refresh is true
        verify(getStats).invoke(refresh = eq(true), any())

        sut.onTabSelected(DEFAULT_SELECTION_TYPE)

        // When granularity changes refresh is false
        verify(getStats).invoke(refresh = eq(false), any())
    }

    @Test
    fun `given no saved custom range, when view model is created, then start with null`() = testBlocking {
        whenever(customDateRangeDataStore.dateRange) doReturn flowOf(null)
        givenObserveTopPerformersEmits(emptyList())
        givenNetworkConnectivity(connected = true)

        whenViewModelIsCreated()

        assertThat(sut.customRange.getOrAwaitValue()).isNull()
    }

    @Test
    fun `given a saved custom range, when view model is created, then start with`() = testBlocking {
        val customRange = StatsTimeRange(Date(), Date())
        whenever(customDateRangeDataStore.dateRange) doReturn flowOf(customRange)
        givenObserveTopPerformersEmits(emptyList())
        givenNetworkConnectivity(connected = true)

        whenViewModelIsCreated()

        assertThat(sut.customRange.getOrAwaitValue()).isEqualTo(customRange)
    }

    @Test
    fun `given no saved custom range, when a custom range is added, then switch to the custom tab`() = testBlocking {
        whenever(customDateRangeDataStore.dateRange) doReturn flowOf(null)
        givenObserveTopPerformersEmits(emptyList())
        givenNetworkConnectivity(connected = true)

        whenViewModelIsCreated()
        val selectedRange = sut.selectedDateRange.runAndCaptureValues {
            sut.onCustomRangeSelected(StatsTimeRange(Date(), Date()))
        }.last()

        assertThat(selectedRange.selectionType).isEqualTo(StatsTimeRangeSelection.SelectionType.CUSTOM)
    }

    @Test
    fun `given no saved custom range, when a custom range is added, then save it`() = testBlocking {
        whenever(customDateRangeDataStore.dateRange) doReturn flowOf(null)
        givenObserveTopPerformersEmits(emptyList())
        givenNetworkConnectivity(connected = true)

        whenViewModelIsCreated()
        val customRange = StatsTimeRange(Date(), Date())
        sut.onCustomRangeSelected(customRange)

        verify(customDateRangeDataStore).updateDateRange(customRange)
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
            customDateRangeDataStore,
            dateUtils,
            shouldShowPrivacyBanner,
        )
    }

    private fun givenNetworkConnectivity(connected: Boolean) {
        whenever(networkStatus.isConnected()).thenReturn(connected)
    }

    private companion object {
        val DEFAULT_SELECTION_TYPE = StatsTimeRangeSelection.SelectionType.TODAY
        val DEFAULT_STATS_RANGE_SELECTION = StatsTimeRangeSelection.build(
            selectionType = DEFAULT_SELECTION_TYPE,
            referenceDate = Date(),
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val ANY_SELECTION_TYPE = StatsTimeRangeSelection.SelectionType.WEEK_TO_DATE
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
