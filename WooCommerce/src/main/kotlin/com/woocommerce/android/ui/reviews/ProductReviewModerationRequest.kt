package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.DelayedUndoRequest
import com.woocommerce.android.model.ProductReview

class ProductReviewModerationRequest(
    val productReview: ProductReview,
    val newStatus: ProductReviewStatus,
    requestStatus: RequestStatus = RequestStatus.PENDING
) : DelayedUndoRequest(requestStatus)
