package org.wordpress.android.fluxc.persistence.dao.pos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity

@Dao
abstract class WooPosVariationsDao {
    companion object {
        private const val VARIATION_STATUS_PUBLISH = "publish"
        private const val DOWNLOADABLE_FALSE = 0
        private const val VARIATION_TYPE = "variation"
    }

    @Query(
        "SELECT * FROM PosVariationEntity " +
            "WHERE localSiteId = :localSiteId " +
            "AND remoteProductId = :productId " +
            "AND status = '$VARIATION_STATUS_PUBLISH' " +
            "AND downloadable = '$DOWNLOADABLE_FALSE' " +
            "AND type = '$VARIATION_TYPE' " +
            "ORDER BY variationName ASC"
    )
    abstract fun observeVariationsForProduct(
        localSiteId: Int,
        productId: Long
    ): Flow<List<WooPosVariationEntity>>

    @Query(
        "SELECT * FROM PosVariationEntity " +
            "WHERE localSiteId = :localSiteId " +
            "AND remoteProductId = :productId " +
            "AND remoteVariationId = :variationId " +
            "AND type = '$VARIATION_TYPE'"
    )
    abstract suspend fun getVariation(
        localSiteId: Int,
        productId: Long,
        variationId: Long
    ): WooPosVariationEntity?

    @Query("SELECT COUNT(*) FROM PosVariationEntity WHERE localSiteId = :localSiteId")
    abstract suspend fun getVariationCount(localSiteId: LocalId): Int

    @Query("SELECT * FROM PosVariationEntity WHERE localSiteId = :localSiteId")
    abstract suspend fun getAllVariations(localSiteId: LocalId): List<WooPosVariationEntity>

    @Query(
        "SELECT * FROM PosVariationEntity " +
            "WHERE localSiteId = :localSiteId " +
            "AND remoteVariationId IN (:variationIds)"
    )
    abstract suspend fun getVariationsByIds(
        localSiteId: LocalId,
        variationIds: List<RemoteId>
    ): List<WooPosVariationEntity>

    @Upsert
    abstract suspend fun upsertVariations(variations: List<WooPosVariationEntity>)

    @Upsert
    abstract suspend fun upsertVariation(variation: WooPosVariationEntity)

    @Query("DELETE FROM PosVariationEntity WHERE localSiteId = :localSiteId AND remoteVariationId IN (:variationIds)")
    abstract suspend fun deleteVariationsByIds(localSiteId: LocalId, variationIds: List<RemoteId>)

    @Query("DELETE FROM PosVariationEntity WHERE localSiteId = :localSiteId AND remoteProductId IN (:productIds)")
    abstract suspend fun deleteVariationsForProducts(localSiteId: LocalId, productIds: List<RemoteId>)

    @Query("DELETE FROM PosVariationEntity WHERE localSiteId = :localSiteId")
    abstract suspend fun deleteAllVariationsForSite(localSiteId: LocalId)

    @Query(
        "SELECT * FROM PosVariationEntity " +
            "WHERE localSiteId = :localSiteId " +
            "AND globalUniqueId = :identifier COLLATE NOCASE " +
            "AND type = '$VARIATION_TYPE' " +
            "LIMIT 1"
    )
    abstract suspend fun findVariationByIdentifier(localSiteId: LocalId, identifier: String): WooPosVariationEntity?
}
