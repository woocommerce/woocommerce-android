package com.woocommerce.android.ui.analytics.hub.daterangeselector

import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType

data class AnalyticsHubDateRangeSelectorViewState(
    val currentRange: String,
    val previousRange: String,
    val selectionType: SelectionType
) {
    companion object {
        val EMPTY = AnalyticsHubDateRangeSelectorViewState(
            currentRange = "",
            previousRange = "",
            selectionType = SelectionType.CUSTOM
        )
    }
}
