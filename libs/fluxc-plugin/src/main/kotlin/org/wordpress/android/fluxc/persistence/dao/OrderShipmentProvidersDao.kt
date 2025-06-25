package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
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
    abstract fun observeOrderShipmentProviders(siteId: LocalId): Flow<List<WCOrderShipmentProviderModel>>

    @Transaction
    open suspend fun replaceAll(siteId: LocalId, providers: List<WCOrderShipmentProviderModel>) {
        deleteAll(siteId)
        insertAll(providers)
    }

    @Query(
        """
    DELETE FROM OrderShipmentProviderEntity
    WHERE localSiteId = :siteId
    """
    )
    protected abstract suspend fun deleteAll(siteId: LocalId): Int

    @Insert
    protected abstract suspend fun insertAll(providers: List<WCOrderShipmentProviderModel>)
}
