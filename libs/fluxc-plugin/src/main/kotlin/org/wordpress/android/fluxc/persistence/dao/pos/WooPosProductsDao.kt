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
    companion object {
        private const val PRODUCT_STATUS_PUBLISH = "publish"
        private const val PRODUCT_TYPE_SIMPLE = "simple"
        private const val PRODUCT_TYPE_VARIABLE = "variable"
        private const val DOWNLOADABLE_FALSE = 0
    }

    @Query(
        "SELECT * FROM PosProductEntity " +
            "WHERE localSiteId = :localSiteId " +
            "AND status = '$PRODUCT_STATUS_PUBLISH' " +
            "AND (type = '$PRODUCT_TYPE_SIMPLE' OR type = '$PRODUCT_TYPE_VARIABLE') " +
            "AND downloadable = '$DOWNLOADABLE_FALSE' " +
            "ORDER BY LOWER(name)"
    )
    abstract fun observeAllProducts(localSiteId: LocalId): Flow<List<WooPosProductEntity>>

    @Query("SELECT * FROM PosProductEntity WHERE localSiteId = :localSiteId AND remoteId = :remoteId")
    abstract suspend fun getProduct(localSiteId: LocalId, remoteId: RemoteId): WooPosProductEntity?

    @Upsert
    abstract suspend fun upsertProducts(products: List<WooPosProductEntity>)

    @Query("DELETE FROM PosProductEntity WHERE localSiteId = :localSiteId AND remoteId = :remoteId")
    abstract suspend fun deleteProduct(localSiteId: LocalId, remoteId: RemoteId)

    @Query("DELETE FROM PosProductEntity WHERE localSiteId = :localSiteId")
    abstract suspend fun deleteAllProductsForSite(localSiteId: LocalId)
}
