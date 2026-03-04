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

    @Query(
        "DELETE FROM PosSearchableFts " +
            "WHERE localSiteId = :localSiteId " +
            "AND parentProductId IN (:parentProductIds)"
    )
    abstract suspend fun deleteVariationsByParentProductIds(
        localSiteId: String,
        parentProductIds: List<String>
    )

    /**
     * Search products and variations with simple ranking.
     *
     * Ranking:
     * 1. Products appear before variations (parentProductId = '' first)
     * 2. Shorter names appear first (more specific matches)
     *
     * This ranking is fast because it only uses values already in each row,
     * avoiding expensive LIKE scans that would defeat FTS optimization.
     *
     * @param query FTS query with wildcards (e.g., "shirt*" or "blue* shirt*")
     */
    @Query(
        "SELECT * FROM PosSearchableFts " +
            "WHERE localSiteId = :localSiteId " +
            "AND PosSearchableFts MATCH :query " +
            "ORDER BY " +
            "CASE WHEN parentProductId = '' THEN 0 ELSE 1 END, " +
            "LENGTH(name) " +
            "LIMIT :limit OFFSET :offset"
    )
    abstract suspend fun search(
        localSiteId: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<WooPosSearchableFtsEntity>

    @Query("SELECT COUNT(*) FROM PosSearchableFts WHERE localSiteId = :localSiteId")
    abstract suspend fun countAllForSite(localSiteId: String): Int
}
