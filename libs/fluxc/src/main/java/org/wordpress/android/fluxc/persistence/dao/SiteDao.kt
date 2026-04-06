package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.SiteEntity

@Dao
@Suppress("TooManyFunctions")
abstract class SiteDao {
    // region Read operations

    @Query("SELECT * FROM SiteEntity")
    abstract suspend fun getAllSites(): List<SiteEntity>

    @Query(
        """
        SELECT * FROM SiteEntity
        WHERE hasWooCommerce = 1
        """
    )
    abstract suspend fun getWooSites(): List<SiteEntity>

    @Query(
        """
        SELECT * FROM SiteEntity
        WHERE id = :id
        """
    )
    abstract suspend fun getByLocalId(id: Int): SiteEntity?

    @Query(
        """
        SELECT * FROM SiteEntity
        WHERE siteId = :siteId
        """
    )
    abstract suspend fun getByRemoteId(siteId: Long): List<SiteEntity>

    @Query(
        """
        SELECT * FROM SiteEntity
        WHERE siteId = :siteId
        AND url = :url
        """
    )
    abstract suspend fun getBySiteIdAndUrl(siteId: Long, url: String): List<SiteEntity>

    @Query(
        """
        SELECT * FROM SiteEntity
        WHERE xmlRpcUrl = :xmlRpcUrlHttp
        OR xmlRpcUrl = :xmlRpcUrlHttps
        """
    )
    abstract suspend fun getByXmlRpcUrl(xmlRpcUrlHttp: String, xmlRpcUrlHttps: String): List<SiteEntity>

    @Query(
        """
        SELECT * FROM SiteEntity
        WHERE origin = :origin
        """
    )
    abstract suspend fun getByOrigin(origin: Int): List<SiteEntity>

    @Query(
        """
        SELECT * FROM SiteEntity
        WHERE url LIKE '%' || :searchString || '%'
        OR name LIKE '%' || :searchString || '%'
        """
    )
    abstract suspend fun getByNameOrUrlMatching(searchString: String): List<SiteEntity>

    @Query(
        """
        SELECT * FROM SiteEntity
        WHERE origin = :origin
        AND (
            url LIKE '%' || :searchString || '%'
            OR name LIKE '%' || :searchString || '%'
        )
        """
    )
    abstract suspend fun getByOriginAndNameOrUrlMatching(origin: Int, searchString: String): List<SiteEntity>

    // endregion

    // region Write operations

    @Insert
    abstract suspend fun insert(entity: SiteEntity): Long

    @Update
    abstract suspend fun update(entity: SiteEntity)

    @Query(
        """
        DELETE FROM SiteEntity
        WHERE id = :id
        """
    )
    abstract suspend fun deleteByLocalId(id: Int)

    @Query("DELETE FROM SiteEntity")
    abstract suspend fun deleteAll()

    // endregion

    // region Compound operations

    /**
     * Removes all sites with the given [origin] whose remote site ID is not
     * present in [siteIds]. For example, passing [SiteModel.ORIGIN_WPCOM_REST]
     * removes WP.com/Jetpack sites that are no longer returned by the server.
     *
     * @param origin the origin value to filter by (e.g., [SiteModel.ORIGIN_WPCOM_REST])
     * @param siteIds the remote site IDs to keep
     */
    @Transaction
    open suspend fun deleteByOriginNotInList(origin: Int, siteIds: List<Long>) {
        val localSites = getByOrigin(origin)
        val toDelete = localSites.filter { it.siteId !in siteIds }
        for (site in toDelete) {
            deleteByLocalId(site.id)
        }
    }

    // endregion
}
