package com.woocommerce.android.ui.dashboard.topperformers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.DASHBOARD_TOP_PERFORMERS_LOADED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore.AnalyticData.TOP_PERFORMERS
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshState
import com.woocommerce.android.ui.dashboard.TopPerformerProductUiModel
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformers
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformers.TopPerformerProduct
import com.woocommerce.android.ui.dashboard.domain.ObserveLastUpdate
import com.woocommerce.android.ui.dashboard.stats.GetSelectedDateRange
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class DashBoardTopPerformersViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val observeLastUpdate: ObserveLastUpdate,
    private val resourceProvider: ResourceProvider,
    private val getTopPerformers: GetTopPerformers,
    private val currencyFormatter: CurrencyFormatter,
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val wooCommerceStore: WooCommerceStore,
    getSelectedDateRange: GetSelectedDateRange,
) : ScopedViewModel(savedState) {

    private val refreshTrigger = MutableSharedFlow<RefreshState>(extraBufferCapacity = 1)

    private val _selectedDateRange = getSelectedDateRange()
    val selectedDateRange: LiveData<StatsTimeRangeSelection> = _selectedDateRange.asLiveData()

    private var _topPerformersState = MutableLiveData<TopPerformersState>()
    val topPerformersState: LiveData<TopPerformersState> = _topPerformersState

    private var _lastUpdateTopPerformers = MutableLiveData<Long?>()
    val lastUpdateTopPerformers: LiveData<Long?> = _lastUpdateTopPerformers

    init {
        _topPerformersState.value = TopPerformersState(isLoading = true)
        viewModelScope.launch {
            combine(
                _selectedDateRange,
                refreshTrigger.onStart { emit(RefreshState()) }
            ) { selectedRange, refreshEvent ->
                Pair(selectedRange, refreshEvent.shouldRefresh)
            }.collectLatest { (selectedRange, isForceRefresh) ->
                loadTopPerformersStats(selectedRange, isForceRefresh)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTopPerformerUpdates() {
        viewModelScope.launch {
            _selectedDateRange
                .flatMapLatest { dateRange ->
                    getTopPerformers.observeTopPerformers(dateRange)
                }
                .collectLatest {
                    _topPerformersState.value = _topPerformersState.value?.copy(
                        topPerformers = it.toTopPerformersUiList(),
                    )
                }
        }
    }

    private fun onTopPerformerSelected(productId: Long) {
        triggerEvent(DashboardEvent.OpenTopPerformer(productId))
        analyticsTrackerWrapper.track(AnalyticsEvent.TOP_EARNER_PRODUCT_TAPPED)
        usageTracksEventEmitter.interacted()
    }

    private suspend fun loadTopPerformersStats(selectedRange: StatsTimeRangeSelection, forceRefresh: Boolean) =
        coroutineScope {
            if (!networkStatus.isConnected()) return@coroutineScope

            _topPerformersState.value = _topPerformersState.value?.copy(isLoading = true, isError = false)
            val result = getTopPerformers.fetchTopPerformers(selectedRange, forceRefresh)
            result.fold(
                onFailure = { _topPerformersState.value = _topPerformersState.value?.copy(isError = true) },
                onSuccess = {
                    analyticsTrackerWrapper.track(
                        DASHBOARD_TOP_PERFORMERS_LOADED,
                        mapOf(AnalyticsTracker.KEY_RANGE to selectedRange.selectionType.identifier)
                    )
                }
            )
            _topPerformersState.value = _topPerformersState.value?.copy(isLoading = false)

            launch {
                observeLastUpdate(
                    selectedRange,
                    TOP_PERFORMERS
                ).collect { lastUpdateMillis -> _lastUpdateTopPerformers.value = lastUpdateMillis }
            }
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

    data class TopPerformersState(
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val topPerformers: List<TopPerformerProductUiModel> = emptyList(),
    )
}
