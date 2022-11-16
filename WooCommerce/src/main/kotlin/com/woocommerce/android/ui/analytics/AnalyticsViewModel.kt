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
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.CUSTOM
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.LAST_MONTH
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.LAST_QUARTER
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.LAST_WEEK
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.LAST_YEAR
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.MONTH_TO_DATE
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.QUARTER_TO_DATE
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.TODAY
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.WEEK_TO_DATE
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.YEAR_TO_DATE
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.YESTERDAY
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeCalculator
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeFormatter
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

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val analyticsDateRange: AnalyticsDateRangeCalculator,
    private val currencyFormatter: CurrencyFormatter,
    private val analyticsRepository: AnalyticsRepository,
    private val transactionLauncher: AnalyticsHubTransactionLauncher,
    private val usageTracksEventEmitter: MyStoreStatsUsageTracksEventEmitter,
    private val analyticsDateRangeFormatter: AnalyticsDateRangeFormatter,
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

    init {
        viewModelScope.launch {
            refreshAllAnalyticsAtOnce(isRefreshing = false, showSkeleton = true)
        }
    }

    fun onCustomDateRangeClicked() {
        val savedRange = getSavedDateRange()
        val currentPeriod = savedRange.getSelectedPeriod()
        val fromMillis = currentPeriod.from.time
        val toMillis = currentPeriod.to.time
        triggerEvent(AnalyticsViewEvent.OpenDatePicker(fromMillis, toMillis))
    }

    fun onCustomDateRangeChanged(fromMillis: Long, toMillis: Long) {
        val dateFormat = SimpleDateFormat("EEE, LLL d, yy", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val fromDateStr = dateFormat.format(Date(fromMillis))
        val toDateStr = dateFormat.format(Date(toMillis))

        dateFormat.timeZone = TimeZone.getDefault()
        val fromDateUtc = dateFormat.parse(fromDateStr)
        val toDateUtc = dateFormat.parse(toDateStr)

        mutableState.value = state.value.copy(
            analyticsDateRangeSelectorState = state.value.analyticsDateRangeSelectorState.copy(
                fromDatePeriod = resourceProvider.getString(
                    R.string.analytics_date_range_custom,
                    fromDateStr,
                    toDateStr
                ),
                toDatePeriod = resourceProvider.getString(R.string.date_timeframe_custom_date_range_title),
                selectedPeriod = getTimePeriodDescription(CUSTOM)
            )
        )

        val dateRange = analyticsDateRange.getAnalyticsDateRangeFromCustom(fromDateUtc!!, toDateUtc!!)
        saveSelectedDateRange(dateRange)
        saveSelectedTimePeriod(CUSTOM)
        trackSelectedDateRange(CUSTOM)

        viewModelScope.launch {
            refreshAllAnalyticsAtOnce(isRefreshing = false, showSkeleton = true)
        }
    }

    fun onRefreshRequested() {
        viewModelScope.launch {
            refreshAllAnalyticsAtOnce(isRefreshing = true, showSkeleton = false)
        }
    }

    fun onSelectedTimePeriodChanged(selectedTimePeriod: AnalyticTimePeriod) {
        val dateRange = analyticsDateRange.getAnalyticsDateRangeFrom(selectedTimePeriod)
        saveSelectedTimePeriod(selectedTimePeriod)
        saveSelectedDateRange(dateRange)
        updateDateSelector()
        trackSelectedDateRange(selectedTimePeriod)
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
            val timePeriod = getSavedTimePeriod()
            val dateRange = getSavedDateRange()
            val fetchStrategy = getFetchStrategy(isRefreshing)

            if (showSkeleton) mutableState.value = state.value.copy(revenueState = LoadingViewState)
            mutableState.value = state.value.copy(
                refreshIndicator = if (isRefreshing) ShowIndicator else NotShowIndicator
            )

            analyticsRepository.fetchRevenueData(dateRange, timePeriod, fetchStrategy)
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
            val timePeriod = getSavedTimePeriod()
            val dateRange = getSavedDateRange()
            val fetchStrategy = getFetchStrategy(isRefreshing)

            if (showSkeleton) mutableState.value = state.value.copy(ordersState = LoadingViewState)
            mutableState.value = state.value.copy(
                refreshIndicator = if (isRefreshing) ShowIndicator else NotShowIndicator
            )
            analyticsRepository.fetchOrdersData(dateRange, timePeriod, fetchStrategy)
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
            val timePeriod = getSavedTimePeriod()
            val dateRange = getSavedDateRange()
            val fetchStrategy = getFetchStrategy(isRefreshing)
            if (showSkeleton) mutableState.value = state.value.copy(productsState = LoadingProductsViewState)
            mutableState.value = state.value.copy(
                refreshIndicator = if (isRefreshing) ShowIndicator else NotShowIndicator
            )
            analyticsRepository.fetchProductsData(dateRange, timePeriod, fetchStrategy)
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
            val timePeriod = getSavedTimePeriod()
            val dateRange = getSavedDateRange()
            val fetchStrategy = getFetchStrategy(isRefreshing)
            val isQuarterSelection = (timePeriod == QUARTER_TO_DATE) || (timePeriod == LAST_QUARTER)

            if (timePeriod == CUSTOM) {
                mutableState.value = state.value.copy(visitorsState = AnalyticsInformationViewState.HiddenState)
                transactionLauncher.onVisitorsFetched()
                return@launch
            }

            if (showSkeleton) mutableState.value = state.value.copy(visitorsState = LoadingViewState)
            mutableState.value = state.value.copy(
                refreshIndicator = if (isRefreshing) ShowIndicator else NotShowIndicator
            )

            if (isQuarterSelection) {
                analyticsRepository.fetchQuarterVisitorsData(dateRange, timePeriod, fetchStrategy)
            } else {
                analyticsRepository.fetchRecentVisitorsData(dateRange, timePeriod, fetchStrategy)
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
        val timePeriod = getSavedTimePeriod()
        val dateRange = getSavedDateRange()
        val timePeriodDescription = getTimePeriodDescription(timePeriod)
        mutableState.value = state.value.copy(
            analyticsDateRangeSelectorState = state.value.analyticsDateRangeSelectorState.copy(
                fromDatePeriod = analyticsDateRangeFormatter.fromDescription(dateRange),
                toDatePeriod = analyticsDateRangeFormatter.toDescription(dateRange, timePeriodDescription),
                selectedPeriod = getTimePeriodDescription(timePeriod)
            )
        )
    }

    private fun getAvailableDateRanges() =
        resourceProvider.getStringArray(R.array.analytics_date_range_selectors).asList()

    private fun getDefaultTimePeriod() = navArgs.targetGranularity

    private fun getDefaultDateRange() = analyticsDateRange.getAnalyticsDateRangeFrom(getDefaultTimePeriod())

    private fun getTimePeriodDescription(analyticTimeRange: AnalyticTimePeriod): String =
        when (analyticTimeRange) {
            TODAY -> resourceProvider.getString(R.string.date_timeframe_today)
            YESTERDAY -> resourceProvider.getString(R.string.date_timeframe_yesterday)
            LAST_WEEK -> resourceProvider.getString(R.string.date_timeframe_last_week)
            LAST_MONTH -> resourceProvider.getString(R.string.date_timeframe_last_month)
            LAST_QUARTER -> resourceProvider.getString(R.string.date_timeframe_last_quarter)
            LAST_YEAR -> resourceProvider.getString(R.string.date_timeframe_last_year)
            WEEK_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_week_to_date)
            MONTH_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_month_to_date)
            QUARTER_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_quarter_to_date)
            YEAR_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_year_to_date)
            CUSTOM -> resourceProvider.getString(R.string.date_timeframe_custom)
        }

    private fun formatValue(value: String, currencyCode: String?) = currencyCode
        ?.let { currencyFormatter.formatCurrency(value, it) }
        ?: value

    private fun buildAnalyticsDateRangeSelectorViewState(): AnalyticsDateRangeSelectorViewState {
        val timePeriod = getSavedTimePeriod()
        val dateRange = getSavedDateRange()
        val timePeriodDescription = getTimePeriodDescription(timePeriod)

        return AnalyticsDateRangeSelectorViewState(
            fromDatePeriod = analyticsDateRangeFormatter.fromDescription(dateRange),
            toDatePeriod = analyticsDateRangeFormatter.toDescription(dateRange, timePeriodDescription),
            availableRangeDates = getAvailableDateRanges(),
            selectedPeriod = getTimePeriodDescription(getSavedTimePeriod())
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

    private fun saveSelectedTimePeriod(range: AnalyticTimePeriod) {
        savedState[TIME_PERIOD_SELECTED_KEY] = range
    }

    private fun saveSelectedDateRange(dateRange: AnalyticsDateRange) {
        savedState[DATE_RANGE_SELECTED_KEY] = dateRange
    }

    private fun getSavedDateRange(): AnalyticsDateRange = savedState[DATE_RANGE_SELECTED_KEY] ?: getDefaultDateRange()
    private fun getSavedTimePeriod(): AnalyticTimePeriod = savedState[TIME_PERIOD_SELECTED_KEY]
        ?: getDefaultTimePeriod()

    private fun trackSelectedDateRange(selectedTimePeriod: AnalyticTimePeriod) {
        onTrackableUIInteraction()
        AnalyticsTracker.track(
            AnalyticsEvent.ANALYTICS_HUB_DATE_RANGE_SELECTED,
            mapOf(
                AnalyticsTracker.KEY_OPTION to selectedTimePeriod.description
            )
        )
    }

    companion object {
        const val TIME_PERIOD_SELECTED_KEY = "time_period_selected_key"
        const val DATE_RANGE_SELECTED_KEY = "date_range_selected_key"
    }
}
