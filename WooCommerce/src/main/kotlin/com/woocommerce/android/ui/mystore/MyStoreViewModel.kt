package com.woocommerce.android.ui.mystore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.collect
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MyStoreViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val wooCommerceStore: WooCommerceStore, // Required to ensure the WooCommerceStore is initialized!
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

    private var activeStatsGranularity: StatsGranularity =
        savedState.get<StatsGranularity>(ACTIVE_STATS_GRANULARITY_KEY) ?: StatsGranularity.DAYS

    private var _revenueStatsState = MutableLiveData<RevenueStatsViewState>()
    val revenueStatsState: LiveData<RevenueStatsViewState> = _revenueStatsState

    private var _visitorStatsState = MutableLiveData<VisitorStatsViewState>()
    val visitorStatsState: LiveData<VisitorStatsViewState> = _visitorStatsState

    private var _topPerformersState = MutableLiveData<TopPerformersViewState>()
    val topPerformersState: LiveData<TopPerformersViewState> = _topPerformersState

    private var _hasOrders = MutableLiveData<OrderState>()
    val hasOrders: LiveData<OrderState> = _hasOrders

    private var _jetpackBenefitsBanerState = MutableLiveData<JetpackBenefitsBannerState>()
    val jetpackBenefitsBanerState: LiveData<JetpackBenefitsBannerState> = _jetpackBenefitsBanerState

    private val refreshStoreStats = BooleanArray(StatsGranularity.values().size)
    private val refreshTopPerformerStats = BooleanArray(StatsGranularity.values().size)

    init {
        ConnectionChangeReceiver.getEventBus().register(this)
        refreshAll()
        showJetpackBenefitsIfNeeded()
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
                refreshAll()
            }
        }
    }

    fun onStatsGranularityChanged(granularity: StatsGranularity) {
        activeStatsGranularity = granularity
        savedState[ACTIVE_STATS_GRANULARITY_KEY] = granularity
        loadStoreStats()
        loadTopPerformersStats()
    }

    fun onSwipeToRefresh() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.DASHBOARD_PULLED_TO_REFRESH)
        refreshAll()
    }

    fun getSelectedSiteName(): String =
        selectedSite.getIfExists()?.let { site ->
            if (!site.displayName.isNullOrBlank()) {
                site.displayName
            } else {
                site.name
            }
        } ?: ""

    private fun refreshAll() {
        resetForceRefresh()
        loadStoreStats()
        loadTopPerformersStats()
    }

    private fun showJetpackBenefitsIfNeeded() {
        val showBanner = if (selectedSite.getIfExists()?.isJetpackCPConnected == true) {
            val daysSinceDismissal = TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - appPrefsWrapper.getJetpackBenefitsDismissalDate()
            )
            daysSinceDismissal >= DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER
        } else false

        when (showBanner) {
            false -> _jetpackBenefitsBanerState.value = JetpackBenefitsBannerState.Hide
            else -> {
                AnalyticsTracker.track(
                    stat = AnalyticsTracker.Stat.FEATURE_JETPACK_BENEFITS_BANNER,
                    properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "shown")
                )
                _jetpackBenefitsBanerState.value = JetpackBenefitsBannerState.Show(
                    onDismiss = {
                        _jetpackBenefitsBanerState.value = JetpackBenefitsBannerState.Hide
                        appPrefsWrapper.recordJetpackBenefitsDismissal()
                        AnalyticsTracker.track(
                            stat = AnalyticsTracker.Stat.FEATURE_JETPACK_BENEFITS_BANNER,
                            properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "dismissed")
                        )
                    }
                )
            }
        }
    }

    private fun loadStoreStats() {
        if (!networkStatus.isConnected()) {
            refreshStoreStats[activeStatsGranularity.ordinal] = true
            _revenueStatsState.value = RevenueStatsViewState.Content(null)
            _visitorStatsState.value = VisitorStatsViewState.Content(emptyMap())
            return
        }

        val forceRefresh = refreshStoreStats[activeStatsGranularity.ordinal]
        if (forceRefresh) {
            refreshStoreStats[activeStatsGranularity.ordinal] = false
        }
        _revenueStatsState.value = RevenueStatsViewState.Loading
        launch {
            getStats(forceRefresh, activeStatsGranularity)
                .collect {
                    when (it) {
                        is RevenueStatsSuccess -> {
                            _revenueStatsState.value = RevenueStatsViewState.Content(it.stats?.toStoreStatsUiModel())
                            AnalyticsTracker.track(
                                AnalyticsTracker.Stat.DASHBOARD_MAIN_STATS_LOADED,
                                mapOf(AnalyticsTracker.KEY_RANGE to activeStatsGranularity.name.lowercase())
                            )
                        }
                        is RevenueStatsError -> _revenueStatsState.value = RevenueStatsViewState.GenericError
                        PluginNotActive -> _revenueStatsState.value = RevenueStatsViewState.PluginNotActiveError
                        is VisitorsStatsSuccess -> _visitorStatsState.value = VisitorStatsViewState.Content(it.stats)
                        is VisitorsStatsError -> _visitorStatsState.value = VisitorStatsViewState.Error
                        IsJetPackCPEnabled -> _visitorStatsState.value = VisitorStatsViewState.JetPackCPEmpty
                        is HasOrders -> _hasOrders.value =
                            if (it.hasOrder) OrderState.AtLeastOne
                            else OrderState.Empty
                    }
                }
        }
    }

    private fun loadTopPerformersStats() {
        if (!networkStatus.isConnected()) {
            refreshTopPerformerStats[activeStatsGranularity.ordinal] = true
            _topPerformersState.value = TopPerformersViewState.Content(emptyList())
            return
        }

        val forceRefresh = refreshTopPerformerStats[activeStatsGranularity.ordinal]
        if (forceRefresh) {
            refreshTopPerformerStats[activeStatsGranularity.ordinal] = false
        }

        _topPerformersState.value = TopPerformersViewState.Loading
        launch {
            getTopPerformers(forceRefresh, activeStatsGranularity, NUM_TOP_PERFORMERS)
                .collect {
                    when (it) {
                        is TopPerformersSuccess -> {
                            _topPerformersState.value =
                                TopPerformersViewState.Content(it.topPerformers.toTopPerformersUiList())
                            AnalyticsTracker.track(
                                AnalyticsTracker.Stat.DASHBOARD_TOP_PERFORMERS_LOADED,
                                mapOf(AnalyticsTracker.KEY_RANGE to activeStatsGranularity.name.lowercase())
                            )
                        }
                        TopPerformersError -> _topPerformersState.value = TopPerformersViewState.Error
                    }
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

    sealed class RevenueStatsViewState {
        object Loading : RevenueStatsViewState()
        object GenericError : RevenueStatsViewState()
        object PluginNotActiveError : RevenueStatsViewState()
        data class Content(
            val revenueStats: RevenueStatsUiModel?
        ) : RevenueStatsViewState()
    }

    sealed class VisitorStatsViewState {
        object Error : VisitorStatsViewState()
        object JetPackCPEmpty : VisitorStatsViewState()
        data class Content(
            val stats: Map<String, Int>
        ) : VisitorStatsViewState()
    }

    sealed class TopPerformersViewState {
        object Loading : TopPerformersViewState()
        object Error : TopPerformersViewState()
        data class Content(
            val topPerformers: List<TopPerformerProductUiModel> = emptyList(),
        ) : TopPerformersViewState()
    }

    sealed class OrderState {
        object Empty : OrderState()
        object AtLeastOne : OrderState()
    }

    sealed class JetpackBenefitsBannerState {
        data class Show(
            val onDismiss: () -> Unit
        ) : JetpackBenefitsBannerState()

        object Hide : JetpackBenefitsBannerState()
    }

    sealed class MyStoreEvent : MultiLiveEvent.Event() {
        data class OpenTopPerformer(
            val productId: Long
        ) : MyStoreEvent()
    }

    data class RevenueStatsUiModel(
        val intervalList: List<StatsIntervalUiModel> = emptyList(),
        val totalOrdersCount: Int? = null,
        val totalSales: Double? = null,
        val currencyCode: String?
    )

    data class StatsIntervalUiModel(
        val interval: String? = null,
        val ordersCount: Long? = null,
        val sales: Double? = null
    )

    data class TopPerformerProductUiModel(
        val productId: Long,
        val name: String,
        val timesOrdered: String,
        val totalSpend: String,
        val imageUrl: String?,
        val onClick: (Long) -> Unit
    )

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
            totalSpend = currencyFormatter.formatCurrencyRounded(
                total,
                wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: currency
            ),
            imageUrl = product.getFirstImageUrl()?.toImageUrl(),
            onClick = ::onTopPerformerSelected
        )

    private fun String.toImageUrl() =
        PhotonUtils.getPhotonImageUrl(
            this,
            resourceProvider.getDimensionPixelSize(R.dimen.image_minor_100),
            0
        )
}
