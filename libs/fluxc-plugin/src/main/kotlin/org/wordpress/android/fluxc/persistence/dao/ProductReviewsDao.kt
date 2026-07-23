package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductReviewModel

@Dao
internal abstract class ProductReviewsDao {

    @Upsert
    abstract suspend fun upsertProductReview(productReview: WCProductReviewModel)

    @Upsert
    abstract suspend fun upsertProductReviews(productReviews: List<WCProductReviewModel>)

    @Delete
    abstract suspend fun deleteProductReview(productReview: WCProductReviewModel)

    @Query(
        """
            SELECT * FROM ProductReviewEntity
            WHERE localSiteId = :siteId
            AND remoteProductReviewId = :id
        """
    )
    abstract suspend fun getProductReview(siteId: LocalId, id: RemoteId): WCProductReviewModel?

    @Query(
        """
            SELECT * FROM ProductReviewEntity
            WHERE localSiteId = :siteId
            ORDER BY dateCreated DESC
        """
    )
    abstract suspend fun getProductReviews(siteId: LocalId): List<WCProductReviewModel>

    @Query(
        """
            SELECT * FROM ProductReviewEntity
            WHERE localSiteId = :siteId
            AND remoteProductReviewId IN (:ids)
            ORDER BY dateCreated DESC
        """
    )
    abstract suspend fun getProductReviews(siteId: LocalId, ids: List<RemoteId>): List<WCProductReviewModel>

    @Query(
        """
            SELECT * FROM ProductReviewEntity
            WHERE localSiteId = :siteId
            AND remoteProductId = :productId
            ORDER BY dateCreated DESC
        """
    )
    abstract suspend fun getProductReviews(siteId: LocalId, productId: RemoteId): List<WCProductReviewModel>

    @Query(
        """
            DELETE FROM ProductReviewEntity
            WHERE localSiteId = :siteId
        """
    )
    abstract suspend fun deleteProductReviewsForSite(siteId: LocalId): Int
}
