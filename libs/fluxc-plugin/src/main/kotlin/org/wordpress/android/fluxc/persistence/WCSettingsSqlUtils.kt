package org.wordpress.android.fluxc.persistence

import android.content.ContentValues
import com.wellsql.generated.WCSettingsModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.persistence.mappers.SettingsMapper.toBuilder

object WCSettingsSqlUtils {
    fun insertOrUpdateSettings(settings: WCSettingsModel): Int {
        val orderResult = WellSql.select(WCSettingsBuilder::class.java)
                .where()
                .equals(WCSettingsModelTable.LOCAL_SITE_ID, settings.localSiteId)
                .endWhere()
                .asModel

        return if (orderResult.isEmpty()) {
            // Insert
            WellSql.insert(settings.toBuilder()).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = orderResult[0].id
            WellSql.update(WCSettingsBuilder::class.java).whereId(oldId)
                    .put(settings.toBuilder(), UpdateAllExceptId(WCSettingsBuilder::class.java)).execute()
        }
    }

    fun setCouponsEnabled(site: SiteModel, value: Boolean): Int {
        return WellSql.update(WCSettingsBuilder::class.java)
            .whereId(site.id)
            .put(value) {
                val cv = ContentValues()
                cv.put(WCSettingsModelTable.COUPONS_ENABLED, it)
                cv
            }.execute()
    }

    fun getSettingsForSite(site: SiteModel): WCSettingsModel? {
        return WellSql.select(WCSettingsBuilder::class.java)
                .where()
                .equals(WCSettingsModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel.firstOrNull()?.build()
    }

}
