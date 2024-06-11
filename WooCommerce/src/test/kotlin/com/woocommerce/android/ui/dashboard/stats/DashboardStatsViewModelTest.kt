package com.woocommerce.android.ui.dashboard.stats

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.DashboardTransactionLauncher
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.data.StatsCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.domain.DashboardDateRangeFormatter
import com.woocommerce.android.ui.dashboard.domain.ObserveLastUpdate
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.TimezoneProvider
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardStatsViewModelTest : BaseUnitTest() {
    companion object {
        val DEFAULT_SELECTION_TYPE = StatsTimeRangeSelection.SelectionType.TODAY
        val ANY_SELECTION_TYPE = StatsTimeRangeSelection.SelectionType.WEEK_TO_DATE
        const val DEFAULT_LAST_UPDATE = 1690382344865L
    }

    private val getStats: GetStats = mock {
        onBlocking { invoke(any(), any()) } doReturn flowOf(GetStats.LoadStatsResult.RevenueStatsSuccess(null))
    }
    private val networkStatus: NetworkStatus = mock {
        on { isConnected() } doReturn true
    }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        val prefsChangesFlow = MutableStateFlow(DEFAULT_SELECTION_TYPE.name)
        on { observePrefs() } doAnswer { prefsChangesFlow.map { Unit } }
        on { getActiveStoreStatsTab() } doAnswer { prefsChangesFlow.value }
        on { setActiveStatsTab(any()) } doAnswer { prefsChangesFlow.value = it.getArgument(0) }
    }
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val dashboardTransactionLauncher: DashboardTransactionLauncher = mock()
    private val customDateRangeDataStore: StatsCustomDateRangeDataStore = mock {
        on { dateRange } doReturn flowOf(null)
    }
    private val timezoneProvider: TimezoneProvider = mock()
    private val observeLastUpdate: ObserveLastUpdate = mock {
        onBlocking { invoke(any(), ArgumentMatchers.anyList()) } doReturn flowOf(DEFAULT_LAST_UPDATE)
    }
    private val dateUtils: DateUtils = mock()
    private val parentViewModel: DashboardViewModel = mock {
        on { refreshTrigger } doReturn emptyFlow()
    }
    private val dateRangeFormatter: DashboardDateRangeFormatter = mock {
        on { formatRangeDate(any()) } doReturn "Jan 1"
    }

    private lateinit var viewModel: DashboardStatsViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        val getSelectedDateRange = GetSelectedRangeForDashboardStats(
            appPrefs = appPrefsWrapper,
            customDateRangeDataStore = customDateRangeDataStore,
            dateUtils = dateUtils
        )

        viewModel = DashboardStatsViewModel(
            savedStateHandle = SavedStateHandle(),
            parentViewModel = parentViewModel,
            selectedSite = selectedSite,
            getStats = getStats,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            dashboardTransactionLauncher = dashboardTransactionLauncher,
            appPrefsWrapper = appPrefsWrapper,
            customDateRangeDataStore = customDateRangeDataStore,
            getSelectedDateRange = getSelectedDateRange,
            networkStatus = networkStatus,
            observeLastUpdate = observeLastUpdate,
            timezoneProvider = timezoneProvider,
            wooCommerceStore = wooCommerceStore,
            dateRangeFormatter = dateRangeFormatter,
            usageTracksEventEmitter = usageTracksEventEmitter,
            dateUtils = dateUtils,
            currencyFormatter = mock()
        )
    }

    @Test
    fun `given there is network connectivity, when view model is created, stats are fetched`() =
        testBlocking {
            setup {
                whenever(networkStatus.isConnected()).thenReturn(true)
            }

            verify(getStats).invoke(refresh = ArgumentMatchers.eq(false), selectedRange = any())
        }

    @Test
    fun `given there is no network, when view model is created, stats are not fetched from API`() =
        testBlocking {
            setup {
                whenever(networkStatus.isConnected()).thenReturn(false)
            }

            verify(getStats, never()).invoke(any(), any())
        }

    @Test
    fun `given there is no network, when tab changed, stats are not fetched from API`() =
        testBlocking {
            setup {
                whenever(networkStatus.isConnected()).thenReturn(false)
            }

            viewModel.onTabSelected(ANY_SELECTION_TYPE)

            verify(getStats, never()).invoke(any(), any())
        }

    @Test
    fun `given cached stats, when tab changes, then load stats for given tab from cache`() = testBlocking {
        val getStatsArgumentCaptor = argumentCaptor<StatsTimeRangeSelection>()
        setup {
            whenever(appPrefsWrapper.getActiveStoreStatsTab())
                .doReturn(DEFAULT_SELECTION_TYPE.name)
                .thenReturn(ANY_SELECTION_TYPE.name)
        }

        viewModel.onTabSelected(ANY_SELECTION_TYPE)

        verify(getStats, times(2)).invoke(
            refresh = ArgumentMatchers.eq(false),
            selectedRange = getStatsArgumentCaptor.capture()
        )
        Assertions.assertThat(getStatsArgumentCaptor.firstValue.selectionType)
            .isEqualTo(DEFAULT_SELECTION_TYPE)
        Assertions.assertThat(getStatsArgumentCaptor.secondValue.selectionType)
            .isEqualTo(ANY_SELECTION_TYPE)
    }

    @Test
    fun `given network connection, when on swipe to refresh, then stats are refreshed for selected range`() =
        testBlocking {
            val refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)
            setup {
                whenever(parentViewModel.refreshTrigger).doReturn(refreshTrigger)
            }

            refreshTrigger.tryEmit(RefreshEvent(isForced = true))

            verify(getStats).invoke(
                refresh = eq(true),
                selectedRange = argThat {
                    selectionType == DEFAULT_SELECTION_TYPE
                }
            )
        }

    @Test
    fun `given success loading revenue, when stats granularity changes, then UI is updated for new selection type`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any()))
                    .thenReturn(flow { emit(GetStats.LoadStatsResult.RevenueStatsSuccess(null)) })
                whenever(appPrefsWrapper.getActiveStoreStatsTab())
                    .doReturn(DEFAULT_SELECTION_TYPE.name)
                    .thenReturn(ANY_SELECTION_TYPE.name)
            }

            viewModel.onTabSelected(ANY_SELECTION_TYPE)

            Assertions.assertThat(viewModel.revenueStatsState.value)
                .isInstanceOf(DashboardStatsViewModel.RevenueStatsViewState.Content::class.java)
            val content = viewModel.revenueStatsState.value as DashboardStatsViewModel.RevenueStatsViewState.Content
            Assertions.assertThat(content.statsRangeSelection.selectionType).isEqualTo(ANY_SELECTION_TYPE)
        }

    @Test
    fun `when stats granularity changes, then selected option is saved into prefs`() =
        testBlocking {
            setup()

            viewModel.onTabSelected(ANY_SELECTION_TYPE)

            verify(appPrefsWrapper).setActiveStatsTab(ANY_SELECTION_TYPE.name)
        }

    @Test
    fun `given error loading revenue, when screen starts, then UI is updated with error`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any()))
                    .thenReturn(flowOf(GetStats.LoadStatsResult.RevenueStatsError))
            }

            Assertions.assertThat(viewModel.revenueStatsState.value)
                .isEqualTo(DashboardStatsViewModel.RevenueStatsViewState.GenericError)
        }

    @Test
    fun `given stats plugin not active, when screen starts, then UI is updated with jetpack error`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any()))
                    .thenReturn(flowOf(GetStats.LoadStatsResult.PluginNotActive))
            }

            Assertions.assertThat(viewModel.revenueStatsState.value).isEqualTo(
                DashboardStatsViewModel.RevenueStatsViewState.PluginNotActiveError
            )
        }

    @Test
    fun `given success loading visitor stats, when screen starts, then UI is updated with visitor stats`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any()))
                    .thenReturn(flowOf(GetStats.LoadStatsResult.VisitorsStatsSuccess(emptyMap(), 0)))
            }

            Assertions.assertThat(viewModel.visitorStatsState.value).isEqualTo(
                DashboardStatsViewModel.VisitorStatsViewState.Content(emptyMap(), 0)
            )
        }

    @Test
    fun `given error loading visitor stats, when screen starts, then UI is updated with error`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any()))
                    .thenReturn(flowOf(GetStats.LoadStatsResult.VisitorsStatsError))
            }

            Assertions.assertThat(viewModel.visitorStatsState.value).isEqualTo(
                DashboardStatsViewModel.VisitorStatsViewState.Error
            )
        }

    @Test
    fun `given jetpack CP connected, when screen starts, then show jetpack CP connected state`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any()))
                    .thenReturn(flowOf(GetStats.LoadStatsResult.VisitorStatUnavailable))
            }

            Assertions.assertThat(viewModel.visitorStatsState.value)
                .isInstanceOf(DashboardStatsViewModel.VisitorStatsViewState.Unavailable::class.java)
        }

    @Test
    fun `when changing tabs, clear selected date`() = testBlocking {
        setup {
            whenever(dateRangeFormatter.formatSelectedDate(any(), argThat { selectionType == DEFAULT_SELECTION_TYPE }))
                .thenReturn("11:00")
        }

        val state = viewModel.dateRangeState.runAndCaptureValues {
            viewModel.onChartDateSelected("11")
            viewModel.onTabSelected(ANY_SELECTION_TYPE)
        }.last()

        verify(dateRangeFormatter, never())
            .formatSelectedDate(eq("11"), argThat { selectionType == ANY_SELECTION_TYPE })
        Assertions.assertThat(state.selectedDateFormatted).isNull()
    }
}
