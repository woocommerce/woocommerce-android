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

    fun insertSingleAttribute(attribute: WCGlobalAttributeModel) = attribute.apply {
        WellSql.insert(attribute)
                .asSingleTransaction(true)
                .execute()
    }

    fun fetchSingleStoredAttribute(attributeId: Int, siteID: Int) =
        WellSql.select(WCGlobalAttributeModel::class.java)
                .where().beginGroup()
                .equals(WCGlobalAttributeModelTable.REMOTE_ID, attributeId)
                .equals(WCGlobalAttributeModelTable.LOCAL_SITE_ID, siteID)
                .endGroup().endWhere()
                .asModel
                .takeIf { it.isNotEmpty() }
                ?.first()

    fun deleteSingleStoredAttribute(attribute: WCGlobalAttributeModel, siteID: Int) = attribute.apply {
        WellSql.delete(WCGlobalAttributeModel::class.java)
                .where().beginGroup()
                .equals(WCGlobalAttributeModelTable.REMOTE_ID, attribute.remoteId)
                .equals(WCGlobalAttributeModelTable.LOCAL_SITE_ID, siteID)
                .endGroup().endWhere()
                .execute()
    }

    fun updateSingleStoredAttribute(attribute: WCGlobalAttributeModel, siteID: Int) = attribute.apply {
        WellSql.update(WCGlobalAttributeModel::class.java)
                .where().beginGroup()
                .equals(WCGlobalAttributeModelTable.REMOTE_ID, attribute.remoteId)
                .equals(WCGlobalAttributeModelTable.LOCAL_SITE_ID, siteID)
                .endGroup().endWhere()
                .put(attribute)
                .execute()
    }

    fun insertOrUpdateSingleAttribute(attribute: WCGlobalAttributeModel, siteID: Int) =
            fetchSingleStoredAttribute(attribute.remoteId, siteID)
                    ?.let {
                        attribute.id = it.id
                        updateSingleStoredAttribute(attribute, siteID)
                    }
                    ?: insertSingleAttribute(attribute)

    private fun deleteCompleteAttributesList(siteID: Int) =
            WellSql.delete(WCGlobalAttributeModel::class.java)
                    .where().beginGroup()
                    .equals(WCGlobalAttributeModelTable.LOCAL_SITE_ID, siteID)
                    .endGroup().endWhere()
                    .execute()
}
