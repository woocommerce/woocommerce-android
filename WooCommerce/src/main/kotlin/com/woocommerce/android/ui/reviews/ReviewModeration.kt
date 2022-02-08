package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.fluxc.store.WCProductStore

interface ReviewModeration {
    interface Handler {
        suspend fun submitReviewStatusChange(request: ProductReviewModerationRequest)
            : WCProductStore.OnProductReviewChanged
        suspend fun postProductReviewModerationRequest(event: OnRequestModerateReviewEvent)
        fun resetPendingModerationVariables()
        val reviewRequest: StateFlow<ProductReviewModerationRequest?>
        var pendingModerationRemoteReviewId: Long?
        var pendingModerationNewStatus: String?
        var pendingModerationRequest: ProductReviewModerationRequest?
    }
    interface Processing {
        suspend fun observeModerationRequest()
        fun submitReviewStatusChange(review: ProductReview, newStatus: ProductReviewStatus)
        fun sendReviewModerationUpdate(newRequestStatus: ActionStatus)
        fun resetPendingModerationVariables()
        fun getPendingModerationRequest(): ProductReviewModerationRequest?
        fun getPendingModerationNewStatus(): String?
        fun setPendingModerationRequest(request: ProductReviewModerationRequest?)
    }

    interface View {
        //host the UI for Snakcbar
        fun setUpModerationUndo(request: ProductReviewModerationRequest)
    }
}
