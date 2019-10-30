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
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.push.NotificationHandler.NotificationChannelType.REVIEW
import com.woocommerce.android.push.NotificationHandler.NotificationReceivedEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.RequestResult.ERROR
import com.woocommerce.android.ui.reviews.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.ui.reviews.RequestResult.SUCCESS
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.NotificationAction.MARK_NOTIFICATIONS_READ
import org.wordpress.android.fluxc.action.WCProductAction.UPDATE_PRODUCT_REVIEW_STATUS
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductReviewChanged
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductReviewStatusPayload

@OpenClassOnDebug
class ReviewListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateHandle,
    dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val reviewRepository: ReviewListRepository
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val TAG = "ReviewListViewModel"
    }
    private val _reviewList = MutableLiveData<List<ProductReview>>()
    val reviewList: LiveData<List<ProductReview>> = _reviewList

    private val _isSkeletonShown = MutableLiveData<Boolean>()
    val isSkeletonShown: LiveData<Boolean> = _isSkeletonShown

    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _hasUnreadReviews = MutableLiveData<Boolean>()
    val hasUnreadReviews: LiveData<Boolean> = _hasUnreadReviews

    private val _isMarkingAllAsRead = MutableLiveData<ActionStatus>()
    val isMarkingAllAsRead: LiveData<ActionStatus> = _isMarkingAllAsRead

    private val _moderateProductReview = SingleLiveEvent<ProductReviewModerationRequest>()
    val moderateProductReview: LiveData<ProductReviewModerationRequest> = _moderateProductReview

    init {
        EventBus.getDefault().register(this)
        dispatcher.register(this)
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
        dispatcher.unregister(this)
        reviewRepository.onCleanup()
    }

    /**
     * Fetch and load cached reviews from the database, then fetch fresh reviews
     * from the API.
     */
    fun start() {
        launch {
            _isSkeletonShown.value = true

            // Initial load. Get and show reviewList from the db if any
            val reviewsInDb = reviewRepository.getCachedProductReviews()
            if (reviewsInDb.isNotEmpty()) {
                _isSkeletonShown.value = false
                _reviewList.value = reviewsInDb
            }
            fetchReviewList(loadMore = false)
        }
    }

    /**
     * Reload reviews from the database. Useful when a change happens on the backend
     * when the list view was not visible.
     */
    fun reloadReviewsFromCache() {
        launch {
            _reviewList.value = reviewRepository.getCachedProductReviews()
        }
    }

    fun loadMoreReviews() {
        if (!reviewRepository.canLoadMore) {
            WooLog.d(REVIEWS, "$TAG : No more product reviews to load")
            return
        }

        _isLoadingMore.value = true

        launch {
            fetchReviewList(loadMore = true)
        }
    }

    fun forceRefreshReviews() {
        _isRefreshing.value = true
        launch {
            fetchReviewList(loadMore = false)
        }
    }

    fun checkForUnreadReviews() {
        launch {
            _hasUnreadReviews.value = reviewRepository.getHasUnreadCachedProductReviews()
        }
    }

    fun markAllReviewsAsRead() {
        if (networkStatus.isConnected()) {
            _isMarkingAllAsRead.value = ActionStatus.SUBMITTED

            launch {
                when (reviewRepository.markAllProductReviewsAsRead()) {
                    ERROR -> {
                        _isMarkingAllAsRead.value = ActionStatus.ERROR
                        _showSnackbarMessage.value = R.string.wc_mark_all_read_error
                    }
                    NO_ACTION_NEEDED, SUCCESS -> {
                        _isMarkingAllAsRead.value = ActionStatus.SUCCESS
                        _showSnackbarMessage.value = R.string.wc_mark_all_read_success
                    }
                }
            }
        } else {
            // Network is not connected
            showOfflineSnack()
        }
    }

    // region Review Moderation
    fun submitReviewStatusChange(review: ProductReview, newStatus: ProductReviewStatus) {
        if (networkStatus.isConnected()) {
            val payload = UpdateProductReviewStatusPayload(
                    selectedSite.get(),
                    review.remoteId,
                    newStatus.toString()
            )
            dispatcher.dispatch(WCProductActionBuilder.newUpdateProductReviewStatusAction(payload))

            AnalyticsTracker.track(
                    Stat.REVIEW_ACTION,
                    mapOf(AnalyticsTracker.KEY_TYPE to newStatus.toString()))

            sendReviewModerationUpdate(ActionStatus.SUBMITTED)
        } else {
            // Network is not connected
            showOfflineSnack()
            sendReviewModerationUpdate(ActionStatus.ERROR)
        }
    }

    private fun sendReviewModerationUpdate(newRequestStatus: ActionStatus) {
        _moderateProductReview.value = _moderateProductReview.value?.apply { actionStatus = newRequestStatus }

        // If the request has been completed, set the event to null to prevent issues later.
        if (newRequestStatus.isComplete()) {
            _moderateProductReview.value = null
        }
    }
    // endregion

    private suspend fun fetchReviewList(loadMore: Boolean) {
        if (networkStatus.isConnected()) {
            when (reviewRepository.fetchProductReviews(loadMore)) {
                SUCCESS, NO_ACTION_NEEDED -> _reviewList.value = reviewRepository.getCachedProductReviews()
                ERROR -> _showSnackbarMessage.value = R.string.review_fetch_error
            }

            checkForUnreadReviews()
        } else {
            // Network is not connected
            showOfflineSnack()
        }

        _isSkeletonShown.value = false
        _isLoadingMore.value = false
        _isRefreshing.value = false
    }

    private fun showOfflineSnack() {
        // Network is not connected
        _showSnackbarMessage.value = R.string.offline_error
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh data now that a connection is active if needed
            forceRefreshReviews()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: NotificationReceivedEvent) {
        if (event.channel == REVIEW) {
            // New review notification received. Request the list of reviews be refreshed.
            forceRefreshReviews()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnRequestModerateReviewEvent) {
        if (networkStatus.isConnected()) {
            // Send the request to the UI to show the UNDO snackbar
            _moderateProductReview.value = event.request
        } else {
            // Network not connected
            showOfflineSnack()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        if (event.causeOfChange == MARK_NOTIFICATIONS_READ) {
            if (!event.isError) {
                reloadReviewsFromCache()
                checkForUnreadReviews()
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductReviewChanged(event: OnProductReviewChanged) {
        if (event.causeOfChange == UPDATE_PRODUCT_REVIEW_STATUS) {
            if (event.isError) {
                // Show an error in the UI and reload the view
                _showSnackbarMessage.value = R.string.wc_moderate_review_error
                sendReviewModerationUpdate(ActionStatus.ERROR)
            } else {
                sendReviewModerationUpdate(ActionStatus.SUCCESS)
            }
        }
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ReviewListViewModel>
}
