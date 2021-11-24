package com.woocommerce.android.ui.analytics.daterangeselector

class AnalyticsDateRangeSelectorContract {
    data class AnalyticsDateRangeSelectorViewState(
        val toDatePeriod: String,
        val fromDatePeriod: String,
        val availableRangeDates: List<String>,
        val selectedPeriod: String
    )

    interface DateRangeEvent {
        fun onDateRangeCalendarClickEvent()
    }
}
