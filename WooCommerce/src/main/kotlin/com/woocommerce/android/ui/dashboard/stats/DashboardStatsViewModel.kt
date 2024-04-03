package com.woocommerce.android.ui.dashboard.stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.DASHBOARD_STORE_TIMEZONE_DIFFER_FROM_DEVICE
import com.woocommerce.android.analytics.AnalyticsEvent.FEATURE_JETPACK_BENEFITS_BANNER
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.extensions.offsetInHours
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.DashboardTransactionLauncher
import com.woocommerce.android.ui.dashboard.DashboardViewModel.OrderState
import com.woocommerce.android.ui.dashboard.DashboardViewModel.OrderState.AtLeastOne
import com.woocommerce.android.ui.dashboard.DashboardViewModel.OrderState.Empty
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshState
import com.woocommerce.android.ui.dashboard.JetpackBenefitsBannerUiModel
import com.woocommerce.android.ui.dashboard.domain.GetStats
import com.woocommerce.android.ui.dashboard.domain.GetStats.LoadStatsResult
import com.woocommerce.android.ui.dashboard.domain.ObserveLastUpdate
import com.woocommerce.android.ui.mystore.data.CustomDateRangeDataStore
import com.woocommerce.android.util.TimezoneProvider
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.putIfNotNull
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class DashboardStatsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val getStats: GetStats,
    private val customDateRangeDataStore: CustomDateRangeDataStore,
    getSelectedDateRange: GetSelectedDateRange,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val networkStatus: NetworkStatus,
    private val dashboardTransactionLauncher: DashboardTransactionLauncher,
    private val timezoneProvider: TimezoneProvider,
    private val observeLastUpdate: ObserveLastUpdate,
    private val wooCommerceStore: WooCommerceStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER = 5
    }

    private val refreshTrigger = MutableSharedFlow<RefreshState>(extraBufferCapacity = 1)

    private var _hasOrders = MutableLiveData<OrderState>()

    private val _selectedDateRange = getSelectedDateRange()
    val selectedDateRange: LiveData<StatsTimeRangeSelection> = _selectedDateRange.asLiveData()

    private var _revenueStatsState = MutableLiveData<RevenueStatsViewState>()
    val revenueStatsState: LiveData<RevenueStatsViewState> = _revenueStatsState

    private var _visitorStatsState = MutableLiveData<VisitorStatsViewState>()
    val visitorStatsState: LiveData<VisitorStatsViewState> = _visitorStatsState

    private var _lastUpdateStats = MutableLiveData<Long?>()
    val lastUpdateStats: LiveData<Long?> = _lastUpdateStats

    val customRange = customDateRangeDataStore.dateRange.asLiveData()

    init {
        ConnectionChangeReceiver.getEventBus().register(this)

        viewModelScope.launch {
            combine(
                _selectedDateRange,
                refreshTrigger.onStart { emit(RefreshState()) }
            ) { selectedRange, refreshEvent ->
                Pair(selectedRange, refreshEvent.shouldRefresh)
            }.collectLatest { (selectedRange, isForceRefresh) ->
                loadStoreStats(selectedRange, isForceRefresh)
            }
        }
        trackLocalTimezoneDifferenceFromStore()
    }

    fun onTabSelected(selectionType: SelectionType) {
        usageTracksEventEmitter.interacted()
        appPrefsWrapper.setActiveStatsTab(selectionType.name)

        if (selectionType == SelectionType.CUSTOM) {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_TAB_SELECTED
            )
        }
    }

    fun onCustomRangeSelected(range: StatsTimeRange) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_CONFIRMED,
            mapOf(
                AnalyticsTracker.KEY_IS_EDITING to (customRange.value != null),
            )
        )

        if (selectedDateRange.value?.selectionType != SelectionType.CUSTOM) {
            onTabSelected(SelectionType.CUSTOM)
        }
        viewModelScope.launch {
            customDateRangeDataStore.updateDateRange(range)
        }
    }

    fun onAddCustomRangeClicked() {
        triggerEvent(
            OpenDatePicker(
                fromDate = customRange.value?.start ?: Date(),
                toDate = customRange.value?.end ?: Date()
            )
        )

        val event = if (customRange.value == null) {
            AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_ADD_BUTTON_TAPPED
        } else {
            AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_EDIT_BUTTON_TAPPED
        }
        analyticsTrackerWrapper.track(event)
    }

    fun onViewAnalyticsClicked() {
        AnalyticsTracker.track(AnalyticsEvent.DASHBOARD_SEE_MORE_ANALYTICS_TAPPED)
        selectedDateRange.value?.let {
            triggerEvent(OpenAnalytics(it))
        }
    }

    override fun onCleared() {
        ConnectionChangeReceiver.getEventBus().unregister(this)
        super.onCleared()
    }

    private suspend fun loadStoreStats(selectedRange: StatsTimeRangeSelection, forceRefresh: Boolean) = coroutineScope {
        if (!networkStatus.isConnected()) {
            _revenueStatsState.value = RevenueStatsViewState.Content(null, selectedRange)
            _visitorStatsState.value = VisitorStatsViewState.NotLoaded
            return@coroutineScope
        }
        _revenueStatsState.value = RevenueStatsViewState.Loading
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
                    is LoadStatsResult.VisitorStatUnavailable -> onVisitorStatsUnavailable(it.connectionType)
                    is LoadStatsResult.HasOrders -> _hasOrders.value = if (it.hasOrder) AtLeastOne else Empty
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

    private fun onVisitorStatsUnavailable(connectionType: SiteConnectionType) {
        val daysSinceDismissal = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - appPrefsWrapper.getJetpackBenefitsDismissalDate()
        )
        val supportsJetpackInstallation = connectionType == SiteConnectionType.JetpackConnectionPackage ||
            connectionType == SiteConnectionType.ApplicationPasswords
        val showBanner =
            supportsJetpackInstallation && daysSinceDismissal >= DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER
        val benefitsBanner = JetpackBenefitsBannerUiModel(
            show = showBanner,
            onDismiss = {
                _visitorStatsState.value = VisitorStatsViewState.Unavailable(JetpackBenefitsBannerUiModel(show = false))
                appPrefsWrapper.recordJetpackBenefitsDismissal()
                analyticsTrackerWrapper.track(
                    stat = FEATURE_JETPACK_BENEFITS_BANNER,
                    properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "dismissed")
                )
            }
        )
        _visitorStatsState.value = VisitorStatsViewState.Unavailable(benefitsBanner)
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            refreshTrigger.tryEmit(RefreshState())
        }
    }

    sealed class RevenueStatsViewState {
        data object Loading : RevenueStatsViewState()
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
        data class Unavailable(
            val benefitsBanner: JetpackBenefitsBannerUiModel
        ) : VisitorStatsViewState()

        data class Content(
            val stats: Map<String, Int>,
            val totalVisitorCount: Int?
        ) : VisitorStatsViewState()
    }

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
}
