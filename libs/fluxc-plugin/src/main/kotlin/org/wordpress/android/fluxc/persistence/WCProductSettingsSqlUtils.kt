package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCProductSettingsModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel

object WCProductSettingsSqlUtils {
    fun insertOrUpdateProductSettings(settings: WCProductSettingsModel): Int {
        val result = WellSql.select(WCProductSettingsModel::class.java)
                .where()
                .equals(WCProductSettingsModelTable.LOCAL_SITE_ID, settings.localSiteId)
                .endWhere()
                .asModel.firstOrNull()

        return if (result == null) {
            WellSql.insert(settings).asSingleTransaction(true).execute()
            1
        } else {
            val oldId = result.id
            WellSql.update(WCProductSettingsModel::class.java).whereId(oldId)
                    .put(settings, UpdateAllExceptId(WCProductSettingsModel::class.java)).execute()
        }
    }

    fun getProductSettingsForSite(site: SiteModel): WCProductSettingsModel? {
        return WellSql.select(WCProductSettingsModel::class.java)
                .where()
                .equals(WCProductSettingsModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel.firstOrNull()
    }
}
