package com.woocommerce.android.wear

import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.revenueStatsGranularity
import com.woocommerce.android.ui.analytics.ranges.visitorStatsGranularity
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.ui.dashboard.data.StatsRepository.SiteStats
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.locale.LocaleProvider
import org.wordpress.android.fluxc.model.SiteModel
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class GetWearableMyStoreStats @Inject constructor(
    private val statsRepository: StatsRepository,
    private val dateUtils: DateUtils,
    private val localeProvider: LocaleProvider
) {
    suspend operator fun invoke(selectedSite: SiteModel): SiteStats? {
        val todayRange = StatsTimeRangeSelection.build(
            selectionType = StatsTimeRangeSelection.SelectionType.TODAY,
            referenceDate = dateUtils.getCurrentDateInSiteTimeZone() ?: Date(),
            calendar = Calendar.getInstance(),
            locale = localeProvider.provideLocale() ?: Locale.getDefault()
        )
        return statsRepository.fetchStats(
            range = todayRange.currentRange,
            revenueStatsGranularity = todayRange.revenueStatsGranularity,
            visitorStatsGranularity = todayRange.visitorStatsGranularity,
            forced = true,
            includeVisitorStats = true,
            site = selectedSite
        ).getOrNull()
    }
}
