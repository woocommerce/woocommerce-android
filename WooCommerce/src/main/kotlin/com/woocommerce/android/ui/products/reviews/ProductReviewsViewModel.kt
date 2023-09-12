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
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.ReviewModerationConsumer
import com.woocommerce.android.ui.reviews.ReviewModerationHandler
import com.woocommerce.android.ui.reviews.observeModerationEvents
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.PRODUCTS
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCProductStore.OnProductReviewChanged
import javax.inject.Inject

@HiltViewModel
class ProductReviewsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val reviewsRepository: ProductReviewsRepository,
    private val reviewModerationHandler: ReviewModerationHandler,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState), ReviewModerationConsumer {
    private val _reviewList = MutableLiveData<List<ProductReview>>()

    override val ReviewModerationConsumer.reviewModerationHandler: ReviewModerationHandler
        get() = this@ProductReviewsViewModel.reviewModerationHandler

    override val ReviewModerationConsumer.rawReviewList: LiveData<List<ProductReview>>
        get() = _reviewList

    val productReviewsViewStateData = LiveDataDelegate(savedState, ProductReviewsViewState())
    private var productReviewsViewState by productReviewsViewStateData

    private val navArgs: ProductReviewsFragmentArgs by savedState.navArgs()

    private var hasModifiedReviews: Boolean = false

    init {
        if (_reviewList.value == null) {
            loadProductReviews()
        }
        launch { observeModerationEvents() }
    }

    fun refreshProductReviews() {
        productReviewsViewState = productReviewsViewState.copy(isRefreshing = true)
        launch { fetchProductReviews(remoteProductId = navArgs.remoteProductId, loadMore = false) }
    }

    fun loadMoreReviews() {
        if (!reviewsRepository.canLoadMore) {
            WooLog.d(PRODUCTS, "No more reviews to load for product: ${navArgs.remoteProductId}")
            return
        }

        productReviewsViewState = productReviewsViewState.copy(isLoadingMore = true)
        launch { fetchProductReviews(remoteProductId = navArgs.remoteProductId, loadMore = true) }
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
            _reviewList.value = reviewsRepository.getProductReviewsFromDB(navArgs.remoteProductId)
            productReviewsViewState = productReviewsViewState.copy(
                isEmptyViewVisible = _reviewList.value?.isEmpty() == true
            )
        }
    }

    private fun loadProductReviews() = launch {
        // Initial load. Get and show reviewList from the db if any
        val reviewsInDb = reviewsRepository.getProductReviewsFromDB(navArgs.remoteProductId)
        if (reviewsInDb.isNotEmpty()) {
            _reviewList.value = reviewsInDb
            productReviewsViewState = productReviewsViewState.copy(isSkeletonShown = false)
        } else {
            productReviewsViewState = productReviewsViewState.copy(isSkeletonShown = true)
        }

        fetchProductReviews(navArgs.remoteProductId, loadMore = false)
    }

    private suspend fun fetchProductReviews(
        remoteProductId: Long,
        loadMore: Boolean
    ) {
        if (networkStatus.isConnected()) {
            val result = reviewsRepository.fetchApprovedProductReviewsFromApi(remoteProductId, loadMore)
            trackFetchProductReviewsResult(result, loadMore)
            if (result.isError) {
                triggerEvent(ShowSnackbar(R.string.product_review_list_fetching_failed))
            } else {
                val productReviews = reviewsRepository.getProductReviewsFromDB(remoteProductId)
                if (productReviewsViewState.isUnreadFilterEnabled) {
                    applyUnreadFilter(productReviews)
                } else _reviewList.value = productReviews
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

    private fun trackFetchProductReviewsResult(
        result: OnProductReviewChanged,
        loadMore: Boolean
    ) {
        if (result.isError) {
            AnalyticsTracker.track(
                PRODUCT_REVIEWS_LOAD_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                    AnalyticsTracker.KEY_ERROR_TYPE to result.error?.type?.toString(),
                    AnalyticsTracker.KEY_ERROR_DESC to result.error?.message
                )
            )
        } else {
            AnalyticsTracker.track(
                PRODUCT_REVIEWS_LOADED,
                mapOf(
                    AnalyticsTracker.KEY_IS_LOADING_MORE to loadMore
                )
            )
        }
    }

    fun onUnreadReviewsFilterChanged(isEnabled: Boolean) {
        productReviewsViewState = productReviewsViewState.copy(isUnreadFilterEnabled = isEnabled)
        launch {
            if (isEnabled) {
                applyUnreadFilter(reviewsRepository.getProductReviewsFromDB(navArgs.remoteProductId))
            } else {
                _reviewList.value = reviewsRepository.getProductReviewsFromDB(navArgs.remoteProductId)
            }
        }
    }

    private suspend fun applyUnreadFilter(productReviews: List<ProductReview>) {
        val unreadReviews = productReviews.filter { it.read == false }
        if (shouldLoadMore(unreadReviews)) {
            productReviewsViewState = productReviewsViewState.copy(
                isLoadingMore = unreadReviews.isNotEmpty(),
                isSkeletonShown = unreadReviews.isEmpty()
            )
            if (unreadReviews.isNotEmpty()) _reviewList.value = unreadReviews
            fetchProductReviews(
                remoteProductId = navArgs.remoteProductId,
                loadMore = true
            )
        } else
            _reviewList.value = unreadReviews
    }

    private fun shouldLoadMore(unreadReviews: List<ProductReview>) =
        reviewsRepository.canLoadMore
            && (unreadReviews.size < resourceProvider.getInteger(R.integer.min_number_of_items_to_make_list_scrollable)
            || unreadReviews.size == _reviewList.value?.size)

    @Parcelize
    data class ProductReviewsViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null,
        val isUnreadFilterEnabled: Boolean = false
    ) : Parcelable
}
