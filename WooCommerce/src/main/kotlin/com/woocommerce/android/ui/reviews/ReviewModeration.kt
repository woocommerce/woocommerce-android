package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.flow.SharedFlow
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductReviewStatusPayload

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
            object ResetModeration: ReviewModerationActionEvent()

        }
        sealed class ReviewModerationUIEvent {
            data class ShowUndoUI(
                val productReviewModerationRequest :ProductReviewModerationRequest ): ReviewModerationUIEvent()
            data class ShowRefresh(val isRefreshing:Boolean = false): ReviewModerationUIEvent()
            object ShowResponseError: ReviewModerationUIEvent()
            object ShowOffLineError:ReviewModerationUIEvent()
        }

        suspend fun handleOffLineError()
        //fun resetPendingModerationVariables()
    }
    interface Processing {
        fun observeModerationEvents()
        fun submitReviewStatusChange(review: ProductReview, newStatus: ProductReviewStatus)
        fun showRefresh(isRefreshing:Boolean = false)
        fun showReviewModeratiopnUpdateError()
        fun getPendingModerationRequest(): ProductReviewModerationRequest?
        fun getPendingModerationNewStatus(): String?
        fun setPendingModerationRequest(request: ProductReviewModerationRequest?)
        fun reloadReviews()
        fun relayUndoModerationEvent(productReviewModerationRequest: ProductReviewModerationRequest)
        fun relayRemoveHiddenReviews()
        fun relayRemovePendingModerationState()
        fun relayRemoveProductReviewFromList(remoteReviewId: Long)
        fun showOfflineError()
        fun relayResetModeration()
        fun relayUndoReviewModeration()

        sealed class ReviewModerationProcessingEvent : MultiLiveEvent.Event() {
            data class SetUpModerationUndo(val request: ProductReviewModerationRequest) : ReviewModerationProcessingEvent()
            data class RemoveProductReviewFromList(val remoteReviewId: Long ) :ReviewModerationProcessingEvent()
            object UndoReviewModeration :ReviewModerationProcessingEvent()
            object RemoveHiddenReviews: ReviewModerationProcessingEvent()
        }
    }


    interface View {
        //host the UI for Snakcbar
        fun setUpModerationUndo(request: ProductReviewModerationRequest)
        fun removeProductReviewFromList(remoteReviewId: Long)
        fun undoReviewModeration()
        fun removeHiddenReviews()
        fun resetPendingModerationState()

    }
}
