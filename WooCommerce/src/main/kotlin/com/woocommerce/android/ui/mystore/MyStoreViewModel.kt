package com.woocommerce.android.ui.mystore

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.tools.NetworkStatus
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
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.PhotonUtils
import javax.inject.Inject

@HiltViewModel
class MyStoreViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val wooCommerceStore: WooCommerceStore,
    private val getTopPerformers: GetTopPerformers,
    private val currencyFormatter: CurrencyFormatter,
) : ScopedViewModel(savedState) {
    companion object {
        const val NUM_TOP_PERFORMERS = 5
    }

    val viewState = LiveDataDelegate(
        savedState,
        ViewState(
            activeStatsGranularity = StatsGranularity.DAYS,
            isLoadingTopPerformers = true
        )
    )
    private var _viewState by viewState

    private var isRefreshPending = false
    private val refreshTopPerformerStats = BooleanArray(StatsGranularity.values().size)

    init {
        refreshTopPerformerStats.forEachIndexed { index, _ ->
            refreshTopPerformerStats[index] = true
        }
        loadTopPerformersStats()
    }

    fun onStatsGranularityChanged(granularity: StatsGranularity) {
        _viewState = _viewState.copy(activeStatsGranularity = granularity)
        loadTopPerformersStats()
    }

    private fun loadTopPerformersStats() {
        if (!networkStatus.isConnected()) {
            isRefreshPending = true
            return
        }

        val forceRefresh = refreshTopPerformerStats[_viewState.activeStatsGranularity.ordinal]
        if (forceRefresh) {
            refreshTopPerformerStats[_viewState.activeStatsGranularity.ordinal] = false
        }

        _viewState = _viewState.copy(isLoadingTopPerformers = true)
        launch {
            getTopPerformers(forceRefresh, _viewState.activeStatsGranularity, NUM_TOP_PERFORMERS)
                .collect {
                    _viewState = _viewState.copy(isLoadingTopPerformers = false)
                    when (it) {
                        is TopPerformersSuccess -> {
                            _viewState = _viewState.copy(topPerformers = it.topPerformers.toTopPerformersUiList())
                            AnalyticsTracker.track(
                                AnalyticsTracker.Stat.DASHBOARD_TOP_PERFORMERS_LOADED,
                                mapOf(AnalyticsTracker.KEY_RANGE to _viewState.activeStatsGranularity.name.lowercase())
                            )
                        }
                        TopPerformersError -> _viewState = _viewState.copy(topPerformersError = true)
                    }
                }
        }
    }

    private fun onTopPerformerSelected(productId: Long) {
        triggerEvent(MyStoreEvent.OpenTopPerformer(productId))
        AnalyticsTracker.track(AnalyticsTracker.Stat.TOP_EARNER_PRODUCT_TAPPED)
    }

//    private fun getStatsCurrency() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode


//    @Suppress("unused")
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    fun onEventMainThread(event: ConnectionChangeReceiver.ConnectionChangeEvent) {
//        if (event.isConnected && isRefreshPending) {
//            // Refresh data if needed now that a connection is active
//            myStoreView?.let { view ->
//                if (view.isRefreshPending) {
//                    view.refreshMyStoreStats(forced = false)
//                }
//            }
//        }
//    }

    @Parcelize
    data class ViewState(
        val activeStatsGranularity: StatsGranularity = StatsGranularity.DAYS,
        val topPerformers: List<TopPerformerProductUiModel> = emptyList(),
        val isLoadingTopPerformers: Boolean = false,
        val topPerformersError: Boolean = false
    ) : Parcelable

    sealed class MyStoreEvent : MultiLiveEvent.Event() {
        data class OpenTopPerformer(
            val productId: Long
        ) : MyStoreEvent()
    }

    @Parcelize
    data class TopPerformerProductUiModel(
        val productId: Long,
        val name: String,
        val timesOrdered: String,
        val totalSpend: String,
        val imageUrl: String?,
        val onClick: (Long) -> Unit
    ) : Parcelable

    private fun List<WCTopPerformerProductModel>.toTopPerformersUiList() = map { it.toTopPerformersUiModel() }

    private fun WCTopPerformerProductModel.toTopPerformersUiModel() =
        TopPerformerProductUiModel(
            productId = product.remoteProductId,
            name = StringEscapeUtils.unescapeHtml4(product.name),
            timesOrdered = FormatUtils.formatDecimal(quantity),
            totalSpend = currencyFormatter.formatCurrencyRounded(total, currency),
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
