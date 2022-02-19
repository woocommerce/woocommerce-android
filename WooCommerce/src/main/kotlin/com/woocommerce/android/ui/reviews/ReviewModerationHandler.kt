package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationActionEvent
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationUIEvent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductReviewStatusPayload
import javax.inject.Inject

@ActivityRetainedScoped
class ReviewModerationHandler @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus
) : ReviewModeration.Handler {
    private val _reviewModerationActionEvents =
        MutableSharedFlow<ReviewModerationActionEvent>(0)

    private val _reviewModerationUIEvents =
        MutableSharedFlow<ReviewModerationUIEvent>(0)

    val reviewModerationActionEvents: SharedFlow<ReviewModerationActionEvent> =
        _reviewModerationActionEvents.asSharedFlow()

    val reviewModerationUIEvents: SharedFlow<ReviewModerationUIEvent> = _reviewModerationUIEvents.asSharedFlow()

    // This can only be read from outside this class.
    private var pendingModerationRequest: ProductReviewModerationRequest? = null

    override suspend fun launchProductReviewModerationRequestFlow(event: OnRequestModerateReviewEvent) {
        pendingModerationRequest = event.request
        processReviewModerationActionStatus(event.request)
    }

    private suspend fun processReviewModerationActionStatus(request: ProductReviewModerationRequest) {
        with(request) {
            when (actionStatus) {
                ActionStatus.PENDING -> {
                    emitUiEvent(ReviewModerationUIEvent.ShowUndoUI(request))
                    if (newStatus == ProductReviewStatus.SPAM || newStatus == ProductReviewStatus.TRASH) {
                        emitActionEvent(ReviewModerationActionEvent.RemoveProductReviewFromList(productReview.remoteId))
                    }
                    emitUiEvent(ReviewModerationUIEvent.ShowRefresh(true))
                }
                ActionStatus.SUCCESS -> {
                    emitActionEvent(ReviewModerationActionEvent.RemoveHiddenReviews)
                    emitActionEvent(ReviewModerationActionEvent.ResetPendingState)
                    pendingModerationRequest = null
                    emitActionEvent(ReviewModerationActionEvent.ReloadReviews)
                }
                ActionStatus.ERROR -> {
                    emitUiEvent(ReviewModerationUIEvent.ShowResponseError)
                    undoReviewModerationAndResetState()
                }
                else -> { /* do nothing */
                }
            }
        }
    }

    override suspend fun submitReviewStatusChange(
        review: ProductReview,
        newStatus: ProductReviewStatus
    ) {
        if (networkStatus.isConnected()) {
            val payload = UpdateProductReviewStatusPayload(
                selectedSite.get(),
                review.remoteId,
                newStatus.toString()
            )
            emitUiEvent(ReviewModerationUIEvent.ShowRefresh(true))
            val reviewModerationUpdateResponse = productStore.updateProductReviewStatus(payload)
            emitUiEvent(ReviewModerationUIEvent.ShowRefresh(false))
            if (reviewModerationUpdateResponse.isError) {
                pendingModerationRequest?.apply { actionStatus = ActionStatus.ERROR }
            } else {
                pendingModerationRequest?.apply { actionStatus = ActionStatus.SUCCESS }
            }
            pendingModerationRequest?.let { processReviewModerationActionStatus(it) }
        } else {
            emitUiEvent(ReviewModerationUIEvent.ShowOffLineError)
        }
    }

    override suspend fun undoReviewModerationAndResetState() {
        val newStatus = pendingModerationRequest?.newStatus
        val status = ProductReviewStatus.fromString(newStatus.toString())
        if (status == ProductReviewStatus.SPAM || status == ProductReviewStatus.TRASH) {
            emitActionEvent(ReviewModerationActionEvent.RevertHiddenReviews)
        }
        pendingModerationRequest = null
        emitActionEvent(ReviewModerationActionEvent.ResetPendingState)
        emitUiEvent(ReviewModerationUIEvent.ShowRefresh(false))
    }

    private suspend fun emitUiEvent(event: ReviewModerationUIEvent) {
        _reviewModerationUIEvents.emit(event)
    }

    private suspend fun emitActionEvent(event: ReviewModerationActionEvent) {
        _reviewModerationActionEvents.emit(event)
    }

    fun getPendingReviewModerationRequest() = pendingModerationRequest
}
