package com.woocommerce.android.ui.dashboard.topperformers

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.DASHBOARD_TOP_PERFORMERS_LOADED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore.AnalyticData
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.TopPerformerProductUiModel
import com.woocommerce.android.ui.dashboard.data.TopPerformersCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.dashboard.domain.DashboardDateRangeFormatter
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformers
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformers.TopPerformerProduct
import com.woocommerce.android.ui.dashboard.domain.ObserveLastUpdate
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = DashboardTopPerformersViewModel.Factory::class)
@Suppress("LongParameterList")
class DashboardTopPerformersViewModel @AssistedInject constructor(
    savedState: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val observeLastUpdate: ObserveLastUpdate,
    private val resourceProvider: ResourceProvider,
    private val getTopPerformers: GetTopPerformers,
    private val currencyFormatter: CurrencyFormatter,
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val wooCommerceStore: WooCommerceStore,
    private val dateUtils: DateUtils,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val customDateRangeDataStore: TopPerformersCustomDateRangeDataStore,
    private val dateFormatter: DashboardDateRangeFormatter,
    getSelectedDateRange: GetSelectedRangeForTopPerformers,
) : ScopedViewModel(savedState) {
    private val _selectedDateRange = getSelectedDateRange()
    val selectedDateRange: LiveData<TopPerformersDateRange> = combine(
        _selectedDateRange,
        customDateRangeDataStore.dateRange
    ) { selectedRange, customRange ->
        TopPerformersDateRange(
            rangeSelection = selectedRange,
            customRange = customRange,
            dateFormatted = dateFormatter.formatRangeDate(selectedRange)
        )
    }.asLiveData()

    private var _topPerformersState = MutableLiveData<TopPerformersState>()
    val topPerformersState: LiveData<TopPerformersState> = _topPerformersState

    private var _lastUpdateTopPerformers = MutableStateFlow<Long?>(null)
    val lastUpdateTopPerformers: LiveData<String?> = _lastUpdateTopPerformers
        .map { lastUpdateMillis ->
            if (lastUpdateMillis == null) return@map null
            String.format(
                Locale.getDefault(),
                resourceProvider.getString(R.string.last_update),
                dateUtils.getDateOrTimeFromMillis(lastUpdateMillis)
            )
        }.asLiveData()

    private val refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)

    init {
        _topPerformersState.value = TopPerformersState(
            isLoading = true,
            titleStringRes = DashboardWidget.Type.POPULAR_PRODUCTS.titleResource,
            menu = DashboardWidgetMenu(
                items = listOf(
                    DashboardWidget.Type.POPULAR_PRODUCTS.defaultHideMenuEntry {
                        parentViewModel.onHideWidgetClicked(DashboardWidget.Type.POPULAR_PRODUCTS)
                    }
                )
            ),
            onOpenAnalyticsTapped = DashboardWidgetAction(
                titleResource = R.string.analytics_section_see_all,
                action = ::onViewAllAnalyticsTapped
            )
        )

        viewModelScope.launch {
            _selectedDateRange.flatMapLatest { selectedRange ->
                merge(refreshTrigger, parentViewModel.refreshTrigger)
                    .onStart { emit(RefreshEvent()) }
                    .map {
                        Pair(selectedRange, it.isForced)
                    }
            }.collectLatest { (selectedRange, isForceRefresh) ->
                loadTopPerformersStats(selectedRange, isForceRefresh)
            }
        }
        observeTopPerformerUpdates()
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

    fun onTabSelected(selectionType: SelectionType) {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.POPULAR_PRODUCTS.trackingIdentifier)
        usageTracksEventEmitter.interacted()
        if (selectionType != SelectionType.CUSTOM) {
            appPrefsWrapper.setActiveTopPerformersTab(selectionType.name)
        } else {
            if (selectedDateRange.value?.customRange == null) {
                onEditCustomRangeTapped()
            } else {
                appPrefsWrapper.setActiveTopPerformersTab(SelectionType.CUSTOM.name)
                analyticsTrackerWrapper.track(AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_TAB_SELECTED)
            }
        }
    }

    fun onEditCustomRangeTapped() {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.POPULAR_PRODUCTS.trackingIdentifier)
        val event = if (selectedDateRange.value?.customRange == null) {
            AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_ADD_BUTTON_TAPPED
        } else {
            AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_EDIT_BUTTON_TAPPED
        }
        analyticsTrackerWrapper.track(event)

        triggerEvent(
            OpenDatePicker(
                fromDate = selectedDateRange.value?.customRange?.start ?: Date(),
                toDate = selectedDateRange.value?.customRange?.end ?: Date()
            )
        )
    }

    fun onRefresh() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_RETRY_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_TYPE to DashboardWidget.Type.POPULAR_PRODUCTS.trackingIdentifier
            )
        )
        refreshTrigger.tryEmit(RefreshEvent(isForced = true))
    }

    private fun onTopPerformerTapped(productId: Long) {
        triggerEvent(OpenTopPerformer(productId))
        parentViewModel.trackCardInteracted(DashboardWidget.Type.POPULAR_PRODUCTS.trackingIdentifier)
        analyticsTrackerWrapper.track(AnalyticsEvent.TOP_EARNER_PRODUCT_TAPPED)
        usageTracksEventEmitter.interacted()
    }

    private suspend fun loadTopPerformersStats(selectedRange: StatsTimeRangeSelection, forceRefresh: Boolean) =
        coroutineScope {
            if (!networkStatus.isConnected()) {
                _topPerformersState.value = _topPerformersState.value?.copy(error = ErrorType.Generic)
                return@coroutineScope
            }

            _topPerformersState.value = _topPerformersState.value?.copy(isLoading = true, error = null)
            val result = getTopPerformers.fetchTopPerformers(selectedRange, forceRefresh)
            result.fold(
                onFailure = {
                    _topPerformersState.value = _topPerformersState.value?.copy(
                        error = if ((it as? WooException)?.error?.type == WooErrorType.API_NOT_FOUND) {
                            ErrorType.WCAnalyticsInactive
                        } else {
                            ErrorType.Generic
                        },
                    )
                },
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
                    AnalyticData.TOP_PERFORMERS
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
            onClick = ::onTopPerformerTapped
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

    fun onCustomRangeSelected(statsTimeRange: StatsTimeRange) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_CONFIRMED,
            mapOf(
                AnalyticsTracker.KEY_IS_EDITING to (selectedDateRange.value?.customRange != null),
            )
        )
        viewModelScope.launch {
            customDateRangeDataStore.updateDateRange(statsTimeRange)
            if (selectedDateRange.value?.rangeSelection?.selectionType != SelectionType.CUSTOM) {
                onTabSelected(SelectionType.CUSTOM)
            }
        }
    }

    private fun onViewAllAnalyticsTapped() {
        AnalyticsTracker.track(AnalyticsEvent.DASHBOARD_SEE_MORE_ANALYTICS_TAPPED)
        selectedDateRange.value?.let {
            triggerEvent(OpenAnalytics(it.rangeSelection))
        }
    }

    data class TopPerformersDateRange(
        val rangeSelection: StatsTimeRangeSelection,
        val customRange: StatsTimeRange?,
        val dateFormatted: String
    )

    data class TopPerformersState(
        val isLoading: Boolean = false,
        val error: ErrorType? = null,
        @StringRes val titleStringRes: Int,
        val topPerformers: List<TopPerformerProductUiModel> = emptyList(),
        val menu: DashboardWidgetMenu,
        val onOpenAnalyticsTapped: DashboardWidgetAction
    )

    enum class ErrorType {
        Generic, WCAnalyticsInactive
    }

    data class OpenTopPerformer(
        val productId: Long
    ) : MultiLiveEvent.Event()

    data class OpenDatePicker(val fromDate: Date, val toDate: Date) : MultiLiveEvent.Event()
    data class OpenAnalytics(val analyticsPeriod: StatsTimeRangeSelection) : MultiLiveEvent.Event()

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardTopPerformersViewModel
    }
}
