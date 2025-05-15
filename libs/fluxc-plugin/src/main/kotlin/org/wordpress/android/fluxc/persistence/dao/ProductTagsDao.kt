package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCProductTagModel

@Dao
internal abstract class ProductTagsDao {

    @Query(
        """
        SELECT * FROM ProductTagEntity
        WHERE localSiteId = :siteId
        """
    )
    abstract suspend fun getProductTags(siteId: LocalId): List<WCProductTagModel>

    @Query(
        """
        SELECT * FROM ProductTagEntity
        WHERE localSiteId = :siteId
        AND name IN (:tagsNames)
        """
    )
    abstract suspend fun getProductTags(siteId: LocalId, tagsNames: List<String>): List<WCProductTagModel>

    @Query(
        """
        SELECT * FROM ProductTagEntity
        WHERE localSiteId = :siteId
        AND name = :name
        """
    )
    abstract suspend fun getProductTag(siteId: LocalId, name: String): WCProductTagModel?

    @Query(
        """
        DELETE FROM ProductTagEntity
        WHERE localSiteId = :siteId
        """
    )
    abstract suspend fun deleteProductTagsForSite(siteId: LocalId): Int

    @Upsert
    abstract suspend fun upsertProductTag(tag: WCProductTagModel)

    @Upsert
    abstract suspend fun upsertProductTags(tags: List<WCProductTagModel>)
}
