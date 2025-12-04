package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelCreationEligibility

@Dao
internal abstract class ShippingLabelCreationEligibilityDao {

    @Upsert
    abstract suspend fun upsertShippingLabelCreationEligibility(
        eligibility: WCShippingLabelCreationEligibility
    )

    @Query(
        """
        SELECT * FROM ShippingLabelCreationEligibilityEntity
        WHERE siteId = :siteId
        AND orderId = :orderId
        LIMIT 1
        """
    )
    abstract suspend fun getShippingLabelCreationEligibility(
        siteId: LocalId,
        orderId: RemoteId
    ): WCShippingLabelCreationEligibility?
}
