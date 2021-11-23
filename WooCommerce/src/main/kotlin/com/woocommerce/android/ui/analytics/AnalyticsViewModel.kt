package com.woocommerce.android.ui.analytics

import androidx.lifecycle.*
import com.woocommerce.android.R
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeSelectorContract.AnalyticsDateRangeSelectorViewState
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRanges
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val dateUtils: DateUtils
) : ViewModel() {

    private val mutableEffect = Channel<AnalyticsContract.AnalyticsEffect>()
    private val mutableEvent = MutableSharedFlow<AnalyticsContract.AnalyticsEvent>()
    private val mutableState = MutableLiveData<AnalyticsContract.AnalyticsState>()
        .apply {
            value = AnalyticsContract.AnalyticsState(
                analyticsDateRangeSelectorState = AnalyticsDateRangeSelectorViewState(
                    toDatePeriod = defaultToDatePeriod(),
                    fromDatePeriod = defaultFromDatePeriod(),
                    availableRangeDates = getAvailableDateRanges(),
                    defaultSelectedPeriod = getDefaultSelectedPeriod()
                )
            )
        }

    val state: LiveData<AnalyticsContract.AnalyticsState> = mutableState
    val effect: Flow<AnalyticsContract.AnalyticsEffect> = mutableEffect.receiveAsFlow()

    internal fun sendEvent(event: AnalyticsContract.AnalyticsEvent) = viewModelScope.launch { mutableEvent.emit(event) }

    fun onSelectedDateRangeChanged(dateRange: String) {
        val analyticsSelectedDateRange: AnalyticsDateRanges = AnalyticsDateRanges.valueOf(dateRange)

        mutableState.value = state.value!!.copy(
            analyticsDateRangeSelectorState = state.value!!.analyticsDateRangeSelectorState.copy(
                toDatePeriod = calculateToDatePeriod(analyticsSelectedDateRange),
                fromDatePeriod = calculateFromDatePeriod(analyticsSelectedDateRange),
            )
        )

    }

    private fun calculateToDatePeriod(analyticsSelectedDateRange: AnalyticsDateRanges) = resourceProvider.getString(
        R.string.analytics_date_range_to_date,
        dateUtils.getShortMonthDayAndYearString(
            dateUtils.getYearMonthDayStringFromDate(Date(dateUtils.getCurrentDateTimeMinusDays(1)))
        ) ?: ""
    )

    private fun calculateFromDatePeriod(analyticsSelectedDateRange: AnalyticsDateRanges) = resourceProvider.getString(
        R.string.analytics_date_range_to_date,
        dateUtils.getShortMonthDayAndYearString(
            dateUtils.getYearMonthDayStringFromDate(Date(dateUtils.getCurrentDateTimeMinusDays(1)))
        ) ?: ""
    )

    private fun getAvailableDateRanges() = resourceProvider.getStringArray(R.array.date_range_selectors).asList()
    private fun getDefaultSelectedPeriod() = resourceProvider.getString(R.string.date_timeframe_today)
    private fun defaultFromDatePeriod() = resourceProvider.getString(
        R.string.analytics_date_range_to_date,
        dateUtils.getShortMonthDayAndYearString(
            dateUtils.getYearMonthDayStringFromDate(Date(dateUtils.getCurrentDateTimeMinusDays(1)))
        ) ?: ""
    )

    private fun getAnalyticsFormatDateFor(date: Date = dateUtils.getCurrentDate()) =
        dateUtils.getShortMonthDayAndYearString(dateUtils.getYearMonthDayStringFromDate(date)) ?: ""

    private fun defaultToDatePeriod() =
        resourceProvider.getString(
            R.string.analytics_date_range_from_date,
            resourceProvider.getString(R.string.date_timeframe_today),
            getAnalyticsFormatDateFor(dateUtils.getCurrentDate())
        )


}
