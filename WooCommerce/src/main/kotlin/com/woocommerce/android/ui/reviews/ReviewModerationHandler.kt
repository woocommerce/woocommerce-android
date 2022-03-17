package com.woocommerce.android.ui.reviews

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ActionStatus.*
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
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
    companion object {
        const val UNDO_DELAY = 2750L
    }

    private val _queue = MutableSharedFlow<ReviewModerationRequest>(extraBufferCapacity = Int.MAX_VALUE)

    private val _pendingModerationStatus = MutableSharedFlow<ReviewModerationStatus>(replay = 1)
    val pendingModerationStatus: Flow<ReviewModerationStatus> = _pendingModerationStatus.asSharedFlow()

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
        AnalyticsTracker.track(AnalyticsEvent.REVIEW_ACTION_UNDO)
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
            delay(UNDO_DELAY)

            AnalyticsTracker.track(
                AnalyticsEvent.REVIEW_ACTION,
                mapOf(AnalyticsTracker.KEY_TYPE to request.review.toString())
            )

            _pendingModerationStatus.emit(status.copy(actionStatus = SUBMITTED))
            WooLog.d(
                T.REVIEWS,
                "Submit API call to moderate review ${request.review.remoteId} to status: ${request.newStatus}"
            )
            submitReviewStatusToTheApi(request)
        } catch (@Suppress("SwallowedException") e: CancellationException) {
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
            // Clear the status cache to avoid resubmitting completed operations
            _pendingModerationStatus.resetReplayCache()
        }
    }

    private suspend fun submitReviewStatusToTheApi(request: ReviewModerationRequest) {
        val remoteOperationResult = productStore.updateProductReviewStatus(
            siteModel,
            reviewId = request.review.remoteId,
            newStatus = request.newStatus.toString()
        )
        if (remoteOperationResult.isError) {
            WooLog.e(
                T.REVIEWS,
                "Error pushing product review status " +
                    "changes to server!: " +
                    "${remoteOperationResult.error?.type} - ${remoteOperationResult.error?.message}"
            )
            AnalyticsTracker.track(
                AnalyticsEvent.REVIEW_ACTION_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                    AnalyticsTracker.KEY_ERROR_TYPE to remoteOperationResult.error?.type?.toString(),
                    AnalyticsTracker.KEY_ERROR_DESC to remoteOperationResult.error?.message
                )
            )
            _pendingModerationStatus.emit(ReviewModerationStatus(request, actionStatus = ERROR))
        } else {
            val updatedReview = remoteOperationResult.model!!
            WooLog.w(
                T.REVIEWS,
                "Review status updated, id: ${updatedReview.remoteProductReviewId}, status:${updatedReview.status}"
            )
            AnalyticsTracker.track(AnalyticsEvent.REVIEW_ACTION_SUCCESS)
            _pendingModerationStatus.emit(ReviewModerationStatus(request, actionStatus = SUCCESS))
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
    constructor(request: ReviewModerationRequest, actionStatus: ActionStatus = PENDING) : this(
        request.review,
        request.newStatus,
        actionStatus
    )
}
