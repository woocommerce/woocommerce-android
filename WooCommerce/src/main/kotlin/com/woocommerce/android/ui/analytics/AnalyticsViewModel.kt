package com.woocommerce.android.ui.analytics

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.AnalyticsRepository.OrdersResult.OrdersData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueError
import com.woocommerce.android.ui.analytics.AnalyticsViewEvent.OpenUrl
import com.woocommerce.android.ui.analytics.AnalyticsViewEvent.OpenWPComWebView
import com.woocommerce.android.ui.analytics.daterangeselector.*
import com.woocommerce.android.ui.analytics.daterangeselector.DateRange.MultipleDateRange
import com.woocommerce.android.ui.analytics.daterangeselector.DateRange.SimpleDateRange
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationSectionViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.*
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.zendesk.util.DateUtils.isSameDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val dateUtils: DateUtils,
    private val analyticsDateRange: AnalyticsDateRangeCalculator,
    private val currencyFormatter: CurrencyFormatter,
    private val analyticsRepository: AnalyticsRepository,
    private val selectedSite: SelectedSite,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val mutableState = MutableStateFlow(
        AnalyticsViewState(
            buildAnalyticsDateRangeSelectorViewState(),
            LoadingViewState,
            LoadingViewState
        )
    )

    val state: StateFlow<AnalyticsViewState> = mutableState

    init {
        updateRevenue()
        updateOrders()
    }

    fun onRefreshRequested() {
        updateRevenue(getCurrentRange(), getCurrentDateRange())
        updateOrders(getCurrentRange(), getCurrentDateRange())
    }

    fun onSelectedDateRangeChanged(newSelection: String) {
        val selectedRange: AnalyticsDateRanges = AnalyticsDateRanges.from(newSelection)
        val newDateRange = analyticsDateRange.getAnalyticsDateRangeFrom(selectedRange)
        saveCurrentRange(selectedRange)
        saveCurrentDateRange(newDateRange)
        updateDateRangeCalendarView(selectedRange, newDateRange)
        updateRevenue(selectedRange, newDateRange)
        updateOrders(selectedRange, newDateRange)
    }

    fun onRevenueSeeReportClick() {
        if (selectedSite.getIfExists()?.isWPCom == true || selectedSite.getIfExists()?.isWPComAtomic == true) {
            triggerEvent(OpenWPComWebView(analyticsRepository.getRevenueAdminPanelUrl()))
        } else {
            triggerEvent(OpenUrl(analyticsRepository.getRevenueAdminPanelUrl()))
        }
    }

    fun onOrdersSeeReportClick() {
        if (selectedSite.getIfExists()?.isWPCom == true || selectedSite.getIfExists()?.isWPComAtomic == true) {
            triggerEvent(OpenWPComWebView(analyticsRepository.getOrdersAdminPanelUrl()))
        } else {
            triggerEvent(OpenUrl(analyticsRepository.getOrdersAdminPanelUrl()))
        }
    }

    private fun updateRevenue(
        range: AnalyticsDateRanges = AnalyticsDateRanges.from(getDefaultSelectedPeriod()),
        dateRange: DateRange = getDefaultDateRange()
    ) =
        launch {
            mutableState.value = state.value.copy(revenueState = LoadingViewState)
            analyticsRepository.fetchRevenueData(dateRange, range)
                .collect {
                    when (it) {
                        is RevenueData -> mutableState.value = state.value.copy(
                            revenueState = buildRevenueDataViewState(
                                formatValue(it.revenueStat.totalValue.toString(), it.revenueStat.currencyCode),
                                it.revenueStat.totalDelta,
                                formatValue(it.revenueStat.netValue.toString(), it.revenueStat.currencyCode),
                                it.revenueStat.netDelta
                            )
                        )
                        is RevenueError -> mutableState.value = state.value.copy(
                            revenueState = NoDataState(resourceProvider.getString(R.string.analytics_revenue_no_data))
                        )
                    }
                }
        }

    private fun updateOrders(
        range: AnalyticsDateRanges = AnalyticsDateRanges.from(getDefaultSelectedPeriod()),
        dateRange: DateRange = getDefaultDateRange()
    ) =
        launch {
            mutableState.value = state.value.copy(ordersState = LoadingViewState)
            analyticsRepository.fetchOrdersData(dateRange, range)
                .collect {
                    when (it) {
                        is OrdersData -> mutableState.value = state.value.copy(
                            ordersState = buildOrdersDataViewState(
                                formatValue(it.ordersStat.ordersCount.toString(), it.ordersStat.currencyCode),
                                it.ordersStat.ordersCountDelta,
                                formatValue(it.ordersStat.avgOrderValue.toString(), it.ordersStat.currencyCode),
                                it.ordersStat.avgOrderDelta
                            )
                        )
                        is AnalyticsRepository.OrdersResult.OrdersError -> mutableState.value = state.value.copy(
                            ordersState = NoDataState(resourceProvider.getString(R.string.analytics_orders_no_data))
                        )
                    }
                }
        }

    private fun updateDateRangeCalendarView(newRange: AnalyticsDateRanges, newDateRange: DateRange) {
        mutableState.value = state.value.copy(
            analyticsDateRangeSelectorState = state.value.analyticsDateRangeSelectorState.copy(
                fromDatePeriod = calculateFromDatePeriod(newDateRange),
                toDatePeriod = calculateToDatePeriod(newRange, newDateRange),
                selectedPeriod = getDateSelectedMessage(newRange)
            )
        )
    }

    private fun calculateToDatePeriod(analyticsDateRange: AnalyticsDateRanges, dateRange: DateRange) =
        when (dateRange) {
            is SimpleDateRange -> resourceProvider.getString(
                R.string.analytics_date_range_to_date,
                getDateSelectedMessage(analyticsDateRange),
                dateUtils.getShortMonthDayAndYearString(
                    dateUtils.getYearMonthDayStringFromDate(dateRange.to)
                ).orEmpty()
            )
            is MultipleDateRange ->
                if (isSameDay(dateRange.to.from, dateRange.to.to)) {
                    resourceProvider.getString(
                        R.string.analytics_date_range_to_date,
                        getDateSelectedMessage(analyticsDateRange),
                        dateUtils.getShortMonthDayAndYearString(
                            dateUtils.getYearMonthDayStringFromDate(dateRange.to.from)
                        ).orEmpty()
                    )
                } else {
                    resourceProvider.getString(
                        R.string.analytics_date_range_to_date,
                        getDateSelectedMessage(analyticsDateRange),
                        dateRange.to.formatDatesToFriendlyPeriod()
                    )
                }
        }

    private fun calculateFromDatePeriod(dateRange: DateRange) = when (dateRange) {
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

    private fun getAvailableDateRanges() = resourceProvider.getStringArray(R.array.date_range_selectors).asList()
    private fun getDefaultSelectedPeriod() = getDateSelectedMessage(AnalyticsDateRanges.TODAY)
    private fun getDefaultDateRange() = SimpleDateRange(
        Date(dateUtils.getCurrentDateTimeMinusDays(1)),
        dateUtils.getCurrentDate()
    )

    private fun getDateSelectedMessage(analyticsDateRange: AnalyticsDateRanges): String =
        when (analyticsDateRange) {
            AnalyticsDateRanges.TODAY -> resourceProvider.getString(R.string.date_timeframe_today)
            AnalyticsDateRanges.YESTERDAY -> resourceProvider.getString(R.string.date_timeframe_yesterday)
            AnalyticsDateRanges.LAST_WEEK -> resourceProvider.getString(R.string.date_timeframe_last_week)
            AnalyticsDateRanges.LAST_MONTH -> resourceProvider.getString(R.string.date_timeframe_last_month)
            AnalyticsDateRanges.LAST_QUARTER -> resourceProvider.getString(R.string.date_timeframe_last_quarter)
            AnalyticsDateRanges.LAST_YEAR -> resourceProvider.getString(R.string.date_timeframe_last_year)
            AnalyticsDateRanges.WEEK_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_week_to_date)
            AnalyticsDateRanges.MONTH_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_month_to_date)
            AnalyticsDateRanges.QUARTER_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_quarter_to_date)
            AnalyticsDateRanges.YEAR_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_year_to_date)
        }

    private fun formatValue(value: String, currencyCode: String?) = currencyCode
        ?.let { currencyFormatter.formatCurrency(value, it) }
        ?: value

    private fun buildAnalyticsDateRangeSelectorViewState() = AnalyticsDateRangeSelectorViewState(
        fromDatePeriod = calculateFromDatePeriod(getDefaultDateRange()),
        toDatePeriod = calculateToDatePeriod(AnalyticsDateRanges.TODAY, getDefaultDateRange()),
        availableRangeDates = getAvailableDateRanges(),
        selectedPeriod = getDefaultSelectedPeriod()
    )

    private fun buildRevenueDataViewState(totalValue: String, totalDelta: Int, netValue: String, netDelta: Int) =
        DataViewState(
            title = resourceProvider.getString(R.string.analytics_revenue_card_title),
            leftSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_total_sales_title),
                totalValue, totalDelta
            ),
            rightSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_net_sales_title),
                netValue, netDelta
            )
        )

    private fun buildOrdersDataViewState(totalOrders: String, totalDelta: Int, avgValue: String, avgDelta: Int) =
        DataViewState(
            title = resourceProvider.getString(R.string.analytics_orders_card_title),
            leftSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_total_orders_title),
                totalOrders, totalDelta
            ),
            rightSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_avg_orders_title),
                avgValue, avgDelta
            )
        )

    private fun saveCurrentRange(range: AnalyticsDateRanges) {
        savedState[RANGE_SELECTION_KEY] = range
    }

    private fun saveCurrentDateRange(dateRange: DateRange) {
        savedState[DATE_RANGE_SELECTION_KEY] = dateRange
    }

    private fun getCurrentDateRange(): DateRange = savedState[DATE_RANGE_SELECTION_KEY] ?: getDefaultDateRange()
    private fun getCurrentRange(): AnalyticsDateRanges = savedState[RANGE_SELECTION_KEY]
        ?: AnalyticsDateRanges.from(getDefaultSelectedPeriod())

    companion object {
        const val RANGE_SELECTION_KEY = "range_selection_key"
        const val DATE_RANGE_SELECTION_KEY = "date_range_selection_key"
    }
}
