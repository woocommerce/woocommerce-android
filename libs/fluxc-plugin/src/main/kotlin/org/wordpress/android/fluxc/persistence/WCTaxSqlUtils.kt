package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCTaxClassModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.taxes.WCTaxClassModel

object WCTaxSqlUtils {
    fun getTaxClassesForSite(
        localSiteId: Int
    ): List<WCTaxClassModel> {
        return WellSql.select(WCTaxClassModel::class.java)
                .where()
                .equals(WCTaxClassModelTable.LOCAL_SITE_ID, localSiteId)
                .endWhere()
                .asModel
    }

    fun deleteTaxClassesForSite(site: SiteModel): Int {
        return WellSql.delete(WCTaxClassModel::class.java)
                .where()
                .equals(WCTaxClassModelTable.LOCAL_SITE_ID, site.id)
                .or()
                .equals(WCTaxClassModelTable.LOCAL_SITE_ID, 0) // Should never happen, but sanity cleanup
                .endWhere().execute()
    }

    fun insertOrUpdateTaxClasses(taxClassList: List<WCTaxClassModel>): Int {
        var rowsAffected = 0
        taxClassList.forEach {
            rowsAffected += insertOrUpdateTaxClass(it)
        }
        return rowsAffected
    }

    fun insertOrUpdateTaxClass(taxClass: WCTaxClassModel): Int {
        val result = WellSql.select(WCTaxClassModel::class.java)
                .where().beginGroup()
                .equals(WCTaxClassModelTable.ID, taxClass.id)
                .or()
                .beginGroup()
                .equals(WCTaxClassModelTable.LOCAL_SITE_ID, taxClass.localSiteId)
                .equals(WCTaxClassModelTable.SLUG, taxClass.slug)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return if (result == null) {
            // Insert
            WellSql.insert(taxClass).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = result.id
            WellSql.update(WCTaxClassModel::class.java).whereId(oldId)
                    .put(taxClass, UpdateAllExceptId(WCTaxClassModel::class.java)).execute()
        }
    }
}
