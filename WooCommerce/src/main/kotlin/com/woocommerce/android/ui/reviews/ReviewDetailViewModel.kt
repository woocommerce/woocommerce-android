package com.woocommerce.android.ui.reviews

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.RequestResult.ERROR
import com.woocommerce.android.ui.reviews.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.ui.reviews.RequestResult.SUCCESS
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

@OpenClassOnDebug
final class ReviewDetailViewModel @AssistedInject constructor(
    dispatchers: CoroutineDispatchers,
    private val repository: ReviewDetailRepository,
    private val networkStatus: NetworkStatus,
    @Assisted private val handle: SavedStateHandle
) : ScopedViewModel(dispatchers) {
    private var remoteReviewId = 0L

    private val _productReview = MutableLiveData<ProductReview>()
    val productReview: LiveData<ProductReview> = _productReview

    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    private val _refreshProductImage = MutableLiveData<Long>()
    val refreshProductImage: LiveData<Long> = _refreshProductImage

    private val _exit = SingleLiveEvent<Unit>()
    val exit: LiveData<Unit> = _exit

    private val _isSkeletonShown = MutableLiveData<Boolean>()
    val isSkeletonShown: LiveData<Boolean> = _isSkeletonShown

    private val _markAsRead = MutableLiveData<Long>()
    val markAsRead: LiveData<Long> = _markAsRead

    fun start(remoteReviewId: Long) {
        loadProductReview(remoteReviewId)
    }

    override fun onCleared() {
        super.onCleared()
        repository.onCleanup()
    }

    fun moderateReview(newStatus: ProductReviewStatus) {
        if (networkStatus.isConnected()) {
            productReview.value?.let { review ->
                // post an event to tell the notification list to moderate this
                // review, then close the fragment
                val event = OnRequestModerateReviewEvent(
                        ProductReviewModerationRequest(review, newStatus)
                )
                EventBus.getDefault().post(event)

                // Close the detail view
                _exit.call()
            }
        } else {
            // Network is not connected
            _showSnackbarMessage.value = R.string.offline_error
        }
    }

    private fun loadProductReview(remoteReviewId: Long) {
        // Mark the notification as read
        launch {
            markAsRead(remoteReviewId)
        }

        val shouldFetch = remoteReviewId != this.remoteReviewId
        this.remoteReviewId = remoteReviewId

        launch {
            _isSkeletonShown.value = true

            val reviewInDb = repository.getCachedProductReview(remoteReviewId)
            if (reviewInDb != null) {
                _isSkeletonShown.value = false
                _productReview.value = reviewInDb

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
                            _productReview.value = review
                            _isSkeletonShown.value = false
                        }
                    }
                    ERROR -> _showSnackbarMessage.value = R.string.wc_load_review_error
                }
            }
        } else {
            // Network is not connected
            _showSnackbarMessage.value = R.string.offline_error
        }
    }

    private suspend fun markAsRead(remoteReviewId: Long) {
        repository.getCachedNotificationForReview(remoteReviewId)?.let {
            // remove notification from the notification panel if it exists
            _markAsRead.value = it.remoteNoteId

            // send request to mark notification as read to the server
            repository.markNotificationAsRead(it)

            // Send the track event that a product review notification was opened
            AnalyticsTracker.track(Stat.NOTIFICATION_OPEN, mapOf(
                    AnalyticsTracker.KEY_TYPE to AnalyticsTracker.VALUE_REVIEW,
                    AnalyticsTracker.KEY_ALREADY_READ to it.read))
        }
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ReviewDetailViewModel>
}
