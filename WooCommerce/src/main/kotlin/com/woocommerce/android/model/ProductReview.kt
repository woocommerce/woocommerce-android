package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductReviewModel.AvatarSize.SMALL
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

@Parcelize
data class ProductReview(
    val remoteId: Long,
    val dateCreated: Date,
    val review: String,
    val rating: Int,
    val reviewerName: String,
    val reviewerAvatarUrl: String?,
    val remoteProductId: Long,
    var status: String,
    var read: Boolean? = null, // Only has a value if it's been set using a matching Notification
    var product: ProductReviewProduct? = null
) : Parcelable

fun WCProductReviewModel.toAppModel(): ProductReview {
    return ProductReview(
            remoteId = this.remoteProductReviewId,
            dateCreated = DateTimeUtils.dateUTCFromIso8601(this.dateCreated),
            review = this.review,
            rating = this.rating,
            reviewerName = this.reviewerName,
            reviewerAvatarUrl = this.reviewerAvatarUrlBySize[SMALL],
            remoteProductId = this.remoteProductId,
            status = this.status
    )
}
