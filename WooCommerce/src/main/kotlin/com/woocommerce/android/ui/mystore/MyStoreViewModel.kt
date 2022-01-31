package com.woocommerce.android.ui.mystore

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.domain.GetStats
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.*
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.TopPerformersResult.TopPerformersError
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.TopPerformersResult.TopPerformersSuccess
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
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
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedState) {
    private companion object {
        const val NUM_TOP_PERFORMERS = 5
        const val DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER = 5
        const val ACTIVE_STATS_GRANULARITY_KEY = "active_stats_granularity_key"
    }

    private var _revenueStatsState = MutableLiveData<RevenueStatsViewState>()
    val revenueStatsState: LiveData<RevenueStatsViewState> = _revenueStatsState

    private var _visitorStatsState = MutableLiveData<VisitorStatsViewState>()
    val visitorStatsState: LiveData<VisitorStatsViewState> = _visitorStatsState

    private var _topPerformersState = MutableLiveData<TopPerformersViewState>()
    val topPerformersState: LiveData<TopPerformersViewState> = _topPerformersState

    private var _hasOrders = MutableLiveData<OrderState>()
    val hasOrders: LiveData<OrderState> = _hasOrders

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var activeStatsGranularity = MutableStateFlow(
        savedState.get<StatsGranularity>(ACTIVE_STATS_GRANULARITY_KEY) ?: StatsGranularity.DAYS
    )

    @VisibleForTesting val refreshStoreStats = BooleanArray(StatsGranularity.values().size) { true }
    @VisibleForTesting val refreshTopPerformerStats = BooleanArray(StatsGranularity.values().size) { true }

    private var jetpackMonitoringJob: Job? = null

    init {
        ConnectionChangeReceiver.getEventBus().register(this)
        viewModelScope.launch {
            combine(
                activeStatsGranularity,
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
        activeStatsGranularity.update { granularity }
        savedState[ACTIVE_STATS_GRANULARITY_KEY] = granularity
    }

    fun onSwipeToRefresh() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.DASHBOARD_PULLED_TO_REFRESH)
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
                    IsJetPackCPEnabled -> onJetPackCpConnected()
                    is HasOrders -> _hasOrders.value = if (it.hasOrder) OrderState.AtLeastOne else OrderState.Empty
                }
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
        AnalyticsTracker.track(
            AnalyticsTracker.Stat.DASHBOARD_MAIN_STATS_LOADED,
            mapOf(AnalyticsTracker.KEY_RANGE to selectedGranularity.name.lowercase())
        )
    }

    private fun onJetPackCpConnected() {
        val daysSinceDismissal = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - appPrefsWrapper.getJetpackBenefitsDismissalDate()
        )
        val showBanner = daysSinceDismissal >= DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER
        val benefitsBanner =
            BenefitsBannerUiModel(
                show = showBanner,
                onDismiss = {
                    _visitorStatsState.value =
                        VisitorStatsViewState.JetpackCpConnected(BenefitsBannerUiModel(show = false))
                    appPrefsWrapper.recordJetpackBenefitsDismissal()
                    AnalyticsTracker.track(
                        stat = AnalyticsTracker.Stat.FEATURE_JETPACK_BENEFITS_BANNER,
                        properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "dismissed")
                    )
                }
            )
        _visitorStatsState.value = VisitorStatsViewState.JetpackCpConnected(benefitsBanner)
        monitorJetpackInstallation()
    }

    private fun monitorJetpackInstallation() {
        jetpackMonitoringJob?.cancel()
        jetpackMonitoringJob = viewModelScope.launch {
            selectedSite.observe()
                .filter { it?.isJetpackConnected == true }
                .take(1)
                .collect { loadStoreStats(activeStatsGranularity.value) }
        }
    }

    private suspend fun loadTopPerformersStats(granularity: StatsGranularity) {
        if (!networkStatus.isConnected()) {
            refreshTopPerformerStats[granularity.ordinal] = true
            _topPerformersState.value = TopPerformersViewState.Content(emptyList(), granularity)
            return
        }

        val forceRefresh = refreshTopPerformerStats[granularity.ordinal]
        if (forceRefresh) {
            refreshTopPerformerStats[granularity.ordinal] = false
        }

        _topPerformersState.value = TopPerformersViewState.Loading
        getTopPerformers(forceRefresh, granularity, NUM_TOP_PERFORMERS)
            .collect {
                when (it) {
                    is TopPerformersSuccess -> {
                        _topPerformersState.value =
                            TopPerformersViewState.Content(
                                it.topPerformers.toTopPerformersUiList(),
                                granularity
                            )
                        AnalyticsTracker.track(
                            AnalyticsTracker.Stat.DASHBOARD_TOP_PERFORMERS_LOADED,
                            mapOf(AnalyticsTracker.KEY_RANGE to granularity.name.lowercase())
                        )
                    }
                    TopPerformersError -> _topPerformersState.value = TopPerformersViewState.Error
                }
            }
    }

    private fun resetForceRefresh() {
        refreshTopPerformerStats.forEachIndexed { index, _ ->
            refreshTopPerformerStats[index] = true
        }
        refreshStoreStats.forEachIndexed { index, _ ->
            refreshStoreStats[index] = true
        }
    }

    private fun onTopPerformerSelected(productId: Long) {
        triggerEvent(MyStoreEvent.OpenTopPerformer(productId))
        AnalyticsTracker.track(AnalyticsTracker.Stat.TOP_EARNER_PRODUCT_TAPPED)
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

    private fun List<WCTopPerformerProductModel>.toTopPerformersUiList() = map { it.toTopPerformersUiModel() }

    private fun WCTopPerformerProductModel.toTopPerformersUiModel() =
        TopPerformerProductUiModel(
            productId = product.remoteProductId,
            name = StringEscapeUtils.unescapeHtml4(product.name),
            timesOrdered = FormatUtils.formatDecimal(quantity),
            netSales = resourceProvider.getString(
                R.string.dashboard_top_performers_net_sales,
                getTotalSpendFormatted(total.toBigDecimal(), currency)
            ),
            imageUrl = product.getFirstImageUrl()?.toImageUrl(),
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
        data class JetpackCpConnected(
            val benefitsBanner: BenefitsBannerUiModel
        ) : VisitorStatsViewState()

        data class Content(
            val stats: Map<String, Int>
        ) : VisitorStatsViewState()
    }

    sealed class TopPerformersViewState {
        object Loading : TopPerformersViewState()
        object Error : TopPerformersViewState()
        data class Content(
            val topPerformers: List<TopPerformerProductUiModel> = emptyList(),
            val granularity: StatsGranularity
        ) : TopPerformersViewState()
    }

    sealed class OrderState {
        object Empty : OrderState()
        object AtLeastOne : OrderState()
    }

    sealed class MyStoreEvent : MultiLiveEvent.Event() {
        data class OpenTopPerformer(
            val productId: Long
        ) : MyStoreEvent()
    }
}
