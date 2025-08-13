package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCGlobalAttributeModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel

object WCGlobalAttributeSqlUtils {
    fun getCurrentAttributes(siteID: Int) =
            WellSql.select(WCGlobalAttributeModel::class.java)
                    .where().beginGroup()
                    .equals(WCGlobalAttributeModelTable.LOCAL_SITE_ID, siteID)
                    .endGroup().endWhere()
                    .asModel
                    ?.toList()
                    .orEmpty()

    fun insertFromScratchCompleteAttributesList(attributes: List<WCGlobalAttributeModel>, siteID: Int) {
        deleteCompleteAttributesList(siteID)
        WellSql.insert(attributes)
                .asSingleTransaction(true).execute()
    }

    private fun deleteCompleteAttributesList(siteID: Int) =
            WellSql.delete(WCGlobalAttributeModel::class.java)
                    .where().beginGroup()
                    .equals(WCGlobalAttributeModelTable.LOCAL_SITE_ID, siteID)
                    .endGroup().endWhere()
                    .execute()
}
