package com.woocommerce.android.ui.mystore.stats

import com.woocommerce.commons.extensions.formatToYYYYmmDDhhmmss
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

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

    suspend fun fetchVisitorStats(
        selectedSite: SiteModel
    ): Result<Int?> {
        val todayRange = TodayRangeData(
            selectedSite = selectedSite,
            locale = Locale.getDefault(),
            referenceCalendar = Calendar.getInstance()
        ).currentRange

        val result = wcStatsStore.fetchNewVisitorStats(
            WCStatsStore.FetchNewVisitorStatsPayload(
                site = selectedSite,
                granularity = StatsGranularity.DAYS,
                startDate = todayRange.start.formatToYYYYmmDDhhmmss(),
                endDate = todayRange.end.formatToYYYYmmDDhhmmss()
            )
        )

        return when {
            result.isError -> Result.failure(Exception())
            else -> wcStatsStore.getNewVisitorStats(
                selectedSite,
                result.granularity,
                result.quantity,
                result.date,
                result.isCustomField
            ).let {
                Result.success(it.values.sum())
            }
        }
    }
}
