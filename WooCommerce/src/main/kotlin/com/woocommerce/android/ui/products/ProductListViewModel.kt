package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_CONFIRMED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_FAILURE
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_REQUESTED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_SELECT_ALL_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PROPERTY
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SELECTED_PRODUCTS_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PRICE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STATUS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.EXPAND_COLLAPSE_ANIMATION_DURATION_MILLIS
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ScrollToTop
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.SelectProducts
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ShowAddProductBottomSheet
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ShowProductFilterScreen
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ShowProductSortingBottomSheet
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ShowUpdateDialog
import com.woocommerce.android.ui.products.selector.ProductListHandler
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.SkuSearchOptions
import javax.inject.Inject

@HiltViewModel
class ProductListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val productRepository: ProductListRepository,
    private val networkStatus: NetworkStatus,
    private val listHandler: ProductListHandler,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState) {
    companion object {
        private const val KEY_PRODUCT_FILTER_OPTIONS = "key_product_filter_options"
        private const val KEY_PRODUCT_FILTER_SELECTED_CATEGORY_NAME = "key_product_filter_selected_category_name"
    }

     val products = listHandler.productsFlow

    private var fetchProductsJob: Job? = null

    private var loadMoreJob: Job? = null

//    private val _productList = MutableLiveData<List<Product>>()
//    val productList: LiveData<List<Product>> = _productList

    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    private val productFilterOptions: MutableMap<ProductFilterOption, String> by lazy {
        val params = savedState.get<MutableMap<ProductFilterOption, String>>(KEY_PRODUCT_FILTER_OPTIONS)
            ?: mutableMapOf()
        savedState[KEY_PRODUCT_FILTER_OPTIONS] = params
        params
    }

    private var selectedCategoryName: String? = null
//    private var searchJob: Job? = null
//    private var loadJob: Job? = null

    init {
        EventBus.getDefault().register(this)
        launch {
            listHandler.loadFromCacheAndFetch("", emptyMap(),  ProductListHandler.SearchType.DEFAULT)
        }
        viewState = viewState.copy(sortingTitleResource = getSortingTitle())

        selectedCategoryName = savedState.get<String>(KEY_PRODUCT_FILTER_SELECTED_CATEGORY_NAME)
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
        EventBus.getDefault().unregister(this)
    }

    fun isSearching() = viewState.isSearchActive == true

    fun isSelecting() = viewState.productListState == ProductListState.Selecting

    fun isSkuSearch() = isSearching() && viewState.isSkuSearch

    private fun isLoading() = viewState.isLoading == true

    fun getSearchQuery() = viewState.query

    fun onSearchQueryChanged(
        query: String,
    ) {
        // If the view is not searching, ignore this change
        if (!isSearching()) return
        viewState = viewState.copy(
            query = query,
            isEmptyViewVisible = false
        )

        if (query.length > 2) {
            onSearchRequested()
        } else {
            launch {
                fetchProductList(query, skuSearchOptions = SkuSearchOptions.Disabled)
                viewState = viewState.copy(isEmptyViewVisible = false)
            }
        }
    }

    fun onFiltersChanged(
        stockStatus: String?,
        productStatus: String?,
        productType: String?,
        productCategory: String?,
        productCategoryName: String?
    ) {
        if (areFiltersChanged(stockStatus, productStatus, productType, productCategory)) {
            productFilterOptions.clear()
            stockStatus?.let { productFilterOptions[ProductFilterOption.STOCK_STATUS] = it }
            productStatus?.let { productFilterOptions[ProductFilterOption.STATUS] = it }
            productType?.let { productFilterOptions[ProductFilterOption.TYPE] = it }
            productCategory?.let { productFilterOptions[ProductFilterOption.CATEGORY] = it }
            productCategoryName?.let {
                selectedCategoryName = it
                savedState[KEY_PRODUCT_FILTER_SELECTED_CATEGORY_NAME] = it
            }

            viewState = viewState.copy(filterCount = productFilterOptions.size)
            refreshProducts()
        }
    }

    private fun areFiltersChanged(
        stockStatus: String?,
        productStatus: String?,
        productType: String?,
        productCategory: String?
    ): Boolean {
        return stockStatus != productFilterOptions[ProductFilterOption.STOCK_STATUS] ||
            productStatus != productFilterOptions[ProductFilterOption.STATUS] ||
            productType != productFilterOptions[ProductFilterOption.TYPE] ||
            productCategory != productFilterOptions[ProductFilterOption.CATEGORY]
    }

    fun onFiltersButtonTapped() {
        AnalyticsTracker.track(AnalyticsEvent.PRODUCT_LIST_VIEW_FILTER_OPTIONS_TAPPED)
        triggerEvent(
            ShowProductFilterScreen(
                productFilterOptions[ProductFilterOption.STOCK_STATUS],
                productFilterOptions[ProductFilterOption.TYPE],
                productFilterOptions[ProductFilterOption.STATUS],
                productFilterOptions[ProductFilterOption.CATEGORY],
                selectedCategoryName
            )
        )
    }

    fun onSortButtonTapped() {
        AnalyticsTracker.track(AnalyticsEvent.PRODUCT_LIST_VIEW_SORTING_OPTIONS_TAPPED)
        triggerEvent(ShowProductSortingBottomSheet)
    }

    fun onRefreshRequested() {
        AnalyticsTracker.track(AnalyticsEvent.PRODUCT_LIST_PULLED_TO_REFRESH)
        refreshProducts()
    }

    fun onAddProductButtonClicked() {
        launch {
            AnalyticsTracker.track(AnalyticsEvent.PRODUCT_LIST_ADD_PRODUCT_BUTTON_TAPPED)
            triggerEvent(ShowAddProductBottomSheet)
        }
    }

    fun onSearchOpened() {
        viewState = viewState.copy(
            isSearchActive = true,
            displaySortAndFilterCard = false,
            isAddProductButtonVisible = false
        )
    }

    fun onSearchClosed() {
        launch {
            viewState = viewState.copy(
                query = null,
                isSearchActive = false,
                isEmptyViewVisible = false,
                displaySortAndFilterCard = true,
                isAddProductButtonVisible = true
            )
            fetchProductList(null, skuSearchOptions = SkuSearchOptions.Disabled)
        }
    }

    fun onSearchTypeChanged(isSkuSearch: Boolean) {
        viewState = viewState.copy(isSkuSearch = isSkuSearch)
        viewState.query?.let { query ->
            if (query.length > 2) {
                onSearchRequested()
            }
        }
    }

    fun onSearchRequested() {
        val searchFilter = if (viewState.isSkuSearch) {
            AnalyticsTracker.VALUE_SEARCH_SKU
        } else {
            AnalyticsTracker.VALUE_SEARCH_ALL
        }
        AnalyticsTracker.track(
            AnalyticsEvent.PRODUCT_LIST_SEARCHED,
            mapOf(
                AnalyticsTracker.KEY_SEARCH to viewState.query,
                AnalyticsTracker.KEY_SEARCH_FILTER to searchFilter
            )
        )
        refreshProducts()
    }

    /**
     * Resets the view state following a refresh
     */
    private fun resetViewState() {
        // Conditionals for showing / hiding the Add Product FAB:
        // If there are no products:
        // - in default view, hide the Add Product FAB, because the empty view has its own add button.
        // - in search/filter result view, show the Add Product FAB, because the empty view doesn't have add button.
        //
        // If there is at least one product in default or search/filter result view, show the Add Product FAB.

        // TODO: Need to handle this in a better way
//        val shouldShowAddProductButton =
//            if (_productList.value?.isEmpty() == true) {
//                when {
//                    viewState.query != null -> true
//                    productFilterOptions.isNotEmpty() -> true
//                    else -> false
//                }
//            } else {
//                !isSearching() && !isSelecting()
//            }
//
//        val shouldShowEmptyView = if (isSearching()) {
//            viewState.query?.isNotEmpty() == true && _productList.value?.isEmpty() == true
//        } else {
//            _productList.value?.isEmpty() == true
//        }

        viewState = viewState.copy(
            isSkeletonShown = false,
            isLoading = false,
            isLoadingMore = false,
            isRefreshing = false,
            canLoadMore = productRepository.canLoadMoreProducts,
            isEmptyViewVisible = true,
            isAddProductButtonVisible = true,
            displaySortAndFilterCard = !isSearching() &&
                (productFilterOptions.isNotEmpty() || true)
        )
    }

    fun onSelectionChanged(count: Int) {
        when {
            count == 0 -> exitSelectionMode()
            count > 0 && !isSelecting() -> enterSelectionMode(count)
            count > 0 -> viewState = viewState.copy(selectionCount = count)
        }
    }

    fun onRestoreSelection(selectedProductsIds: List<Long>) {
        triggerEvent(SelectProducts(selectedProductsIds))
    }

    fun onSelectAllProductsClicked() {
        analyticsTracker.track(PRODUCT_LIST_BULK_UPDATE_SELECT_ALL_TAPPED)
        throw NotImplementedError()
    }

    fun enterSelectionMode(count: Int) {
        viewState = viewState.copy(
            productListState = ProductListState.Selecting,
            isAddProductButtonVisible = false,
            selectionCount = count
        )
    }

    fun exitSelectionMode() {
        viewState = viewState.copy(
            productListState = ProductListState.Browsing,
            isAddProductButtonVisible = true,
            selectionCount = null
        )
    }

    fun refreshProducts() {
        if (checkConnection()) {
            launch { fetchProductList(null, skuSearchOptions = SkuSearchOptions.Disabled) }
        } else {
            resetViewState()
        }
    }

    @Suppress("NestedBlockDepth")
    private suspend fun fetchProductList(
        searchQuery: String? = null,
        skuSearchOptions: SkuSearchOptions = SkuSearchOptions.Disabled,
        loadMore: Boolean = false,
        scrollToTop: Boolean = false
    ) {
        WooLog.d(WooLog.T.PRODUCTS,"fetchProductList: searchQuery=$searchQuery, skuSearchOptions=$skuSearchOptions, loadMore=$loadMore, scrollToTop=$scrollToTop")
        loadMoreJob?.cancel()
        fetchProductsJob?.cancel()
        fetchProductsJob = viewModelScope.launch {
            // TODO show loading state
//            loadingState.value = ProductSelectorViewModel.LoadingState.LOADING
            listHandler.loadFromCacheAndFetch(
                filters = emptyMap(), // TODO pass filters
                searchQuery = searchQuery ?: "",
                searchType = ProductListHandler.SearchType.DEFAULT, // TODO make sure to pass default vs SKU based on the selected tab.
            ).onFailure {
                val isSearch = searchQuery?.isEmpty() != false
                val message = if (isSearch) R.string.product_selector_loading_failed
                else R.string.product_selector_search_failed
                triggerEvent(ShowSnackbar(message))
            }
            // TODO disable loading state
//            loadingState.value = ProductSelectorViewModel.LoadingState.IDLE
        }

        if (scrollToTop) {
            triggerEvent(ScrollToTop)
        }

        resetViewState()
    }

    fun onLoadMoreRequested() {
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
//            loadingState.value = ProductSelectorViewModel.LoadingState.APPENDING
            listHandler.loadMore()
//            loadingState.value = ProductSelectorViewModel.LoadingState.IDLE
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

    fun trashProduct(remoteProductId: Long) {
        if (checkConnection()) {
            launch {
                if (!productRepository.trashProduct(remoteProductId)) {
                    triggerEvent(ShowSnackbar(R.string.product_trash_error))
                }
            }
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshProducts(event: OnProductSortingChanged) {
        viewState = viewState.copy(sortingTitleResource = getSortingTitle())
        refreshProducts()
    }

    fun onUpdateStatusConfirmed(
        selectedProductsRemoteIds: List<Long>,
        newStatus: ProductStatus,
    ) {
        analyticsTracker.track(
            PRODUCT_LIST_BULK_UPDATE_CONFIRMED,
            mapOf(
                KEY_PROPERTY to VALUE_STATUS,
                KEY_SELECTED_PRODUCTS_COUNT to selectedProductsRemoteIds.size
            )
        )
        bulkUpdateProducts(
            update = { productRepository.bulkUpdateProductsStatus(selectedProductsRemoteIds, newStatus) },
            onSuccess = {
                analyticsTracker.track(
                    PRODUCT_LIST_BULK_UPDATE_SUCCESS,
                    mapOf(KEY_PROPERTY to VALUE_STATUS)
                )
            },
            onFailure = {
                analyticsTracker.track(
                    PRODUCT_LIST_BULK_UPDATE_FAILURE,
                    mapOf(KEY_PROPERTY to VALUE_STATUS)
                )
            },
            successMessage = R.string.product_bulk_update_status_updated
        )
    }

    fun onUpdatePriceConfirmed(
        selectedProductsRemoteIds: List<Long>,
        newPrice: String,
    ) {
        analyticsTracker.track(
            PRODUCT_LIST_BULK_UPDATE_CONFIRMED,
            mapOf(
                KEY_PROPERTY to VALUE_PRICE,
                KEY_SELECTED_PRODUCTS_COUNT to selectedProductsRemoteIds.size
            )
        )
        bulkUpdateProducts(
            update = { productRepository.bulkUpdateProductsPrice(selectedProductsRemoteIds, newPrice) },
            onSuccess = {
                analyticsTracker.track(
                    PRODUCT_LIST_BULK_UPDATE_SUCCESS,
                    mapOf(KEY_PROPERTY to VALUE_PRICE)
                )
            },
            onFailure = {
                analyticsTracker.track(
                    PRODUCT_LIST_BULK_UPDATE_FAILURE,
                    mapOf(KEY_PROPERTY to VALUE_PRICE)
                )
            },
            successMessage = R.string.product_bulk_update_price_updated
        )
    }

    private fun bulkUpdateProducts(
        update: suspend () -> RequestResult,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
        @StringRes successMessage: Int
    ) {
        launch {
            viewState = viewState.copy(isRefreshing = true)
            when (update.invoke()) {
                RequestResult.SUCCESS -> {
                    onSuccess()
                    refreshProducts()
                    exitSelectionMode()
                    triggerEventWithDelay(
                        event = ShowSnackbar(successMessage),
                        delay = EXPAND_COLLAPSE_ANIMATION_DURATION_MILLIS
                    )
                }
                else -> {
                    exitSelectionMode()
                    onFailure()
                    triggerEventWithDelay(
                        event = ShowSnackbar(R.string.error_generic),
                        delay = EXPAND_COLLAPSE_ANIMATION_DURATION_MILLIS
                    )
                }
            }
            viewState = viewState.copy(isRefreshing = false)
        }
    }

    fun onBulkUpdatePriceClicked(selectedProductsRemoteIds: List<Long>) {
        analyticsTracker.track(
            PRODUCT_LIST_BULK_UPDATE_REQUESTED,
            mapOf(
                KEY_PROPERTY to VALUE_PRICE,
                KEY_SELECTED_PRODUCTS_COUNT to selectedProductsRemoteIds.size
            )
        )
        triggerEvent(ShowUpdateDialog.Price(selectedProductsRemoteIds))
    }

    fun onBulkUpdateStatusClicked(selectedProductsRemoteIds: List<Long>) {
        analyticsTracker.track(
            PRODUCT_LIST_BULK_UPDATE_REQUESTED,
            mapOf(
                KEY_PROPERTY to VALUE_STATUS,
                KEY_SELECTED_PRODUCTS_COUNT to selectedProductsRemoteIds.size
            )
        )
        triggerEvent(ShowUpdateDialog.Status(selectedProductsRemoteIds))
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
        val isSkuSearch: Boolean = false,
        val filterCount: Int? = null,
        val isSearchActive: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null,
        val sortingTitleResource: Int? = null,
        val displaySortAndFilterCard: Boolean? = null,
        val isAddProductButtonVisible: Boolean? = null,
        val productListState: ProductListState? = null,
        val selectionCount: Int? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val isBottomNavBarVisible = isSearchActive != true && productListState != ProductListState.Selecting

        @IgnoredOnParcel
        val isFilteringActive = filterCount != null && filterCount > 0
    }

    sealed class ProductListEvent : Event() {
        object ScrollToTop : ProductListEvent()
        object ShowAddProductBottomSheet : ProductListEvent()
        object ShowProductSortingBottomSheet : ProductListEvent()
        data class ShowProductFilterScreen(
            val stockStatusFilter: String?,
            val productTypeFilter: String?,
            val productStatusFilter: String?,
            val productCategoryFilter: String?,
            val selectedCategoryName: String?
        ) : ProductListEvent()
        data class SelectProducts(val productsIds: List<Long>) : ProductListEvent()
        sealed class ShowUpdateDialog : ProductListEvent() {
            abstract val productsIds: List<Long>

            data class Price(override val productsIds: List<Long>) : ShowUpdateDialog()
            data class Status(override val productsIds: List<Long>) : ShowUpdateDialog()
        }
    }

    enum class ProductListState { Selecting, Browsing }
}
