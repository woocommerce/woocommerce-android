package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.woocommerce.android.R.string
import kotlinx.coroutines.launch

@OpenClassOnDebug
class ProductSelectionListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
    private val productRepository: ProductListRepository
) : ScopedViewModel(savedState, dispatchers) {
    private val navArgs: ProductSelectionListFragmentArgs by savedState.navArgs()
    private val excludedProductIds = listOf(navArgs.remoteProductId)

    private val _productList = MutableLiveData<List<Product>>()
    val productList: LiveData<List<Product>> = _productList

    final val productSelectionListViewStateLiveData = LiveDataDelegate(savedState, ProductSelectionListViewState())
    private var productSelectionListViewState by productSelectionListViewStateLiveData

    private val isRefreshing
        get() = productSelectionListViewState.isRefreshing == true

    private val isLoading
        get() = productSelectionListViewState.isLoading == true

    private var loadJob: Job? = null

    init {
        if (_productList.value == null) {
            loadProducts()
        }
    }

    private final fun loadProducts(loadMore: Boolean = false) {
        if (isLoading) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading products")
            return
        }

        if (loadMore && !productRepository.canLoadMoreProducts) {
            WooLog.d(WooLog.T.PRODUCTS, "can't load more products")
            return
        }

        // if a fetch is already active, wait for it to finish before we start another one
        waitForExistingLoad()

        loadJob = launch {
            val showSkeleton: Boolean
            if (loadMore) {
                showSkeleton = false
            } else {
                // if this is the initial load, first get the products from the db and show them immediately
                val productsInDb = productRepository.getProductList(
                    excludedProductIds = excludedProductIds
                )
                if (productsInDb.isEmpty()) {
                    showSkeleton = true
                } else {
                    _productList.value = productsInDb
                    showSkeleton = !isRefreshing
                }
            }
            productSelectionListViewState = productSelectionListViewState.copy(
                isLoading = true,
                isLoadingMore = loadMore,
                isSkeletonShown = showSkeleton,
                isEmptyViewVisible = false
            )
            fetchProductList(loadMore = loadMore)
        }
    }

    /**
     * If products are already being fetched, wait for the existing job to finish
     */
    private fun waitForExistingLoad() {
        if (loadJob?.isActive == true) {
            launch {
                try {
                    loadJob?.join()
                } catch (e: CancellationException) {
                    WooLog.d(WooLog.T.PRODUCTS, "CancellationException while waiting for existing fetch")
                }
            }
        }
    }

    private suspend fun fetchProductList(loadMore: Boolean = false) {
        if (networkStatus.isConnected()) {
            _productList.value = productRepository.fetchProductList(
                loadMore, excludedProductIds = excludedProductIds
            )

            productSelectionListViewState = productSelectionListViewState.copy(
                isLoading = true,
                canLoadMore = productRepository.canLoadMoreProducts,
                isEmptyViewVisible = _productList.value?.isEmpty() == true
            )
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }

        productSelectionListViewState = productSelectionListViewState.copy(
            isSkeletonShown = false,
            isLoading = false,
            isLoadingMore = false,
            isRefreshing = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
    }

    @Parcelize
    data class ProductSelectionListViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoading: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val canLoadMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductSelectionListViewModel>
}
