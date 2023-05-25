package com.woocommerce.android.ui.mystore

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.notifications.local.LocalNotificationType.STORE_CREATION_FINISHED
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.LAUNCH_YOUR_STORE
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
import com.woocommerce.android.ui.prefs.privacy.banner.domain.ShouldShowPrivacyBanner
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal
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
    private val onboardingRepository: StoreOnboardingRepository,
    notificationScheduler: LocalNotificationScheduler,
    shouldShowPrivacyBanner: ShouldShowPrivacyBanner
) : ScopedViewModel(savedState) {
    companion object {
        private const val DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER = 5
        const val UTM_SOURCE = "my_store"
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

    private var _appbarState = MutableLiveData<AppbarState>()
    val appbarState: LiveData<AppbarState> = _appbarState

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val _activeStatsGranularity = savedState.getStateFlow(viewModelScope, getSelectedStatsGranularityIfAny())
    val activeStatsGranularity = _activeStatsGranularity.asLiveData()

    @VisibleForTesting
    val refreshStoreStats = BooleanArray(StatsGranularity.values().size) { true }

    @VisibleForTesting
    val refreshTopPerformerStats = BooleanArray(StatsGranularity.values().size) { true }

    private var jetpackMonitoringJob: Job? = null

    init {
        ConnectionChangeReceiver.getEventBus().register(this)

        _topPerformersState.value = TopPerformersState(isLoading = true)

        viewModelScope.launch {
            combine(
                _activeStatsGranularity,
                refreshTrigger.onStart { emit(Unit) }
            ) { granularity, _ ->
                granularity
            }.collectLatest { granularity ->
                coroutineScope {
                    launch { loadStoreStats(granularity) }
                    launch { loadTopPerformersStats(granularity) }
                }
            }
        }
        observeTopPerformerUpdates()

        if (FeatureFlag.PRIVACY_CHOICES.isEnabled()) {
            shouldShowPrivacyBanner().let {
                if (it) {
                    triggerEvent(MyStoreEvent.ShowPrivacyBanner)
                }
            }
        }

        // A notification is only displayed when the store has never been opened before
        notificationScheduler.cancelScheduledNotification(STORE_CREATION_FINISHED)
        updateShareStoreButtonVisibility()
    }

    private fun updateShareStoreButtonVisibility() {
        _appbarState.value = AppbarState(showShareStoreButton = false)
        if (appPrefsWrapper.isOnboardingCompleted(selectedSite.getSelectedSiteId())) {
            _appbarState.value = _appbarState.value?.copy(showShareStoreButton = true)
        } else {
            launch {
                onboardingRepository.observeOnboardingTasks()
                    .collectLatest { tasks ->
                        _appbarState.value = _appbarState.value?.copy(
                            showShareStoreButton = tasks
                                .filter { it.type == LAUNCH_YOUR_STORE }
                                .all { it.isComplete }
                        )
                    }
            }
        }
    }

    override fun onCleared() {
        ConnectionChangeReceiver.getEventBus().unregister(this)
        super.onCleared()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            if (refreshStoreStats.any { it } || refreshTopPerformerStats.any { it }) {
                refreshTrigger.tryEmit(Unit)
            }
        }
    }

    fun onStatsGranularityChanged(granularity: StatsGranularity) {
        usageTracksEventEmitter.interacted()
        _activeStatsGranularity.update { granularity }
        launch {
            appPrefsWrapper.setActiveStatsGranularity(selectedSite.getSelectedSiteId(), granularity.name)
        }
    }

    fun onPullToRefresh() {
        usageTracksEventEmitter.interacted()
        analyticsTrackerWrapper.track(AnalyticsEvent.DASHBOARD_PULLED_TO_REFRESH)
        resetForceRefresh()
        refreshTrigger.tryEmit(Unit)
    }

    fun getSelectedSiteName(): String =
        selectedSite.getIfExists()?.let { site ->
            if (!site.displayName.isNullOrBlank()) {
                site.displayName
            } else {
                site.name
            }
        } ?: ""

    fun onViewAnalyticsClicked() {
        AnalyticsTracker.track(AnalyticsEvent.DASHBOARD_SEE_MORE_ANALYTICS_TAPPED)
        val targetPeriod = when (val state = revenueStatsState.value) {
            is RevenueStatsViewState.Content -> state.granularity.toAnalyticTimePeriod()
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

    private suspend fun loadStoreStats(granularity: StatsGranularity) {
        if (!networkStatus.isConnected()) {
            refreshStoreStats[granularity.ordinal] = true
            _revenueStatsState.value = RevenueStatsViewState.Content(null, granularity)
            _visitorStatsState.value = VisitorStatsViewState.Content(emptyMap())
            return
        }

        val forceRefresh = refreshStoreStats[granularity.ordinal]
        if (forceRefresh) {
            refreshStoreStats[granularity.ordinal] = false
        }
        _revenueStatsState.value = RevenueStatsViewState.Loading
        getStats(forceRefresh, granularity)
            .collect {
                when (it) {
                    is RevenueStatsSuccess -> onRevenueStatsSuccess(it, granularity)
                    is RevenueStatsError -> _revenueStatsState.value = RevenueStatsViewState.GenericError
                    PluginNotActive -> _revenueStatsState.value = RevenueStatsViewState.PluginNotActiveError
                    is VisitorsStatsSuccess -> _visitorStatsState.value = VisitorStatsViewState.Content(it.stats)
                    is VisitorsStatsError -> _visitorStatsState.value = VisitorStatsViewState.Error
                    is VisitorStatUnavailable -> onVisitorStatsUnavailable(it.connectionType)
                    is HasOrders -> _hasOrders.value = if (it.hasOrder) OrderState.AtLeastOne else OrderState.Empty
                }
                myStoreTransactionLauncher.onStoreStatisticsFetched()
            }
    }

    private fun onRevenueStatsSuccess(
        it: RevenueStatsSuccess,
        selectedGranularity: StatsGranularity
    ) {
        _revenueStatsState.value = RevenueStatsViewState.Content(
            it.stats?.toStoreStatsUiModel(),
            selectedGranularity
        )
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DASHBOARD_MAIN_STATS_LOADED,
            mapOf(AnalyticsTracker.KEY_RANGE to selectedGranularity.name.lowercase())
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
        monitorJetpackInstallation()
    }

    private fun monitorJetpackInstallation() {
        jetpackMonitoringJob?.cancel()
        jetpackMonitoringJob = viewModelScope.launch {
            selectedSite.observe()
                .filter { it?.connectionType == SiteConnectionType.Jetpack }
                .take(1)
                .collect {
                    loadStoreStats(_activeStatsGranularity.value)
                }
        }
    }

    private suspend fun loadTopPerformersStats(granularity: StatsGranularity) {
        if (!networkStatus.isConnected()) {
            refreshTopPerformerStats[granularity.ordinal] = true
            return
        }

        val forceRefresh = refreshTopPerformerStats[granularity.ordinal]
        if (forceRefresh) {
            refreshTopPerformerStats[granularity.ordinal] = false
        }

        _topPerformersState.value = _topPerformersState.value?.copy(isLoading = true, isError = false)
        val result = getTopPerformers.fetchTopPerformers(granularity, forceRefresh)
        result.fold(
            onFailure = { _topPerformersState.value = _topPerformersState.value?.copy(isError = true) },
            onSuccess = {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.DASHBOARD_TOP_PERFORMERS_LOADED,
                    mapOf(AnalyticsTracker.KEY_RANGE to granularity.name.lowercase())
                )
            }
        )
        _topPerformersState.value = _topPerformersState.value?.copy(isLoading = false)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTopPerformerUpdates() {
        viewModelScope.launch {
            _activeStatsGranularity
                .flatMapLatest { granularity -> getTopPerformers.observeTopPerformers(granularity) }
                .collectLatest {
                    _topPerformersState.value = _topPerformersState.value?.copy(
                        topPerformers = it.toTopPerformersUiList(),
                    )
                }
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
            currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
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

    private fun resetForceRefresh() {
        refreshTopPerformerStats.forEachIndexed { index, _ ->
            refreshTopPerformerStats[index] = true
        }
        refreshStoreStats.forEachIndexed { index, _ ->
            refreshStoreStats[index] = true
        }
    }

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

    private fun getSelectedStatsGranularityIfAny(): StatsGranularity {
        val previouslySelectedGranularity = appPrefsWrapper.getActiveStatsGranularity(selectedSite.getSelectedSiteId())
        return runCatching { StatsGranularity.valueOf(previouslySelectedGranularity.uppercase()) }
            .getOrElse { StatsGranularity.DAYS }
    }

    private fun StatsGranularity.toAnalyticTimePeriod() = when (this) {
        StatsGranularity.DAYS -> SelectionType.TODAY
        StatsGranularity.WEEKS -> SelectionType.WEEK_TO_DATE
        StatsGranularity.MONTHS -> SelectionType.MONTH_TO_DATE
        StatsGranularity.YEARS -> SelectionType.YEAR_TO_DATE
    }

    sealed class RevenueStatsViewState {
        object Loading : RevenueStatsViewState()
        object GenericError : RevenueStatsViewState()
        object PluginNotActiveError : RevenueStatsViewState()
        data class Content(
            val revenueStats: RevenueStatsUiModel?,
            val granularity: StatsGranularity
        ) : RevenueStatsViewState()
    }

    sealed class VisitorStatsViewState {
        object Error : VisitorStatsViewState()
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
        object Empty : OrderState()
        object AtLeastOne : OrderState()
    }

    data class AppbarState(
        val showShareStoreButton: Boolean = false,
    )

    sealed class MyStoreEvent : MultiLiveEvent.Event() {
        data class OpenTopPerformer(
            val productId: Long
        ) : MyStoreEvent()

        data class OpenAnalytics(val analyticsPeriod: SelectionType) : MyStoreEvent()

        object ShowPrivacyBanner : MyStoreEvent()

        data class ShareStore(val storeUrl: String) : MyStoreEvent()
    }
}
