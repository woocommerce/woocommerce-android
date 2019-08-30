package com.woocommerce.android.ui.reviews

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject
import javax.inject.Named

@OpenClassOnDebug
class ReviewListViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val selectedSite: SelectedSite,
    private val reviewRepository: ReviewListRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(mainDispatcher) {
    private var canLoadMore = true

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

    fun start() {
        loadReviews()
    }

    override fun onCleared() {
        super.onCleared()
        reviewRepository.onCleanup()
    }

    fun loadReviews(loadMore: Boolean = false) {
        if (loadMore && !reviewRepository.canLoadMoreReviews) {
            WooLog.d(REVIEWS, "No more product reviews to load")
            return
        }

        _isLoadingMore.value = loadMore
        _isSkeletonShown.value = true

        launch {
            if (!loadMore) {
                // Initial load. Get and show reviewList from the db if any. Otherwise show the loading skeleton.
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
        loadReviews()
    }

    private fun fetchReviewList(loadMore: Boolean) {
        if (networkStatus.isConnected()) {
            launch {
                reviewList.value = reviewRepository.fetchAndLoadProductReviews(loadMore)
                canLoadMore = reviewRepository.canLoadMoreReviews
            }
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
}
