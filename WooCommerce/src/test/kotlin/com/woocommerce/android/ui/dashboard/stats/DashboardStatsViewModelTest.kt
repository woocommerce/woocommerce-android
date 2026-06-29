package com.woocommerce.android.ui.dashboard.stats

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.DashboardTransactionLauncher
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.data.StatsCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.ui.dashboard.domain.DashboardDateRangeFormatter
import com.woocommerce.android.ui.dashboard.domain.ObserveLastUpdate
import com.woocommerce.android.util.CalendarHelper
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.TimezoneProvider
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.settings.WCAnalyticsOrderDateType
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardStatsViewModelTest : BaseUnitTest() {
    companion object {
        val DEFAULT_SELECTION_TYPE = StatsTimeRangeSelection.SelectionType.TODAY
        val ANY_SELECTION_TYPE = StatsTimeRangeSelection.SelectionType.WEEK_TO_DATE
        const val DEFAULT_LAST_UPDATE = 1690382344865L
        val ANY_REVENUE_STATS = WCRevenueStatsModel(LocalId(1), "", "", "", "", "", "")
    }

    private val getStats: GetStats = mock {
        on { invoke(any(), any(), anyOrNull()) } doReturn flowOf(GetStats.LoadStatsResult.RevenueStatsSuccess(null))
    }
    private val statsRepository: StatsRepository = mock()
    private val networkStatus: NetworkStatus = mock {
        on { isConnected() } doReturn true
    }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        val prefsChangesFlow = MutableStateFlow(DEFAULT_SELECTION_TYPE.name)
        val revenueStatsType = MutableStateFlow(DashboardStatsViewModel.RevenueStatsType.TOTAL.name)
        on { observePrefs() } doAnswer { prefsChangesFlow.map { Unit } }
        on { getActiveStoreStatsTab() } doAnswer { prefsChangesFlow.value }
        on { setActiveStatsTab(any()) } doAnswer { prefsChangesFlow.value = it.getArgument(0) }
        on { getDashboardRevenueStatsType() } doAnswer { revenueStatsType.value }
        on { setDashboardRevenueStatsType(any()) } doAnswer {
            revenueStatsType.value = it.getArgument(0)
        }
    }
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val dashboardTransactionLauncher: DashboardTransactionLauncher = mock()
    private val customDateRangeDataStore: StatsCustomDateRangeDataStore = mock {
        on { dateRange } doReturn flowOf(null)
    }
    private val timezoneProvider: TimezoneProvider = mock()
    private val observeLastUpdate: ObserveLastUpdate = mock {
        on { invoke(any(), ArgumentMatchers.anyList(), eq(false)) } doReturn flowOf(DEFAULT_LAST_UPDATE)
    }
    private val dateUtils: DateUtils = mock()
    private val parentViewModel: DashboardViewModel = mock {
        on { refreshTrigger } doReturn emptyFlow()
    }
    private val dateRangeFormatter: DashboardDateRangeFormatter = mock {
        on { formatRangeDate(any()) } doReturn "Jan 1"
    }
    private val calendarHelper: CalendarHelper = mock()

    private lateinit var viewModel: DashboardStatsViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        whenever(statsRepository.fetchAnalyticsOrderDateType())
            .thenReturn(Result.success(WCAnalyticsOrderDateType.PAID))
        whenever(statsRepository.updateAnalyticsOrderDateType(any()))
            .thenReturn(Result.success(WCAnalyticsOrderDateType.PAID))
        prepareMocks()
        val getSelectedDateRange = GetSelectedRangeForDashboardStats(
            appPrefs = appPrefsWrapper,
            customDateRangeDataStore = customDateRangeDataStore,
            dateUtils = dateUtils,
            calendarHelper = calendarHelper
        )

        viewModel = DashboardStatsViewModel(
            savedStateHandle = SavedStateHandle(),
            parentViewModel = parentViewModel,
            selectedSite = selectedSite,
            getStats = getStats,
            statsRepository = statsRepository,
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

            verify(getStats).invoke(
                refresh = ArgumentMatchers.eq(false),
                selectedRange = any(),
                orderDateType = eq(WCAnalyticsOrderDateType.PAID)
            )
        }

    @Test
    fun `given there is no network, when view model is created, stats are not fetched from API`() =
        testBlocking {
            setup {
                whenever(networkStatus.isConnected()).thenReturn(false)
            }

            verify(getStats, never()).invoke(any(), any(), anyOrNull())
        }

    @Test
    fun `given there is no network, when tab changed, stats are not fetched from API`() =
        testBlocking {
            setup {
                whenever(networkStatus.isConnected()).thenReturn(false)
            }

            viewModel.onRangeChanged(ANY_SELECTION_TYPE)

            verify(getStats, never()).invoke(any(), any(), anyOrNull())
        }

    @Test
    fun `given cached stats, when tab changes, then load stats for given tab from cache`() = testBlocking {
        val getStatsArgumentCaptor = argumentCaptor<StatsTimeRangeSelection>()
        setup {
            whenever(appPrefsWrapper.getActiveStoreStatsTab())
                .doReturn(DEFAULT_SELECTION_TYPE.name)
                .thenReturn(ANY_SELECTION_TYPE.name)
        }

        viewModel.onRangeChanged(ANY_SELECTION_TYPE)

        verify(getStats, times(2)).invoke(
            refresh = ArgumentMatchers.eq(false),
            selectedRange = getStatsArgumentCaptor.capture(),
            orderDateType = eq(WCAnalyticsOrderDateType.PAID)
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
                },
                orderDateType = eq(WCAnalyticsOrderDateType.PAID)
            )
        }

    @Test
    fun `given success loading revenue, when stats granularity changes, then UI is updated for new selection type`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(flow { emit(GetStats.LoadStatsResult.RevenueStatsSuccess(null)) })
                whenever(appPrefsWrapper.getActiveStoreStatsTab())
                    .doReturn(DEFAULT_SELECTION_TYPE.name)
                    .thenReturn(ANY_SELECTION_TYPE.name)
            }

            viewModel.onRangeChanged(ANY_SELECTION_TYPE)

            Assertions.assertThat(viewModel.revenueStatsState.value)
                .isInstanceOf(DashboardStatsViewModel.RevenueStatsViewState.Content::class.java)
            val content = viewModel.revenueStatsState.value as DashboardStatsViewModel.RevenueStatsViewState.Content
            Assertions.assertThat(content.statsRangeSelection.selectionType).isEqualTo(ANY_SELECTION_TYPE)
        }

    @Test
    fun `given revenue stats with all sales types, when screen starts, then UI model exposes all sales types`() =
        testBlocking {
            val revenueStats = WCRevenueStatsModel(
                localSiteId = LocalId(1),
                interval = "",
                startDate = "",
                endDate = "",
                data = """
                    [
                        {
                            "interval": "2026-04-27",
                            "subtotals": {
                                "orders_count": 3,
                                "gross_sales": 45.25,
                                "net_revenue": 30.15,
                                "total_sales": 50.35
                            }
                        }
                    ]
                """.trimIndent(),
                total = """
                    {
                        "orders_count": 6,
                        "gross_sales": 150.25,
                        "net_revenue": 120.15,
                        "total_sales": 170.35
                    }
                """.trimIndent(),
                rangeId = "",
            )
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(flowOf(GetStats.LoadStatsResult.RevenueStatsSuccess(revenueStats)))
            }

            val content = viewModel.revenueStatsState.value as DashboardStatsViewModel.RevenueStatsViewState.Content
            val uiModel = content.revenueStats!!

            Assertions.assertThat(uiModel.grossSales).isEqualTo(150.25)
            Assertions.assertThat(uiModel.netSales).isEqualTo(120.15)
            Assertions.assertThat(uiModel.totalSales).isEqualTo(170.35)
            Assertions.assertThat(uiModel.intervalList.first().grossSales).isEqualTo(45.25)
            Assertions.assertThat(uiModel.intervalList.first().netSales).isEqualTo(30.15)
            Assertions.assertThat(uiModel.intervalList.first().sales).isEqualTo(50.35)
        }

    @Test
    fun `when revenue stats type changes, then selected type is updated and tracked`() =
        testBlocking {
            setup()

            viewModel.onRevenueStatsTypeSelected(DashboardStatsViewModel.RevenueStatsType.NET)

            Assertions.assertThat(viewModel.selectedRevenueStatsType.value)
                .isEqualTo(DashboardStatsViewModel.RevenueStatsType.NET)
            verify(appPrefsWrapper).setDashboardRevenueStatsType(DashboardStatsViewModel.RevenueStatsType.NET.name)
            verify(usageTracksEventEmitter).interacted(any())
            verify(parentViewModel).trackCardInteracted("performance")
            verify(analyticsTrackerWrapper).track(
                stat = eq(AnalyticsEvent.DASHBOARD_STATS_REVENUE_TYPE_SELECTED),
                properties = argThat {
                    this[AnalyticsTracker.KEY_OPTION] == "net" &&
                        this[AnalyticsTracker.KEY_TYPE] == "performance"
                }
            )
        }

    @Test
    fun `given saved revenue stats type, when screen starts, then selected type is restored`() =
        testBlocking {
            setup {
                whenever(appPrefsWrapper.getDashboardRevenueStatsType())
                    .thenReturn(DashboardStatsViewModel.RevenueStatsType.GROSS.name)
            }

            Assertions.assertThat(viewModel.selectedRevenueStatsType.value)
                .isEqualTo(DashboardStatsViewModel.RevenueStatsType.GROSS)
        }

    @Test
    fun `given invalid saved revenue stats type, when screen starts, then selected type defaults to total`() =
        testBlocking {
            setup {
                whenever(appPrefsWrapper.getDashboardRevenueStatsType()).thenReturn("UNKNOWN")
            }

            Assertions.assertThat(viewModel.selectedRevenueStatsType.value)
                .isEqualTo(DashboardStatsViewModel.RevenueStatsType.TOTAL)
        }

    @Test
    fun `given saved order date type, when screen starts, then selected order date type is updated`() =
        testBlocking {
            setup {
                whenever(statsRepository.fetchAnalyticsOrderDateType())
                    .thenReturn(Result.success(WCAnalyticsOrderDateType.CREATED))
            }

            Assertions.assertThat(viewModel.orderDateTypeState.value.selectedType)
                .isEqualTo(WCAnalyticsOrderDateType.CREATED)
            verify(getStats).invoke(
                refresh = eq(true),
                selectedRange = argThat {
                    selectionType == DEFAULT_SELECTION_TYPE
                },
                orderDateType = eq(WCAnalyticsOrderDateType.CREATED)
            )
        }

    @Test
    fun `when order date type selector is tapped, then interaction is tracked`() =
        testBlocking {
            setup()

            viewModel.onOrderDateTypeSelectorTapped()

            verify(usageTracksEventEmitter).interacted(any())
            verify(parentViewModel).trackCardInteracted("performance")
            verify(analyticsTrackerWrapper).track(
                stat = eq(AnalyticsEvent.DASHBOARD_STATS_ORDER_DATE_TYPE_SELECTOR_TAPPED),
                properties = argThat {
                    this[AnalyticsTracker.KEY_TYPE] == "performance"
                }
            )
        }

    @Test
    fun `when order date type is selected successfully, then setting is saved and stats are refreshed`() =
        testBlocking {
            var dismissed = false
            setup {
                whenever(statsRepository.updateAnalyticsOrderDateType(WCAnalyticsOrderDateType.COMPLETED))
                    .thenReturn(Result.success(WCAnalyticsOrderDateType.COMPLETED))
            }

            viewModel.onOrderDateTypeSelected(WCAnalyticsOrderDateType.COMPLETED) {
                dismissed = true
            }
            advanceUntilIdle()

            Assertions.assertThat(viewModel.orderDateTypeState.value).isEqualTo(
                DashboardStatsViewModel.OrderDateTypeUiState(selectedType = WCAnalyticsOrderDateType.COMPLETED)
            )
            Assertions.assertThat(dismissed).isTrue()
            verify(statsRepository).updateAnalyticsOrderDateType(WCAnalyticsOrderDateType.COMPLETED)
            verify(getStats).invoke(
                refresh = eq(true),
                selectedRange = argThat {
                    selectionType == DEFAULT_SELECTION_TYPE
                },
                orderDateType = eq(WCAnalyticsOrderDateType.COMPLETED)
            )
            verify(analyticsTrackerWrapper).track(
                stat = eq(AnalyticsEvent.DASHBOARD_STATS_ORDER_DATE_TYPE_SELECTED),
                properties = argThat {
                    this[AnalyticsTracker.KEY_OPTION] == WCAnalyticsOrderDateType.COMPLETED.value &&
                        this[AnalyticsTracker.KEY_TYPE] == "performance"
                }
            )
        }

    @Test
    fun `when order date type update fails, then previous selection remains and error is shown`() =
        testBlocking {
            var dismissed = false
            val failure = Exception("network down")
            setup {
                whenever(statsRepository.updateAnalyticsOrderDateType(WCAnalyticsOrderDateType.COMPLETED))
                    .thenReturn(Result.failure(failure))
            }

            viewModel.onOrderDateTypeSelected(WCAnalyticsOrderDateType.COMPLETED) {
                dismissed = true
            }
            advanceUntilIdle()

            Assertions.assertThat(viewModel.orderDateTypeState.value).isEqualTo(
                DashboardStatsViewModel.OrderDateTypeUiState(
                    selectedType = WCAnalyticsOrderDateType.PAID,
                    hasUpdateError = true
                )
            )
            Assertions.assertThat(dismissed).isFalse()
            verify(analyticsTrackerWrapper, never()).track(
                stat = eq(AnalyticsEvent.DASHBOARD_STATS_ORDER_DATE_TYPE_SELECTED),
                properties = any()
            )
            verify(analyticsTrackerWrapper).track(
                stat = eq(AnalyticsEvent.DASHBOARD_STATS_ORDER_DATE_TYPE_UPDATE_FAILED),
                properties = argThat {
                    this[AnalyticsTracker.KEY_OPTION] == WCAnalyticsOrderDateType.COMPLETED.value &&
                        this[AnalyticsTracker.KEY_ERROR_TYPE] == failure::class.java.simpleName &&
                        this[AnalyticsTracker.KEY_ERROR_DESC] == "network down" &&
                        this[AnalyticsTracker.KEY_TYPE] == "performance"
                }
            )
        }

    @Test
    fun `when stats granularity changes, then selected option is saved into prefs`() =
        testBlocking {
            setup()

            viewModel.onRangeChanged(ANY_SELECTION_TYPE)

            verify(appPrefsWrapper).setActiveStatsTab(ANY_SELECTION_TYPE.name)
        }

    @Test
    fun `given error loading revenue, when screen starts, then UI is updated with error`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(flowOf(GetStats.LoadStatsResult.RevenueStatsError("")))
            }

            Assertions.assertThat(viewModel.revenueStatsState.value)
                .isEqualTo(DashboardStatsViewModel.RevenueStatsViewState.GenericError)
        }

    @Test
    fun `given stats plugin not active, when screen starts, then UI is updated with jetpack error`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(flowOf(GetStats.LoadStatsResult.PluginNotActive))
            }

            Assertions.assertThat(viewModel.revenueStatsState.value).isEqualTo(
                DashboardStatsViewModel.RevenueStatsViewState.WCAnalyticsInactive
            )
        }

    @Test
    fun `given success loading visitor stats, when screen starts, then UI is updated with visitor stats`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
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
                whenever(getStats.invoke(any(), any(), anyOrNull()))
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
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(flowOf(GetStats.LoadStatsResult.VisitorStatUnavailable))
            }

            Assertions.assertThat(viewModel.visitorStatsState.value)
                .isInstanceOf(DashboardStatsViewModel.VisitorStatsViewState.Unavailable::class.java)
        }

    @Test
    fun `given visitor stats unavailable, when screen starts, then data loading failed is not tracked`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(flowOf(GetStats.LoadStatsResult.VisitorStatUnavailable))
            }

            verify(analyticsTrackerWrapper, never()).track(
                stat = eq(AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_DATA_LOADING_FAILED),
                properties = any()
            )
        }

    @Test
    fun `when changing tabs, clear selected date`() = testBlocking {
        setup {
            whenever(dateRangeFormatter.formatSelectedDate(any(), argThat { selectionType == DEFAULT_SELECTION_TYPE }))
                .thenReturn("11:00")
        }

        val state = viewModel.dateRangeState.runAndCaptureValues {
            viewModel.onChartDateSelected("11")
            viewModel.onRangeChanged(ANY_SELECTION_TYPE)
        }.last()

        verify(dateRangeFormatter, never())
            .formatSelectedDate(eq("11"), argThat { selectionType == ANY_SELECTION_TYPE })
        Assertions.assertThat(state.selectedDateFormatted).isNull()
    }

    @Test
    fun `given several outdated visitor stats is returned then refreshing indicator is called only once`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(
                        flowOf(
                            GetStats.LoadStatsResult.VisitorsStatsSuccess(mapOf("test" to 3), 2, true),
                            GetStats.LoadStatsResult.VisitorsStatsSuccess(mapOf("test" to 3), 4, true),
                            GetStats.LoadStatsResult.VisitorsStatsSuccess(mapOf("test" to 4), 3, true)
                        )
                    )
            }
            verify(parentViewModel).displayRefreshingIndicator()
        }

    @Test
    fun `given up to date visitor stats is returned after outdated stats then hide refreshing indicator`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(
                        flowOf(
                            GetStats.LoadStatsResult.VisitorsStatsSuccess(mapOf("test" to 3), 2, true),
                            GetStats.LoadStatsResult.VisitorsStatsSuccess(mapOf("test" to 3), 4, false)
                        )
                    )
            }
            verify(parentViewModel).displayRefreshingIndicator()
            verify(parentViewModel).hideRefreshingIndicator()
        }

    @Test
    fun `given an error is returned after outdated visitor stats then hide refreshing indicator`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(
                        flowOf(
                            GetStats.LoadStatsResult.VisitorsStatsSuccess(mapOf("test" to 3), 2, true),
                            GetStats.LoadStatsResult.VisitorsStatsError
                        )
                    )
            }
            verify(parentViewModel).displayRefreshingIndicator()
            verify(parentViewModel).hideRefreshingIndicator()
        }

    @Test
    fun `given a visitor stats unavailable is returned after outdated data then hide refreshing indicator`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(
                        flowOf(
                            GetStats.LoadStatsResult.VisitorsStatsSuccess(mapOf("test" to 3), 2, true),
                            GetStats.LoadStatsResult.VisitorStatUnavailable
                        )
                    )
            }
            verify(parentViewModel).displayRefreshingIndicator()
            verify(parentViewModel).hideRefreshingIndicator()
        }

    @Test
    fun `given several outdated revenue stats is returned then refreshing indicator is called only once`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(
                        flowOf(
                            GetStats.LoadStatsResult.RevenueStatsSuccess(ANY_REVENUE_STATS, true),
                            GetStats.LoadStatsResult.RevenueStatsSuccess(ANY_REVENUE_STATS, true),
                            GetStats.LoadStatsResult.RevenueStatsSuccess(ANY_REVENUE_STATS, true),
                        )
                    )
            }
            verify(parentViewModel).displayRefreshingIndicator()
        }

    @Test
    fun `given up to date revenue stats is returned after outdated stats then hide refreshing indicator`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(
                        flowOf(
                            GetStats.LoadStatsResult.RevenueStatsSuccess(ANY_REVENUE_STATS, true),
                            GetStats.LoadStatsResult.RevenueStatsSuccess(ANY_REVENUE_STATS, false)
                        )
                    )
            }
            verify(parentViewModel).displayRefreshingIndicator()
            verify(parentViewModel).hideRefreshingIndicator()
        }

    @Test
    fun `given revenue error is returned after outdated revenue stats then hide refreshing indicator`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(
                        flowOf(
                            GetStats.LoadStatsResult.RevenueStatsSuccess(ANY_REVENUE_STATS, true),
                            GetStats.LoadStatsResult.RevenueStatsError("This is an error")
                        )
                    )
            }
            verify(parentViewModel).displayRefreshingIndicator()
            verify(parentViewModel).hideRefreshingIndicator()
        }

    @Test
    fun `given plugin not active error is returned after outdated revenue stats then hide refreshing indicator`() =
        testBlocking {
            setup {
                whenever(getStats.invoke(any(), any(), anyOrNull()))
                    .thenReturn(
                        flowOf(
                            GetStats.LoadStatsResult.RevenueStatsSuccess(ANY_REVENUE_STATS, true),
                            GetStats.LoadStatsResult.PluginNotActive
                        )
                    )
            }
            verify(parentViewModel).displayRefreshingIndicator()
            verify(parentViewModel).hideRefreshingIndicator()
        }

    @Test
    fun `given site is WPCom suspended, when visitor stats placeholder, then hide Jetpack icon`() = testBlocking {
        setup {
            whenever(getStats.invoke(any(), any(), anyOrNull()))
                .thenReturn(flowOf(GetStats.LoadStatsResult.VisitorStatUnavailable))
            whenever(appPrefsWrapper.isSiteWPComSuspended).thenReturn(true)
        }

        val visitorStatsState = viewModel.visitorStatsState.getOrAwaitValue()

        Assertions.assertThat(visitorStatsState)
            .isEqualTo(DashboardStatsViewModel.VisitorStatsViewState.Unavailable(showJetpackIcon = false))
    }
}
