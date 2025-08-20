package org.wordpress.android.fluxc.persistence.dao.pos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.pos.WCPosVariationModel

@Dao
abstract class PosVariationsDao {

    @Query("SELECT * FROM PosVariationEntity WHERE localSiteId = :localSiteId AND remoteProductId = :productId ORDER BY variationName ASC")
    abstract fun observeVariationsForProduct(localSiteId: LocalId, productId: RemoteId): Flow<List<WCPosVariationModel>>

    @Query("SELECT * FROM PosVariationEntity WHERE localSiteId = :localSiteId AND remoteProductId = :productId AND remoteVariationId = :variationId")
    abstract suspend fun getVariation(localSiteId: LocalId, productId: RemoteId, variationId: RemoteId): WCPosVariationModel?

    @Query("SELECT * FROM PosVariationEntity WHERE localSiteId = :localSiteId AND sku = :sku LIMIT 1")
    abstract suspend fun getVariationBySku(localSiteId: LocalId, sku: String): WCPosVariationModel?

    @Upsert
    abstract suspend fun upsertVariations(variations: List<WCPosVariationModel>)

    @Upsert
    abstract suspend fun upsertVariation(variation: WCPosVariationModel)

    @Query("DELETE FROM PosVariationEntity WHERE localSiteId = :localSiteId AND remoteProductId = :productId AND remoteVariationId = :variationId")
    abstract suspend fun deleteVariation(localSiteId: LocalId, productId: RemoteId, variationId: RemoteId)

    @Query("DELETE FROM PosVariationEntity WHERE localSiteId = :localSiteId AND remoteProductId = :productId")
    abstract suspend fun deleteVariationsForProduct(localSiteId: LocalId, productId: RemoteId)

    @Query("DELETE FROM PosVariationEntity WHERE localSiteId = :localSiteId")
    abstract suspend fun deleteAllVariationsForSite(localSiteId: LocalId)
}
