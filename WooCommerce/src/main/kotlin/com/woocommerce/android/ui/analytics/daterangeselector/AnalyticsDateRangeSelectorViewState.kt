package com.woocommerce.android.ui.analytics.daterangeselector

data class AnalyticsDateRangeSelectorViewState(
    val currentRange: String,
    val previousRange: String,
    val selectionTitle: String
) {
    companion object {
        val EMPTY = AnalyticsDateRangeSelectorViewState(
            currentRange = "",
            previousRange = "",
            selectionTitle = ""
        )
    }
}
