 package org.wordpress.android.fluxc.persistence

 import com.wellsql.generated.WCShippingLabelCreationEligibilityTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelCreationEligibility

 object WCShippingLabelSqlUtils {
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
