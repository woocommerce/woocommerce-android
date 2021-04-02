package com.woocommerce.android.ui.products.reviews

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import com.woocommerce.android.R.string
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.PRODUCTS
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch

@OpenClassOnDebug
class ProductReviewsViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
    private val reviewsRepository: ProductReviewsRepository
) : ScopedViewModel(savedState, dispatchers) {
    private val _reviewList = MutableLiveData<List<ProductReview>>()
    val reviewList: LiveData<List<ProductReview>> = _reviewList

    final val productReviewsViewStateData = LiveDataDelegate(savedState, ProductReviewsViewState())
    private var productReviewsViewState by productReviewsViewStateData

    private val navArgs: ProductReviewsFragmentArgs by savedState.navArgs()

    override fun onCleared() {
        super.onCleared()
        reviewsRepository.onCleanup()
    }

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
            _reviewList.value = reviewsRepository.fetchApprovedProductReviewsFromApi(remoteProductId, loadMore)
        } else {
            // Network is not connected
            triggerEvent(ShowSnackbar(string.offline_error))
        }

        productReviewsViewState = productReviewsViewState.copy(
            isSkeletonShown = false,
            isLoadingMore = false,
            isRefreshing = false,
            isEmptyViewVisible = _reviewList.value?.isEmpty() == true
        )
    }

    @Parcelize
    data class ProductReviewsViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null
    ) : Parcelable

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<ProductReviewsViewModel>
}
