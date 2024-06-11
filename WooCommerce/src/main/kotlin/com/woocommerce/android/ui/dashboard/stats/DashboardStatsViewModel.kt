package com.woocommerce.android.ui.dashboard.stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.DASHBOARD_STORE_TIMEZONE_DIFFER_FROM_DEVICE
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.extensions.offsetInHours
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.DashboardTransactionLauncher
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.data.StatsCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.domain.DashboardDateRangeFormatter
import com.woocommerce.android.ui.dashboard.domain.ObserveLastUpdate
import com.woocommerce.android.ui.dashboard.stats.GetStats.LoadStatsResult
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.TimezoneProvider
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.putIfNotNull
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = DashboardStatsViewModel.Factory::class)
@Suppress("LongParameterList")
class DashboardStatsViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val selectedSite: SelectedSite,
    private val getStats: GetStats,
    private val customDateRangeDataStore: StatsCustomDateRangeDataStore,
    getSelectedDateRange: GetSelectedRangeForDashboardStats,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val networkStatus: NetworkStatus,
    private val dashboardTransactionLauncher: DashboardTransactionLauncher,
    private val timezoneProvider: TimezoneProvider,
    private val observeLastUpdate: ObserveLastUpdate,
    private val wooCommerceStore: WooCommerceStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val dateRangeFormatter: DashboardDateRangeFormatter,
    val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    val dateUtils: DateUtils,
    val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedStateHandle) {
    private val selectedDateRange = getSelectedDateRange()
        .onEach {
            // Reset selected chart date when date range changes
            selectedChartDate.value = null
        }
        .shareIn(viewModelScope, started = SharingStarted.WhileSubscribed(), replay = 1)

    private val selectedChartDate = MutableStateFlow<String?>(null)

    val dateRangeState = combine(
        selectedDateRange,
        customDateRangeDataStore.dateRange,
        selectedChartDate
    ) { selectedRange, custom, selectedDate ->
        DateRangeState(
            rangeSelection = selectedRange,
            customRange = custom,
            rangeFormatted = dateRangeFormatter.formatRangeDate(selectedRange),
            selectedDateFormatted = selectedDate?.let { dateRangeFormatter.formatSelectedDate(it, selectedRange) }
        )
    }.asLiveData()

    private var _revenueStatsState = MutableLiveData<RevenueStatsViewState>()
    val revenueStatsState: LiveData<RevenueStatsViewState> = _revenueStatsState

    private var _visitorStatsState = MutableLiveData<VisitorStatsViewState>()
    val visitorStatsState: LiveData<VisitorStatsViewState> = _visitorStatsState

    private var _lastUpdateStats = MutableLiveData<Long?>()
    val lastUpdateStats: LiveData<Long?> = _lastUpdateStats

    private val refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            selectedDateRange.flatMapLatest { selectedRange ->
                merge(refreshTrigger, parentViewModel.refreshTrigger).onStart { emit(RefreshEvent()) }.map {
                    Pair(selectedRange, it.isForced)
                }
            }.collectLatest { (selectedRange, isForceRefresh) ->
                loadStoreStats(selectedRange, isForceRefresh)
            }
        }
        trackLocalTimezoneDifferenceFromStore()
    }

    fun onTabSelected(selectionType: SelectionType) {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.STATS.trackingIdentifier)
        usageTracksEventEmitter.interacted()
        if (selectionType != SelectionType.CUSTOM) {
            appPrefsWrapper.setActiveStatsTab(selectionType.name)
        } else {
            if (dateRangeState.value?.customRange == null) {
                onAddCustomRangeClicked()
            } else {
                appPrefsWrapper.setActiveStatsTab(SelectionType.CUSTOM.name)
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_TAB_SELECTED
                )
            }
        }
    }

    fun onCustomRangeSelected(range: StatsTimeRange) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_CONFIRMED,
            mapOf(
                AnalyticsTracker.KEY_IS_EDITING to (dateRangeState.value?.customRange != null),
            )
        )

        viewModelScope.launch {
            customDateRangeDataStore.updateDateRange(range)
            if (dateRangeState.value?.rangeSelection?.selectionType != SelectionType.CUSTOM) {
                onTabSelected(SelectionType.CUSTOM)
            }
        }
    }

    fun onAddCustomRangeClicked() {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.STATS.trackingIdentifier)
        triggerEvent(
            OpenDatePicker(
                fromDate = dateRangeState.value?.customRange?.start ?: Date(),
                toDate = dateRangeState.value?.customRange?.end ?: Date()
            )
        )

        val event = if (dateRangeState.value?.customRange == null) {
            AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_ADD_BUTTON_TAPPED
        } else {
            AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_EDIT_BUTTON_TAPPED
        }
        analyticsTrackerWrapper.track(event)
    }

    fun onViewAnalyticsClicked() {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.STATS.trackingIdentifier)
        AnalyticsTracker.track(AnalyticsEvent.DASHBOARD_SEE_MORE_ANALYTICS_TAPPED)
        dateRangeState.value?.rangeSelection?.let {
            triggerEvent(OpenAnalytics(it))
        }
    }

    fun onChartDateSelected(date: String?) {
        selectedChartDate.value = date
    }

    fun onRefresh() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_RETRY_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_TYPE to DashboardWidget.Type.STATS.trackingIdentifier
            )
        )
        refreshTrigger.tryEmit(RefreshEvent(isForced = true))
    }

    private suspend fun loadStoreStats(selectedRange: StatsTimeRangeSelection, forceRefresh: Boolean) = coroutineScope {
        if (!networkStatus.isConnected()) {
            _revenueStatsState.value = RevenueStatsViewState.GenericError
            _visitorStatsState.value = VisitorStatsViewState.NotLoaded
            return@coroutineScope
        }
        _revenueStatsState.value = RevenueStatsViewState.Loading(isForced = forceRefresh)
        if (forceRefresh) {
            _visitorStatsState.value = VisitorStatsViewState.NotLoaded
        }
        getStats(forceRefresh, selectedRange)
            .collect {
                when (it) {
                    is LoadStatsResult.RevenueStatsSuccess -> onRevenueStatsSuccess(it, selectedRange)
                    is LoadStatsResult.RevenueStatsError ->
                        _revenueStatsState.value = RevenueStatsViewState.GenericError

                    LoadStatsResult.PluginNotActive ->
                        _revenueStatsState.value = RevenueStatsViewState.PluginNotActiveError

                    is LoadStatsResult.VisitorsStatsSuccess -> _visitorStatsState.value = VisitorStatsViewState.Content(
                        stats = it.stats, totalVisitorCount = it.totalVisitorCount
                    )

                    is LoadStatsResult.VisitorsStatsError -> _visitorStatsState.value = VisitorStatsViewState.Error
                    is LoadStatsResult.VisitorStatUnavailable ->
                        _visitorStatsState.value = VisitorStatsViewState.Unavailable
                }
                dashboardTransactionLauncher.onStoreStatisticsFetched()
            }

        launch {
            observeLastUpdate(
                selectedRange,
                listOf(
                    AnalyticsUpdateDataStore.AnalyticData.REVENUE,
                    AnalyticsUpdateDataStore.AnalyticData.VISITORS
                )
            ).collect { lastUpdateMillis -> _lastUpdateStats.value = lastUpdateMillis }
        }
    }

    private fun onRevenueStatsSuccess(
        result: LoadStatsResult.RevenueStatsSuccess,
        selectedRange: StatsTimeRangeSelection
    ) {
        _revenueStatsState.value = RevenueStatsViewState.Content(
            result.stats?.toStoreStatsUiModel(),
            selectedRange
        )
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DASHBOARD_MAIN_STATS_LOADED,
            buildMap {
                put(AnalyticsTracker.KEY_RANGE, selectedRange.selectionType.identifier)
                putIfNotNull(AnalyticsTracker.KEY_ID to result.stats?.rangeId)
            }
        )
    }

    private fun trackLocalTimezoneDifferenceFromStore() {
        val selectedSite = selectedSite.getIfExists() ?: return
        val siteTimezone = selectedSite.timezone.takeIf { it.isNotNullOrEmpty() } ?: return
        val localTimeZoneOffset = timezoneProvider.deviceTimezone.offsetInHours.toString()

        val shouldTriggerTimezoneTrack = appPrefsWrapper.isTimezoneTrackEventNeverTriggeredFor(
            siteId = selectedSite.siteId,
            localTimezone = localTimeZoneOffset,
            storeTimezone = siteTimezone
        ) && selectedSite.timezone != localTimeZoneOffset

        if (shouldTriggerTimezoneTrack) {
            analyticsTrackerWrapper.track(
                stat = DASHBOARD_STORE_TIMEZONE_DIFFER_FROM_DEVICE,
                properties = mapOf(
                    AnalyticsTracker.KEY_STORE_TIMEZONE to siteTimezone,
                    AnalyticsTracker.KEY_LOCAL_TIMEZONE to localTimeZoneOffset
                )
            )
            appPrefsWrapper.setTimezoneTrackEventTriggeredFor(
                siteId = selectedSite.siteId,
                localTimezone = localTimeZoneOffset,
                storeTimezone = siteTimezone
            )
        }
    }

    private fun WCRevenueStatsModel.toStoreStatsUiModel(): RevenueStatsUiModel {
        val totals = parseTotal()
        return RevenueStatsUiModel(
            intervalList = getIntervalList().toStatsIntervalUiModelList(),
            totalOrdersCount = totals?.ordersCount,
            totalSales = totals?.totalSales,
            currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode,
            rangeId = rangeId
        )
    }

    private fun List<WCRevenueStatsModel.Interval>.toStatsIntervalUiModelList() =
        map {
            StatsIntervalUiModel(
                it.interval,
                it.subtotals?.ordersCount,
                it.subtotals?.totalSales
            )
        }

    sealed class RevenueStatsViewState {
        data class Loading(val isForced: Boolean) : RevenueStatsViewState()
        data object GenericError : RevenueStatsViewState()
        data object PluginNotActiveError : RevenueStatsViewState()
        data class Content(
            val revenueStats: RevenueStatsUiModel?,
            val statsRangeSelection: StatsTimeRangeSelection
        ) : RevenueStatsViewState()
    }

    sealed class VisitorStatsViewState {
        data object Error : VisitorStatsViewState()
        data object NotLoaded : VisitorStatsViewState()
        data object Unavailable : VisitorStatsViewState()

        data class Content(
            val stats: Map<String, Int>,
            val totalVisitorCount: Int?
        ) : VisitorStatsViewState()
    }

    data class DateRangeState(
        val rangeSelection: StatsTimeRangeSelection,
        val customRange: StatsTimeRange?,
        val rangeFormatted: String,
        val selectedDateFormatted: String?
    )

    data class RevenueStatsUiModel(
        val intervalList: List<StatsIntervalUiModel> = emptyList(),
        val totalOrdersCount: Int? = null,
        val totalSales: Double? = null,
        val currencyCode: String?,
        val rangeId: String
    )

    data class StatsIntervalUiModel(
        val interval: String? = null,
        val ordersCount: Long? = null,
        val sales: Double? = null
    )

    data class OpenDatePicker(val fromDate: Date, val toDate: Date) : MultiLiveEvent.Event()
    data class OpenAnalytics(val analyticsPeriod: StatsTimeRangeSelection) : MultiLiveEvent.Event()

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardStatsViewModel
    }
}
