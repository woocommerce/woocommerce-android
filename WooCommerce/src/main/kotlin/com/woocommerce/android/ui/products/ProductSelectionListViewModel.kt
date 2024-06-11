package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

@HiltViewModel
class ProductSelectionListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val productRepository: ProductListRepository
) : ScopedViewModel(savedState) {
    private val navArgs: ProductSelectionListFragmentArgs by savedState.navArgs()

    private val _productList = MutableLiveData<List<Product>>()
    val productList: LiveData<List<Product>> = _productList

    val productSelectionListViewStateLiveData = LiveDataDelegate(savedState, ProductSelectionListViewState())
    private var productSelectionListViewState by productSelectionListViewStateLiveData

    private val isRefreshing
        get() = productSelectionListViewState.isRefreshing == true

    private val isLoading
        get() = productSelectionListViewState.isLoading == true

    private val isSearching
        get() = productSelectionListViewState.isSearchActive == true

    val groupedProductListType
        get() = navArgs.groupedProductListType

    val searchQuery
        get() = productSelectionListViewState.searchQuery

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    private val excludedProductIds
        get() = navArgs.excludedProductIds.toList().plus(navArgs.remoteProductId)

    init {
        if (_productList.value == null) {
            loadProducts()
        }
    }

    fun onLoadMoreRequested() {
        loadProducts(loadMore = true)
    }

    fun onRefreshRequested() {
        productSelectionListViewState = productSelectionListViewState.copy(isRefreshing = true)
        loadProducts()
    }

    fun onDoneButtonClicked(selectedProductIds: List<Long>? = emptyList()) {
        triggerEvent(ExitWithResult(selectedProductIds))
    }

    fun onSearchQueryChanged(query: String) {
        productSelectionListViewState = productSelectionListViewState.copy(
            searchQuery = query, isEmptyViewVisible = false
        )

        if (query.length > 2) {
            onRefreshRequested()
        } else {
            launch {
                searchJob?.cancelAndJoin()

                _productList.value = emptyList()
                productSelectionListViewState = productSelectionListViewState.copy(isEmptyViewVisible = false)
            }
        }
    }

    fun onSearchOpened() {
        _productList.value = emptyList()
        productSelectionListViewState = productSelectionListViewState.copy(isSearchActive = true)
    }

    fun onSearchClosed() {
        launch {
            searchJob?.cancelAndJoin()
            productSelectionListViewState = productSelectionListViewState.copy(
                searchQuery = null,
                isSearchActive = false,
                isEmptyViewVisible = false
            )
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

        if (isSearching) {
            // cancel any existing search, then start a new one after a brief delay so we don't actually perform
            // the fetch until the user stops typing
            searchJob?.cancel()
            searchJob = launch {
                delay(AppConstants.SEARCH_TYPING_DELAY_MS)
                productSelectionListViewState = productSelectionListViewState.copy(
                    isLoading = true,
                    isLoadingMore = loadMore,
                    isSkeletonShown = !loadMore,
                    isEmptyViewVisible = false
                )
                fetchProductList(productSelectionListViewState.searchQuery, loadMore = loadMore)
            }
        } else {
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

    private suspend fun fetchProductList(
        searchQuery: String? = null,
        loadMore: Boolean = false
    ) {
        if (networkStatus.isConnected()) {
            if (searchQuery.isNullOrEmpty()) {
                productRepository.fetchProductList(
                    loadMore,
                    excludedProductIds = excludedProductIds
                ).onFailure {
                    triggerEvent(ShowSnackbar(R.string.product_list_fetch_error))
                }.getOrNull()?.let {
                    _productList.value = it
                }
            } else {
                productRepository.searchProductList(
                    searchQuery = searchQuery,
                    loadMore = loadMore,
                    excludedProductIds = excludedProductIds,
                    skuSearchOptions = WCProductStore.SkuSearchOptions.Disabled
                )?.let { fetchedProducts ->
                    // make sure the search query hasn't changed while the fetch was processing
                    if (searchQuery == productRepository.lastSearchQuery) {
                        if (loadMore) {
                            _productList.value = _productList.value.orEmpty() + fetchedProducts
                        } else {
                            _productList.value = fetchedProducts
                        }
                    } else {
                        WooLog.d(WooLog.T.PRODUCTS, "Search query changed")
                    }
                }
            }

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
        val isEmptyViewVisible: Boolean? = null,
        val searchQuery: String? = null,
        val isSearchActive: Boolean? = null
    ) : Parcelable
}
