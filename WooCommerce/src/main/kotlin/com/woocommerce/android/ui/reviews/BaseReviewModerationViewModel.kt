package com.woocommerce.android.ui.reviews

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.viewmodel.QueuedLiveData
import com.woocommerce.android.viewmodel.ScopedViewModel
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
 * implementation . Any viewmodel class can implement ReviewHandler.Relay
 * interface to execute the same functionality.
 */
abstract class BaseReviewModerationViewModel (
    savedState: SavedStateHandle,
    private val reviewModerationHandler : ReviewModerationHandler
    ):ScopedViewModel(savedState) ,ReviewModeration.Relay {

    private var _reviewModerationEvents = QueuedLiveData<ReviewModeration.Relay.ReviewModerationRelayEvent>()
    var reviewModerationEvents : LiveData<ReviewModeration.Relay.ReviewModerationRelayEvent> = _reviewModerationEvents



    override fun collectModerationEvents() {
        reviewModerationHandler.getReviewModerationActionEventFlow().onEach{ event ->
            when(event){
                is ReviewModeration.Handler.ReviewModerationActionEvent.RemoveHiddenReviews -> relayRemoveHiddenReviews()
                is ReviewModeration.Handler.ReviewModerationActionEvent.ReloadReviews-> relayReloadReviews()
                is ReviewModeration.Handler.ReviewModerationActionEvent.RemoveProductReviewFromList -> relayRemoveProductReviewFromList(event.remoteReviewId)
                is ReviewModeration.Handler.ReviewModerationActionEvent.RevertHiddenReviews -> relayRevertHiddenReviews()
                is ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState -> relayResetPendingState()

            }
        }.launchIn(viewModelScope)
        reviewModerationHandler.getReviewModerationUiEventFlow().onEach{ uiEvent ->
            when(uiEvent){
                is ReviewModeration.Handler.ReviewModerationUIEvent.ShowUndoUI -> relayUndoModerationEvent(uiEvent.productReviewModerationRequest)
                is ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh -> relayRefresh(uiEvent.isRefreshing)
                is ReviewModeration.Handler.ReviewModerationUIEvent.ShowResponseError -> relayReviewModerationUpdateError()
                is ReviewModeration.Handler.ReviewModerationUIEvent.ShowOffLineError->relayShowOffLineError()

            }
        }.launchIn(viewModelScope)
    }

    override fun relaytReviewStatusChange(review: ProductReview, newStatus: ProductReviewStatus) {
        launch {
            reviewModerationHandler.submitReviewStatusChange(review,newStatus)
        }
        AnalyticsTracker.track(
            AnalyticsTracker.Stat.REVIEW_ACTION,
            mapOf(AnalyticsTracker.KEY_TYPE to newStatus.toString())
        )
    }



    override fun getPendingModerationRequest(): ProductReviewModerationRequest? {
        return reviewModerationHandler.getPendingReviewModerationRequest()
    }

    override fun getPendingModerationNewStatus():String? {
        reviewModerationHandler.getPendingReviewModerationRequest()?.let{
            return it.newStatus.toString()
        }
        return null
    }

    override fun setPendingModerationRequest(request: ProductReviewModerationRequest?) {
        //reviewModerationHandler.pendingModerationRequest = request
        TODO()
    }

    override fun relayUndoModerationEvent(productReviewModerationRequest: ProductReviewModerationRequest) {
        triggerModerationEvent(ReviewModeration.Relay.ReviewModerationRelayEvent.SetUpModerationUndo(productReviewModerationRequest))

    }
    /* Using QueuedLiveData all events are reaching the observer , however sometimes it is before onResume
     * which can lead to undesired consequence  for example when removing a certain review from a list it
     * will check for its position and will be -1 if it does before resume
     * this launches a new coroutine every time and ensures
     * that it always reaches at the correct moment and thus eliminating the need to add delay
     */
    private fun triggerModerationEvent(event: ReviewModeration.Relay.ReviewModerationRelayEvent) {
        launch{
            _reviewModerationEvents.value = event
        }

    }

    override fun relayRemoveHiddenReviews() {
        triggerModerationEvent(ReviewModeration.Relay.ReviewModerationRelayEvent.RelayRemoveHiddenReviews)
    }


    override fun relayRemoveProductReviewFromList(remoteReviewId: Long) {
        triggerModerationEvent(ReviewModeration.Relay.ReviewModerationRelayEvent.RemoveProductReviewFromList(remoteReviewId))
    }



    override fun relayRevertHiddenReviews() {
        triggerModerationEvent(ReviewModeration.Relay.ReviewModerationRelayEvent.RelayRevertHidenReviews)
    }

    override fun relayResetPendingState() {
        triggerModerationEvent(ReviewModeration.Relay.ReviewModerationRelayEvent.RelayResetPendingModerationState)
    }

    override fun relayReloadReviews() {
        triggerModerationEvent(ReviewModeration.Relay.ReviewModerationRelayEvent.RelayReloadReviews)
    }

    override fun relayShowOffLineError() {
        triggerModerationEvent(ReviewModeration.Relay.ReviewModerationRelayEvent.RelayShowError(R.string.offline_error))
    }

    override fun relayReviewModerationUpdateError() {
        triggerModerationEvent(ReviewModeration.Relay.ReviewModerationRelayEvent.RelayShowError(R.string.wc_moderate_review_error))
    }

    override fun relayRefresh(isRefreshing: Boolean) {
        triggerModerationEvent(ReviewModeration.Relay.ReviewModerationRelayEvent.RelayToggleRefresh(isRefreshing))
    }

    override fun relayUndoReviewModeration() {
        launch {
            reviewModerationHandler.undoReviewModerationAndResetState()
        }

    }

}
