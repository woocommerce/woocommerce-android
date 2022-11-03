package com.woocommerce.android.ui.analytics

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.model.ProductItem
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy.ForceNew
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy.Saved
import com.woocommerce.android.ui.analytics.AnalyticsRepository.OrdersResult.OrdersData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.OrdersResult.OrdersError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.ProductsResult.ProductsData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.ProductsResult.ProductsError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.VisitorsResult.VisitorsData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.VisitorsResult.VisitorsError
import com.woocommerce.android.ui.analytics.AnalyticsViewEvent.OpenUrl
import com.woocommerce.android.ui.analytics.AnalyticsViewEvent.OpenWPComWebView
import com.woocommerce.android.ui.analytics.RefreshIndicator.NotShowIndicator
import com.woocommerce.android.ui.analytics.RefreshIndicator.ShowIndicator
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange.MultipleDateRange
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange.SimpleDateRange
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeCalculator
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeSelectorViewState
import com.woocommerce.android.ui.analytics.daterangeselector.formatDatesToFriendlyPeriod
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationSectionViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.DataViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.LoadingViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.NoDataState
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListCardItemViewState
import com.woocommerce.android.ui.mystore.MyStoreStatsUsageTracksEventEmitter
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import com.zendesk.util.DateUtils.isSameDay
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
    private val dateUtils: DateUtils,
    private val analyticsDateRange: AnalyticsDateRangeCalculator,
    private val currencyFormatter: CurrencyFormatter,
    private val analyticsRepository: AnalyticsRepository,
    private val selectedSite: SelectedSite,
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

    init {
        viewModelScope.launch {
            refreshAllAnalyticsAtOnce(isRefreshing = false, showSkeleton = true)
        }
    }

    fun onCustomDateRangeClicked() {
        val savedRange = getSavedDateRange()
        val fromMillis = when (savedRange) {
            is SimpleDateRange -> savedRange.from.time
            is MultipleDateRange -> savedRange.to.from.time
        }
        val toMillis = when (savedRange) {
            is SimpleDateRange -> savedRange.to.time
            is MultipleDateRange -> savedRange.to.to.time
        }
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
                selectedPeriod = getTimePeriodDescription(AnalyticTimePeriod.CUSTOM)
            )
        )

        val dateRange = analyticsDateRange.getAnalyticsDateRangeFromCustom(fromDateUtc, toDateUtc)
        saveSelectedDateRange(dateRange)
        saveSelectedTimePeriod(AnalyticTimePeriod.CUSTOM)
        trackSelectedDateRange(AnalyticTimePeriod.CUSTOM)

        viewModelScope.launch {
            refreshAllAnalyticsAtOnce(isRefreshing = false, showSkeleton = true)
        }
    }

    fun onRefreshRequested() {
        viewModelScope.launch {
            refreshAllAnalyticsAtOnce(isRefreshing = true, showSkeleton = false)
        }
    }

    fun onSelectedTimePeriodChanged(newSelection: String) {
        val selectedTimePeriod: AnalyticTimePeriod = AnalyticTimePeriod.from(newSelection)
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

    fun onRevenueSeeReportClick() {
        trackSeeReportClicked(AnalyticsTracker.VALUE_REVENUE_CARD_SELECTED)
        if (selectedSite.getIfExists()?.isWPCom == true || selectedSite.getIfExists()?.isWPComAtomic == true) {
            triggerEvent(OpenWPComWebView(analyticsRepository.getRevenueAdminPanelUrl()))
        } else {
            triggerEvent(OpenUrl(analyticsRepository.getRevenueAdminPanelUrl()))
        }
    }

    fun onOrdersSeeReportClick() {
        trackSeeReportClicked(AnalyticsTracker.VALUE_ORDERS_CARD_SELECTED)
        if (selectedSite.getIfExists()?.isWPCom == true || selectedSite.getIfExists()?.isWPComAtomic == true) {
            triggerEvent(OpenWPComWebView(analyticsRepository.getOrdersAdminPanelUrl()))
        } else {
            triggerEvent(OpenUrl(analyticsRepository.getOrdersAdminPanelUrl()))
        }
    }

    fun onProductsSeeReportClick() {
        trackSeeReportClicked(AnalyticsTracker.VALUE_PRODUCTS_CARD_SELECTED)
        if (selectedSite.getIfExists()?.isWPCom == true || selectedSite.getIfExists()?.isWPComAtomic == true) {
            triggerEvent(OpenWPComWebView(analyticsRepository.getProductsAdminPanelUrl()))
        } else {
            triggerEvent(OpenUrl(analyticsRepository.getProductsAdminPanelUrl()))
        }
    }

    fun onTrackableUIInteraction() = usageTracksEventEmitter.interacted()

    private fun refreshAllAnalyticsAtOnce(isRefreshing: Boolean, showSkeleton: Boolean) {
        updateRevenue(isRefreshing = isRefreshing, showSkeleton = showSkeleton)
        updateOrders(isRefreshing = isRefreshing, showSkeleton = showSkeleton)
        updateProducts(isRefreshing = isRefreshing, showSkeleton = showSkeleton)
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
                                revenueState = buildRevenueDataViewState(
                                    formatValue(it.revenueStat.totalValue.toString(), it.revenueStat.currencyCode),
                                    it.revenueStat.totalDelta,
                                    formatValue(it.revenueStat.netValue.toString(), it.revenueStat.currencyCode),
                                    it.revenueStat.netDelta
                                )
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
                                ordersState = buildOrdersDataViewState(
                                    it.ordersStat.ordersCount.toString(),
                                    it.ordersStat.ordersCountDelta,
                                    formatValue(it.ordersStat.avgOrderValue.toString(), it.ordersStat.currencyCode),
                                    it.ordersStat.avgOrderDelta
                                )
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
            if (!FeatureFlag.ANALYTICS_HUB_PRODUCTS_AND_REPORTS.isEnabled()) {
                return@launch
            }
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
                        is ProductsData -> mutableState.value = state.value.copy(
                            productsState = buildProductsDataState(
                                it.productsStat.itemsSold,
                                it.productsStat.itemsSoldDelta,
                                it.productsStat.products
                            )
                        )
                        ProductsError -> mutableState.value = state.value.copy(
                            productsState = ProductsNoDataState(
                                resourceProvider.getString(R.string.analytics_products_no_data)
                            )
                        )
                    }
                }
        }

    private fun updateDateSelector() {
        val timePeriod = getSavedTimePeriod()
        val dateRange = getSavedDateRange()
        mutableState.value = state.value.copy(
            analyticsDateRangeSelectorState = state.value.analyticsDateRangeSelectorState.copy(
                fromDatePeriod = calculateFromDatePeriod(dateRange),
                toDatePeriod = calculateToDatePeriod(timePeriod, dateRange),
                selectedPeriod = getTimePeriodDescription(timePeriod)
            )
        )
    }

    private fun calculateToDatePeriod(analyticTimeRange: AnalyticTimePeriod, dateRange: AnalyticsDateRange) =
        when (dateRange) {
            is SimpleDateRange -> resourceProvider.getString(
                R.string.analytics_date_range_to_date,
                getTimePeriodDescription(analyticTimeRange),
                dateUtils.getShortMonthDayAndYearString(
                    dateUtils.getYearMonthDayStringFromDate(dateRange.to)
                ).orEmpty()
            )
            is MultipleDateRange ->
                if (isSameDay(dateRange.to.from, dateRange.to.to)) {
                    resourceProvider.getString(
                        R.string.analytics_date_range_to_date,
                        getTimePeriodDescription(analyticTimeRange),
                        dateUtils.getShortMonthDayAndYearString(
                            dateUtils.getYearMonthDayStringFromDate(dateRange.to.from)
                        ).orEmpty()
                    )
                } else {
                    resourceProvider.getString(
                        R.string.analytics_date_range_to_date,
                        getTimePeriodDescription(analyticTimeRange),
                        dateRange.to.formatDatesToFriendlyPeriod()
                    )
                }
        }

    private fun calculateFromDatePeriod(dateRange: AnalyticsDateRange) = when (dateRange) {
        is SimpleDateRange -> resourceProvider.getString(
            R.string.analytics_date_range_from_date,
            dateUtils.getShortMonthDayAndYearString(dateUtils.getYearMonthDayStringFromDate(dateRange.from)).orEmpty()
        )
        is MultipleDateRange ->
            if (isSameDay(dateRange.from.from, dateRange.from.to)) {
                resourceProvider.getString(
                    R.string.analytics_date_range_from_date,
                    dateUtils.getShortMonthDayAndYearString(
                        dateUtils.getYearMonthDayStringFromDate(dateRange.from.from)
                    ).orEmpty()
                )
            } else {
                resourceProvider.getString(
                    R.string.analytics_date_range_from_date,
                    dateRange.from.formatDatesToFriendlyPeriod()
                )
            }
    }

    private fun getAvailableDateRanges() =
        resourceProvider.getStringArray(R.array.analytics_date_range_selectors).asList()
    private fun getDefaultTimePeriod() = navArgs.targetGranularity

    private fun getDefaultDateRange() = analyticsDateRange.getAnalyticsDateRangeFrom(getDefaultTimePeriod())

    private fun getTimePeriodDescription(analyticTimeRange: AnalyticTimePeriod): String =
        when (analyticTimeRange) {
            AnalyticTimePeriod.TODAY -> resourceProvider.getString(R.string.date_timeframe_today)
            AnalyticTimePeriod.YESTERDAY -> resourceProvider.getString(R.string.date_timeframe_yesterday)
            AnalyticTimePeriod.LAST_WEEK -> resourceProvider.getString(R.string.date_timeframe_last_week)
            AnalyticTimePeriod.LAST_MONTH -> resourceProvider.getString(R.string.date_timeframe_last_month)
            AnalyticTimePeriod.LAST_QUARTER -> resourceProvider.getString(R.string.date_timeframe_last_quarter)
            AnalyticTimePeriod.LAST_YEAR -> resourceProvider.getString(R.string.date_timeframe_last_year)
            AnalyticTimePeriod.WEEK_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_week_to_date)
            AnalyticTimePeriod.MONTH_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_month_to_date)
            AnalyticTimePeriod.QUARTER_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_quarter_to_date)
            AnalyticTimePeriod.YEAR_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_year_to_date)
            AnalyticTimePeriod.CUSTOM -> resourceProvider.getString(R.string.date_timeframe_custom)
        }

    private fun formatValue(value: String, currencyCode: String?) = currencyCode
        ?.let { currencyFormatter.formatCurrency(value, it) }
        ?: value

    private fun buildAnalyticsDateRangeSelectorViewState() = AnalyticsDateRangeSelectorViewState(
        fromDatePeriod = calculateFromDatePeriod(getSavedDateRange()),
        toDatePeriod = calculateToDatePeriod(getSavedTimePeriod(), getSavedDateRange()),
        availableRangeDates = getAvailableDateRanges(),
        selectedPeriod = getTimePeriodDescription(getSavedTimePeriod())
    )

    private fun buildVisitorsDataViewState(
        visitorsCount: Int,
        viewsCount: Int
    ) = DataViewState(
        title = "Visitors and Views",
        leftSection = AnalyticsInformationSectionViewState(
            "Visitors",
            visitorsCount.toString(),
            null
        ),
        rightSection = AnalyticsInformationSectionViewState(
            "Views",
            viewsCount.toString(),
            null
        )
    )

    private fun buildRevenueDataViewState(
        totalValue: String,
        totalDelta: DeltaPercentage,
        netValue: String,
        netDelta: DeltaPercentage
    ) =
        DataViewState(
            title = resourceProvider.getString(R.string.analytics_revenue_card_title),
            leftSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_total_sales_title),
                totalValue,
                if (totalDelta is DeltaPercentage.Value) totalDelta.value else null
            ),
            rightSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_net_sales_title),
                netValue,
                if (netDelta is DeltaPercentage.Value) netDelta.value else null
            )
        )

    private fun buildOrdersDataViewState(
        totalOrders: String,
        totalDelta: DeltaPercentage,
        avgValue: String,
        avgDelta: DeltaPercentage
    ) =
        DataViewState(
            title = resourceProvider.getString(R.string.analytics_orders_card_title),
            leftSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_total_orders_title),
                totalOrders,
                if (totalDelta is DeltaPercentage.Value) totalDelta.value else null
            ),
            rightSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_avg_orders_title),
                avgValue,
                if (avgDelta is DeltaPercentage.Value) avgDelta.value else null
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
                .mapIndexed { index, it ->
                    AnalyticsListCardItemViewState(
                        it.image,
                        it.name,
                        it.quantity.toString(),
                        resourceProvider.getString(
                            R.string.analytics_products_list_item_description,
                            formatValue(it.netSales.toString(), it.currencyCode)
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

    private fun trackSeeReportClicked(selectedCardType: String) {
        onTrackableUIInteraction()
        AnalyticsTracker.track(
            AnalyticsEvent.ANALYTICS_HUB_SEE_REPORT_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_CARD to selectedCardType
            )
        )
    }

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
