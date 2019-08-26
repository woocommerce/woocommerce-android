package com.woocommerce.android.model

import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductReviewModel.AvatarSize.SMALL
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

data class ProductReview(
    val remoteId: Long,
    val dateCreated: Date,
    val review: String,
    val rating: Int,
    val reviewerName: String,
    val reviewerAvatarUrl: String?,
    val remoteProductId: Long
)

fun WCProductReviewModel.toAppModel(): ProductReview {
    return ProductReview(
            this.remoteProductReviewId,
            DateTimeUtils.dateFromIso8601(this.dateCreated),
            this.review,
            this.rating,
            this.reviewerName,
            this.reviewerAvatarUrlBySize[SMALL],
            this.remoteProductId
    )
}
