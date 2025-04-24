package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
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
    abstract suspend fun getVariations(localSiteId: Long, remoteProductId: Long): List<WCProductVariationModel>

    @Query(DEFAULT_SELECT_QUERY)
    abstract fun observeVariations(localSiteId: Long, remoteProductId: Long): Flow<List<WCProductVariationModel>>


}
