package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationActionEvent
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationUIEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductReviewStatusPayload
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ReviewModerationHandler @Inject constructor(
    private val productStore:WCProductStore
): ReviewModeration.Handler {

    private val _reviewModerationActionEvents =
        MutableStateFlow<ReviewModerationActionEvent>(ReviewModerationActionEvent.Idle)

    private val _reviewModerationUIEvents =
        MutableStateFlow<ReviewModerationUIEvent>(ReviewModerationUIEvent.Idle)

    override val reviewModerationActionEvents: StateFlow<ReviewModerationActionEvent> = _reviewModerationActionEvents

    override val reviewModerationUIEvents: StateFlow<ReviewModerationUIEvent> = _reviewModerationUIEvents

    override var pendingModerationRemoteReviewId: Long? = null

    override var pendingModerationNewStatus: String? = null

    override var pendingModerationRequest: ProductReviewModerationRequest? = null

    override suspend fun launchProductReviewModerationRequestFlow(event:OnRequestModerateReviewEvent) {
        pendingModerationRequest = event.request
        processReviewModerationActionStatus(event.request)
    }

    private fun processReviewModerationActionStatus(request:ProductReviewModerationRequest){
        with(request){
            when(actionStatus){
                ActionStatus.PENDING -> {
                    _reviewModerationUIEvents.value = ReviewModerationUIEvent.ShowUndoUI(request)
                    if (newStatus == ProductReviewStatus.SPAM || newStatus == ProductReviewStatus.TRASH) {
                        _reviewModerationActionEvents.value = ReviewModerationActionEvent.RemoveProductReviewFromList(productReview.remoteId)

                    }
                }
                ActionStatus.SUCCESS -> {
                    _reviewModerationActionEvents.value = ReviewModerationActionEvent.RemoveHiddenReviews
                    resetPendingModerationVariables()
                    _reviewModerationActionEvents.value = ReviewModerationActionEvent.ReloadReviews
                }
                ActionStatus.ERROR -> {
                    _reviewModerationActionEvents.value =
                        ReviewModerationActionEvent.RevertPendingModerationState
                    _reviewModerationUIEvents.value = ReviewModerationUIEvent.ShowResponseError
                }
                else -> { /* do nothing */
                }
            }
        }
    }

    override suspend fun handleOffLineError(){
        pendingModerationRequest?.apply { actionStatus = ActionStatus.ERROR }
            ?.also { processReviewModerationActionStatus(it) }
    }



    override suspend fun submitReviewStatusChange(payload: UpdateProductReviewStatusPayload) {
        pendingModerationRemoteReviewId = payload.remoteReviewId
        pendingModerationNewStatus = payload.newStatus
        _reviewModerationUIEvents.value = ReviewModerationUIEvent.showRefresh(true)
        val reviewModerationUpdateResponse = productStore.updateProductReviewStatus(payload)
        _reviewModerationUIEvents.value = ReviewModerationUIEvent.showRefresh(false)
        if(reviewModerationUpdateResponse.isError) {
            pendingModerationRequest?.apply { actionStatus = ActionStatus.ERROR }
        }
        else {
            pendingModerationRequest?.apply { actionStatus = ActionStatus.SUCCESS }
        }
        pendingModerationRequest?.let  { processReviewModerationActionStatus(it) }
    }

    override fun resetPendingModerationVariables() {
        pendingModerationNewStatus = null
        pendingModerationRemoteReviewId = null
        pendingModerationRequest = null
    }



}
