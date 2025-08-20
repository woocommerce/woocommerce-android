package org.wordpress.android.fluxc.persistence.dao.pos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.pos.WCPosProductModel

@Dao
abstract class PosProductsDao {
    @Query("SELECT * FROM PosProductEntity WHERE localSiteId = :localSiteId")
    abstract fun observeAllProducts(localSiteId: LocalId): Flow<List<WCPosProductModel>>

    @Query("SELECT * FROM PosProductEntity WHERE localSiteId = :localSiteId AND remoteId = :remoteId")
    abstract suspend fun getProduct(localSiteId: LocalId, remoteId: RemoteId): WCPosProductModel?

    @Upsert
    abstract suspend fun upsertProducts(products: List<WCPosProductModel>)

    @Query("DELETE FROM PosProductEntity WHERE localSiteId = :localSiteId AND remoteId = :remoteId")
    abstract suspend fun deleteProduct(localSiteId: LocalId, remoteId: RemoteId)
}
