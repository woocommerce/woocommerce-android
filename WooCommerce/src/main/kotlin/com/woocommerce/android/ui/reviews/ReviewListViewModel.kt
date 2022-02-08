package com.woocommerce.android.ui.reviews

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.RequestResult.ERROR
import com.woocommerce.android.model.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.push.NotificationChannelType.REVIEW
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.ReviewListViewModel.ReviewListEvent.MarkAllAsRead
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.UPDATE_PRODUCT_REVIEW_STATUS
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore.OnProductReviewChanged
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductReviewStatusPayload
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class ReviewListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val reviewRepository: ReviewListRepository,
    private val reviewModerationHandler : ReviewModerationHandler
) : ScopedViewModel(savedState), ReviewModeration.Processing {
    companion object {
        private const val TAG = "ReviewListViewModel"
    }


    private val _moderateProductReview = SingleLiveEvent<ProductReviewModerationRequest?>()
    val moderateProductReview: LiveData<ProductReviewModerationRequest?> = _moderateProductReview

    private val _reviewList = MutableLiveData<List<ProductReview>>()
    val reviewList: LiveData<List<ProductReview>> = _reviewList

    final val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        EventBus.getDefault().register(this)
        dispatcher.register(this)
        launch {
            observeModerationRequest()
        }

    }

    override suspend fun observeModerationRequest() {
        reviewModerationHandler.reviewRequest.collect {
            it?.let {
                _moderateProductReview.value = it
                viewState = viewState.copy(isRefreshing = true)
            }
        }
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
            // Initial load. Get and show reviewList from the db if any
            val reviewsInDb = reviewRepository.getCachedProductReviews()
            if (reviewsInDb.isNotEmpty()) {
                _reviewList.value = reviewsInDb
                viewState = viewState.copy(isSkeletonShown = false)
            } else {
                viewState = viewState.copy(isSkeletonShown = true)
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

        viewState = viewState.copy(isLoadingMore = true)
        launch {
            fetchReviewList(loadMore = true)
        }
    }

    fun forceRefreshReviews() {
        viewState = viewState.copy(isRefreshing = true)
        launch {
            fetchReviewList(loadMore = false)
        }
    }

    fun checkForUnreadReviews() {
        launch {
            viewState = viewState.copy(hasUnreadReviews = reviewRepository.getHasUnreadCachedProductReviews())
        }
    }

    fun markAllReviewsAsRead() {
        if (networkStatus.isConnected()) {
            triggerEvent(MarkAllAsRead(ActionStatus.SUBMITTED))

            launch {
                when (reviewRepository.markAllProductReviewsAsRead()) {
                    ERROR -> {
                        triggerEvent(MarkAllAsRead(ActionStatus.ERROR))
                        triggerEvent(ShowSnackbar(R.string.wc_mark_all_read_error))
                    }
                    NO_ACTION_NEEDED, SUCCESS -> {
                        triggerEvent(MarkAllAsRead(ActionStatus.SUCCESS))
                        triggerEvent(ShowSnackbar(R.string.wc_mark_all_read_success))
                    }
                }
            }
        } else {
            // Network is not connected
            showOfflineSnack()
        }
    }

    // region Review Moderation
    override fun submitReviewStatusChange(review: ProductReview, newStatus: ProductReviewStatus) {
        if (networkStatus.isConnected()) {
            val payload = UpdateProductReviewStatusPayload(
                selectedSite.get(),
                review.remoteId,
                newStatus.toString()
            )
            launch {
                val request = ProductReviewModerationRequest(review,newStatus)
                val onReviewChanged = reviewModerationHandler.submitReviewStatusChange(request)
                if(onReviewChanged.isError) {
                    triggerEvent(ShowSnackbar(R.string.wc_moderate_review_error))
                    sendReviewModerationUpdate(ActionStatus.ERROR)
                }
                else{
                    sendReviewModerationUpdate(ActionStatus.SUCCESS)
                    reloadReviewsFromCache()
                }
            }

            AnalyticsTracker.track(
                Stat.REVIEW_ACTION,
                mapOf(AnalyticsTracker.KEY_TYPE to newStatus.toString())
            )

            sendReviewModerationUpdate(ActionStatus.SUBMITTED)
        } else {
            // Network is not connected
            showOfflineSnack()
            sendReviewModerationUpdate(ActionStatus.ERROR)
        }
    }

    override fun sendReviewModerationUpdate(newRequestStatus: ActionStatus) {
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
                SUCCESS, NO_ACTION_NEEDED -> {
                    _reviewList.value = reviewRepository.getCachedProductReviews()
                }
                else -> triggerEvent(ShowSnackbar(R.string.review_fetch_error))
            }

            checkForUnreadReviews()
        } else {
            // Network is not connected
            showOfflineSnack()
        }

        viewState = viewState.copy(
            isSkeletonShown = false,
            isLoadingMore = false,
            isRefreshing = false
        )
    }

    private fun showOfflineSnack() {
        // Network is not connected
        triggerEvent(ShowSnackbar(R.string.offline_error))
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

    override fun resetPendingModerationVariables() {
        reviewModerationHandler.resetPendingModerationVariables()
    }

    override fun getPendingModerationRequest(): ProductReviewModerationRequest? {
        return reviewModerationHandler.pendingModerationRequest
    }

    override fun getPendingModerationNewStatus():String? {
        return reviewModerationHandler.pendingModerationNewStatus
    }

    override fun setPendingModerationRequest(request: ProductReviewModerationRequest?) {
        reviewModerationHandler.pendingModerationRequest = request
    }


    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val hasUnreadReviews: Boolean? = null
    ) : Parcelable

    sealed class ReviewListEvent : Event() {
        data class MarkAllAsRead(val status: ActionStatus) : ReviewListEvent()
    }
}
