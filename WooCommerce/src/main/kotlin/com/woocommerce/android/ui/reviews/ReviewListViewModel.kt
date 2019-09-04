package com.woocommerce.android.ui.reviews

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.RequestResult.ERROR
import com.woocommerce.android.ui.reviews.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.ui.reviews.RequestResult.SUCCESS
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_REVIEWS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT_REVIEW
import org.wordpress.android.fluxc.store.WCProductStore.OnProductReviewChanged
import javax.inject.Inject
import javax.inject.Named

@OpenClassOnDebug
class ReviewListViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val reviewRepository: ProductReviewsRepositoryContract,
    private val networkStatus: NetworkStatus,
    private val dispatcher: Dispatcher
) : ScopedViewModel(mainDispatcher) {
    // TODO AMANDA: should this MutableLiveData object be exposed publicly?
    val reviewList = MutableLiveData<List<ProductReview>>()

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

    fun start() {
        dispatcher.register(this)
        loadReviews()
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(this)
        reviewRepository.onCleanup()
    }

    fun loadReviews(loadMore: Boolean = false) {
        if (loadMore && !reviewRepository.canLoadMore) {
            WooLog.d(REVIEWS, "No more product reviews to load")
            return
        }

        _isLoadingMore.value = loadMore

        launch {
            if (!loadMore) {
                _isSkeletonShown.value = true

                // Initial load. Get and show reviewList from the db if any
                val reviewsInDb = reviewRepository.getCachedProductReviews()
                if (reviewsInDb.isNotEmpty()) {
                    _isSkeletonShown.value = false
                    reviewList.value = reviewsInDb
                }
            }
            fetchReviewList(loadMore)
        }
    }

    fun refreshReviewList() {
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
            _isMarkingAllAsRead.value = ActionStatus.PROCESSING

            launch {
                when (reviewRepository.markAllProductReviewsAsRead()) {
                    ERROR -> {
                        _isMarkingAllAsRead.value = ActionStatus.ERROR
                        _showSnackbarMessage.value = R.string.wc_mark_all_read_error
                    }
                    NO_ACTION_NEEDED, SUCCESS -> {
                        _isMarkingAllAsRead.value = ActionStatus.COMPLETE
                        _showSnackbarMessage.value = R.string.wc_mark_all_read_success
                    }
                }

                // Update the menu option visibility
                checkForUnreadReviews()
            }
        } else {
            // Network is not connected
            _showSnackbarMessage.value = R.string.offline_error
        }
    }

    private suspend fun fetchReviewList(loadMore: Boolean) {
        if (networkStatus.isConnected()) {
            when (reviewRepository.fetchProductReviews(loadMore)) {
                SUCCESS, NO_ACTION_NEEDED -> { reviewList.value = reviewRepository.getCachedProductReviews()}
                ERROR -> { /* todo amanda : show error */ }
            }

            checkForUnreadReviews()
        } else {
            // Network is not connected
            _showSnackbarMessage.value = R.string.offline_error
        }

        _isSkeletonShown.value = false
        _isLoadingMore.value = false
        _isRefreshing.value = false
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh data now that a connection is active if needed
            refreshReviewList()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductReviewChanged(event: OnProductReviewChanged) {
        if (event.causeOfChange == FETCH_SINGLE_PRODUCT_REVIEW) {
            if (event.isError) {
                WooLog.e(REVIEWS, "Error fetching single product review: ${event.error.message}")
                _showSnackbarMessage.value = R.string.review_single_fetch_error
            } else {
                refreshReviewList()
            }
        } else if (event.causeOfChange == FETCH_PRODUCT_REVIEWS) {
            if (event.isError) {
                _showSnackbarMessage.value = R.string.review_fetch_error
            }
        }
    }
}
