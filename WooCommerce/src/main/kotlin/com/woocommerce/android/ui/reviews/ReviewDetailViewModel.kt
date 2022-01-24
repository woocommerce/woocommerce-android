package com.woocommerce.android.ui.reviews

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.RequestResult.ERROR
import com.woocommerce.android.model.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.ReviewDetailViewModel.ReviewDetailEvent.MarkNotificationAsRead
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val repository: ReviewDetailRepository
) : ScopedViewModel(savedState) {
    private var remoteReviewId = 0L

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    fun start(remoteReviewId: Long, launchedFromNotification: Boolean) {
        loadProductReview(remoteReviewId, launchedFromNotification)
    }

    fun moderateReview(newStatus: ProductReviewStatus) {
        if (networkStatus.isConnected()) {
            viewState.productReview?.let { review ->
                // post an event to tell the notification list to moderate this
                // review, then close the fragment
                val event = OnRequestModerateReviewEvent(
                    ProductReviewModerationRequest(review, newStatus)
                )
                EventBus.getDefault().post(event)

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
            markAsRead(remoteReviewId, launchedFromNotification)
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

    private suspend fun markAsRead(remoteReviewId: Long, launchedFromNotification: Boolean) {
        repository.getCachedNotificationForReview(remoteReviewId)?.let {
            // remove notification from the notification panel if it exists
            triggerEvent(MarkNotificationAsRead(it.remoteNoteId))

            // send request to mark notification as read to the server
            repository.markNotificationAsRead(it, remoteReviewId)

            if (launchedFromNotification) {
                // Send the track event that a product review notification was opened
                AnalyticsTracker.track(
                    Stat.NOTIFICATION_OPEN,
                    mapOf(
                        AnalyticsTracker.KEY_TYPE to AnalyticsTracker.VALUE_REVIEW,
                        AnalyticsTracker.KEY_ALREADY_READ to it.read
                    )
                )
            }
        }
    }

    @Parcelize
    data class ViewState(
        val productReview: ProductReview? = null,
        val isSkeletonShown: Boolean? = null
    ) : Parcelable

    sealed class ReviewDetailEvent : Event() {
        data class MarkNotificationAsRead(val remoteNoteId: Long) : ReviewDetailEvent()
    }
}
