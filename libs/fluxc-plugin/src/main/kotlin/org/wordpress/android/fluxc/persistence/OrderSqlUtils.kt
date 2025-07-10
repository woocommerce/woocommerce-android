package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCOrderShipmentTrackingModelTable
import com.wellsql.generated.WCOrderStatusModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel

object OrderSqlUtils {
    fun insertOrUpdateOrderStatusOption(orderStatus: WCOrderStatusModel): Int {
        val result = WellSql.select(WCOrderStatusModel::class.java)
                .where().beginGroup()
                .equals(WCOrderStatusModelTable.ID, orderStatus.id)
                .or()
                .equals(WCOrderStatusModelTable.STATUS_KEY, orderStatus.statusKey)
                .endGroup().endWhere().asModel

        return if (result.isEmpty()) {
            // Insert
            WellSql.insert(orderStatus).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = result[0].id
            WellSql.update(WCOrderStatusModel::class.java).whereId(oldId)
                    .put(orderStatus, UpdateAllExceptId(WCOrderStatusModel::class.java)).execute()
        }
    }

    fun getOrderStatusOptionsForSite(site: SiteModel): List<WCOrderStatusModel> =
            WellSql.select(WCOrderStatusModel::class.java)
                    .where()
                    .equals(WCOrderStatusModelTable.LOCAL_SITE_ID, site.id)
                    .endWhere().asModel

    fun getOrderStatusOptionForSiteByKey(site: SiteModel, key: String): WCOrderStatusModel? =
            WellSql.select(WCOrderStatusModel::class.java)
                    .where().beginGroup()
                    .equals(WCOrderStatusModelTable.STATUS_KEY, key)
                    .equals(WCOrderStatusModelTable.LOCAL_SITE_ID, site.id)
                    .endGroup().endWhere().asModel.firstOrNull()

    fun deleteOrderStatusOption(orderStatus: WCOrderStatusModel): Int =
            WellSql.delete(WCOrderStatusModel::class.java).whereId(orderStatus.id)

    fun insertOrIgnoreOrderShipmentTracking(tracking: WCOrderShipmentTrackingModel): Int {
        val result = WellSql.select(WCOrderShipmentTrackingModel::class.java)
                .where().beginGroup()
                .equals(WCOrderShipmentTrackingModelTable.ID, tracking.id)
                .or()
                .beginGroup()
                .equals(WCOrderShipmentTrackingModelTable.LOCAL_SITE_ID, tracking.localSiteId)
                .equals(WCOrderShipmentTrackingModelTable.LOCAL_ORDER_ID, tracking.orderId)
                .equals(WCOrderShipmentTrackingModelTable.REMOTE_TRACKING_ID, tracking.remoteTrackingId)
                .endGroup().endGroup().endWhere().asModel

        return if (result.isEmpty()) {
            WellSql.insert(tracking).asSingleTransaction(true).execute()
            1
        } else {
            0
        }
    }

    fun getShipmentTrackingsForOrder(
        site: SiteModel,
        orderId: Long
    ): List<WCOrderShipmentTrackingModel> {
        return WellSql.select(WCOrderShipmentTrackingModel::class.java)
                .where()
                .beginGroup()
                .equals(WCOrderShipmentTrackingModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCOrderShipmentTrackingModelTable.LOCAL_ORDER_ID, orderId)
                .endGroup().endWhere()
                .orderBy(WCOrderShipmentTrackingModelTable.DATE_SHIPPED, SelectQuery.ORDER_DESCENDING).asModel
    }

    fun getShipmentTrackingByTrackingNumber(
        site: SiteModel,
        orderId: Long,
        trackingNumber: String
    ): WCOrderShipmentTrackingModel? {
        return WellSql.select(WCOrderShipmentTrackingModel::class.java)
                .where()
                .beginGroup()
                .equals(WCOrderShipmentTrackingModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCOrderShipmentTrackingModelTable.LOCAL_ORDER_ID, orderId)
                .equals(WCOrderShipmentTrackingModelTable.TRACKING_NUMBER, trackingNumber)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun deleteOrderShipmentTrackingById(tracking: WCOrderShipmentTrackingModel): Int =
            WellSql.delete(WCOrderShipmentTrackingModel::class.java).whereId(tracking.id)

    fun deleteOrderShipmentTrackingsForSite(site: SiteModel): Int =
            WellSql.delete(WCOrderShipmentTrackingModel::class.java)
                    .where()
                    .equals(WCOrderShipmentTrackingModelTable.LOCAL_SITE_ID, site.id)
                    .endWhere()
                    .execute()
}
