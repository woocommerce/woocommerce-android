package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCShippingLabelCreationEligibilityTable
import com.wellsql.generated.WCShippingLabelModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelCreationEligibility
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel

object WCShippingLabelSqlUtils {
    fun getShippingClassesForOrder(
        localSiteId: Int,
        orderId: Long
    ): List<WCShippingLabelModel> {
        return WellSql.select(WCShippingLabelModel::class.java)
                .where()
                .equals(WCShippingLabelModelTable.LOCAL_SITE_ID, localSiteId)
                .equals(WCShippingLabelModelTable.REMOTE_ORDER_ID, orderId)
                .endWhere()
                .asModel
    }

    fun getShippingLabelById(
        localSiteId: Int,
        orderId: Long,
        remoteShippingLabelId: Long
    ): WCShippingLabelModel? {
        return WellSql.select(WCShippingLabelModel::class.java)
                .where()
                .equals(WCShippingLabelModelTable.LOCAL_SITE_ID, localSiteId)
                .equals(WCShippingLabelModelTable.REMOTE_ORDER_ID, orderId)
                .equals(WCShippingLabelModelTable.REMOTE_SHIPPING_LABEL_ID, remoteShippingLabelId)
                .endWhere()
                .asModel.firstOrNull()
    }

    fun insertOrUpdateShippingLabels(shippingLabels: List<WCShippingLabelModel>): Int {
        var totalChanged = 0
        shippingLabels.forEach { totalChanged += insertOrUpdateShippingLabel(it) }
        return totalChanged
    }

    fun insertOrUpdateShippingLabel(shippingLabel: WCShippingLabelModel): Int {
        val orderResult = WellSql.select(WCShippingLabelModel::class.java)
                .where().beginGroup()
                .equals(WCShippingLabelModelTable.ID, shippingLabel.id)
                .or()
                .beginGroup()
                .equals(WCShippingLabelModelTable.REMOTE_SHIPPING_LABEL_ID, shippingLabel.remoteShippingLabelId)
                .equals(WCShippingLabelModelTable.REMOTE_ORDER_ID, shippingLabel.remoteOrderId)
                .equals(WCShippingLabelModelTable.LOCAL_SITE_ID, shippingLabel.localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel

        return if (orderResult.isEmpty()) {
            // Insert
            WellSql.insert(shippingLabel).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = orderResult[0].id
            WellSql.update(WCShippingLabelModel::class.java).whereId(oldId)
                    .put(shippingLabel, UpdateAllExceptId(WCShippingLabelModel::class.java)).execute()
        }
    }

    fun deleteShippingLabelsForOrder(orderId: Long): Int =
            WellSql.delete(WCShippingLabelModel::class.java)
                    .where()
                    .equals(WCShippingLabelModelTable.REMOTE_ORDER_ID, orderId)
                    .endWhere().execute()

    fun deleteShippingLabelsForSite(localSiteId: Int): Int {
        return WellSql.delete(WCShippingLabelModel::class.java)
                .where()
                .equals(WCShippingLabelModelTable.LOCAL_SITE_ID, localSiteId)
                .or()
                .equals(WCShippingLabelModelTable.LOCAL_SITE_ID, 0) // Should never happen, but sanity cleanup
                .endWhere().execute()
    }

    fun insertOrUpdateSLCreationEligibility(eligibility: WCShippingLabelCreationEligibility): Int {
        val result = WellSql.select(WCShippingLabelCreationEligibility::class.java)
                .where().beginGroup()
                .equals(WCShippingLabelCreationEligibilityTable.ID, eligibility.id)
                .or()
                .beginGroup()
                .equals(WCShippingLabelCreationEligibilityTable.REMOTE_ORDER_ID, eligibility.remoteOrderId)
                .equals(WCShippingLabelCreationEligibilityTable.LOCAL_SITE_ID, eligibility.localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel

        return if (result.isEmpty()) {
            // Insert
            WellSql.insert(eligibility).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = result[0].id
            WellSql.update(WCShippingLabelCreationEligibility::class.java).whereId(oldId)
                    .put(eligibility, UpdateAllExceptId(WCShippingLabelCreationEligibility::class.java)).execute()
        }
    }

    fun getSLCreationEligibilityForOrder(
        localSiteId: Int,
        orderId: Long
    ): WCShippingLabelCreationEligibility? {
        return WellSql.select(WCShippingLabelCreationEligibility::class.java)
                .where()
                .equals(WCShippingLabelCreationEligibilityTable.LOCAL_SITE_ID, localSiteId)
                .equals(WCShippingLabelCreationEligibilityTable.REMOTE_ORDER_ID, orderId)
                .endWhere()
                .asModel
                .firstOrNull()
    }
}
