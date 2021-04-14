package com.woocommerce.android.ui.reviews

import android.os.Parcelable
import com.woocommerce.android.model.ActionRequest
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ActionStatus.PENDING
import com.woocommerce.android.model.ProductReview
import kotlinx.parcelize.Parcelize

@Parcelize
class ProductReviewModerationRequest(
    val productReview: ProductReview,
    val newStatus: ProductReviewStatus,
    private val requestStatus: ActionStatus = PENDING
) : ActionRequest(requestStatus), Parcelable
