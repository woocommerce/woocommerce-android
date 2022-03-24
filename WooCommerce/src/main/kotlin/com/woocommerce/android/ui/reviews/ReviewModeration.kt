package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ActionStatus.PENDING
import com.woocommerce.android.model.ProductReview

data class ReviewModerationRequest(
    val review: ProductReview,
    val newStatus: ProductReviewStatus
)

data class ReviewModerationStatus(
    val review: ProductReview,
    val newStatus: ProductReviewStatus,
    val actionStatus: ActionStatus
) {
    constructor(request: ReviewModerationRequest, actionStatus: ActionStatus = PENDING) : this(
        request.review,
        request.newStatus,
        actionStatus
    )
}
