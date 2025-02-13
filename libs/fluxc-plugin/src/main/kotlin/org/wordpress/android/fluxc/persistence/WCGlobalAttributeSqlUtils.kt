package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCAttributeTermModelTable
import com.wellsql.generated.WCGlobalAttributeModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel
import org.wordpress.android.fluxc.model.attribute.terms.WCAttributeTermModel

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

    fun updateSingleAttributeTermsMapping(attributeID: Int, termsMapping: String, siteID: Int) =
        fetchSingleStoredAttribute(attributeID, siteID)
                ?.let {
                    it.termsId = termsMapping
                    updateSingleStoredAttribute(it, siteID)
                }

    fun getTerm(siteID: Int, termID: Int) =
            WellSql.select(WCAttributeTermModel::class.java)
                    .where().beginGroup()
                    .equals(WCAttributeTermModelTable.REMOTE_ID, termID)
                    .equals(WCAttributeTermModelTable.LOCAL_SITE_ID, siteID)
                    .endGroup().endWhere()
                    .asModel
                    ?.toList()
                    ?.firstOrNull()

    fun insertAttributeTermsFromScratch(
        attributeID: Int,
        siteID: Int,
        terms: List<WCAttributeTermModel>
    ) {
        deleteAllTermsFromSingleAttribute(attributeID, siteID)
        WellSql.insert(terms)
                .asSingleTransaction(true)
                .execute()
    }

    private fun deleteCompleteAttributesList(siteID: Int) =
            WellSql.delete(WCGlobalAttributeModel::class.java)
                    .where().beginGroup()
                    .equals(WCGlobalAttributeModelTable.LOCAL_SITE_ID, siteID)
                    .endGroup().endWhere()
                    .execute()

    private fun deleteAllTermsFromSingleAttribute(attributeID: Int, siteID: Int) =
            WellSql.delete(WCAttributeTermModel::class.java)
                    .where().beginGroup()
                    .equals(WCAttributeTermModelTable.LOCAL_SITE_ID, siteID)
                    .equals(WCAttributeTermModelTable.ATTRIBUTE_ID, attributeID)
                    .endGroup().endWhere()
                    .execute()
}
