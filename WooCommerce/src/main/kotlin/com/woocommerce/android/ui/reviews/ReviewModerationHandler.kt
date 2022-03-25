package com.woocommerce.android.ui.reviews

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.model.ActionStatus.*
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.ReviewModerationHandler.Companion.SUCCESS_DELAY
import com.woocommerce.android.ui.reviews.ReviewModerationHandler.Companion.UNDO_DELAY
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class is responsible for handling moderation requests of Product Reviews, the implementation allows
 * offering a way to undo the last operation when the user is taken back to the reviews list.
 *
 * A review moderation is handled by the following steps:
 * 1. The function [postModerationRequest] is called after the user's action.
 * 2. The handler will set the status of the request to [PENDING] and publishes it in [pendingModerationStatus].
 * 3. Then we wait for the delay defined by [UNDO_DELAY]
 * 3. After the delay is expired, the handler will fire a request to the server.
 * 4. Depending on the result of the API request, the server will update the status to either [SUCCESS] or [ERROR]
 * 5. If the request succeeded, we keep the status for the delay [SUCCESS_DELAY] to
 *    allow clients to refresh the list of reviews
 * 6. If the request fails, we keep the status for the duration [ERROR] to allow clients
 *    to display an error Snackbar
 * 7. After we are done, the handler will remove the request and its status.
 *
 * For this to work, the client class which should observe the review moderation (generally the ViewModel
 * of the reviews list) needs to implement the interface [ReviewModerationConsumer], and the corresponding screen
 * needs to implement [ReviewModerationUi]
 *
 */
@Singleton
class ReviewModerationHandler @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {
    companion object {
        @VisibleForTesting
        const val UNDO_DELAY = 2750L

        @VisibleForTesting
        const val ERROR_SNACKBAR_DELAY = 1000L

        /** This is needed to allow clients to refresh reviews list after success */
        @VisibleForTesting
        const val SUCCESS_DELAY = 100L
    }

    private val queue = MutableSharedFlow<ReviewModerationRequest>(extraBufferCapacity = Int.MAX_VALUE)

    private val _pendingModerationStatus = MutableStateFlow<List<ReviewModerationStatus>>(emptyList())
    val pendingModerationStatus = _pendingModerationStatus.map { it.sorted() }

    private val _skipDelayTrigger = MutableSharedFlow<Unit>()

    private var pendingJobs = mutableMapOf<Long, Job>()

    init {
        enqueueAndProcessIncomingRequests()
    }

    fun postModerationRequest(review: ProductReview, newStatus: ProductReviewStatus) {
        appCoroutineScope.launch {
            queue.emit(
                ReviewModerationRequest(
                    review = review,
                    newStatus = newStatus
                )
            )
        }
    }

    fun undoOperation(review: ProductReview) {
        pendingJobs[review.remoteId]?.cancel()
    }

    /**
     * Handles incoming requests, by enqueuing them, then executing them one by one
     */
    private fun enqueueAndProcessIncomingRequests() {
        appCoroutineScope.launch {
            queue
                .onEach { request ->
                    WooLog.d(
                        T.REVIEWS,
                        "Enqueue moderation request for review " +
                            "${request.review.remoteId} to status: ${request.newStatus}"
                    )

                    // Cancel any past requests for the same review
                    pendingJobs[request.review.remoteId]?.let {
                        it.cancel()
                        it.join()
                    }

                    _pendingModerationStatus.update { statusList ->
                        statusList.filterNot { it.review == request.review } + ReviewModerationStatus(request)
                    }

                    // Skip delay for the previous request if there is any
                    if (_skipDelayTrigger.subscriptionCount.value > 0) {
                        _skipDelayTrigger.emit(Unit)
                    }
                }
                .buffer()
                .collect { request ->
                    coroutineScope {
                        pendingJobs[request.review.remoteId] = processModerationRequest(request)
                    }
                }
        }
    }

    private fun CoroutineScope.processModerationRequest(request: ReviewModerationRequest): Job = launch {
        try {
            // A delay to allow the user to undo the action
            wait()

            AnalyticsTracker.track(
                AnalyticsEvent.REVIEW_ACTION,
                mapOf(AnalyticsTracker.KEY_TYPE to request.review.toString())
            )

            updateStatus(ReviewModerationStatus(request = request, actionStatus = SUBMITTED))
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
            updateStatus(
                ReviewModerationStatus(
                    request = ReviewModerationRequest(
                        review = request.review,
                        newStatus = ProductReviewStatus.fromString(request.review.status)
                    ),
                    actionStatus = SUCCESS
                )
            )
        } finally {
            // Clear the status cache to avoid resubmitting completed operations
            _pendingModerationStatus.update { statuses -> statuses.filterNot { it.review == request.review } }
        }
    }

    private suspend fun wait() {
        merge(
            _skipDelayTrigger,
            flow {
                delay(UNDO_DELAY)
                emit(Unit)
            }
        ).first()
    }

    private fun updateStatus(status: ReviewModerationStatus) {
        _pendingModerationStatus.update { statuses -> statuses.filterNot { it.review == status.review } + status }
    }

    private suspend fun submitReviewStatusToTheApi(request: ReviewModerationRequest) {
        val remoteOperationResult = productStore.updateProductReviewStatus(
            selectedSite.get(),
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
            updateStatus(ReviewModerationStatus(request, actionStatus = ERROR))
            delay(ERROR_SNACKBAR_DELAY)
        } else {
            val updatedReview = remoteOperationResult.model!!
            WooLog.d(
                T.REVIEWS,
                "Review status updated, id: ${updatedReview.remoteProductReviewId}, status:${updatedReview.status}"
            )
            AnalyticsTracker.track(AnalyticsEvent.REVIEW_ACTION_SUCCESS)
            updateStatus(ReviewModerationStatus(request, actionStatus = SUCCESS))

            // Wait a bit to give clients a chance to reload reviews from DB
            delay(SUCCESS_DELAY)
        }
    }
}
