package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ActionStatus.PENDING
import com.woocommerce.android.model.ProductReview

@Suppress("DataClassPrivateConstructor")
data class ReviewModerationRequest private constructor(
    val review: ProductReview,
    val newStatus: ProductReviewStatus,
    private val timeOfRequest: Long
) : Comparable<ReviewModerationRequest> {
    constructor(review: ProductReview, newStatus: ProductReviewStatus) : this(
        review,
        newStatus,
        System.currentTimeMillis()
    )

    override fun compareTo(other: ReviewModerationRequest): Int {
        return timeOfRequest.compareTo(other.timeOfRequest)
    }
}

data class ReviewModerationStatus(
    val request: ReviewModerationRequest,
    val actionStatus: ActionStatus = PENDING
) : Comparable<ReviewModerationStatus> {
    val review
        get() = request.review
    val newStatus
        get() = request.newStatus

    override fun compareTo(other: ReviewModerationStatus): Int {
        return request.compareTo(other.request)
    }
}
