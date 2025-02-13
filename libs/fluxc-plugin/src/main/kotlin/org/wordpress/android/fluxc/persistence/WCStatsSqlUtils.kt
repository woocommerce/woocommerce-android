package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCRevenueStatsModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

object WCStatsSqlUtils {
    /**
     * Methods to support the new v4 revenue stats api
     */
    fun insertOrUpdateRevenueStats(stats: WCRevenueStatsModel): Int {
        val statsResult = searchMatchingRevenueStatsInDatabase(stats)

        return if (statsResult.isEmpty()) {
            // insert
            WellSql.insert(stats).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = statsResult[0].id
            WellSql.update(WCRevenueStatsModel::class.java).whereId(oldId)
                    .put(stats, UpdateAllExceptId(WCRevenueStatsModel::class.java)).execute()
        }
    }

    private fun searchMatchingRevenueStatsInDatabase(stats: WCRevenueStatsModel): List<WCRevenueStatsModel> {
        return if (stats.rangeId.isNotEmpty()) {
            WellSql.select(WCRevenueStatsModel::class.java)
                .where().beginGroup()
                .equals(WCRevenueStatsModelTable.LOCAL_SITE_ID, stats.localSiteId)
                .equals(WCRevenueStatsModelTable.RANGE_ID, stats.rangeId)
                .endGroup().endWhere()
                .asModel
        } else {
            WellSql.select(WCRevenueStatsModel::class.java)
                .where().beginGroup()
                .equals(WCRevenueStatsModelTable.LOCAL_SITE_ID, stats.localSiteId)
                .equals(WCRevenueStatsModelTable.INTERVAL, stats.interval)
                .equals(WCRevenueStatsModelTable.START_DATE, stats.startDate)
                .equals(WCRevenueStatsModelTable.END_DATE, stats.endDate)
                .endGroup().endWhere()
                .asModel
        }
    }

    fun getRevenueStatsForSiteIntervalAndDate(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String
    ): WCRevenueStatsModel? {
        return WellSql.select(WCRevenueStatsModel::class.java)
                .where()
                .beginGroup()
                .equals(WCRevenueStatsModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCRevenueStatsModelTable.INTERVAL, granularity)
                .equals(WCRevenueStatsModelTable.START_DATE, startDate)
                .equals(WCRevenueStatsModelTable.END_DATE, endDate)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun getRevenueStatsFromRangeId(
        site: SiteModel,
        revenueRangeId: String
    ) = WellSql.select(WCRevenueStatsModel::class.java)
        .where()
        .beginGroup()
        .equals(WCRevenueStatsModelTable.LOCAL_SITE_ID, site.id)
        .equals(WCRevenueStatsModelTable.RANGE_ID, revenueRangeId)
        .endGroup().endWhere()
        .asModel.firstOrNull()
}
