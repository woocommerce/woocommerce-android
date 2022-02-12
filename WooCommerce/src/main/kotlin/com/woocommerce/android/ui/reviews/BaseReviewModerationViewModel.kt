package com.woocommerce.android.ui.reviews

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.QueuedLiveData
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
/* This class has (almost) all the logic required for ReviewModeration Interaction
 * between View(Fragment) and The ReviewModerationHandler
 * The number of function to be overridden was becoming too large
 * to contain in the RewviewlistViewModel. There are few functions
 * that needs to be handled by the derived classes.
 * This approach is actually quite flexible , if for any reason the
 * Base class cannot be extended then this can serve as a reference
 * implementation . Any viewmodel class can implement ReviewHandler.Processing
 * interface to execute the same functionality.
 */
abstract class BaseReviewModerationViewModel (
    savedState: SavedStateHandle,
    private val reviewModerationHandler : ReviewModerationHandler
    ):ScopedViewModel(savedState) ,ReviewModeration.Processing {

    private var _reviewModerationEvents = QueuedLiveData<ReviewModeration.Processing.ReviewModerationProcessingEvent>()
    var reviewModerationEvents : LiveData<ReviewModeration.Processing.ReviewModerationProcessingEvent> = _reviewModerationEvents

    init {
        collectModerationEvents()
    }


    override fun collectModerationEvents() {
        reviewModerationHandler.reviewModerationActionEvents.onEach{ event ->
            when(event){
                is ReviewModeration.Handler.ReviewModerationActionEvent.RemoveHiddenReviews -> relayRemoveHiddenReviews()
                is ReviewModeration.Handler.ReviewModerationActionEvent.ReloadReviews-> reloadReviews()
                is ReviewModeration.Handler.ReviewModerationActionEvent.RemoveProductReviewFromList -> relayRemoveProductReviewFromList(event.remoteReviewId)
                is ReviewModeration.Handler.ReviewModerationActionEvent.RevertHiddenReviews -> relayRevertHiddenReviews()
                is ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState -> relayResetPendingState()

            }
        }.launchIn(viewModelScope)
        reviewModerationHandler.reviewModerationUIEvents.onEach{ uiEvent ->
            when(uiEvent){
                is ReviewModeration.Handler.ReviewModerationUIEvent.ShowUndoUI -> relayUndoModerationEvent(uiEvent.productReviewModerationRequest)
                is ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh -> showRefresh(uiEvent.isRefreshing)
                is ReviewModeration.Handler.ReviewModerationUIEvent.ShowResponseError -> showReviewModeratiopnUpdateError()
                is ReviewModeration.Handler.ReviewModerationUIEvent.ShowOffLineError->showOfflineError()

            }
        }.launchIn(viewModelScope)
    }

    override fun submitReviewStatusChange(review: ProductReview, newStatus: ProductReviewStatus) {
        launch {
            reviewModerationHandler.submitReviewStatusChange(review,newStatus)
        }
        AnalyticsTracker.track(
            AnalyticsTracker.Stat.REVIEW_ACTION,
            mapOf(AnalyticsTracker.KEY_TYPE to newStatus.toString())
        )
    }



    override fun showReviewModeratiopnUpdateError() {
        triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.wc_moderate_review_error))
    }

    override fun getPendingModerationRequest(): ProductReviewModerationRequest? {
        return reviewModerationHandler.pendingModerationRequest
    }

    override fun getPendingModerationNewStatus():String? {
        reviewModerationHandler.pendingModerationRequest?.let{
            return it.newStatus.toString()
        }
        return null
    }

    override fun setPendingModerationRequest(request: ProductReviewModerationRequest?) {
        reviewModerationHandler.pendingModerationRequest = request
    }

    override fun relayUndoModerationEvent(productReviewModerationRequest: ProductReviewModerationRequest) {
        triggerModerationEvent(ReviewModeration.Processing.ReviewModerationProcessingEvent.SetUpModerationUndo(productReviewModerationRequest))

    }

    private fun triggerModerationEvent(event: ReviewModeration.Processing.ReviewModerationProcessingEvent) {
        _reviewModerationEvents.value = event
    }

    override fun relayRemoveHiddenReviews() {
        triggerModerationEvent(ReviewModeration.Processing.ReviewModerationProcessingEvent.RemoveHiddenReviews)
    }


    override fun relayRemoveProductReviewFromList(remoteReviewId: Long) {
        /*The event transmitted from the Handler when  Spam and Trash is selected reaches before the onResume
         * of the View . when this event is trigerred the logic is to hide the selected review .
         * Now the logic for reviewAdapter searches for the position of the review
         * in the list which will always be -1 as this is called before onResume
         * There are a few ways to fix this
         * A) Delay emitting event from ReviewModerationHandler, however this is not really handler's problem
         * B) Run as a post delayed from the view , again this is burdening the client which will
         * require every client to implement this way or some logic to queue this and implement in onResume
         * D)Expose a flow from ViewModel which can be used in onResume , again burdening the client
         * C) Keep this on this base class , where this critical logic will remain  with every extension
         * yes the time is still an unfortunate requirement and there can be debate about time but this
         * may not be as bad considering all possibilities. Currently setting this to ActivityPaused
         * timeout
         * #https://android.googlesource.com/platform/frameworks/base/+/f76a50c/services/java/com/android/server/am/ActivityStack.java#103
         */
        launch {
            delay(500)
            triggerModerationEvent(ReviewModeration.Processing.ReviewModerationProcessingEvent.RemoveProductReviewFromList(remoteReviewId))
        }
    }



    override fun relayRevertHiddenReviews() {
        triggerModerationEvent(ReviewModeration.Processing.ReviewModerationProcessingEvent.RevertHidenReviews)
    }

    override fun relayResetPendingState() {
        triggerModerationEvent(ReviewModeration.Processing.ReviewModerationProcessingEvent.ResetPendingModerationState)
    }

    override fun relayUndoReviewModeration() {
        launch {
            reviewModerationHandler.undoReviewModerationAndResetState()
        }

    }

}
