package com.woocommerce.android.ui.mystore

import com.woocommerce.commons.extensions.formatToYYYYmmDDhhmmss
import org.wordpress.android.fluxc.store.WCStatsStore
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

class StatsRepository @Inject constructor(
    private val wcStatsStore: WCStatsStore,
) {
    suspend fun fetchRevenueStats(
        selectedSite: SiteModel
    ): Result<WCRevenueStatsModel?> {
        val todayRange = TodayRangeData(
            selectedSite = selectedSite,
            locale = Locale.getDefault(),
            referenceCalendar = Calendar.getInstance()
        ).currentRange

        val result = wcStatsStore.fetchRevenueStats(
            FetchRevenueStatsPayload(
                site = selectedSite,
                granularity = StatsGranularity.DAYS,
                startDate = todayRange.start.formatToYYYYmmDDhhmmss(),
                endDate = todayRange.end.formatToYYYYmmDDhhmmss()
            )
        )

        return when {
            result.isError -> Result.failure(Exception())
            else -> wcStatsStore.getRawRevenueStats(
                selectedSite,
                result.granularity,
                result.startDate.orEmpty(),
                result.endDate.orEmpty()
            ).let { Result.success(it) }
        }
    }
}
