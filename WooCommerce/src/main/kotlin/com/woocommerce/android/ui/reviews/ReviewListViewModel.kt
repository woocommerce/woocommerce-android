package com.woocommerce.android.ui.reviews

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.RequestResult.ERROR
import com.woocommerce.android.model.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.notifications.UnseenReviewsCountHandler
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.ReviewListViewModel.ReviewListEvent.MarkAllAsRead
import com.woocommerce.android.ui.reviews.domain.MarkAllReviewsAsSeen
import com.woocommerce.android.ui.reviews.domain.MarkAllReviewsAsSeen.Fail
import com.woocommerce.android.ui.reviews.domain.MarkAllReviewsAsSeen.Success
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import javax.inject.Inject

@HiltViewModel
class ReviewListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val dispatcher: Dispatcher,
    private val reviewRepository: ReviewListRepository,
    private val markAllReviewsAsSeen: MarkAllReviewsAsSeen,
    private val unseenReviewsCountHandler: UnseenReviewsCountHandler,
    private val reviewModerationHandler: ReviewModerationHandler
) : ScopedViewModel(savedState), ReviewModerationConsumer {
    companion object {
        private const val TAG = "ReviewListViewModel"
    }

    private var fetchingReviewsJob: Job? = null

    private val _reviewList = MutableLiveData<List<ProductReview>>()
    override val ReviewModerationConsumer.rawReviewList: LiveData<List<ProductReview>>
        get() = _reviewList

    override val ReviewModerationConsumer.reviewModerationHandler: ReviewModerationHandler
        get() = this@ReviewListViewModel.reviewModerationHandler

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        EventBus.getDefault().register(this)
        dispatcher.register(this)
        observeReviewUpdates()
        launch {
            observeModerationEvents()
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
        }
        fetchReviewList(loadMore = false)
    }

    override fun ReviewModerationConsumer.onReviewModerationSuccess() {
        reloadReviewsFromCache()
    }

    private fun reloadReviewsFromCache() {
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
        fetchReviewList(loadMore = true)
    }

    fun forceRefreshReviews() {
        viewState = viewState.copy(isRefreshing = true)
        fetchReviewList(loadMore = false)
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
                when (markAllReviewsAsSeen()) {
                    Fail -> {
                        triggerEvent(MarkAllAsRead(ActionStatus.ERROR))
                        triggerEvent(ShowSnackbar(R.string.wc_mark_all_read_error))
                    }

                    Success -> {
                        triggerEvent(MarkAllAsRead(ActionStatus.SUCCESS))
                        triggerEvent(ShowSnackbar(R.string.wc_mark_all_read_success))
                        _reviewList.value = reviewRepository.getCachedProductReviews()
                    }
                }
            }
        } else {
            showOfflineSnack()
        }
    }

    private fun fetchReviewList(loadMore: Boolean) {
        fetchingReviewsJob = launch {
            if (networkStatus.isConnected()) {
                if (viewState.isUnreadFilterEnabled) {
                    fetchUnreadReviews(loadMore = loadMore)
                } else {
                    reviewRepository.fetchProductReviews(loadMore).collect { result ->
                        when (result) {
                            ReviewListRepository.FetchReviewsResult.NothingFetched -> {
                                // No action needed
                            }
                            is ReviewListRepository.FetchReviewsResult.NotificationsFetched -> {
                                if (result.requestResult == SUCCESS) {
                                    val reviews = reviewRepository.getCachedProductReviews()
                                    if (reviews.isNotEmpty()) {
                                        _reviewList.value = reviews
                                    }
                                }
                            }

                            is ReviewListRepository.FetchReviewsResult.ReviewsFetched -> {
                                when (result.requestResult) {
                                    SUCCESS,
                                    NO_ACTION_NEEDED -> _reviewList.value = reviewRepository.getCachedProductReviews()

                                    ERROR,
                                    RequestResult.API_ERROR,
                                    RequestResult.RETRY -> triggerEvent(ShowSnackbar(R.string.review_fetch_error))
                                }
                            }
                        }
                    }
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
    }

    private suspend fun fetchUnreadReviews(loadMore: Boolean) {
        viewState = viewState.copy(isLoadingMore = loadMore)
        when (reviewRepository.fetchOnlyUnreadProductReviews(loadMore)) {
            SUCCESS,
            NO_ACTION_NEEDED -> _reviewList.value = reviewRepository.getCachedUnreadProductReviews()

            ERROR -> triggerEvent(ShowSnackbar(R.string.review_fetch_error))
            else -> {}
        }
    }

    private fun showOfflineSnack() {
        // Network is not connected
        triggerEvent(ShowSnackbar(R.string.offline_error))
    }

    private fun observeReviewUpdates() {
        viewModelScope.launch {
            unseenReviewsCountHandler.observeUnseenCount()
                .drop(1)
                .collectLatest { forceRefreshReviews() }
        }
    }

    fun onUnreadReviewsFilterChanged(isEnabled: Boolean) {
        fetchingReviewsJob?.cancel()
        viewState = viewState.copy(
            isUnreadFilterEnabled = isEnabled,
            isSkeletonShown = true
        )
        fetchReviewList(loadMore = false)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh data now that a connection is active if needed
            forceRefreshReviews()
        }
    }

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val hasUnreadReviews: Boolean? = null,
        val isUnreadFilterEnabled: Boolean = false
    ) : Parcelable

    sealed class ReviewListEvent : Event() {
        data class MarkAllAsRead(val status: ActionStatus) : ReviewListEvent()
    }
}
