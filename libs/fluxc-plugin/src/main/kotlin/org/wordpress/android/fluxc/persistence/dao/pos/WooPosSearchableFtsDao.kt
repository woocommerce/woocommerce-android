package org.wordpress.android.fluxc.persistence.dao.pos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosSearchableFtsEntity

@Dao
abstract class WooPosSearchableFtsDao {

    @Insert
    abstract suspend fun insertAll(entities: List<WooPosSearchableFtsEntity>)

    @Query("DELETE FROM PosSearchableFts WHERE localSiteId = :localSiteId")
    abstract suspend fun deleteAllForSite(localSiteId: String)

    @Query(
        "DELETE FROM PosSearchableFts " +
            "WHERE localSiteId = :localSiteId " +
            "AND parentProductId = '' " +
            "AND itemId IN (:productIds)"
    )
    abstract suspend fun deleteProducts(localSiteId: String, productIds: List<String>)

    @Query(
        "DELETE FROM PosSearchableFts " +
            "WHERE localSiteId = :localSiteId " +
            "AND parentProductId != '' " +
            "AND itemId IN (:variationIds)"
    )
    abstract suspend fun deleteVariations(localSiteId: String, variationIds: List<String>)

    /**
     * Search products and variations with BM25 ranking.
     *
     * Column weights (lower = more important):
     * - localSiteId: 0 (not searchable, just for filtering)
     * - itemId: 0 (not searchable)
     * - parentProductId: 0 (not searchable)
     * - name: 1 (highest priority)
     * - sku: 2 (medium priority)
     * - barcode: 2 (medium priority)
     * - attributeValues: 3 (lower priority)
     */
    @Query(
        "SELECT * FROM PosSearchableFts " +
            "WHERE localSiteId = :localSiteId " +
            "AND PosSearchableFts MATCH :query " +
            "ORDER BY bm25(PosSearchableFts, 0, 0, 0, 1, 2, 2, 3) " +
            "LIMIT :limit OFFSET :offset"
    )
    abstract suspend fun search(
        localSiteId: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<WooPosSearchableFtsEntity>

    @Query(
        "SELECT COUNT(*) FROM PosSearchableFts " +
            "WHERE localSiteId = :localSiteId " +
            "AND PosSearchableFts MATCH :query"
    )
    abstract suspend fun searchCount(localSiteId: String, query: String): Int
}
