package com.woocommerce.android.ui.products.reviews

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_REVIEWS_LOADED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_REVIEWS_LOAD_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.RequestResult.API_ERROR
import com.woocommerce.android.model.RequestResult.ERROR
import com.woocommerce.android.model.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.model.RequestResult.RETRY
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.ReviewListRepository
import com.woocommerce.android.ui.reviews.ReviewModerationConsumer
import com.woocommerce.android.ui.reviews.ReviewModerationHandler
import com.woocommerce.android.ui.reviews.observeModerationEvents
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.PRODUCTS
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ProductReviewsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val reviewModerationHandler: ReviewModerationHandler,
    private val reviewListRepository: ReviewListRepository
) : ScopedViewModel(savedState), ReviewModerationConsumer {
    private val _reviewList = MutableLiveData<List<ProductReview>>()

    override val ReviewModerationConsumer.reviewModerationHandler: ReviewModerationHandler
        get() = this@ProductReviewsViewModel.reviewModerationHandler

    override val ReviewModerationConsumer.rawReviewList: LiveData<List<ProductReview>>
        get() = _reviewList

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val productReviewsViewStateData = LiveDataDelegate(savedState, ProductReviewsViewState())
    private var productReviewsViewState by productReviewsViewStateData

    private val navArgs: ProductReviewsFragmentArgs by savedState.navArgs()

    private var hasModifiedReviews: Boolean = false
    private var fetchingReviewsJob: Job? = null

    init {
        if (_reviewList.value == null) {
            loadProductReviews()
        }
        launch { observeModerationEvents() }
    }

    fun refreshProductReviews() {
        productReviewsViewState = productReviewsViewState.copy(isRefreshing = true)
        fetchProductReviews(remoteProductId = navArgs.remoteProductId, loadMore = false)
    }

    fun loadMoreReviews() {
        if (!reviewListRepository.canLoadMore) {
            WooLog.d(PRODUCTS, "No more reviews to load for product: ${navArgs.remoteProductId}")
            return
        }

        productReviewsViewState = productReviewsViewState.copy(isLoadingMore = true)
        fetchProductReviews(remoteProductId = navArgs.remoteProductId, loadMore = true)
    }

    override fun ReviewModerationConsumer.onReviewModerationSuccess() {
        reloadReviewsFromCache()
        hasModifiedReviews = true
    }

    fun onBackButtonClicked() {
        if (hasModifiedReviews) {
            triggerEvent(ExitWithResult(Unit))
        } else {
            triggerEvent(Exit)
        }
    }

    private fun reloadReviewsFromCache() {
        launch {
            _reviewList.value = reviewListRepository.getCachedProductReviews()
            productReviewsViewState = productReviewsViewState.copy(
                isEmptyViewVisible = _reviewList.value?.isEmpty() == true
            )
        }
    }

    private fun loadProductReviews() {
        launch {
            // Initial load. Get and show reviewList from the db if any
            val reviewsInDb = reviewListRepository.getCachedProductReviews(navArgs.remoteProductId)
            if (reviewsInDb.isNotEmpty()) {
                _reviewList.value = reviewsInDb
                productReviewsViewState = productReviewsViewState.copy(isSkeletonShown = false)
            } else {
                productReviewsViewState = productReviewsViewState.copy(isSkeletonShown = true)
            }
        }
        fetchProductReviews(navArgs.remoteProductId, loadMore = false)
    }

    private fun fetchProductReviews(
        remoteProductId: Long,
        loadMore: Boolean
    ) {
        fetchingReviewsJob = launch {
            if (networkStatus.isConnected()) {
                if (productReviewsViewState.isUnreadFilterEnabled) {
                    fetchUnreadReviews(loadMore = loadMore, productId = remoteProductId)
                } else {
                    reviewListRepository.fetchProductReviews(
                        loadMore,
                        remoteProductId
                    ).collect { result ->
                        when (result) {
                            ReviewListRepository.FetchReviewsResult.NothingFetched -> {
                                // No action needed
                            }
                            is ReviewListRepository.FetchReviewsResult.NotificationsFetched -> {
                                if (result.requestResult == SUCCESS) {
                                    val reviews = reviewListRepository.getCachedProductReviews()
                                    if (reviews.isNotEmpty()) {
                                        _reviewList.value = reviews
                                    }
                                }
                            }
                            is ReviewListRepository.FetchReviewsResult.ReviewsFetched -> {
                                trackFetchProductReviewsResult(result.requestResult, loadMore)
                                when (result.requestResult) {
                                    SUCCESS,
                                    NO_ACTION_NEEDED -> {
                                        _reviewList.value = reviewListRepository.getCachedProductReviews()
                                    }

                                    ERROR,
                                    API_ERROR,
                                    RETRY -> triggerEvent(ShowSnackbar(R.string.review_fetch_error))
                                }
                            }
                        }
                    }
                }
            } else {
                triggerEvent(ShowSnackbar(R.string.offline_error))
            }

            productReviewsViewState = productReviewsViewState.copy(
                isSkeletonShown = false,
                isLoadingMore = false,
                isRefreshing = false,
                isEmptyViewVisible = _reviewList.value?.isEmpty() == true
            )
        }
    }

    private suspend fun fetchUnreadReviews(loadMore: Boolean, productId: Long) {
        productReviewsViewState = productReviewsViewState.copy(isLoadingMore = loadMore)
        when (reviewListRepository.fetchOnlyUnreadProductReviews(loadMore, productId)) {
            SUCCESS,
            NO_ACTION_NEEDED -> {
                val unreadReviews = reviewListRepository.getCachedUnreadProductReviews()
                _reviewList.value = unreadReviews
            }

            ERROR -> triggerEvent(ShowSnackbar(R.string.review_fetch_error))
            else -> {}
        }
    }

    private fun trackFetchProductReviewsResult(
        result: RequestResult,
        loadMore: Boolean
    ) {
        when (result) {
            SUCCESS -> AnalyticsTracker.track(
                PRODUCT_REVIEWS_LOADED,
                mapOf(
                    AnalyticsTracker.KEY_IS_LOADING_MORE to loadMore
                )
            )

            else -> {
                AnalyticsTracker.track(PRODUCT_REVIEWS_LOAD_FAILED)
            }
        }
    }

    fun onUnreadReviewsFilterChanged(isEnabled: Boolean) {
        fetchingReviewsJob?.cancel()
        productReviewsViewState = productReviewsViewState.copy(
            isUnreadFilterEnabled = isEnabled,
            isSkeletonShown = true
        )
        fetchProductReviews(
            remoteProductId = navArgs.remoteProductId,
            loadMore = false
        )
    }

    @Parcelize
    data class ProductReviewsViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null,
        val isUnreadFilterEnabled: Boolean = false
    ) : Parcelable
}
