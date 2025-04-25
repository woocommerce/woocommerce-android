package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductVariationModel

@Dao
abstract class ProductVariationsDao {

    companion object {
        private const val DEFAULT_SELECT_QUERY = """
                SELECT * FROM ProductVariationEntity
                WHERE remoteProductId = :remoteProductId
                AND localSiteId = :localSiteId
                ORDER BY menuOrder ASC
        """
    }

    @Query(DEFAULT_SELECT_QUERY)
    abstract suspend fun getVariations(localSiteId: LocalId, remoteProductId: RemoteId): List<WCProductVariationModel>

    @Query(DEFAULT_SELECT_QUERY)
    abstract fun observeVariations(localSiteId: LocalId, remoteProductId: RemoteId): Flow<List<WCProductVariationModel>>

    @Query(
        """
        SELECT * FROM ProductVariationEntity
        WHERE remoteProductId = :remoteProductId
        AND remoteVariationId = :remoteVariationId
        AND localSiteId = :localSiteId
        """
    )
    abstract suspend fun getVariation(
        localSiteId: LocalId,
        remoteProductId: RemoteId,
        remoteVariationId: Long
    ): WCProductVariationModel?

    @Upsert
    abstract suspend fun upsertProductVariation(variation: WCProductVariationModel)

    @Upsert
    abstract suspend fun upsertProductVariations(variations: List<WCProductVariationModel>)

    @Query(
        """
        DELETE FROM ProductVariationEntity
        WHERE localSiteId = :localSiteId
        AND remoteProductId = :remoteProductId
        """
    )
    abstract suspend fun deleteVariationsForProduct(localSiteId: LocalId, remoteProductId: RemoteId)


}
