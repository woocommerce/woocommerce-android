package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateCompletedEvent
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ScrollToTop
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_DESC

class ProductListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val productRepository: ProductListRepository,
    private val networkStatus: NetworkStatus,
    private val prefs: AppPrefs
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val SEARCH_TYPING_DELAY_MS = 500L
        private const val KEY_PRODUCT_FILTER_OPTIONS = "key_product_filter_options"
    }

    private val _productList = MutableLiveData<List<Product>>()
    val productList: LiveData<List<Product>> = _productList

    final val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    private final val productFilterOptions: MutableMap<ProductFilterOption, String> by lazy {
        val params = savedState.get<MutableMap<ProductFilterOption, String>>(KEY_PRODUCT_FILTER_OPTIONS)
                ?: mutableMapOf()
        savedState[KEY_PRODUCT_FILTER_OPTIONS] = params
        params
    }

    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        EventBus.getDefault().register(this)
        if (_productList.value == null) {
            loadProducts()
        }
        viewState = viewState.copy(sortingTitleResource = getSortingTitle())
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
        EventBus.getDefault().unregister(this)
    }

    fun isSearching() = viewState.isSearchActive == true

    private fun isLoading() = viewState.isLoading == true

    private fun isRefreshing() = viewState.isRefreshing == true

    fun getSearchQuery() = viewState.query

    fun onSearchQueryChanged(query: String) {
        viewState = viewState.copy(query = query, isEmptyViewVisible = false)

        if (query.length > 2) {
            onSearchRequested()
        } else {
            launch {
                searchJob?.cancelAndJoin()

                _productList.value = emptyList()
                viewState = viewState.copy(isEmptyViewVisible = false)
            }
        }
    }

    fun onFiltersChanged(
        stockStatus: String?,
        productStatus: String?,
        productType: String?
    ) {
        if (stockStatus != productFilterOptions[ProductFilterOption.STOCK_STATUS] ||
            productStatus != productFilterOptions[ProductFilterOption.STATUS] ||
            productType != productFilterOptions[ProductFilterOption.TYPE]) {
            productFilterOptions.clear()
            stockStatus?.let { productFilterOptions[ProductFilterOption.STOCK_STATUS] = it }
            productStatus?.let { productFilterOptions[ProductFilterOption.STATUS] = it }
            productType?.let { productFilterOptions[ProductFilterOption.TYPE] = it }

            viewState = viewState.copy(filterCount = productFilterOptions.size)
            refreshProducts()
        }
    }

    fun getFilterByStockStatus() = productFilterOptions[ProductFilterOption.STOCK_STATUS]

    fun getFilterByProductStatus() = productFilterOptions[ProductFilterOption.STATUS]

    fun getFilterByProductType() = productFilterOptions[ProductFilterOption.TYPE]

    fun onRefreshRequested() {
        AnalyticsTracker.track(Stat.PRODUCT_LIST_PULLED_TO_REFRESH)
        refreshProducts()
    }

    fun onSearchOpened() {
        _productList.value = emptyList()
        viewState = viewState.copy(isSearchActive = true)
    }

    fun onSearchClosed() {
        launch {
            searchJob?.cancelAndJoin()
            viewState = viewState.copy(query = null, isSearchActive = false, isEmptyViewVisible = false)
            loadProducts()
        }
    }

    fun onLoadMoreRequested() {
        loadProducts(loadMore = true)
    }

    fun onSearchRequested() {
        AnalyticsTracker.track(Stat.PRODUCT_LIST_SEARCHED,
                mapOf(AnalyticsTracker.KEY_SEARCH to viewState.query)
        )
        refreshProducts()
    }

    final fun reloadProductsFromDb(excludeProductId: Long? = null) {
        val excludedProductIds: List<Long>? = excludeProductId?.let { id ->
            ArrayList<Long>().also { it.add(id) }
        }
        _productList.value = productRepository.getProductList(productFilterOptions, excludedProductIds)
    }

    final fun loadProducts(loadMore: Boolean = false, scrollToTop: Boolean = false) {
        if (isLoading()) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading products")
            return
        }

        if (loadMore && !productRepository.canLoadMoreProducts) {
            WooLog.d(WooLog.T.PRODUCTS, "can't load more products")
            return
        }

        if (isSearching()) {
            // cancel any existing search, then start a new one after a brief delay so we don't actually perform
            // the fetch until the user stops typing
            searchJob?.cancel()
            searchJob = launch {
                delay(SEARCH_TYPING_DELAY_MS)
                viewState = viewState.copy(
                        isLoading = true,
                        isLoadingMore = loadMore,
                        isSkeletonShown = !loadMore,
                        isEmptyViewVisible = false,
                        displaySortAndFilterCard = false,
                        isAddProductButtonVisible = false
                )
                fetchProductList(viewState.query, loadMore = loadMore)
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
                    val productsInDb = productRepository.getProductList(productFilterOptions)
                    if (productsInDb.isEmpty()) {
                        showSkeleton = true
                    } else {
                        _productList.value = productsInDb
                        showSkeleton = !isRefreshing()
                    }
                }
                viewState = viewState.copy(
                        isLoading = true,
                        isLoadingMore = loadMore,
                        isSkeletonShown = showSkeleton,
                        isEmptyViewVisible = false,
                        displaySortAndFilterCard = !showSkeleton,
                        isAddProductButtonVisible = !showSkeleton
                )
                fetchProductList(loadMore = loadMore, scrollToTop = scrollToTop)
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

    fun refreshProducts(scrollToTop: Boolean = false) {
        viewState = viewState.copy(isRefreshing = true)
        loadProducts(scrollToTop = scrollToTop)
    }

    private suspend fun fetchProductList(
        searchQuery: String? = null,
        loadMore: Boolean = false,
        scrollToTop: Boolean = false
    ) {
        if (!checkConnection()) {
            return
        }

        if (searchQuery.isNullOrEmpty()) {
            _productList.value = productRepository.fetchProductList(loadMore, productFilterOptions)
        } else {
            productRepository.searchProductList(searchQuery, loadMore)?.let { fetchedProducts ->
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

        viewState = viewState.copy(
            isLoading = true,
            canLoadMore = productRepository.canLoadMoreProducts,
            isEmptyViewVisible = _productList.value?.isEmpty() == true,
            displaySortAndFilterCard = (
                productFilterOptions.isNotEmpty() || _productList.value?.isNotEmpty() == true
                )
        )

        viewState = viewState.copy(
                isSkeletonShown = false,
                isLoading = false,
                isLoadingMore = false,
                isRefreshing = false,
                isAddProductButtonVisible = true
        )

        if (scrollToTop) {
            triggerEvent(ScrollToTop)
        }
    }

    private fun getSortingTitle(): Int {
        return when (productRepository.productSortingChoice) {
            DATE_ASC -> R.string.product_list_sorting_oldest_to_newest_short
            DATE_DESC -> R.string.product_list_sorting_newest_to_oldest_short
            TITLE_DESC -> R.string.product_list_sorting_z_to_a_short
            TITLE_ASC -> R.string.product_list_sorting_a_to_z_short
        }
    }

    /**
     * Returns true if the network is connected, otherwise shows an offline snackbar and returns false
     */
    private fun checkConnection(): Boolean {
        return if (networkStatus.isConnected()) {
            true
        } else {
            triggerEvent(ShowSnackbar(R.string.offline_error))
            false
        }
    }

    fun getProduct(remoteProductId: Long) = productRepository.getProduct(remoteProductId)

    fun trashProduct(remoteProductId: Long) {
        if (checkConnection()) {
            launch {
                if (!productRepository.trashProduct(remoteProductId)) {
                    triggerEvent(ShowSnackbar(R.string.product_trash_error))
                }
            }
        }
    }

    fun isShowProductTypeBottomSheet() = prefs.getSelectedProductType().isEmpty()

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUpdateCompletedEvent) {
        loadProducts()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshProducts(event: OnProductSortingChanged) {
        viewState = viewState.copy(sortingTitleResource = getSortingTitle())
        refreshProducts(scrollToTop = true)
    }

    object OnProductSortingChanged

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoading: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val canLoadMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val query: String? = null,
        val filterCount: Int? = null,
        val isSearchActive: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null,
        val sortingTitleResource: Int? = null,
        val displaySortAndFilterCard: Boolean? = null,
        val isAddProductButtonVisible: Boolean? = null
    ) : Parcelable

    sealed class ProductListEvent : Event() {
        object ScrollToTop : ProductListEvent()
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductListViewModel>
}
