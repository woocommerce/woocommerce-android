package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ShowSnackbar
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
    @Assisted savedState: SavedStateHandle,
    dispatchers: CoroutineDispatchers,
    private val productRepository: ProductListRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val SEARCH_TYPING_DELAY_MS = 500L
    }

    final val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        if (viewState == ViewState()) {
            loadProducts()
        }
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
    }

    fun onSearchQueryChanged(query: String) {
        viewState = viewState.copy(query = query, isEmptyViewVisible = false)

        if (query.length > 2) {
            onSearchRequested()
        } else {
            viewState = viewState.copy(productList = emptyList())
        }
    }

    fun onRefreshRequested() {
        AnalyticsTracker.track(Stat.PRODUCT_LIST_PULLED_TO_REFRESH)
        refreshProducts()
    }

    fun onSearchOpened() {
        viewState = viewState.copy(isSearchActive = true, productList = emptyList())
    }

    fun onSearchClosed() {
        viewState = viewState.copy(query = null, isSearchActive = false)
        loadProducts()
    }

    fun onLoadMoreRequested() {
        loadProducts(loadMore = true)
    }

    fun onSearchRequested() {
        AnalyticsTracker.track(Stat.PRODUCT_LIST_SEARCHED,
                mapOf(AnalyticsTracker.KEY_SEARCH to viewState.query)
        )
        loadProducts()
    }

    final fun loadProducts(loadMore: Boolean = false) {
        if (loadMore && !productRepository.canLoadMoreProducts) {
            WooLog.d(WooLog.T.PRODUCTS, "can't load more products")
            return
        }

        if (viewState.isSearchActive == false) {
            if (searchJob?.isActive == true || loadJob?.isActive == true) {
                WooLog.d(WooLog.T.PRODUCTS, "already loading products")
                return
            }

            loadJob = launch {
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
                viewState = viewState.copy(
                        isLoadingMore = loadMore,
                        isSkeletonShown = !loadMore
                )
                fetchProductList(viewState.query, loadMore)
            }
        }
    }

    fun refreshProducts() {
        viewState = viewState.copy(isRefreshing = true)
        loadProducts()
    }

    private suspend fun fetchProductList(searchQuery: String? = null, loadMore: Boolean = false) {
        if (networkStatus.isConnected()) {
            if (searchQuery.isNullOrEmpty()) {
                viewState = viewState.copy(productList = productRepository.fetchProductList(loadMore))
            } else {
                val fetchedProducts = productRepository.searchProductList(searchQuery, loadMore)
                // make sure the search query hasn't changed while the fetch was processing
                if (searchQuery == productRepository.lastSearchQuery) {
                    viewState = if (loadMore) {
                        viewState.copy(productList = viewState.productList.orEmpty() + fetchedProducts)
                    } else {
                        viewState.copy(productList = fetchedProducts)
                    }
                } else {
                    WooLog.d(WooLog.T.PRODUCTS, "Search query changed")
                }
            }
            viewState = viewState.copy(
                    canLoadMore = productRepository.canLoadMoreProducts,
                    isEmptyViewVisible = viewState.productList.isNullOrEmpty()
            )
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }

        viewState = viewState.copy(
                isSkeletonShown = false,
                isLoadingMore = false,
                isRefreshing = false
        )
    }

    @Parcelize
    data class ViewState(
        val productList: List<Product>? = null,
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val canLoadMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val query: String? = null,
        val isSearchActive: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null
    ) : Parcelable

    sealed class ProductListEvent : Event() {
        data class ShowSnackbar(@StringRes val message: Int) : ProductListEvent()
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductListViewModel>
}
