package com.woocommerce.android.ui.mystore

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.DASHBOARD_STORE_TIMEZONE_DIFFER_FROM_DEVICE
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isEligibleForAI
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.extensions.isSitePublic
import com.woocommerce.android.extensions.offsetInHours
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.notifications.local.LocalNotificationType.STORE_CREATION_FINISHED
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.mystore.MyStoreViewModel.MyStoreEvent.OpenDatePicker
import com.woocommerce.android.ui.mystore.MyStoreViewModel.MyStoreEvent.ShowAIProductDescriptionDialog
import com.woocommerce.android.ui.mystore.data.CustomDateRangeDataStore
import com.woocommerce.android.ui.mystore.domain.GetStats
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.HasOrders
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.PluginNotActive
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.RevenueStatsError
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.RevenueStatsSuccess
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.VisitorStatUnavailable
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.VisitorsStatsError
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.VisitorsStatsSuccess
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.TopPerformerProduct
import com.woocommerce.android.ui.mystore.domain.ObserveLastUpdate
import com.woocommerce.android.ui.prefs.privacy.banner.domain.ShouldShowPrivacyBanner
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.TimezoneProvider
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.putIfNotNull
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MyStoreViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val wooCommerceStore: WooCommerceStore,
    private val getStats: GetStats,
    private val getTopPerformers: GetTopPerformers,
    private val currencyFormatter: CurrencyFormatter,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val usageTracksEventEmitter: MyStoreStatsUsageTracksEventEmitter,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val myStoreTransactionLauncher: MyStoreTransactionLauncher,
    private val timezoneProvider: TimezoneProvider,
    private val observeLastUpdate: ObserveLastUpdate,
    private val customDateRangeDataStore: CustomDateRangeDataStore,
    private val dateUtils: DateUtils,
    notificationScheduler: LocalNotificationScheduler,
    shouldShowPrivacyBanner: ShouldShowPrivacyBanner
) : ScopedViewModel(savedState) {
    companion object {
        private const val DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER = 5
        val SUPPORTED_RANGES_ON_MY_STORE_TAB = listOf(
            SelectionType.TODAY,
            SelectionType.WEEK_TO_DATE,
            SelectionType.MONTH_TO_DATE,
            SelectionType.YEAR_TO_DATE,
            SelectionType.CUSTOM
        )
    }

    val performanceObserver: LifecycleObserver = myStoreTransactionLauncher

    private var _revenueStatsState = MutableLiveData<RevenueStatsViewState>()
    val revenueStatsState: LiveData<RevenueStatsViewState> = _revenueStatsState

    private var _visitorStatsState = MutableLiveData<VisitorStatsViewState>()
    val visitorStatsState: LiveData<VisitorStatsViewState> = _visitorStatsState

    private var _topPerformersState = MutableLiveData<TopPerformersState>()
    val topPerformersState: LiveData<TopPerformersState> = _topPerformersState

    private var _hasOrders = MutableLiveData<OrderState>()
    val hasOrders: LiveData<OrderState> = _hasOrders

    private var _lastUpdateStats = MutableLiveData<Long?>()
    val lastUpdateStats: LiveData<Long?> = _lastUpdateStats

    private var _lastUpdateTopPerformers = MutableLiveData<Long?>()
    val lastUpdateTopPerformers: LiveData<Long?> = _lastUpdateTopPerformers

    private var _appbarState = MutableLiveData<AppbarState>()
    val appbarState: LiveData<AppbarState> = _appbarState

    private val refreshTrigger = MutableSharedFlow<RefreshState>(extraBufferCapacity = 1)

    private val _selectedRangeType = savedState.getStateFlow(viewModelScope, getSelectedRangeTypeIfAny())

    private val _customRange = customDateRangeDataStore.dateRange
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )
    val customRange = _customRange.asLiveData()
    private val _selectedDateRange = combine(_selectedRangeType, _customRange) { selectionType, customRange ->
        when (selectionType) {
            SelectionType.CUSTOM -> {
                selectionType.generateSelectionData(
                    calendar = Calendar.getInstance(),
                    locale = Locale.getDefault(),
                    referenceStartDate = customRange?.startDate ?: Date(),
                    referenceEndDate = customRange?.endDate ?: Date()
                )
            }

            else -> {
                selectionType.generateSelectionData(
                    calendar = Calendar.getInstance(),
                    locale = Locale.getDefault(),
                    referenceStartDate = dateUtils.getCurrentDateInSiteTimeZone() ?: Date(),
                    referenceEndDate = dateUtils.getCurrentDateInSiteTimeZone() ?: Date()
                )
            }
        }
    }
    val selectedDateRange: LiveData<StatsTimeRangeSelection> = _selectedDateRange.asLiveData()

    val storeName = selectedSite.observe().map { site ->
        if (!site?.displayName.isNullOrBlank()) {
            site?.displayName
        } else {
            site?.name
        } ?: resourceProvider.getString(R.string.store_name_default)
    }.asLiveData()

    init {
        ConnectionChangeReceiver.getEventBus().register(this)

        _topPerformersState.value = TopPerformersState(isLoading = true)

        viewModelScope.launch {
            combine(
                _selectedDateRange,
                refreshTrigger.onStart { emit(RefreshState()) }
            ) { selectedRange, refreshEvent ->
                Pair(selectedRange, refreshEvent.shouldRefresh)
            }.collectLatest { (selectedRange, isForceRefresh) ->
                coroutineScope {
                    launch { loadStoreStats(selectedRange, isForceRefresh) }
                    launch { loadTopPerformersStats(selectedRange, isForceRefresh) }
                }
            }
        }
        observeTopPerformerUpdates()
        trackLocalTimezoneDifferenceFromStore()

        launch {
            shouldShowPrivacyBanner().let {
                if (it) {
                    triggerEvent(MyStoreEvent.ShowPrivacyBanner)
                }
            }
        }

        if (selectedSite.getOrNull()?.isEligibleForAI == true &&
            !appPrefsWrapper.wasAIProductDescriptionPromoDialogShown
        ) {
            triggerEvent(ShowAIProductDescriptionDialog)
            appPrefsWrapper.wasAIProductDescriptionPromoDialogShown = true
        }

        // A notification is only displayed when the store has never been opened before
        notificationScheduler.cancelScheduledNotification(STORE_CREATION_FINISHED)
        updateShareStoreButtonVisibility()
    }

    private fun updateShareStoreButtonVisibility() {
        _appbarState.value = AppbarState(showShareStoreButton = selectedSite.get().isSitePublic)
    }

    override fun onCleared() {
        ConnectionChangeReceiver.getEventBus().unregister(this)
        super.onCleared()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            refreshTrigger.tryEmit(RefreshState())
        }
    }

    fun onStatsGranularityChanged(granularity: SelectionType) {
        usageTracksEventEmitter.interacted()
        _selectedRangeType.update { granularity }
        appPrefsWrapper.setActiveStatsGranularity(granularity.name)
    }

    fun onPullToRefresh() {
        usageTracksEventEmitter.interacted()
        analyticsTrackerWrapper.track(AnalyticsEvent.DASHBOARD_PULLED_TO_REFRESH)
        refreshTrigger.tryEmit(RefreshState(isForced = true))
    }

    fun onViewAnalyticsClicked() {
        AnalyticsTracker.track(AnalyticsEvent.DASHBOARD_SEE_MORE_ANALYTICS_TAPPED)
        val targetPeriod = when (val state = revenueStatsState.value) {
            is RevenueStatsViewState.Content -> state.statsRangeSelection.selectionType
            else -> SelectionType.TODAY
        }
        triggerEvent(MyStoreEvent.OpenAnalytics(targetPeriod))
    }

    fun onShareStoreClicked() {
        AnalyticsTracker.track(AnalyticsEvent.DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED)
        triggerEvent(
            MyStoreEvent.ShareStore(storeUrl = selectedSite.get().url)
        )
    }

    private suspend fun loadStoreStats(selectedRange: StatsTimeRangeSelection, forceRefresh: Boolean) {
        if (!networkStatus.isConnected()) {
            _revenueStatsState.value = RevenueStatsViewState.Content(null, selectedRange)
            _visitorStatsState.value = VisitorStatsViewState.Content(emptyMap())
            return
        }
        _revenueStatsState.value = RevenueStatsViewState.Loading
        getStats(forceRefresh, selectedRange)
            .collect {
                when (it) {
                    is RevenueStatsSuccess -> onRevenueStatsSuccess(it, selectedRange)
                    is RevenueStatsError -> _revenueStatsState.value = RevenueStatsViewState.GenericError
                    PluginNotActive -> _revenueStatsState.value = RevenueStatsViewState.PluginNotActiveError
                    is VisitorsStatsSuccess -> _visitorStatsState.value = VisitorStatsViewState.Content(it.stats)
                    is VisitorsStatsError -> _visitorStatsState.value = VisitorStatsViewState.Error
                    is VisitorStatUnavailable -> onVisitorStatsUnavailable(it.connectionType)
                    is HasOrders -> _hasOrders.value = if (it.hasOrder) OrderState.AtLeastOne else OrderState.Empty
                }
                myStoreTransactionLauncher.onStoreStatisticsFetched()
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
        launch {
            observeLastUpdate(
                selectedRange,
                AnalyticsUpdateDataStore.AnalyticData.TOP_PERFORMERS
            ).collect { lastUpdateMillis -> _lastUpdateTopPerformers.value = lastUpdateMillis }
        }
    }

    private fun onRevenueStatsSuccess(
        result: RevenueStatsSuccess,
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
        val showBanner = supportsJetpackInstallation && daysSinceDismissal >= DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER
        val benefitsBanner = JetpackBenefitsBannerUiModel(
            show = showBanner,
            onDismiss = {
                _visitorStatsState.value =
                    VisitorStatsViewState.Unavailable(JetpackBenefitsBannerUiModel(show = false))
                appPrefsWrapper.recordJetpackBenefitsDismissal()
                analyticsTrackerWrapper.track(
                    stat = AnalyticsEvent.FEATURE_JETPACK_BENEFITS_BANNER,
                    properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "dismissed")
                )
            }
        )
        _visitorStatsState.value = VisitorStatsViewState.Unavailable(benefitsBanner)
    }

    private suspend fun loadTopPerformersStats(selectedRange: StatsTimeRangeSelection, forceRefresh: Boolean) {
        if (!networkStatus.isConnected()) return

        _topPerformersState.value = _topPerformersState.value?.copy(isLoading = true, isError = false)
        val result = getTopPerformers.fetchTopPerformers(selectedRange, forceRefresh)
        result.fold(
            onFailure = { _topPerformersState.value = _topPerformersState.value?.copy(isError = true) },
            onSuccess = {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.DASHBOARD_TOP_PERFORMERS_LOADED,
                    mapOf(AnalyticsTracker.KEY_RANGE to selectedRange.selectionType.identifier)
                )
            }
        )
        _topPerformersState.value = _topPerformersState.value?.copy(isLoading = false)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTopPerformerUpdates() {
        viewModelScope.launch {
            _selectedDateRange
                .flatMapLatest { granularity ->
                    getTopPerformers.observeTopPerformers(granularity)
                }
                .collectLatest {
                    _topPerformersState.value = _topPerformersState.value?.copy(
                        topPerformers = it.toTopPerformersUiList(),
                    )
                }
        }
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

    private fun onTopPerformerSelected(productId: Long) {
        triggerEvent(MyStoreEvent.OpenTopPerformer(productId))
        analyticsTrackerWrapper.track(AnalyticsEvent.TOP_EARNER_PRODUCT_TAPPED)
        usageTracksEventEmitter.interacted()
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

    private fun List<TopPerformerProduct>.toTopPerformersUiList() = map { it.toTopPerformersUiModel() }

    private fun TopPerformerProduct.toTopPerformersUiModel() =
        TopPerformerProductUiModel(
            productId = productId,
            name = StringEscapeUtils.unescapeHtml4(name),
            timesOrdered = FormatUtils.formatDecimal(quantity),
            netSales = resourceProvider.getString(
                R.string.dashboard_top_performers_net_sales,
                getTotalSpendFormatted(total.toBigDecimal(), currency)
            ),
            imageUrl = imageUrl?.toImageUrl(),
            onClick = ::onTopPerformerSelected
        )

    private fun getTotalSpendFormatted(totalSpend: BigDecimal, currency: String) =
        currencyFormatter.formatCurrency(
            totalSpend,
            wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: currency
        )

    private fun String.toImageUrl() =
        PhotonUtils.getPhotonImageUrl(
            this,
            resourceProvider.getDimensionPixelSize(R.dimen.image_minor_100),
            0
        )

    private fun getSelectedRangeTypeIfAny(): SelectionType {
        val previouslySelectedGranularity = appPrefsWrapper.getActiveStatsGranularity()
        return runCatching {
            SelectionType.valueOf(previouslySelectedGranularity)
        }.getOrDefault(SelectionType.TODAY)
    }

    fun onCustomRangeSelected(fromMillis: Long, toMillis: Long) {
        if (_selectedRangeType.value != SelectionType.CUSTOM) {
            onStatsGranularityChanged(SelectionType.CUSTOM)
        }
        viewModelScope.launch {
            customDateRangeDataStore.updateDateRange(fromMillis, toMillis)
        }
    }

    fun onAddCustomRangeClicked() {
        triggerEvent(
            OpenDatePicker(
                fromDate = _customRange.value?.startDate ?: Date(),
                toDate = _customRange.value?.endDate ?: Date()
            )
        )
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
        data class Unavailable(
            val benefitsBanner: JetpackBenefitsBannerUiModel
        ) : VisitorStatsViewState()

        data class Content(
            val stats: Map<String, Int>
        ) : VisitorStatsViewState()
    }

    data class TopPerformersState(
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val topPerformers: List<TopPerformerProductUiModel> = emptyList(),
    )

    sealed class OrderState {
        data object Empty : OrderState()
        data object AtLeastOne : OrderState()
    }

    data class AppbarState(
        val showShareStoreButton: Boolean = false,
    )

    sealed class MyStoreEvent : MultiLiveEvent.Event() {
        data class OpenTopPerformer(
            val productId: Long
        ) : MyStoreEvent()

        data class OpenAnalytics(val analyticsPeriod: SelectionType) : MyStoreEvent()

        data object ShowPrivacyBanner : MyStoreEvent()

        data object ShowAIProductDescriptionDialog : MyStoreEvent()

        data class ShareStore(val storeUrl: String) : MyStoreEvent()

        data class OpenDatePicker(val fromDate: Date, val toDate: Date) : MyStoreEvent()
    }

    data class RefreshState(private val isForced: Boolean = false) {
        /**
         * [shouldRefresh] will be true only the first time the refresh event is consulted and when
         * isForced is initialized on true. Once the event is handled the property will change its value to false
         */
        var shouldRefresh: Boolean = isForced
            private set
            get(): Boolean {
                val result = field
                if (field) {
                    field = false
                }
                return result
            }
    }
}
