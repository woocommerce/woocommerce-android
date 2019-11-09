package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.viewmodel.SavedState
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OpenClassOnDebug
class ProductListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedState,
    dispatchers: CoroutineDispatchers,
    private val productRepository: ProductListRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val SEARCH_TYPING_DELAY_MS = 500L
    }

    private var canLoadMore = true
    private var isLoadingProducts = false

    final val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    private var searchJob: Job? = null

    fun start(searchQuery: String? = null) {
        loadProducts(searchQuery = searchQuery)
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
    }

    fun loadProducts(searchQuery: String? = null, loadMore: Boolean = false) {
        if (loadMore && !productRepository.canLoadMoreProducts) {
            WooLog.d(WooLog.T.PRODUCTS, "can't load more products")
            return
        }

        if (searchQuery.isNullOrBlank()) {
            if (isLoadingProducts) {
                WooLog.d(WooLog.T.PRODUCTS, "already loading products")
                return
            }

            launch {
                isLoadingProducts = true
                viewState = viewState.copy(isLoadingMore = loadMore)

                if (!loadMore) {
                    // if this is the initial load, first get the products from the db and if there are any show
                    // them immediately, otherwise make sure the skeleton shows
                    val productsInDb = productRepository.getProductList()
                    viewState = if (productsInDb.isEmpty()) {
                        viewState.copy(isSkeletonShown = true)
                    } else {
                        viewState.copy(productList = productsInDb)
                    }
                }

                fetchProductList(loadMore = loadMore)
            }
        } else {
            // cancel any existing search, then start a new one after a brief delay so we don't actually perform
            // the fetch until the user stops typing
            searchJob?.cancel()
            searchJob = launch {
                delay(SEARCH_TYPING_DELAY_MS)
                isLoadingProducts = true
                viewState = viewState.copy(isLoadingMore = loadMore, isSkeletonShown = !loadMore)
                fetchProductList(searchQuery, loadMore)
            }
        }
    }

    fun refreshProducts(searchQuery: String? = null) {
        viewState = viewState.copy(isRefreshing = true)
        loadProducts(searchQuery = searchQuery)
    }

    private suspend fun fetchProductList(searchQuery: String? = null, loadMore: Boolean = false) {
        if (networkStatus.isConnected()) {
            if (searchQuery.isNullOrEmpty()) {
                viewState = viewState.copy(productList = productRepository.fetchProductList(loadMore))
            } else {
                val fetchedProducts = productRepository.searchProductList(searchQuery, loadMore)
                // make sure the search query hasn't changed while the fetch was processing
                if (searchQuery == productRepository.lastSearchQuery) {
                    if (loadMore) {
                        addProducts(fetchedProducts)
                    } else {
                        viewState = viewState.copy(productList = fetchedProducts)
                    }
                } else {
                    WooLog.d(WooLog.T.PRODUCTS, "Search query changed")
                }
            }
            canLoadMore = productRepository.canLoadMoreProducts
        } else {
            triggerEvent(ShowSnackbar(R.string.offline_error))
        }

        viewState = viewState.copy(isSkeletonShown = false, isLoadingMore = false, isRefreshing = false)
        isLoadingProducts = false
    }

    /**
     * Adds the passed list of products to the current list
     */
    private fun addProducts(products: List<Product>) {
        viewState = viewState.copy(productList = viewState.productList.orEmpty() + products)
    }

    @Parcelize
    data class ViewState(
        val productList: List<Product>? = null,
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val isRefreshing: Boolean? = null
    ) : Parcelable

    data class ShowSnackbar(@StringRes val message: Int) : Event()

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductListViewModel>
}
