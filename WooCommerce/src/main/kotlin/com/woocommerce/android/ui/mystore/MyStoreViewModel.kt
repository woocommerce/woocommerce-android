package com.woocommerce.android.ui.mystore

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
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
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.PhotonUtils
import javax.inject.Inject

@HiltViewModel
class MyStoreViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val getTopPerformers: GetTopPerformers,
    private val currencyFormatter: CurrencyFormatter,
) : ScopedViewModel(savedState) {
    companion object {
        const val NUM_TOP_PERFORMERS = 5
    }

    val topPerformersState: LiveDataDelegate<TopPerformersViewState> = LiveDataDelegate(
        savedState,
        TopPerformersViewState.Loading
    )
    private var _topPerformersState by topPerformersState

    private val refreshStoreStats = BooleanArray(StatsGranularity.values().size)
    private val refreshTopPerformerStats = BooleanArray(StatsGranularity.values().size)

    private var activeStatsGranularity: StatsGranularity = StatsGranularity.DAYS

    init {
        refreshTopPerformerStats.forEachIndexed { index, _ ->
            refreshTopPerformerStats[index] = true
        }
        refreshStoreStats.forEachIndexed { index, _ ->
            refreshStoreStats[index] = true
        }
        loadTopPerformersStats()
    }

    fun onStatsGranularityChanged(granularity: StatsGranularity) {
        activeStatsGranularity = granularity
        loadTopPerformersStats()
    }

    fun loadTopPerformersStats(forced: Boolean = false) {
        if (!networkStatus.isConnected()) {
            refreshTopPerformerStats[activeStatsGranularity.ordinal] = true
            return
        }

        val forceRefresh = refreshTopPerformerStats[activeStatsGranularity.ordinal] || forced
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

    private fun onTopPerformerSelected(productId: Long) {
        triggerEvent(MyStoreEvent.OpenTopPerformer(productId))
        AnalyticsTracker.track(AnalyticsTracker.Stat.TOP_EARNER_PRODUCT_TAPPED)
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
