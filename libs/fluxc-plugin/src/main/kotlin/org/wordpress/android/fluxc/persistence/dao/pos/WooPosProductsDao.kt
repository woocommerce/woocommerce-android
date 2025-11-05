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

    @Query("SELECT COUNT(*) FROM PosProductEntity WHERE localSiteId = :localSiteId")
    abstract suspend fun getProductCount(localSiteId: LocalId): Int

    @Upsert
    abstract suspend fun upsertProducts(products: List<WooPosProductEntity>)

    @Query("DELETE FROM PosProductEntity WHERE localSiteId = :localSiteId AND remoteId = :remoteId")
    abstract suspend fun deleteProduct(localSiteId: LocalId, remoteId: RemoteId)

    @Query("DELETE FROM PosProductEntity WHERE localSiteId = :localSiteId")
    abstract suspend fun deleteAllProductsForSite(localSiteId: LocalId)

    @Query(
        "SELECT * FROM PosProductEntity " +
            "WHERE localSiteId = :localSiteId " +
            "AND globalUniqueId = :identifier COLLATE NOCASE " +
            "LIMIT 1"
    )
    abstract suspend fun findProductByIdentifier(localSiteId: LocalId, identifier: String): WooPosProductEntity?

    @Query(
        "SELECT * FROM PosProductEntity " +
            "WHERE localSiteId = :localSiteId " +
            "AND status = '$PRODUCT_STATUS_PUBLISH' " +
            "AND (type = '$PRODUCT_TYPE_SIMPLE' OR type = '$PRODUCT_TYPE_VARIABLE') " +
            "AND downloadable = '$DOWNLOADABLE_FALSE' " +
            "AND (name LIKE '%' || :searchQuery || '%' " +
            "OR sku LIKE '%' || :searchQuery || '%' " +
            "OR globalUniqueId LIKE '%' || :searchQuery || '%') " +
            "ORDER BY LOWER(name) " +
            "LIMIT :limit OFFSET :offset"
    )
    abstract suspend fun searchProducts(
        localSiteId: LocalId,
        searchQuery: String,
        limit: Int,
        offset: Int
    ): List<WooPosProductEntity>
}
