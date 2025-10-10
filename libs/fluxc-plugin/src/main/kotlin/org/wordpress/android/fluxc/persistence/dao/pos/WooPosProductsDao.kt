package org.wordpress.android.fluxc.persistence.dao.pos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity

@Dao
abstract class WooPosProductsDao {
    @Query("SELECT * FROM PosProductEntity WHERE localSiteId = :localSiteId ORDER BY LOWER(name)")
    abstract fun observeAllProducts(localSiteId: LocalId): Flow<List<WooPosProductEntity>>

    @Query("SELECT * FROM PosProductEntity WHERE localSiteId = :localSiteId AND remoteId = :remoteId")
    abstract suspend fun getProduct(localSiteId: LocalId, remoteId: RemoteId): WooPosProductEntity?

    @Upsert
    abstract suspend fun upsertProducts(products: List<WooPosProductEntity>)

    @Query("DELETE FROM PosProductEntity WHERE localSiteId = :localSiteId AND remoteId = :remoteId")
    abstract suspend fun deleteProduct(localSiteId: LocalId, remoteId: RemoteId)

    @Query("DELETE FROM PosProductEntity WHERE localSiteId = :localSiteId AND remoteId NOT IN (:remoteIds)")
    protected abstract suspend fun deleteProductsNotInListQuery(localSiteId: LocalId, remoteIds: List<RemoteId>)

    @Query("DELETE FROM PosProductEntity WHERE localSiteId = :localSiteId")
    protected abstract suspend fun deleteAllProductsForSite(localSiteId: LocalId)

    open suspend fun deleteProductsNotInList(localSiteId: LocalId, remoteIds: List<RemoteId>) {
        if (remoteIds.isEmpty()) {
            deleteAllProductsForSite(localSiteId)
        } else {
            deleteProductsNotInListQuery(localSiteId, remoteIds)
        }
    }
}
