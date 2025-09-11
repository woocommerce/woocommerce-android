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

    @Query("SELECT * FROM PosVariationEntity WHERE localSiteId = :localSiteId AND remoteProductId = :productId ORDER BY variationName ASC")
    abstract fun observeVariationsForProduct(localSiteId: LocalId, productId: RemoteId): Flow<List<WooPosVariationEntity>>

    @Query("SELECT * FROM PosVariationEntity WHERE localSiteId = :localSiteId AND remoteProductId = :productId AND remoteVariationId = :variationId")
    abstract suspend fun getVariation(
        localSiteId: LocalId,
        productId: RemoteId,
        variationId: RemoteId
    ): WooPosVariationEntity?

    @Upsert
    abstract suspend fun upsertVariations(variations: List<WooPosVariationEntity>)

    @Upsert
    abstract suspend fun upsertVariation(variation: WooPosVariationEntity)

    @Query("DELETE FROM PosVariationEntity WHERE localSiteId = :localSiteId AND remoteProductId = :productId AND remoteVariationId = :variationId")
    abstract suspend fun deleteVariation(localSiteId: LocalId, productId: RemoteId, variationId: RemoteId)

    @Query("DELETE FROM PosVariationEntity WHERE localSiteId = :localSiteId AND remoteProductId = :productId")
    abstract suspend fun deleteVariationsForProduct(localSiteId: LocalId, productId: RemoteId)

    @Query("DELETE FROM PosVariationEntity WHERE localSiteId = :localSiteId")
    abstract suspend fun deleteAllVariationsForSite(localSiteId: LocalId)
}
