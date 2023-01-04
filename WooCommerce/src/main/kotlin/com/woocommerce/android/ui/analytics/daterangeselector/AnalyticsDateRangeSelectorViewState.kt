package com.woocommerce.android.ui.analytics.daterangeselector

data class AnalyticsDateRangeSelectorViewState(
    val toDatePeriod: String,
    val fromDatePeriod: String,
    val availableRangeDates: List<String>,
    val selectedPeriod: String
)
