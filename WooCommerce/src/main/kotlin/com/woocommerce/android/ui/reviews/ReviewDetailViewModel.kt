package com.woocommerce.android.ui.reviews

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.REVIEW_REPLY_SEND
import com.woocommerce.android.analytics.AnalyticsEvent.REVIEW_REPLY_SEND_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.REVIEW_REPLY_SEND_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.ReviewDetailViewModel.ReviewDetailEvent.NavigateBackFromNotification
import com.woocommerce.android.ui.reviews.ReviewDetailViewModel.ReviewDetailEvent.Reply
import com.woocommerce.android.ui.reviews.domain.MarkReviewAsSeen
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import javax.inject.Inject

@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val repository: ReviewDetailRepository,
    private val markReviewAsSeen: MarkReviewAsSeen,
    private val reviewModerationHandler: ReviewModerationHandler,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState) {
    private var remoteReviewId = 0L

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private var launchedFromNotification: Boolean = false

    fun start(remoteReviewId: Long, launchedFromNotification: Boolean) {
        this.launchedFromNotification = launchedFromNotification
        loadProductReview(remoteReviewId, launchedFromNotification)
    }

    fun moderateReview(newStatus: ProductReviewStatus) {
        if (networkStatus.isConnected()) {
            viewState.productReview?.let { review ->
                reviewModerationHandler.postModerationRequest(review, newStatus)
                // Close the detail view
                triggerEvent(Exit)
            }
        } else {
            // Network is not connected
            triggerEvent(ShowSnackbar(R.string.offline_error))
        }
    }

    private fun loadProductReview(remoteReviewId: Long, launchedFromNotification: Boolean) {
        // Mark the notification as read
        launch {
            repository.getCachedNotificationForReview(remoteReviewId)?.let {
                markReviewAsSeen(remoteReviewId, it)
                if (launchedFromNotification) {
                    // Send the track event that a product review notification was opened
                    AnalyticsTracker.track(
                        AnalyticsEvent.NOTIFICATION_OPEN,
                        mapOf(
                            AnalyticsTracker.KEY_TYPE to AnalyticsTracker.VALUE_REVIEW,
                            AnalyticsTracker.KEY_ALREADY_READ to it.read
                        )
                    )
                }
            }
        }

        val shouldFetch = remoteReviewId != this.remoteReviewId
        this.remoteReviewId = remoteReviewId

        launch {
            viewState = viewState.copy(isSkeletonShown = true)

            val reviewInDb = repository.getCachedProductReview(remoteReviewId)
            if (reviewInDb != null) {
                viewState = viewState.copy(
                    productReview = reviewInDb,
                    isSkeletonShown = false
                )

                if (shouldFetch) {
                    // Fetch it asynchronously so the db version loads immediately
                    fetchProductReview(remoteReviewId)
                }
            } else {
                fetchProductReview(remoteReviewId)
            }
        }
    }

    private fun fetchProductReview(remoteReviewId: Long) {
        if (networkStatus.isConnected()) {
            launch {
                when (repository.fetchProductReview(remoteReviewId)) {
                    RequestResult.SUCCESS, RequestResult.NO_ACTION_NEEDED -> {
                        repository.getCachedProductReview(remoteReviewId)?.let { review ->
                            viewState = viewState.copy(
                                productReview = review,
                                isSkeletonShown = false
                            )
                        }
                    }
                    RequestResult.ERROR -> triggerEvent(ShowSnackbar(R.string.wc_load_review_error))
                    RequestResult.API_ERROR -> Unit // Do nothing
                    RequestResult.RETRY -> Unit // Do nothing
                }
            }
        } else {
            // Network is not connected
            triggerEvent(ShowSnackbar(R.string.offline_error))
        }
    }

    fun onBackPressed(): Boolean {
        if (launchedFromNotification) {
            triggerEvent(NavigateBackFromNotification)
        } else {
            triggerEvent(Exit)
        }
        return false
    }

    fun onReviewReplied(reviewReply: String) {
        analyticsTracker.track(REVIEW_REPLY_SEND)
        launch {
            viewState.productReview?.let {
                val result: WooResult<Unit> = repository.reply(
                    RemoteId(it.remoteProductId),
                    RemoteId(it.remoteId),
                    reviewReply
                )

                if (result.isError) {
                    analyticsTracker.track(REVIEW_REPLY_SEND_FAILED)
                    triggerEvent(ShowSnackbar(R.string.review_reply_failure))
                } else {
                    analyticsTracker.track(REVIEW_REPLY_SEND_SUCCESS)
                    triggerEvent(ShowSnackbar(R.string.review_reply_success))
                }
            }
        }
    }

    fun onReplyClicked() {
        triggerEvent(Reply)
    }

    @Parcelize
    data class ViewState(
        val productReview: ProductReview? = null,
        val isSkeletonShown: Boolean? = null
    ) : Parcelable

    sealed class ReviewDetailEvent : Event() {
        object Reply : ReviewDetailEvent()
        object NavigateBackFromNotification : ReviewDetailEvent()
    }
}
