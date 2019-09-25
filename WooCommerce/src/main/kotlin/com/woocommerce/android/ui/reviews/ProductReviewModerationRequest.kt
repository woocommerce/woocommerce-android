package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ActionRequest
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview

class ProductReviewModerationRequest(
    val productReview: ProductReview,
    val newStatus: ProductReviewStatus,
    requestStatus: ActionStatus = ActionStatus.PENDING
) : ActionRequest(requestStatus)
