package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCNewVisitorStatsModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCNewVisitorStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

object WCVisitorStatsSqlUtils {
    /**
     * Methods to support the new v4 stats changes
     */
    fun insertOrUpdateNewVisitorStats(stats: WCNewVisitorStatsModel): Int {
        val statsResult = if (stats.isCustomField) {
            WellSql.select(WCNewVisitorStatsModel::class.java)
                    .where().beginGroup()
                    .equals(WCNewVisitorStatsModelTable.LOCAL_SITE_ID, stats.localSiteId)
                    .equals(WCNewVisitorStatsModelTable.GRANULARITY, stats.granularity)
                    .equals(WCNewVisitorStatsModelTable.DATE, stats.date)
                    .equals(WCNewVisitorStatsModelTable.QUANTITY, stats.quantity)
                    .equals(WCNewVisitorStatsModelTable.IS_CUSTOM_FIELD, stats.isCustomField)
                    .endGroup().endWhere()
                    .asModel
        } else {
            WellSql.select(WCNewVisitorStatsModel::class.java)
                    .where().beginGroup()
                    .equals(WCNewVisitorStatsModelTable.LOCAL_SITE_ID, stats.localSiteId)
                    .equals(WCNewVisitorStatsModelTable.GRANULARITY, stats.granularity)
                    .equals(WCNewVisitorStatsModelTable.IS_CUSTOM_FIELD, stats.isCustomField)
                    .endGroup().endWhere()
                    .asModel
        }

        if (statsResult.isEmpty()) {
            /*
             * if no visitor stats available for this particular date, quantity, granularity and site:
             * - check if the incoming data is custom data or default data.
             *      - if custom data, we need to delete any previously cached custom data
             *        for the particular site before inserting incoming data
             */
            if (stats.isCustomField) {
                deleteNewCustomVisitorStatsForSite(stats.localSiteId)
            }

            WellSql.insert(stats).asSingleTransaction(true).execute()
            return 1
        } else {
            // Update
            val oldId = statsResult[0].id
            return WellSql.update(WCNewVisitorStatsModel::class.java).whereId(oldId)
                    .put(stats, UpdateAllExceptId(WCNewVisitorStatsModel::class.java)).execute()
        }
    }

    fun getNewRawVisitorStatsForSiteGranularityQuantityAndDate(
        site: SiteModel,
        granularity: StatsGranularity,
        quantity: String? = null,
        date: String? = null,
        isCustomField: Boolean = false
    ): WCNewVisitorStatsModel? {
        if (!isCustomField)
            return getNewRawVisitorStatsForSiteAndGranularity(site, granularity)

        return WellSql.select(WCNewVisitorStatsModel::class.java)
                .where()
                .beginGroup()
                .equals(WCNewVisitorStatsModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCNewVisitorStatsModelTable.GRANULARITY, granularity)
                .equals(WCNewVisitorStatsModelTable.QUANTITY, quantity)
                .equals(WCNewVisitorStatsModelTable.DATE, date)
                .equals(WCNewVisitorStatsModelTable.IS_CUSTOM_FIELD, isCustomField)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    private fun getNewRawVisitorStatsForSiteAndGranularity(
        site: SiteModel,
        granularity: StatsGranularity
    ): WCNewVisitorStatsModel? {
        return WellSql.select(WCNewVisitorStatsModel::class.java)
                .where()
                .beginGroup()
                .equals(WCNewVisitorStatsModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCNewVisitorStatsModelTable.GRANULARITY, granularity)
                .equals(WCNewVisitorStatsModelTable.IS_CUSTOM_FIELD, false)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    private fun deleteNewCustomVisitorStatsForSite(siteId: Int): Int {
        return WellSql.delete(WCNewVisitorStatsModel::class.java)
                .where()
                .equals(WCNewVisitorStatsModelTable.LOCAL_SITE_ID, siteId)
                .equals(WCNewVisitorStatsModelTable.IS_CUSTOM_FIELD, true)
                .endWhere()
                .execute()
    }
}
