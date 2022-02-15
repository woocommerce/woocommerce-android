package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ProductReview

interface ReviewModeration {
    interface Handler {
        suspend fun submitReviewStatusChange(
            review: ProductReview,
            newStatus: ProductReviewStatus
        )

        suspend fun launchProductReviewModerationRequestFlow(event: OnRequestModerateReviewEvent)
        suspend fun undoReviewModerationAndResetState()

        sealed class ReviewModerationActionEvent {
            object RemoveHiddenReviews : ReviewModerationActionEvent()
            object ReloadReviews : ReviewModerationActionEvent()
            data class RemoveProductReviewFromList(val remoteReviewId: Long) : ReviewModerationActionEvent()
            object RevertHiddenReviews : ReviewModerationActionEvent()
            object ResetPendingState : ReviewModerationActionEvent()
        }

        sealed class ReviewModerationUIEvent {
            data class ShowUndoUI(
                val productReviewModerationRequest: ProductReviewModerationRequest
            ) : ReviewModerationUIEvent()
            data class ShowRefresh(val isRefreshing: Boolean = false) : ReviewModerationUIEvent()
            object ShowResponseError : ReviewModerationUIEvent()
            object ShowOffLineError : ReviewModerationUIEvent()
        }
    }

    interface Relay {
        fun collectModerationEvents()
        fun relaytReviewStatusChange(review: ProductReview, newStatus: ProductReviewStatus)
        fun relayRefresh(isRefreshing: Boolean = false)
        fun relayReviewModerationUpdateError()
        fun relayShowOffLineError()
        fun getPendingModerationRequest(): ProductReviewModerationRequest?
        fun getPendingModerationNewStatus(): String?
        fun relayUndoModerationEvent(productReviewModerationRequest: ProductReviewModerationRequest)
        fun relayRemoveHiddenReviews()
        fun relayRemoveProductReviewFromList(remoteReviewId: Long)
        fun relayRevertHiddenReviews()
        fun relayUndoReviewModeration()
        fun relayResetPendingState()
        fun relayReloadReviews()

        sealed class ReviewModerationRelayEvent {
            data class RelaySetUpModerationUndo(
                val request: ProductReviewModerationRequest
            ) : ReviewModerationRelayEvent()
            data class RelayRemoveProductReviewFromList(val remoteReviewId: Long) : ReviewModerationRelayEvent()
            data class RelayToggleRefresh(val isRefreshing: Boolean) : ReviewModerationRelayEvent()
            object RelayResetPendingModerationState : ReviewModerationRelayEvent()
            object RelayRevertHiddenReviews : ReviewModerationRelayEvent()
            object RelayRemoveHiddenReviews : ReviewModerationRelayEvent()
            object RelayReloadReviews : ReviewModerationRelayEvent()
            data class RelayShowError(val resID: Int) : ReviewModerationRelayEvent()
        }
    }

    interface View {
        // host UI functions
        fun setUpModerationUndo(request: ProductReviewModerationRequest)
        fun removeProductReviewFromList(remoteReviewId: Long)
        fun revertHiddenReviews()
        fun removeHiddenReviews()
        fun resetPendingModerationState()
        fun setupReviewModerationObserver()
        fun showError(resID: Int)
        fun reloadReviews()
        fun toggleRefreshUI(isRefreshing: Boolean)
    }
}
