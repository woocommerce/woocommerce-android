package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel

@Dao
internal abstract class OrderShipmentProvidersDao {
    @Query(
        """
        SELECT * FROM OrderShipmentProviderEntity
        WHERE localSiteId = :siteId
        ORDER BY country ASC
        """
    )
    abstract suspend fun getOrderShipmentProvidersForSite(siteId: LocalId): List<WCOrderShipmentProviderModel>

    @Query(
        """
        DELETE FROM OrderShipmentProviderEntity
        WHERE localSiteId = :siteId
        """
    )
    abstract suspend fun deleteOrderShipmentProvidersForSite(siteId: LocalId): Int

    @Upsert
    abstract suspend fun upsertOrderShipmentProviders(providers: List<WCOrderShipmentProviderModel>)
}
