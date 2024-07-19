package com.woocommerce.android.ui.analytics.hub

import com.woocommerce.android.extensions.adminUrlOrDefault
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import javax.inject.Inject

class GetReportUrl @Inject constructor(
    private val selectedSite: SelectedSite
) {
    operator fun invoke(
        selection: StatsTimeRangeSelection,
        card: ReportCard
    ): String? {
        return selectedSite.getOrNull()?.let { siteModel ->
            val adminURL = siteModel.adminUrlOrDefault
            val page = "page=wc-admin"
            val path = when (card) {
                ReportCard.Revenue -> "path=%2Fanalytics%2Frevenue"
                ReportCard.Orders -> "path=%2Fanalytics%2Forders"
                ReportCard.Products -> "path=%2Fanalytics%2Fproducts"
                ReportCard.Bundles -> "path=%2Fanalytics%2Fbundles"
                ReportCard.GiftCard -> "path=%2Fanalytics%2Fgift-cards"
                ReportCard.GoogleAds -> "path=%2Fgoogle%2Freports"
            }
            val period = getPeriod(selection)
            val compare = "compare=previous_period"
            "${adminURL}admin.php?$page&$path&$period&$compare"
        }
    }

    private fun getPeriod(selection: StatsTimeRangeSelection): String {
        return when (selection.selectionType) {
            StatsTimeRangeSelection.SelectionType.TODAY -> "period=today"
            StatsTimeRangeSelection.SelectionType.YESTERDAY -> "period=yesterday"
            StatsTimeRangeSelection.SelectionType.LAST_WEEK -> "period=last_week"
            StatsTimeRangeSelection.SelectionType.LAST_MONTH -> "period=last_month"
            StatsTimeRangeSelection.SelectionType.LAST_QUARTER -> "period=last_quarter"
            StatsTimeRangeSelection.SelectionType.LAST_YEAR -> "period=last_year"
            StatsTimeRangeSelection.SelectionType.WEEK_TO_DATE -> "period=week"
            StatsTimeRangeSelection.SelectionType.MONTH_TO_DATE -> "period=month"
            StatsTimeRangeSelection.SelectionType.QUARTER_TO_DATE -> "period=quarter"
            StatsTimeRangeSelection.SelectionType.YEAR_TO_DATE -> "period=year"
            StatsTimeRangeSelection.SelectionType.CUSTOM -> {
                val startDate = selection.currentRange.start.formatToYYYYmmDD()
                val endDate = selection.currentRange.end.formatToYYYYmmDD()
                "period=custom&after=$startDate&before=$endDate"
            }
        }
    }
}
