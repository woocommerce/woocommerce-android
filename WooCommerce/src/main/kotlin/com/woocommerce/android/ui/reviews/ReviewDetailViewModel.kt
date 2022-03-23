package com.woocommerce.android.ui.reviews

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.RequestResult.*
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.ReviewDetailViewModel.ReviewDetailEvent.NavigateBackFromNotification
import com.woocommerce.android.ui.reviews.domain.MarkReviewAsSeen
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val repository: ReviewDetailRepository,
    private val markReviewAsSeen: MarkReviewAsSeen,
    private val reviewModerationHandler: ReviewModerationHandler
) : ScopedViewModel(savedState) {
    private var remoteReviewId = 0L

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
                    SUCCESS, NO_ACTION_NEEDED -> {
                        repository.getCachedProductReview(remoteReviewId)?.let { review ->
                            viewState = viewState.copy(
                                productReview = review,
                                isSkeletonShown = false
                            )
                        }
                    }
                    ERROR -> triggerEvent(ShowSnackbar(R.string.wc_load_review_error))
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

    @Parcelize
    data class ViewState(
        val productReview: ProductReview? = null,
        val isSkeletonShown: Boolean? = null
    ) : Parcelable

    sealed class ReviewDetailEvent : Event() {
        object NavigateBackFromNotification : ReviewDetailEvent()
    }
}
