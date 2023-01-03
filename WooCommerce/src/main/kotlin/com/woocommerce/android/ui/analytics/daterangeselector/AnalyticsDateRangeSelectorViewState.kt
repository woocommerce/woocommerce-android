package com.woocommerce.android.ui.analytics.daterangeselector

data class AnalyticsDateRangeSelectorViewState(
    val toDatePeriod: String,
    val fromDatePeriod: String,
    val selectedPeriod: String
) {
    companion object {
        val EMPTY = AnalyticsDateRangeSelectorViewState(
            toDatePeriod = "",
            fromDatePeriod = "",
            selectedPeriod = ""
        )
    }
}
