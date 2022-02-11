package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductReviewStatusPayload

interface ReviewModeration {
    interface Handler {
        suspend fun submitReviewStatusChange(payload: UpdateProductReviewStatusPayload)
        suspend fun launchProductReviewModerationRequestFlow(event: OnRequestModerateReviewEvent)
        val reviewModerationActionEvents: StateFlow<ReviewModerationActionEvent>
        val reviewModerationUIEvents: StateFlow<ReviewModerationUIEvent>
        var pendingModerationRemoteReviewId: Long?
        var pendingModerationNewStatus: String?
        var pendingModerationRequest: ProductReviewModerationRequest?
        sealed class ReviewModerationActionEvent {
            object Idle: ReviewModerationActionEvent()
            object RemoveHiddenReviews:ReviewModerationActionEvent()
            object RevertPendingModerationState:ReviewModerationActionEvent()
            object ReloadReviews: ReviewModerationActionEvent()
            object ExecuteReviewModeration:ReviewModerationActionEvent()
            data class RemoveProductReviewFromList(val remoteReviewId: Long):ReviewModerationActionEvent()

        }
        sealed class ReviewModerationUIEvent {
            data class ShowUndoUI(
                val productReviewModerationRequest :ProductReviewModerationRequest ): ReviewModerationUIEvent()
            data class showRefresh(val isRefreshing:Boolean = false): ReviewModerationUIEvent()
            object ShowResponseError: ReviewModerationUIEvent()
            object Idle: ReviewModerationUIEvent()
        }

        suspend fun handleOffLineError()
        fun resetPendingModerationVariables()
    }
    interface Processing {
        suspend fun observeModerationEvents()
        fun submitReviewStatusChange(review: ProductReview, newStatus: ProductReviewStatus)
        //fun sendReviewModerationUpdate(newRequestStatus: ActionStatus)
        fun resetPendingModerationVariables()
        fun showRefresh(isRefreshing:Boolean = false)
        fun showError()
        fun getPendingModerationRequest(): ProductReviewModerationRequest?
        fun getPendingModerationNewStatus(): String?
        fun setPendingModerationRequest(request: ProductReviewModerationRequest?)
        fun reloadReviews()
        fun relayUndoModerationEvent(productReviewModerationRequest: ProductReviewModerationRequest)
        fun relayRemoveHiddenReviews()
        fun relayRemovePendingModerationState()
        fun relayRemoveProductReviewFromList(remoteReviewId: Long)

        sealed class ReviewModerationEvent : MultiLiveEvent.Event() {
            data class SetUpModerationUndo(val request: ProductReviewModerationRequest) : ReviewModerationEvent()
            data class RemoveProductReviewFromList(val remoteReviewId: Long ) :ReviewModerationEvent()
            object RemoveHiddenReviews: ReviewModerationEvent()
        }
    }


    interface View {
        //host the UI for Snakcbar
        fun setUpModerationUndo(request: ProductReviewModerationRequest)
        fun removeProductReviewFromList(remoteReviewId: Long)
        fun revertPendingModerationState(newStatus: ProductReviewStatus)
        fun removeHiddenreviews()
        fun resetPendingModerationVariables()

    }
}
