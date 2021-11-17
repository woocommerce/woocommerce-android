package com.woocommerce.android.ui.products.reviews

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_REVIEWS_LOADED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_REVIEWS_LOAD_FAILED
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.PRODUCTS
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
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
    private val reviewsRepository: ProductReviewsRepository
) : ScopedViewModel(savedState) {
    private val _reviewList = MutableLiveData<List<ProductReview>>()
    val reviewList: LiveData<List<ProductReview>> = _reviewList

    val productReviewsViewStateData = LiveDataDelegate(savedState, ProductReviewsViewState())
    private var productReviewsViewState by productReviewsViewStateData

    private val navArgs: ProductReviewsFragmentArgs by savedState.navArgs()

    init {
        if (_reviewList.value == null) {
            loadProductReviews()
        }
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

    private fun loadProductReviews() {
        // Initial load. Get and show reviewList from the db if any
        val reviewsInDb = reviewsRepository.getProductReviewsFromDB(navArgs.remoteProductId)
        if (reviewsInDb.isNotEmpty()) {
            _reviewList.value = reviewsInDb
            productReviewsViewState = productReviewsViewState.copy(isSkeletonShown = false)
        } else {
            productReviewsViewState = productReviewsViewState.copy(isSkeletonShown = true)
        }

        launch { fetchProductReviews(navArgs.remoteProductId, loadMore = false) }
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
                _reviewList.value = reviewsRepository.getProductReviewsFromDB(remoteProductId)
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

    @Parcelize
    data class ProductReviewsViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null
    ) : Parcelable
}
