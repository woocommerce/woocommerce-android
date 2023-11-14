package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ProductReview
import org.wordpress.android.fluxc.model.notification.NotificationModel
import java.util.Date

object ProductReviewTestUtils {
    fun generateProductReview(
        id: Long,
        productId: Long,
        isRead: Boolean = false
    ): ProductReview {
        return ProductReview(
            remoteId = id,
            dateCreated = Date(),
            review = "This is a test review",
            rating = 3,
            reviewerName = "Droid Test",
            reviewerAvatarUrl = null,
            remoteProductId = productId,
            status = ProductReviewStatus.toString(),
            read = isRead
        )
    }

    fun generateProductReviewList(): List<ProductReview> {
        with(mutableListOf<ProductReview>()) {
            add(generateProductReview(1L, 100L))
            add(generateProductReview(2L, 100L, true))
            add(generateProductReview(3L, 100L))
            add(generateProductReview(4L, 101L, true))
            add(generateProductReview(5L, 101L, true))
            add(generateProductReview(6L, 101L))
            add(generateProductReview(7L, 102L))
            add(generateProductReview(8L, 103L))
            add(generateProductReview(8L, 103L))

            return this
        }
    }

    fun generateReviewNotification(remoteNoteId: Long) = NotificationModel(remoteNoteId = remoteNoteId)
}
