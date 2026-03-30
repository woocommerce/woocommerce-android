package com.woocommerce.android.ui.dashboard.topcategories

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
import com.woocommerce.android.ui.dashboard.TopPerformerCategoryUiModel
import com.woocommerce.android.ui.dashboard.data.TopCategoriesCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.dashboard.domain.DashboardDateRangeFormatter
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformerCategories
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformerCategories.TopPerformerCategory
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
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.FormatUtils
import java.math.BigDecimal
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = DashboardTopCategoriesViewModel.Factory::class)
@Suppress("LongParameterList")
class DashboardTopCategoriesViewModel @AssistedInject constructor(
    @Assisted private val parentViewModel: DashboardViewModel,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val observeLastUpdate: ObserveLastUpdate,
    private val resourceProvider: ResourceProvider,
    private val getTopPerformerCategories: GetTopPerformerCategories,
    private val currencyFormatter: CurrencyFormatter,
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val wooCommerceStore: WooCommerceStore,
    private val dateUtils: DateUtils,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val customDateRangeDataStore: TopCategoriesCustomDateRangeDataStore,
    private val dateFormatter: DashboardDateRangeFormatter,
    getSelectedDateRange: GetSelectedRangeForTopCategories,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    private val _selectedDateRange = getSelectedDateRange()
    val selectedDateRange: LiveData<TopCategoriesDateRange> = combine(
        _selectedDateRange,
        customDateRangeDataStore.dateRange
    ) { selectedRange, customRange ->
        TopCategoriesDateRange(
            rangeSelection = selectedRange,
            customRange = customRange,
            dateFormatted = dateFormatter.formatRangeDate(selectedRange)
        )
    }.asLiveData()

    private var _topCategoriesState = MutableLiveData<TopCategoriesState>()
    val topCategoriesState: LiveData<TopCategoriesState> = _topCategoriesState

    private var _lastUpdateTopCategories = MutableStateFlow<Long?>(null)
    val lastUpdateTopCategories: LiveData<String?> = _lastUpdateTopCategories
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
        _topCategoriesState.value = TopCategoriesState(
            isLoading = true,
            titleStringRes = DashboardWidget.Type.TOP_CATEGORIES.titleResource,
            menu = DashboardWidgetMenu(
                items = listOf(
                    DashboardWidget.Type.TOP_CATEGORIES.defaultHideMenuEntry {
                        parentViewModel.onHideWidgetClicked(DashboardWidget.Type.TOP_CATEGORIES)
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
                loadTopCategoriesStats(selectedRange, isForceRefresh)
            }
        }
    }

    fun onRangeChanged(selectionType: SelectionType) {
        usageTracksEventEmitter.interacted()
        if (selectionType != SelectionType.CUSTOM) {
            parentViewModel.trackCardInteracted(DashboardWidget.Type.TOP_CATEGORIES.trackingIdentifier)
            appPrefsWrapper.setActiveTopCategoriesTab(selectionType.name)
        } else {
            when {
                selectedDateRange.value?.customRange == null -> onEditCustomRangeTapped()
                else -> {
                    parentViewModel.trackCardInteracted(DashboardWidget.Type.TOP_CATEGORIES.trackingIdentifier)
                    appPrefsWrapper.setActiveTopCategoriesTab(SelectionType.CUSTOM.name)
                }
            }
        }
    }

    fun onEditCustomRangeTapped() {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.TOP_CATEGORIES.trackingIdentifier)
        if (selectedDateRange.value?.customRange == null) {
            analyticsTrackerWrapper.track(AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_ADD_BUTTON_TAPPED)
        } else {
            analyticsTrackerWrapper.track(AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_EDIT_BUTTON_TAPPED)
        }

        triggerEvent(
            OpenDatePicker(
                fromDate = selectedDateRange.value?.customRange?.start ?: Date(),
                toDate = selectedDateRange.value?.customRange?.end ?: Date()
            )
        )
    }

    fun onRefresh() {
        trackEventForTopCategoriesCard(AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_RETRY_TAPPED)
        refreshTrigger.tryEmit(RefreshEvent(isForced = true))
    }

    private fun onTopCategoryTapped(categoryId: Long) {
        val categoryName = _topCategoriesState.value?.topCategories
            ?.firstOrNull { it.categoryId == categoryId }?.name.orEmpty()
        val rangeSelection = selectedDateRange.value?.rangeSelection
        if (rangeSelection != null) {
            triggerEvent(OpenCategoryProducts(categoryId, categoryName, rangeSelection))
        }
        parentViewModel.trackCardInteracted(DashboardWidget.Type.TOP_CATEGORIES.trackingIdentifier)
        usageTracksEventEmitter.interacted()
    }

    private suspend fun loadTopCategoriesStats(selectedRange: StatsTimeRangeSelection, forceRefresh: Boolean) =
        coroutineScope {
            if (!networkStatus.isConnected()) {
                parentViewModel.hideRefreshingIndicator()
                _topCategoriesState.value = _topCategoriesState.value?.copy(
                    error = ErrorType.Generic,
                    isOutdated = false
                )
                return@coroutineScope
            }

            trackEventForTopCategoriesCard(AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_DATA_LOADING_STARTED)
            getTopPerformerCategories(selectedRange, forceRefresh).collect { result ->
                when (result) {
                    is GetTopPerformerCategories.TopPerformerCategoryResult.Error -> {
                        parentViewModel.hideRefreshingIndicator()
                        _topCategoriesState.value = _topCategoriesState.value?.copy(
                            error = if (
                                (result.exception as? WooException)?.error?.type == WooErrorType.API_NOT_FOUND
                            ) {
                                ErrorType.WCAnalyticsInactive
                            } else {
                                ErrorType.Generic
                            },
                            isLoading = false
                        )
                        trackEventForTopCategoriesCard(
                            AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_DATA_LOADING_FAILED,
                            properties = mapOf(
                                AnalyticsTracker.KEY_ERROR to topCategoriesState.value?.error.toString()
                            )
                        )
                    }

                    is GetTopPerformerCategories.TopPerformerCategoryResult.Success -> {
                        trackEventForTopCategoriesCard(AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_DATA_LOADING_COMPLETED)
                        if (_topCategoriesState.value?.isOutdated != true && result.topCategories.isOutdated) {
                            parentViewModel.displayRefreshingIndicator()
                        } else {
                            parentViewModel.hideRefreshingIndicator()
                        }
                        _topCategoriesState.value = _topCategoriesState.value?.copy(
                            isLoading = false,
                            isOutdated = result.topCategories.isOutdated,
                            topCategories = result.topCategories.value.toTopCategoriesUiList(),
                        )
                    }

                    is GetTopPerformerCategories.TopPerformerCategoryResult.Loading -> {
                        parentViewModel.hideRefreshingIndicator()
                        _topCategoriesState.value = _topCategoriesState.value?.copy(isLoading = true)
                    }
                }
            }

            launch {
                observeLastUpdate(
                    selectedRange,
                    AnalyticData.TOP_PERFORMER_CATEGORIES
                ).collect { lastUpdateMillis -> _lastUpdateTopCategories.value = lastUpdateMillis }
            }
        }

    private fun List<TopPerformerCategory>.toTopCategoriesUiList() = map { it.toTopCategoriesUiModel() }

    private fun TopPerformerCategory.toTopCategoriesUiModel() =
        TopPerformerCategoryUiModel(
            categoryId = categoryId,
            name = StringEscapeUtils.unescapeHtml4(name),
            timesOrdered = FormatUtils.formatDecimal(quantity),
            netSales = resourceProvider.getString(
                R.string.dashboard_top_performers_net_sales,
                getTotalSpendFormatted(total.toBigDecimal(), currency)
            ),
            onClick = ::onTopCategoryTapped
        )

    private fun getTotalSpendFormatted(totalSpend: BigDecimal, currency: String) =
        currencyFormatter.formatCurrency(
            totalSpend,
            wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: currency
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
                appPrefsWrapper.setActiveTopCategoriesTab(SelectionType.CUSTOM.name)
            }
        }
    }

    private fun onViewAllAnalyticsTapped() {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.TOP_CATEGORIES.trackingIdentifier)
        AnalyticsTracker.track(AnalyticsEvent.DASHBOARD_SEE_MORE_ANALYTICS_TAPPED)
        selectedDateRange.value?.let {
            triggerEvent(OpenAnalytics(it.rangeSelection))
        }
    }

    private fun trackEventForTopCategoriesCard(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        analyticsTrackerWrapper.track(
            event,
            properties + mapOf(AnalyticsTracker.KEY_TYPE to DashboardWidget.Type.TOP_CATEGORIES.trackingIdentifier)
        )
    }

    data class TopCategoriesDateRange(
        val rangeSelection: StatsTimeRangeSelection,
        val customRange: StatsTimeRange?,
        val dateFormatted: String
    )

    data class TopCategoriesState(
        val isLoading: Boolean = false,
        val error: ErrorType? = null,
        @StringRes val titleStringRes: Int,
        val topCategories: List<TopPerformerCategoryUiModel> = emptyList(),
        val menu: DashboardWidgetMenu,
        val onOpenAnalyticsTapped: DashboardWidgetAction,
        val isOutdated: Boolean = false,
    )

    enum class ErrorType {
        Generic, WCAnalyticsInactive
    }

    data class OpenCategoryProducts(
        val categoryId: Long,
        val categoryName: String,
        val rangeSelection: StatsTimeRangeSelection
    ) : MultiLiveEvent.Event()

    data class OpenDatePicker(val fromDate: Date, val toDate: Date) : MultiLiveEvent.Event()
    data class OpenAnalytics(val analyticsPeriod: StatsTimeRangeSelection) : MultiLiveEvent.Event()

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardTopCategoriesViewModel
    }
}
