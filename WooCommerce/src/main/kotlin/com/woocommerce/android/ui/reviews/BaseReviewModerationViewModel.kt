package com.woocommerce.android.ui.reviews

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationActionEvent.ReloadReviews
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationActionEvent.RemoveHiddenReviews
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationActionEvent.RemoveProductReviewFromList
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationActionEvent.RevertHiddenReviews
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationUIEvent.ShowOffLineError
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationUIEvent.ShowResponseError
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationUIEvent.ShowUndoUI
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayReloadReviews
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayRemoveHiddenReviews
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayRemoveProductReviewFromList
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayResetPendingModerationState
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayRevertHiddenReviews
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelaySetUpModerationUndo
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayShowError
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayToggleRefresh
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
abstract class BaseReviewModerationViewModel(
    savedState: SavedStateHandle,
    private val reviewModerationHandler: ReviewModerationHandler
) : ScopedViewModel(savedState), ReviewModeration.Relay {

    private var _reviewModerationEvents = QueuedLiveData<ReviewModeration.Relay.ReviewModerationRelayEvent>()
    var reviewModerationEvents: LiveData<ReviewModeration.Relay.ReviewModerationRelayEvent> = _reviewModerationEvents

    override fun collectModerationEvents() {
        reviewModerationHandler.reviewModerationActionEvents.onEach { event ->
            when (event) {
                is RemoveHiddenReviews -> relayRemoveHiddenReviews()
                is ReloadReviews -> relayReloadReviews()
                is RemoveProductReviewFromList -> relayRemoveProductReviewFromList(event.remoteReviewId)
                is RevertHiddenReviews -> relayRevertHiddenReviews()
                is ResetPendingState -> relayResetPendingState()
            }
        }.launchIn(viewModelScope)
        reviewModerationHandler.reviewModerationUIEvents.onEach { uiEvent ->
            when (uiEvent) {
                is ShowUndoUI -> relayUndoModerationEvent(uiEvent.productReviewModerationRequest)
                is ShowRefresh -> relayRefresh(uiEvent.isRefreshing)
                is ShowResponseError -> relayReviewModerationUpdateError()
                is ShowOffLineError -> relayShowOffLineError()
            }
        }.launchIn(viewModelScope)
    }

    override fun relaytReviewStatusChange(review: ProductReview, newStatus: ProductReviewStatus) {
        launch {
            reviewModerationHandler.submitReviewStatusChange(review, newStatus)
        }
        AnalyticsTracker.track(
            AnalyticsTracker.Stat.REVIEW_ACTION,
            mapOf(AnalyticsTracker.KEY_TYPE to newStatus.toString())
        )
    }

    override fun getPendingModerationRequest(): ProductReviewModerationRequest? {
        return reviewModerationHandler.getPendingReviewModerationRequest()
    }

    override fun getPendingModerationNewStatus(): String? {
        reviewModerationHandler.getPendingReviewModerationRequest()?.let {
            return it.newStatus.toString()
        }
        return null
    }

    override fun relayUndoModerationEvent(productReviewModerationRequest: ProductReviewModerationRequest) {
        triggerModerationEvent(RelaySetUpModerationUndo(productReviewModerationRequest))
    }
    /* Using QueuedLiveData all events are reaching the observer , however sometimes it is before onResume
     * which can lead to undesired consequence  for example when removing a certain review from a list it
     * will check for its position and will be -1 if it does before resume
     * this launches a new coroutine every time and ensures
     * that it always reaches at the correct moment and thus eliminating the need to add delay
     */
    private fun triggerModerationEvent(event: ReviewModeration.Relay.ReviewModerationRelayEvent) {
        launch {
            _reviewModerationEvents.value = event
        }
    }

    override fun relayRemoveHiddenReviews() {
        triggerModerationEvent(RelayRemoveHiddenReviews)
    }

    override fun relayRemoveProductReviewFromList(remoteReviewId: Long) {
        triggerModerationEvent(RelayRemoveProductReviewFromList(remoteReviewId))
    }

    override fun relayRevertHiddenReviews() {
        triggerModerationEvent(RelayRevertHiddenReviews)
    }

    override fun relayResetPendingState() {
        triggerModerationEvent(RelayResetPendingModerationState)
    }

    override fun relayReloadReviews() {
        triggerModerationEvent(RelayReloadReviews)
    }

    override fun relayShowOffLineError() {
        triggerModerationEvent(RelayShowError(R.string.offline_error))
    }

    override fun relayReviewModerationUpdateError() {
        triggerModerationEvent(RelayShowError(R.string.wc_moderate_review_error))
    }

    override fun relayRefresh(isRefreshing: Boolean) {
        triggerModerationEvent(RelayToggleRefresh(isRefreshing))
    }

    override fun relayUndoReviewModeration() {
        launch {
            reviewModerationHandler.undoReviewModerationAndResetState()
        }
    }
}
