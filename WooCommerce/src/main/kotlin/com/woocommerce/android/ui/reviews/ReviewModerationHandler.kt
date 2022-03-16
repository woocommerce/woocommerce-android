package com.woocommerce.android.ui.reviews

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ActionStatus.*
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

@ActivityRetainedScoped
class ReviewModerationHandler @Inject constructor(
    private val siteModel: SiteModel,
    private val productStore: WCProductStore,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {
    private val _queue = MutableSharedFlow<ReviewModerationRequest>(extraBufferCapacity = Int.MAX_VALUE)

    private val _pendingModerationStatus = MutableSharedFlow<ReviewModerationStatus>(replay = 1)
    val pendingModerationStatus = _pendingModerationStatus.asSharedFlow()

    private var moderationRequestJob: Job? = null

    init {
        appCoroutineScope.launch {
            _queue.collect {
                coroutineScope {
                    moderationRequestJob = handleModerationRequest(it)
                }
            }
        }
    }

    fun postModerationRequest(review: ProductReview, newStatus: ProductReviewStatus) {
        _queue.tryEmit(
            ReviewModerationRequest(
                review = review,
                newStatus = newStatus
            )
        )
    }

    fun undoLastOperation() {
        moderationRequestJob?.cancel()
    }

    private fun CoroutineScope.handleModerationRequest(request: ReviewModerationRequest): Job = launch {
        try {
            WooLog.d(
                T.REVIEWS,
                "Handle moderation request for review ${request.review.remoteId} to status: ${request.newStatus}"
            )
            val status = ReviewModerationStatus(request)
            _pendingModerationStatus.emit(status)
            delay(5000)

            _pendingModerationStatus.emit(status.copy(actionStatus = SUBMITTED))
            WooLog.d(
                T.REVIEWS,
                "Submit API call to moderate review ${request.review.remoteId} to status: ${request.newStatus}"
            )
            val remoteOperationResult = productStore.updateProductReviewStatus(
                siteModel,
                reviewId = request.review.remoteId,
                newStatus = request.newStatus.toString()
            )
            if (remoteOperationResult.isError) {
                WooLog.w(
                    T.REVIEWS,
                    "Changing review status failed because of ${remoteOperationResult.error.type}"
                )
                _pendingModerationStatus.emit(status.copy(actionStatus = ERROR))
            } else {
                val updatedReview = remoteOperationResult.model!!
                WooLog.w(
                    T.REVIEWS,
                    "Review status updated, id: ${updatedReview.remoteProductReviewId}, status:${updatedReview.status}"
                )
                _pendingModerationStatus.emit(status.copy(actionStatus = SUCCESS))
            }
        } catch (e: CancellationException) {
            WooLog.d(
                T.REVIEWS,
                "Moderation request is cancelled, revert to previous status"
            )
            _pendingModerationStatus.tryEmit(
                ReviewModerationStatus(
                    review = request.review,
                    newStatus = ProductReviewStatus.fromString(request.review.status),
                    actionStatus = SUCCESS
                )
            )
        } finally {
            _pendingModerationStatus.resetReplayCache()
        }
    }
}

data class ReviewModerationRequest(
    val review: ProductReview,
    val newStatus: ProductReviewStatus
)

data class ReviewModerationStatus(
    val review: ProductReview,
    val newStatus: ProductReviewStatus,
    val actionStatus: ActionStatus
) {
    constructor(request: ReviewModerationRequest) : this(request.review, request.newStatus, PENDING)
}
