package com.woocommerce.android.ui.analytics

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.model.ProductItem
import com.woocommerce.android.model.VisitorsStat
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy.ForceNew
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy.Saved
import com.woocommerce.android.ui.analytics.AnalyticsRepository.OrdersResult.OrdersData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.OrdersResult.OrdersError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.ProductsResult.ProductsData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.ProductsResult.ProductsError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.VisitorsResult
import com.woocommerce.android.ui.analytics.AnalyticsRepository.VisitorsResult.VisitorsData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.VisitorsResult.VisitorsError
import com.woocommerce.android.ui.analytics.RefreshIndicator.NotShowIndicator
import com.woocommerce.android.ui.analytics.RefreshIndicator.ShowIndicator
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeSelectorViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationSectionViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.DataViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.LoadingViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.NoDataState
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListCardItemViewState
import com.woocommerce.android.ui.mystore.MyStoreStatsUsageTracksEventEmitter
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState as ProductsViewState
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState.LoadingViewState as LoadingProductsViewState
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState.NoDataState as ProductsNoDataState
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.*
import com.woocommerce.android.viewmodel.getStateFlow
import kotlinx.coroutines.flow.map

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val analyticsRepository: AnalyticsRepository,
    private val transactionLauncher: AnalyticsHubTransactionLauncher,
    private val usageTracksEventEmitter: MyStoreStatsUsageTracksEventEmitter,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val navArgs: AnalyticsFragmentArgs by savedState.navArgs()

    val performanceObserver: LifecycleObserver = transactionLauncher

    private val mutableState = MutableStateFlow(
        AnalyticsViewState(
            NotShowIndicator,
            buildAnalyticsDateRangeSelectorViewState(),
            LoadingViewState,
            LoadingViewState,
            LoadingProductsViewState,
            LoadingViewState,
        )
    )
    val state: StateFlow<AnalyticsViewState> = mutableState

    private val selectionTypeState = savedState.getStateFlow(viewModelScope, navArgs.targetGranularity)
    val selectionType = selectionTypeState.asLiveData()

    private val selectionDataState = selectionTypeState
        .map { AnalyticsHubDateRangeSelection(it) }
        .toStateFlow(AnalyticsHubDateRangeSelection(selectionTypeState.value))
    val rangeSelection = selectionDataState.asLiveData()

    private val selectionData
        get() = selectionDataState.value

    private val currentRange
        get() = selectionData.currentRange

    private val previousRange
        get() = selectionData.previousRange

    init {
        viewModelScope.launch {
            refreshAllAnalyticsAtOnce(isRefreshing = false, showSkeleton = true)
        }
    }

    fun onCustomDateRangeClicked() {
        val fromMillis = currentRange.start.time
        val toMillis = currentRange.end.time
        triggerEvent(AnalyticsViewEvent.OpenDatePicker(fromMillis, toMillis))
    }

    fun onCustomDateRangeChanged(fromMillis: Long, toMillis: Long) {
        val dateFormat = SimpleDateFormat("EEE, LLL d, yy", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val fromDateStr = dateFormat.format(Date(fromMillis))
        val toDateStr = dateFormat.format(Date(toMillis))

        dateFormat.timeZone = TimeZone.getDefault()

        mutableState.value = state.value.copy(
            analyticsDateRangeSelectorState = state.value.analyticsDateRangeSelectorState.copy(
                fromDatePeriod = resourceProvider.getString(
                    R.string.analytics_date_range_custom,
                    fromDateStr,
                    toDateStr
                ),
                toDatePeriod = resourceProvider.getString(R.string.date_timeframe_custom_date_range_title),
                selectedPeriod = resourceProvider.getString(selectionData.selectionType.localizedResourceId)
            )
        )

        trackSelectedDateRange()

        viewModelScope.launch {
            refreshAllAnalyticsAtOnce(isRefreshing = false, showSkeleton = true)
        }
    }

    fun onRefreshRequested() {
        viewModelScope.launch {
            refreshAllAnalyticsAtOnce(isRefreshing = true, showSkeleton = false)
        }
    }

    fun onSelectedTimePeriodChanged() {
        updateDateSelector()
        trackSelectedDateRange()
        viewModelScope.launch {
            refreshAllAnalyticsAtOnce(isRefreshing = false, showSkeleton = true)
        }
    }

    fun onDateRangeSelectorClick() {
        onTrackableUIInteraction()
        AnalyticsTracker.track(AnalyticsEvent.ANALYTICS_HUB_DATE_RANGE_BUTTON_TAPPED)
        triggerEvent(AnalyticsViewEvent.OpenDateRangeSelector)
    }

    fun onTrackableUIInteraction() = usageTracksEventEmitter.interacted()

    private fun refreshAllAnalyticsAtOnce(isRefreshing: Boolean, showSkeleton: Boolean) {
        updateRevenue(isRefreshing, showSkeleton)
        updateOrders(isRefreshing, showSkeleton)
        updateProducts(isRefreshing, showSkeleton)
        updateVisitors(isRefreshing, showSkeleton)
    }

    private fun updateRevenue(isRefreshing: Boolean, showSkeleton: Boolean) =
        launch {
            val fetchStrategy = getFetchStrategy(isRefreshing)

            if (showSkeleton) mutableState.value = state.value.copy(revenueState = LoadingViewState)
            mutableState.value = state.value.copy(
                refreshIndicator = if (isRefreshing) ShowIndicator else NotShowIndicator
            )

            analyticsRepository.fetchRevenueData(selectionDataState.value, fetchStrategy)
                .let {
                    when (it) {
                        is RevenueData -> {
                            mutableState.value = state.value.copy(
                                refreshIndicator = NotShowIndicator,
                                revenueState = buildRevenueDataViewState(it)
                            )
                            transactionLauncher.onRevenueFetched()
                        }
                        is RevenueError -> mutableState.value = state.value.copy(
                            refreshIndicator = NotShowIndicator,
                            revenueState = NoDataState(resourceProvider.getString(R.string.analytics_revenue_no_data))
                        )
                    }
                }
        }

    private fun updateOrders(isRefreshing: Boolean, showSkeleton: Boolean) =
        launch {
            val fetchStrategy = getFetchStrategy(isRefreshing)

            if (showSkeleton) mutableState.value = state.value.copy(ordersState = LoadingViewState)
            mutableState.value = state.value.copy(
                refreshIndicator = if (isRefreshing) ShowIndicator else NotShowIndicator
            )
            analyticsRepository.fetchOrdersData(selectionDataState.value, fetchStrategy)
                .let {
                    when (it) {
                        is OrdersData -> {
                            mutableState.value = state.value.copy(
                                ordersState = buildOrdersDataViewState(it)
                            )
                            transactionLauncher.onOrdersFetched()
                        }
                        is OrdersError -> mutableState.value = state.value.copy(
                            ordersState = NoDataState(resourceProvider.getString(R.string.analytics_orders_no_data))
                        )
                    }
                }
        }

    private fun updateProducts(isRefreshing: Boolean, showSkeleton: Boolean) =
        launch {
            val fetchStrategy = getFetchStrategy(isRefreshing)
            if (showSkeleton) mutableState.value = state.value.copy(productsState = LoadingProductsViewState)
            mutableState.value = state.value.copy(
                refreshIndicator = if (isRefreshing) ShowIndicator else NotShowIndicator
            )
            analyticsRepository.fetchProductsData(selectionDataState.value, fetchStrategy)
                .let {
                    when (it) {
                        is ProductsData -> {
                            mutableState.value = state.value.copy(
                                productsState = buildProductsDataState(
                                    it.productsStat.itemsSold,
                                    it.productsStat.itemsSoldDelta,
                                    it.productsStat.products
                                )
                            )
                            transactionLauncher.onProductsFetched()
                        }
                        ProductsError -> mutableState.value = state.value.copy(
                            productsState = ProductsNoDataState(
                                resourceProvider.getString(R.string.analytics_products_no_data)
                            )
                        )
                    }
                }
        }

    private fun updateVisitors(isRefreshing: Boolean, showSkeleton: Boolean) =
        launch {
            val timePeriod = selectionTypeState.value
            val fetchStrategy = getFetchStrategy(isRefreshing)
            val isQuarterSelection = (timePeriod == QUARTER_TO_DATE) || (timePeriod == LAST_QUARTER)

            if (timePeriod == SelectionType.CUSTOM) {
                mutableState.value = state.value.copy(visitorsState = AnalyticsInformationViewState.HiddenState)
                transactionLauncher.onVisitorsFetched()
                return@launch
            }

            if (showSkeleton) mutableState.value = state.value.copy(visitorsState = LoadingViewState)
            mutableState.value = state.value.copy(
                refreshIndicator = if (isRefreshing) ShowIndicator else NotShowIndicator
            )

            if (isQuarterSelection) {
                analyticsRepository.fetchQuarterVisitorsData(selectionDataState.value, fetchStrategy)
            } else {
                analyticsRepository.fetchRecentVisitorsData(selectionDataState.value, fetchStrategy)
            }.handleVisitorsResult()
        }

    private fun VisitorsResult.handleVisitorsResult() {
        when (this) {
            is VisitorsData -> {
                mutableState.value = state.value.copy(
                    refreshIndicator = NotShowIndicator,
                    visitorsState = buildVisitorsDataViewState(visitorsStat)
                )
                transactionLauncher.onVisitorsFetched()
            }
            is VisitorsError -> mutableState.value = state.value.copy(
                refreshIndicator = NotShowIndicator,
                visitorsState = NoDataState("No visitors data")
            )
        }
    }

    private fun updateDateSelector() {
        //TODO: populate the date range selector with the correct values
        mutableState.value = state.value.copy(
            analyticsDateRangeSelectorState = state.value.analyticsDateRangeSelectorState.copy(
                fromDatePeriod = "",
                toDatePeriod = "",
                selectedPeriod = ""
            )
        )
    }

    private fun formatValue(value: String, currencyCode: String?) = currencyCode
        ?.let { currencyFormatter.formatCurrency(value, it) }
        ?: value

    private fun buildAnalyticsDateRangeSelectorViewState(): AnalyticsDateRangeSelectorViewState {
        val rangeOptions = values().map { it.description }

        //TODO: populate the date range selector with the correct values
        return AnalyticsDateRangeSelectorViewState(
            fromDatePeriod = "from date period",
            toDatePeriod = "to date period",
            availableRangeDates = rangeOptions,
            selectedPeriod = ""
        )
    }

    private fun buildVisitorsDataViewState(
        stats: VisitorsStat
    ) = DataViewState(
        title = resourceProvider.getString(R.string.analytics_visitors_and_views_card_title),
        leftSection = AnalyticsInformationSectionViewState(
            title = resourceProvider.getString(R.string.analytics_visitors_subtitle),
            stats.visitorsCount.toString(),
            stats.avgVisitorsDelta.run { this as? DeltaPercentage.Value }?.value,
            listOf() /** Add charts calculation to Visitors and Views stats **/
        ),
        rightSection = AnalyticsInformationSectionViewState(
            resourceProvider.getString(R.string.analytics_views_subtitle),
            stats.viewsCount.toString(),
            stats.avgViewsDelta.run { this as? DeltaPercentage.Value }?.value,
            listOf() /** Add charts calculation to Visitors and Views stats **/
        )
    )

    private fun buildRevenueDataViewState(data: RevenueData) =
        DataViewState(
            title = resourceProvider.getString(R.string.analytics_revenue_card_title),
            leftSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_total_sales_title),
                formatValue(data.revenueStat.totalValue.toString(), data.revenueStat.currencyCode),
                if (data.revenueStat.totalDelta is DeltaPercentage.Value) data.revenueStat.totalDelta.value else null,
                data.revenueStat.totalRevenueByInterval.map { it.toFloat() }
            ),
            rightSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_net_sales_title),
                formatValue(data.revenueStat.netValue.toString(), data.revenueStat.currencyCode),
                if (data.revenueStat.netDelta is DeltaPercentage.Value) data.revenueStat.netDelta.value else null,
                data.revenueStat.netRevenueByInterval.map { it.toFloat() }
            ),
        )

    private fun buildOrdersDataViewState(data: OrdersData) =
        DataViewState(
            title = resourceProvider.getString(R.string.analytics_orders_card_title),
            leftSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_total_orders_title),
                data.ordersStat.ordersCount.toString(),
                if (data.ordersStat.ordersCountDelta is DeltaPercentage.Value) {
                    data.ordersStat.ordersCountDelta.value
                } else {
                    null
                },
                data.ordersStat.ordersCountByInterval.map { it.toFloat() }
            ),
            rightSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_avg_orders_title),
                formatValue(data.ordersStat.avgOrderValue.toString(), data.ordersStat.currencyCode),
                if (data.ordersStat.avgOrderDelta is DeltaPercentage.Value) {
                    data.ordersStat.avgOrderDelta.value
                } else {
                    null
                },
                data.ordersStat.avgOrderValueByInterval.map { it.toFloat() }
            )
        )

    private fun buildProductsDataState(itemsSold: Int, delta: DeltaPercentage, products: List<ProductItem>) =
        ProductsViewState.DataViewState(
            title = resourceProvider.getString(R.string.analytics_products_card_title),
            subTitle = resourceProvider.getString(R.string.analytics_products_list_items_sold),
            subTitleValue = itemsSold.toString(),
            delta = if (delta is DeltaPercentage.Value) delta.value else null,
            listLeftHeader = resourceProvider.getString(R.string.analytics_products_list_header_title),
            listRightHeader = resourceProvider.getString(R.string.analytics_products_list_header_subtitle),
            items = products
                .sortedByDescending { it.quantity }
                .mapIndexed { index, product ->
                    AnalyticsListCardItemViewState(
                        product.image,
                        product.name,
                        product.quantity.toString(),
                        resourceProvider.getString(
                            R.string.analytics_products_list_item_description,
                            formatValue(product.netSales.toString(), product.currencyCode)
                        ),
                        index != products.size - 1
                    )
                }
        )

    private fun getFetchStrategy(isRefreshing: Boolean) = if (isRefreshing) ForceNew else Saved

    private fun trackSelectedDateRange() {
        val rangeDescription = selectionDataState.value.selectionType.description
        onTrackableUIInteraction()
        AnalyticsTracker.track(
            AnalyticsEvent.ANALYTICS_HUB_DATE_RANGE_SELECTED,
            mapOf(
                AnalyticsTracker.KEY_OPTION to rangeDescription
            )
        )
    }

    companion object {
        const val TIME_PERIOD_SELECTED_KEY = "time_period_selected_key"
        const val DATE_RANGE_SELECTED_KEY = "date_range_selected_key"
    }
}
