package com.woocommerce.android.ui.dashboard.salesbychannel

import androidx.annotation.StringRes
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
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore.AnalyticData
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.data.SalesByChannelCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.dashboard.domain.DashboardDateRangeFormatter
import com.woocommerce.android.ui.dashboard.domain.GetSalesByChannel
import com.woocommerce.android.ui.dashboard.domain.GetSalesByChannel.ChannelSales
import com.woocommerce.android.ui.dashboard.domain.GetSalesByChannel.SalesByChannelResult
import com.woocommerce.android.ui.dashboard.domain.ObserveLastUpdate
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.commons.stats.StatsTimeRange
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
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = DashboardSalesByChannelViewModel.Factory::class)
@Suppress("LongParameterList")
class DashboardSalesByChannelViewModel @AssistedInject constructor(
    @Assisted private val parentViewModel: DashboardViewModel,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val observeLastUpdate: ObserveLastUpdate,
    private val resourceProvider: ResourceProvider,
    private val getSalesByChannel: GetSalesByChannel,
    private val currencyFormatter: CurrencyFormatter,
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val wooCommerceStore: WooCommerceStore,
    private val dateUtils: DateUtils,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val customDateRangeDataStore: SalesByChannelCustomDateRangeDataStore,
    private val dateFormatter: DashboardDateRangeFormatter,
    getSelectedDateRange: GetSelectedRangeForSalesByChannel,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    private val _selectedDateRange = getSelectedDateRange()
    val selectedDateRange: LiveData<SalesByChannelDateRange> = combine(
        _selectedDateRange,
        customDateRangeDataStore.dateRange
    ) { selectedRange, customRange ->
        SalesByChannelDateRange(
            rangeSelection = selectedRange,
            customRange = customRange,
            dateFormatted = dateFormatter.formatRangeDate(selectedRange)
        )
    }.asLiveData()

    private var _salesByChannelState = MutableLiveData<SalesByChannelState>()
    val salesByChannelState: LiveData<SalesByChannelState> = _salesByChannelState

    private var _lastUpdate = MutableStateFlow<Long?>(null)
    val lastUpdate: LiveData<String?> = _lastUpdate
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
        _salesByChannelState.value = SalesByChannelState(
            isLoading = true,
            titleStringRes = DashboardWidget.Type.SALES_BY_CHANNEL.titleResource,
            menu = DashboardWidgetMenu(
                items = listOf(
                    DashboardWidget.Type.SALES_BY_CHANNEL.defaultHideMenuEntry {
                        parentViewModel.onHideWidgetClicked(DashboardWidget.Type.SALES_BY_CHANNEL)
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
                loadSalesByChannel(selectedRange, isForceRefresh)
            }
        }
    }

    fun onRangeChanged(selectionType: SelectionType) {
        usageTracksEventEmitter.interacted()
        if (selectionType != SelectionType.CUSTOM) {
            parentViewModel.trackCardInteracted(
                DashboardWidget.Type.SALES_BY_CHANNEL.trackingIdentifier
            )
            appPrefsWrapper.setActiveSalesByChannelTab(selectionType.name)
        } else {
            when {
                selectedDateRange.value?.customRange == null -> onEditCustomRangeTapped()
                else -> {
                    parentViewModel.trackCardInteracted(
                        DashboardWidget.Type.SALES_BY_CHANNEL.trackingIdentifier
                    )
                    appPrefsWrapper.setActiveSalesByChannelTab(SelectionType.CUSTOM.name)
                }
            }
        }
    }

    fun onEditCustomRangeTapped() {
        parentViewModel.trackCardInteracted(
            DashboardWidget.Type.SALES_BY_CHANNEL.trackingIdentifier
        )
        if (selectedDateRange.value?.customRange == null) {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_ADD_BUTTON_TAPPED
            )
        } else {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_EDIT_BUTTON_TAPPED
            )
        }

        triggerEvent(
            OpenDatePicker(
                fromDate = selectedDateRange.value?.customRange?.start ?: Date(),
                toDate = selectedDateRange.value?.customRange?.end ?: Date()
            )
        )
    }

    fun onRefresh() {
        trackEvent(AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_RETRY_TAPPED)
        refreshTrigger.tryEmit(RefreshEvent(isForced = true))
    }

    fun onCustomRangeSelected(statsTimeRange: StatsTimeRange) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_CONFIRMED,
            mapOf(
                AnalyticsTracker.KEY_IS_EDITING to
                    (selectedDateRange.value?.customRange != null),
            )
        )
        viewModelScope.launch {
            customDateRangeDataStore.updateDateRange(statsTimeRange)
            if (selectedDateRange.value?.rangeSelection?.selectionType != SelectionType.CUSTOM) {
                appPrefsWrapper.setActiveSalesByChannelTab(SelectionType.CUSTOM.name)
            }
        }
    }

    private suspend fun loadSalesByChannel(
        selectedRange: StatsTimeRangeSelection,
        forceRefresh: Boolean
    ) = coroutineScope {
        if (!networkStatus.isConnected()) {
            parentViewModel.hideRefreshingIndicator()
            _salesByChannelState.value = _salesByChannelState.value?.copy(
                error = ErrorType.Generic,
                isOutdated = false
            )
            return@coroutineScope
        }

        trackEvent(AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_DATA_LOADING_STARTED)
        getSalesByChannel(selectedRange, forceRefresh).collect { result ->
            when (result) {
                is SalesByChannelResult.Loading -> {
                    parentViewModel.hideRefreshingIndicator()
                    _salesByChannelState.value = _salesByChannelState.value?.copy(
                        isLoading = true
                    )
                }

                is SalesByChannelResult.Error -> {
                    parentViewModel.hideRefreshingIndicator()
                    _salesByChannelState.value = _salesByChannelState.value?.copy(
                        error = ErrorType.Generic,
                        isLoading = false
                    )
                    trackEvent(
                        AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_DATA_LOADING_FAILED,
                        properties = mapOf(
                            AnalyticsTracker.KEY_ERROR to
                                (result.exception.message ?: "unknown")
                        )
                    )
                }

                is SalesByChannelResult.Success -> {
                    trackEvent(AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_DATA_LOADING_COMPLETED)
                    parentViewModel.hideRefreshingIndicator()
                    _salesByChannelState.value = _salesByChannelState.value?.copy(
                        isLoading = false,
                        isOutdated = false,
                        error = null,
                        channels = result.channels.toUiModels()
                    )
                }
            }
        }

        launch {
            observeLastUpdate(
                selectedRange,
                AnalyticData.SALES_BY_CHANNEL
            ).collect { lastUpdateMillis -> _lastUpdate.value = lastUpdateMillis }
        }
    }

    private fun List<ChannelSales>.toUiModels(): List<ChannelSalesUiModel> {
        val maxRevenue = maxOfOrNull { it.revenue } ?: 1.0
        val maxCompareRevenue = maxOfOrNull { it.compareRevenue } ?: 1.0
        val overallMax = maxOf(maxRevenue, maxCompareRevenue).coerceAtLeast(1.0)
        val currencyCode = wooCommerceStore.getSiteSettings(
            selectedSite.get()
        )?.currencyCode.orEmpty()

        return map { channel ->
            val percentageChange = if (channel.compareRevenue != 0.0) {
                ((channel.revenue - channel.compareRevenue) / abs(channel.compareRevenue)) * PERCENTAGE_MULTIPLIER
            } else if (channel.revenue > 0.0) {
                PERCENTAGE_MULTIPLIER
            } else {
                0.0
            }

            val formattedRevenue = currencyFormatter.formatCurrency(
                BigDecimal.valueOf(channel.revenue),
                currencyCode
            )

            val changeSign = if (percentageChange >= 0) "+" else ""
            val formattedChange = String.format(
                Locale.getDefault(),
                "%s%.1f%%",
                changeSign,
                percentageChange
            )

            ChannelSalesUiModel(
                channelName = channel.channelName,
                revenueFormatted = formattedRevenue,
                percentageChange = formattedChange,
                currentBarFraction = (channel.revenue / overallMax).toFloat()
                    .coerceIn(0f, 1f),
                compareBarFraction = (channel.compareRevenue / overallMax).toFloat()
                    .coerceIn(0f, 1f),
                isPositiveChange = percentageChange >= 0
            )
        }
    }

    private fun onViewAllAnalyticsTapped() {
        parentViewModel.trackCardInteracted(
            DashboardWidget.Type.SALES_BY_CHANNEL.trackingIdentifier
        )
        AnalyticsTracker.track(AnalyticsEvent.DASHBOARD_SEE_MORE_ANALYTICS_TAPPED)
        selectedDateRange.value?.let {
            triggerEvent(OpenAnalytics(it.rangeSelection))
        }
    }

    private fun trackEvent(
        event: AnalyticsEvent,
        properties: Map<String, Any> = emptyMap()
    ) {
        analyticsTrackerWrapper.track(
            event,
            properties + mapOf(
                AnalyticsTracker.KEY_TYPE to
                    DashboardWidget.Type.SALES_BY_CHANNEL.trackingIdentifier
            )
        )
    }

    data class SalesByChannelDateRange(
        val rangeSelection: StatsTimeRangeSelection,
        val customRange: StatsTimeRange?,
        val dateFormatted: String
    )

    data class SalesByChannelState(
        val isLoading: Boolean = false,
        val error: ErrorType? = null,
        @StringRes val titleStringRes: Int,
        val channels: List<ChannelSalesUiModel> = emptyList(),
        val menu: DashboardWidgetMenu,
        val onOpenAnalyticsTapped: DashboardWidgetAction,
        val isOutdated: Boolean = false,
    )

    data class ChannelSalesUiModel(
        val channelName: String,
        val revenueFormatted: String,
        val percentageChange: String,
        val currentBarFraction: Float,
        val compareBarFraction: Float,
        val isPositiveChange: Boolean
    )

    enum class ErrorType {
        Generic, WCAnalyticsInactive
    }

    data class OpenDatePicker(
        val fromDate: Date,
        val toDate: Date
    ) : MultiLiveEvent.Event()

    data class OpenAnalytics(
        val analyticsPeriod: StatsTimeRangeSelection
    ) : MultiLiveEvent.Event()

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardSalesByChannelViewModel
    }

    companion object {
        private const val PERCENTAGE_MULTIPLIER = 100.0
    }
}
