package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ProductReview
import kotlinx.coroutines.flow.SharedFlow

interface ReviewModeration {
    interface Handler {
        suspend fun submitReviewStatusChange(
            review: ProductReview ,
            newStatus: ProductReviewStatus)
        suspend fun launchProductReviewModerationRequestFlow(event: OnRequestModerateReviewEvent)
        suspend fun undoReviewModerationAndResetState()
        val reviewModerationActionEvents: SharedFlow<ReviewModerationActionEvent>
        val reviewModerationUIEvents: SharedFlow<ReviewModerationUIEvent>
        //var pendingModerationRemoteReviewId: Long?
        //var pendingModerationNewStatus: String?
        var pendingModerationRequest: ProductReviewModerationRequest?
        sealed class ReviewModerationActionEvent {
            object RemoveHiddenReviews:ReviewModerationActionEvent()
            object ReloadReviews: ReviewModerationActionEvent()
            data class RemoveProductReviewFromList(val remoteReviewId: Long):ReviewModerationActionEvent()
            object RevertHiddenReviews: ReviewModerationActionEvent()
            object ResetPendingState: ReviewModerationActionEvent()

        }
        sealed class ReviewModerationUIEvent {
            data class ShowUndoUI(
                val productReviewModerationRequest :ProductReviewModerationRequest ): ReviewModerationUIEvent()
            data class ShowRefresh(val isRefreshing:Boolean = false): ReviewModerationUIEvent()
            object ShowResponseError: ReviewModerationUIEvent()
            object ShowOffLineError:ReviewModerationUIEvent()
        }

    }
    interface Processing {
        fun collectModerationEvents()
        fun submitReviewStatusChange(review: ProductReview, newStatus: ProductReviewStatus)
        fun showRefresh(isRefreshing:Boolean = false)
        fun showReviewModeratiopnUpdateError()
        fun getPendingModerationRequest(): ProductReviewModerationRequest?
        fun getPendingModerationNewStatus(): String?
        fun setPendingModerationRequest(request: ProductReviewModerationRequest?)
        fun reloadReviews()
        fun relayUndoModerationEvent(productReviewModerationRequest: ProductReviewModerationRequest)
        fun relayRemoveHiddenReviews()
        fun relayRemoveProductReviewFromList(remoteReviewId: Long)
        fun showOfflineError()
        fun relayRevertHiddenReviews()
        fun relayUndoReviewModeration()
        fun relayResetPendingState()

        sealed class ReviewModerationProcessingEvent {
            data class SetUpModerationUndo(val request: ProductReviewModerationRequest) : ReviewModerationProcessingEvent()
            data class RemoveProductReviewFromList(val remoteReviewId: Long ) :ReviewModerationProcessingEvent()
            object ResetPendingModerationState: ReviewModerationProcessingEvent()
            object RevertHidenReviews :ReviewModerationProcessingEvent()
            object RemoveHiddenReviews: ReviewModerationProcessingEvent()
        }
    }


    interface View {
        //host the UI for Snakcbar
        fun setUpModerationUndo(request: ProductReviewModerationRequest)
        fun removeProductReviewFromList(remoteReviewId: Long)
        fun revertHiddenReviews()
        fun removeHiddenReviews()
        fun resetPendingModerationState()
        fun setupReviewModerationObserver()

    }
}
