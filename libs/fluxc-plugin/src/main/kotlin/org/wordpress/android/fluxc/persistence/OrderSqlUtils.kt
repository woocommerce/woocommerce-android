package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCOrderShipmentTrackingModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel

object OrderSqlUtils {

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
