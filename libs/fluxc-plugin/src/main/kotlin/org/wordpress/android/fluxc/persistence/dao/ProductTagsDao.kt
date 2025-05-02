package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.WCProductTagModel

@Dao
internal abstract class ProductTagsDao {

    @Query(
        """
        SELECT * FROM ProductTagEntity
        WHERE localSiteId = :localSiteId
        """
    )
    abstract suspend fun getProductTags(localSiteId: Int): List<WCProductTagModel>

    @Query(
        """
        SELECT * FROM ProductTagEntity
        WHERE localSiteId = :localSiteId
        AND name IN (:tagsNames)
        """
    )
    abstract suspend fun getProductTags(localSiteId: Int, tagsNames: List<String>): List<WCProductTagModel>

    @Query(
        """
        SELECT * FROM ProductTagEntity
        WHERE localSiteId = :localSiteId
        AND name = :name
        """
    )
    abstract suspend fun getProductTag(localSiteId: Int, name: String): WCProductTagModel?

    @Query(
        """
        DELETE FROM ProductTagEntity
        WHERE localSiteId = :localSiteId
        """
    )
    abstract suspend fun deleteProductTagsForSite(localSiteId: Int): Int

    @Upsert
    abstract suspend fun upsertProductTag(tag: WCProductTagModel)

    @Upsert
    abstract suspend fun upsertProductTags(tags: List<WCProductTagModel>)
}
