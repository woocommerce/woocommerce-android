package com.woocommerce.android.ui.analytics.ranges

class AnalyticsHubDateRangeSelection(
    private val selectionType: AnalyticsHubRangeSelectionType
) {
    private val currentRange: AnalyticsHubTimeRange?
    private val previousRange: AnalyticsHubTimeRange?

    init {
        this.currentRange = null
        this.previousRange = null
    }
}
