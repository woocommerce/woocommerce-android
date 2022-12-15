package com.woocommerce.android.ui.analytics.ranges

class AnalyticsHubDateRangeSelection(
    private val selectionType: SelectionType
) {
    private val currentRange: AnalyticsHubTimeRange?
    private val previousRange: AnalyticsHubTimeRange?

    init {
        this.currentRange = null
        this.previousRange = null
    }

    enum class SelectionType {
        TODAY,
        YESTERDAY,
        LAST_WEEK,
        LAST_MONTH,
        LAST_QUARTER,
        LAST_YEAR,
        WEEK_TO_DATE,
        MONTH_TO_DATE,
        QUARTER_TO_DATE,
        YEAR_TO_DATE,
        CUSTOM
    }
}
