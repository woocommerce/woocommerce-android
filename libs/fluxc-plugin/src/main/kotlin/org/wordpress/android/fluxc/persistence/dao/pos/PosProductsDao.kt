package org.wordpress.android.fluxc.persistence.dao.pos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.persistence.entity.pos.WCPosProductModel

@Dao
abstract class PosProductsDao {
    @Query("SELECT * FROM PosProductEntity WHERE localSiteId = :localSiteId")
    abstract fun observeAllProducts(localSiteId: Int): Flow<List<WCPosProductModel>>

    @Query("SELECT * FROM PosProductEntity WHERE localSiteId = :localSiteId AND remoteId = :remoteId")
    abstract suspend fun getProduct(localSiteId: Int, remoteId: Long): WCPosProductModel?

    @Upsert
    abstract suspend fun upsertProducts(products: List<WCPosProductModel>)

    @Query("DELETE FROM PosProductEntity WHERE localSiteId = :localSiteId AND remoteId = :remoteId")
    abstract suspend fun deleteProduct(localSiteId: Int, remoteId: Long)
}
