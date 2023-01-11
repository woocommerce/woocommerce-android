package com.woocommerce.android.ui.analytics.daterangeselector

import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType

data class AnalyticsDateRangeSelectorViewState(
    val currentRange: String,
    val previousRange: String,
    val selectionType: SelectionType
) {
    companion object {
        val EMPTY = AnalyticsDateRangeSelectorViewState(
            currentRange = "",
            previousRange = "",
            selectionType = SelectionType.CUSTOM
        )
    }
}
