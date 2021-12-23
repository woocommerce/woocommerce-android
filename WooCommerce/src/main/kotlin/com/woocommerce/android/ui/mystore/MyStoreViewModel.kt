package com.woocommerce.android.ui.mystore

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.domain.GetStats
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.*
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.TopPerformersResult.TopPerformersError
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.TopPerformersResult.TopPerformersSuccess
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.apache.commons.text.StringEscapeUtils
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
    }

    private var activeStatsGranularity: StatsGranularity = StatsGranularity.DAYS

    val revenueStatsState: LiveDataDelegate<RevenueStatsViewState> = LiveDataDelegate(
        savedState,
        RevenueStatsViewState.Loading
    )
    private var _revenueStatsState by revenueStatsState

    val visitorStatsState: LiveDataDelegate<VisitorStatsViewState> = LiveDataDelegate(
        savedState,
        VisitorStatsViewState.Content(emptyMap())
    )
    private var _visitorStatsState by visitorStatsState

    val topPerformersState: LiveDataDelegate<TopPerformersViewState> = LiveDataDelegate(
        savedState,
        TopPerformersViewState.Loading
    )
    private var _topPerformersState by topPerformersState

    val hasOrders: LiveDataDelegate<OrderState> = LiveDataDelegate(
        savedState,
        OrderState.AtLeastOne
    )
    private var _hasOrders by hasOrders

    val jetpackBenefitsBanerState: LiveDataDelegate<JetpackBenefitsBannerState> = LiveDataDelegate(
        savedState,
        JetpackBenefitsBannerState.Hide
    )
    private var _jetpackBenefitsBanerState by jetpackBenefitsBanerState


    private val refreshStoreStats = BooleanArray(StatsGranularity.values().size)
    private val refreshTopPerformerStats = BooleanArray(StatsGranularity.values().size)

    init {
        resetForceRefresh()
        loadStoreStats()
        loadTopPerformersStats()
        showJetpackBenefitsIfNeeded()
    }

    fun onStatsGranularityChanged(granularity: StatsGranularity) {
        activeStatsGranularity = granularity
        loadStoreStats()
        loadTopPerformersStats()
    }

    fun onSwipeToRefresh() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.DASHBOARD_PULLED_TO_REFRESH)
        resetForceRefresh()
        loadStoreStats()
        loadTopPerformersStats()
    }

    fun getSelectedSiteName(): String? =
        selectedSite.getIfExists()?.let { site ->
            if (!site.displayName.isNullOrBlank()) {
                site.displayName
            } else {
                site.name
            }
        }

    private fun showJetpackBenefitsIfNeeded() {
        val showBanner = if (selectedSite.getIfExists()?.isJetpackCPConnected == true) {
            val daysSinceDismissal = TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - appPrefsWrapper.getJetpackBenefitsDismissalDate()
            )
            daysSinceDismissal >= DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER
        } else false

        when (showBanner) {
            false -> _jetpackBenefitsBanerState = JetpackBenefitsBannerState.Hide
            else -> {
                _jetpackBenefitsBanerState = JetpackBenefitsBannerState.Show(
                    onDismiss = {
                        _jetpackBenefitsBanerState = JetpackBenefitsBannerState.Hide
                        appPrefsWrapper.recordJetpackBenefitsDismissal()
                    }
                )
            }
        }
    }

    private fun loadStoreStats() {
        if (!networkStatus.isConnected()) {
            refreshStoreStats[activeStatsGranularity.ordinal] = true
            return
        }

        val forceRefresh = refreshStoreStats[activeStatsGranularity.ordinal]
        if (forceRefresh) {
            refreshStoreStats[activeStatsGranularity.ordinal] = false
        }
        _revenueStatsState = RevenueStatsViewState.Loading
        launch {
            getStats(forceRefresh, activeStatsGranularity)
                .collect {
                    when (it) {
                        is RevenueStatsSuccess -> {
                            _revenueStatsState = RevenueStatsViewState.Content(it.stats?.toStoreStatsUiModel())
                            AnalyticsTracker.track(
                                AnalyticsTracker.Stat.DASHBOARD_MAIN_STATS_LOADED,
                                mapOf(AnalyticsTracker.KEY_RANGE to activeStatsGranularity.name.lowercase())
                            )
                        }
                        is RevenueStatsError -> _revenueStatsState = RevenueStatsViewState.GenericError
                        PluginNotActive -> _revenueStatsState = RevenueStatsViewState.PluginNotActiveError
                        is VisitorsStatsSuccess -> _visitorStatsState = VisitorStatsViewState.Content(it.stats)
                        is VisitorsStatsError -> _visitorStatsState = VisitorStatsViewState.Error
                        IsJetPackCPEnabled -> _visitorStatsState = VisitorStatsViewState.JetPackCPEmpty
                        is HasOrders -> _hasOrders =
                            if (it.hasOrder) OrderState.AtLeastOne
                            else OrderState.Empty
                    }
                }
        }
    }

    private fun loadTopPerformersStats() {
        if (!networkStatus.isConnected()) {
            refreshTopPerformerStats[activeStatsGranularity.ordinal] = true
            return
        }

        val forceRefresh = refreshTopPerformerStats[activeStatsGranularity.ordinal]
        if (forceRefresh) {
            refreshTopPerformerStats[activeStatsGranularity.ordinal] = false
        }

        _topPerformersState = TopPerformersViewState.Loading
        launch {
            getTopPerformers(forceRefresh, activeStatsGranularity, NUM_TOP_PERFORMERS)
                .collect {
                    when (it) {
                        is TopPerformersSuccess -> {
                            _topPerformersState =
                                TopPerformersViewState.Content(it.topPerformers.toTopPerformersUiList())
                            AnalyticsTracker.track(
                                AnalyticsTracker.Stat.DASHBOARD_TOP_PERFORMERS_LOADED,
                                mapOf(AnalyticsTracker.KEY_RANGE to activeStatsGranularity.name.lowercase())
                            )
                        }
                        TopPerformersError -> _topPerformersState = TopPerformersViewState.Error
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

    sealed class RevenueStatsViewState : Parcelable {
        @Parcelize
        object Loading : RevenueStatsViewState()

        @Parcelize
        object GenericError : RevenueStatsViewState()

        @Parcelize
        object PluginNotActiveError : RevenueStatsViewState()

        @Parcelize
        data class Content(
            val revenueStats: RevenueStatsUiModel?
        ) : RevenueStatsViewState()
    }

    sealed class VisitorStatsViewState : Parcelable {
        @Parcelize
        object Error : VisitorStatsViewState()

        @Parcelize
        object JetPackCPEmpty : VisitorStatsViewState()

        @Parcelize
        data class Content(
            val stats: Map<String, Int>
        ) : VisitorStatsViewState()
    }

    sealed class TopPerformersViewState : Parcelable {
        @Parcelize
        object Loading : TopPerformersViewState()

        @Parcelize
        object Error : TopPerformersViewState()

        @Parcelize
        data class Content(
            val topPerformers: List<TopPerformerProductUiModel> = emptyList(),
        ) : TopPerformersViewState()
    }

    sealed class OrderState : Parcelable {
        @Parcelize
        object Empty : OrderState()

        @Parcelize
        object AtLeastOne : OrderState()
    }

    sealed class JetpackBenefitsBannerState : Parcelable {
        @Parcelize
        data class Show(
            val onDismiss: () -> Unit
        ) : JetpackBenefitsBannerState()

        @Parcelize
        object Hide : JetpackBenefitsBannerState()
    }


    sealed class MyStoreEvent : MultiLiveEvent.Event() {
        data class OpenTopPerformer(
            val productId: Long
        ) : MyStoreEvent()
    }

    @Parcelize
    data class RevenueStatsUiModel(
        val intervalList: List<StatsIntervalUiModel> = emptyList(),
        val totalOrdersCount: Int? = null,
        val totalSales: Double? = null,
        val currencyCode: String?
    ) : Parcelable

    @Parcelize
    data class StatsIntervalUiModel(
        val interval: String? = null,
        val ordersCount: Long? = null,
        val sales: Double? = null
    ) : Parcelable

    @Parcelize
    data class TopPerformerProductUiModel(
        val productId: Long,
        val name: String,
        val timesOrdered: String,
        val totalSpend: String,
        val imageUrl: String?,
        val onClick: (Long) -> Unit
    ) : Parcelable

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
