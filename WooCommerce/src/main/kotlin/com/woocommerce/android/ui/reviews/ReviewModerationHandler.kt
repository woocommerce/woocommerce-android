package com.woocommerce.android.ui.reviews

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.model.ActionStatus.*
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject
import javax.inject.Singleton

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
        @VisibleForTesting
        const val DELAY_FOR_REFRESHING = 100L
    }

    private val _queue = MutableSharedFlow<ReviewModerationRequest>(extraBufferCapacity = Int.MAX_VALUE)

    private val _pendingModerationStatus = MutableStateFlow<List<ReviewModerationStatus>>(emptyList())
    val pendingModerationStatus = _pendingModerationStatus.map { it.sorted() }

    private val _skipDelayTrigger = MutableSharedFlow<Unit>()

    private var pendingJobs = mutableMapOf<Long, Job>()

    init {
        appCoroutineScope.launch {
            // Queue requests
            _queue
                .onEach { request ->
                    WooLog.d(
                        T.REVIEWS,
                        "Queue moderation request for review " +
                            "${request.review.remoteId} to status: ${request.newStatus}"
                    )

                    if (_skipDelayTrigger.subscriptionCount.value > 0) {
                        _skipDelayTrigger.emit(Unit)
                    }
                    _pendingModerationStatus.update { it + ReviewModerationStatus(request) }
                }
                .launchIn(appCoroutineScope)

            // Process requests
            _queue.collect { request ->
                coroutineScope {
                    pendingJobs[request.review.remoteId] = processModerationRequest(request)
                }
            }
        }
    }

    fun postModerationRequest(review: ProductReview, newStatus: ProductReviewStatus) {
        appCoroutineScope.launch {
            _queue.emit(
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
            WooLog.w(
                T.REVIEWS,
                "Review status updated, id: ${updatedReview.remoteProductReviewId}, status:${updatedReview.status}"
            )
            AnalyticsTracker.track(AnalyticsEvent.REVIEW_ACTION_SUCCESS)
            updateStatus(ReviewModerationStatus(request, actionStatus = SUCCESS))

            // Wait a bit to give clients a chance to reload reviews from DB
            delay(DELAY_FOR_REFRESHING)
        }
    }
}
