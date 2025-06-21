package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel

@Dao
internal abstract class OrderShipmentProvidersDao {

    companion object {
        const val DEFAULT_SELECT_QUERY = """
            SELECT * FROM OrderShipmentProviderEntity
            WHERE localSiteId = :siteId
            ORDER BY country ASC
        """
    }

    @Query(DEFAULT_SELECT_QUERY)
    abstract fun observeOrderShipmentProviders(siteId: LocalId): Flow<List<WCOrderShipmentProviderModel>>

    @Query(DEFAULT_SELECT_QUERY)
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
