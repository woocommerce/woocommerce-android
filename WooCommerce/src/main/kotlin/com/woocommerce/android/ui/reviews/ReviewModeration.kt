package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ProductReview

/* This is the ReviewModeration interface
 * It consists of 3 parts implemented by 3 different classes
 *
 * The first is the ReviewModerationHandler
 * This class contains all the business logic for Review Moderation
 * It is responsible for
 * a) Receiving the moderation event from DetailReviewFragment
 * b) Updating the ProductStore with review status and share the result
 * c) Decide what needs to be done to the UI at various stage of moderation
 * e) It primarily publishes events via shared flow
 *
 * @see com.woocommerce.android.ui.reviews.ReviewModerationHandler
 *
 * The next is the class implementing relay interface
 * A base class implementation is done as ViewModel
 * This class receives events from the Handler via a collector
 * and "relays" them to the View as Live Data events .
 *
 * @see com.woocommerce.android.ui.reviews.BaseReviewModerationViewModel
 *
 * Finally  we come to the View
 * This class implements all the UI related functionality for showing the intermediate  moderation UI
 * (like showing undo snackbar and refresh loader) and updating
 * the Reviews list as per the events received from handler via the Relay Class
 *
 * @see com.woocommerce.android.ui.reviews.ReviewListViewModel
 *
 */
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
