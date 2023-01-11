package com.woocommerce.android.ui.analytics.daterangeselector

import android.content.res.Resources
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType
import com.woocommerce.android.viewmodel.ResourceProvider

data class AnalyticsDateRangeSelectorViewState(
    val currentRange: String,
    val previousRange: String,
    val selectionType: SelectionType
) {
    fun generateSelectionTitle(resources: Resources) =
        resources.getString(selectionType.localizedResourceId)

    companion object {
        val EMPTY = AnalyticsDateRangeSelectorViewState(
            currentRange = "",
            previousRange = "",
            selectionType = SelectionType.CUSTOM
        )
    }
}
