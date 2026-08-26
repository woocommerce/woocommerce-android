package com.woocommerce.android.ui.products.list

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.IsScreenInTwoPaneLayout
import com.woocommerce.android.analytics.deviceTypeToAnalyticsString
import com.woocommerce.android.extensions.EXPAND_COLLAPSE_ANIMATION_DURATION_MILLIS
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.list.ProductListEvent.ScrollToTop
import com.woocommerce.android.ui.products.list.ProductListEvent.ShowAddProductBottomSheet
import com.woocommerce.android.ui.products.list.ProductListEvent.ShowProductFilterScreen
import com.woocommerce.android.ui.products.list.ProductListEvent.ShowProductSortingBottomSheet
import com.woocommerce.android.ui.products.list.ProductListEvent.ShowUpdateDialog
import com.woocommerce.android.util.IsWindowClassLargeThanCompact
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
@Suppress("LargeClass")
class ProductListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val productRepository: ProductListRepository,
    private val networkStatus: NetworkStatus,
    mediaFileUploadHandler: MediaFileUploadHandler,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val isWindowClassLargeThanCompact: IsWindowClassLargeThanCompact,
) : ScopedViewModel(savedState) {
    var productHasChanges: Boolean = false
    private val _productList = MutableLiveData<List<Product>>()
    val productList: LiveData<List<Product>> = _productList

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateLiveData = LiveDataDelegate(savedState, ProductListViewState())
    private var viewState by viewStateLiveData

    private val productFilterOptions: MutableMap<WCProductStore.ProductFilterOption, String> by lazy {
        val params = savedState.get<MutableMap<WCProductStore.ProductFilterOption, String>>(KEY_PRODUCT_FILTER_OPTIONS)
            ?: mutableMapOf()
        savedState[KEY_PRODUCT_FILTER_OPTIONS] = params
        params
    }

    private var selectedCategoryName: String? = null
    private var searchJob: Job? = null
    private var loadJob: Job? = null
    private val savedSelectedProductIds = savedState.getStateFlow(
        scope = this,
        initialValue = linkedSetOf<Long>(),
        key = KEY_SELECTED_PRODUCT_IDS
    )
    val selectedProductIds: StateFlow<Set<Long>> = savedSelectedProductIds
    private var hasReconciledSelectionAfterAuthoritativeLoad = false
    val selectedProductIdOnBigScreen = savedState.getNullableStateFlow(
        scope = this,
        initialValue = null,
        clazz = Long::class.java,
        key = KEY_PRODUCT_SELECTED_ON_BIG_SCREEN
    )

    private val isLoading
        get() = viewState.isLoading == true

    private val isTrashing
        get() = viewState.isTrashing == true

    init {
        EventBus.getDefault().register(this)
        if (_productList.value == null) {
            loadProducts()
        }
        viewState = viewState.copy(sortingTitleResource = getSortingTitle())

        selectedCategoryName = savedState.get<String>(KEY_PRODUCT_FILTER_SELECTED_CATEGORY_NAME)

        // Reload products if any image changes occur
        mediaFileUploadHandler.observeProductImageChanges()
            .onEach { loadProducts() }
            .launchIn(this)
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
        EventBus.getDefault().unregister(this)
    }

    fun isSearching() = viewState.isSearchActive == true

    fun isSelecting() = selectedProductIds.value.isNotEmpty()

    fun isSkuSearch() = isSearching() && viewState.isSkuSearch

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
                searchJob?.cancelAndJoin()

                updateProductList(emptyList())
                viewState = viewState.copy(
                    isEmptyViewVisible = false,
                    isAddProductButtonVisible = false
                )
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
            stockStatus?.let { productFilterOptions[WCProductStore.ProductFilterOption.STOCK_STATUS] = it }
            productStatus?.let { productFilterOptions[WCProductStore.ProductFilterOption.STATUS] = it }
            productType?.let { productFilterOptions[WCProductStore.ProductFilterOption.TYPE] = it }
            productCategory?.let { productFilterOptions[WCProductStore.ProductFilterOption.CATEGORY] = it }
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
        return stockStatus != productFilterOptions[WCProductStore.ProductFilterOption.STOCK_STATUS] ||
            productStatus != productFilterOptions[WCProductStore.ProductFilterOption.STATUS] ||
            productType != productFilterOptions[WCProductStore.ProductFilterOption.TYPE] ||
            productCategory != productFilterOptions[WCProductStore.ProductFilterOption.CATEGORY]
    }

    fun onFiltersButtonTapped() {
        analyticsTracker.track(AnalyticsEvent.PRODUCT_LIST_VIEW_FILTER_OPTIONS_TAPPED)
        triggerEvent(
            ShowProductFilterScreen(
                productFilterOptions[WCProductStore.ProductFilterOption.STOCK_STATUS],
                productFilterOptions[WCProductStore.ProductFilterOption.TYPE],
                productFilterOptions[WCProductStore.ProductFilterOption.STATUS],
                productFilterOptions[WCProductStore.ProductFilterOption.CATEGORY],
                selectedCategoryName
            )
        )
    }

    fun onSortButtonTapped() {
        analyticsTracker.track(AnalyticsEvent.PRODUCT_LIST_VIEW_SORTING_OPTIONS_TAPPED)
        triggerEvent(ShowProductSortingBottomSheet)
    }

    fun onRefreshRequested() {
        analyticsTracker.track(AnalyticsEvent.PRODUCT_LIST_PULLED_TO_REFRESH)
        refreshProducts()
    }

    fun onAddProductButtonClicked() {
        launch {
            analyticsTracker.track(
                AnalyticsEvent.PRODUCT_LIST_ADD_PRODUCT_BUTTON_TAPPED,
                mapOf(
                    AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to IsScreenInTwoPaneLayout(
                        isWindowClassLargeThanCompact()
                    ).deviceTypeToAnalyticsString
                )
            )
            triggerEvent(ShowAddProductBottomSheet)
        }
    }

    fun onSearchButtonClicked() {
        analyticsTracker.track(AnalyticsEvent.PRODUCT_LIST_MENU_SEARCH_TAPPED)
        onSearchOpened()
    }

    fun onSearchOpened() {
        viewState = viewState.copy(
            isSearchActive = true,
            displaySortAndFilterCard = false,
            isAddProductButtonVisible = false
        )
        updateProductList(emptyList())
    }

    fun onSearchClosed() {
        launch {
            searchJob?.cancelAndJoin()
            viewState = viewState.copy(
                query = null,
                isSearchActive = false,
                isEmptyViewVisible = false,
                displaySortAndFilterCard = true
            )
            loadProducts()
        }
    }

    fun onLoadMoreRequested() {
        loadProducts(loadMore = true)
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
        if (viewState.query.orEmpty().isNotEmpty()) {
            refreshProducts(shouldTrackSearch = true)
        }
    }

    fun onBarcodeScannerClicked() {
        analyticsTracker.track(AnalyticsEvent.PRODUCT_LIST_PRODUCT_BARCODE_SCANNING_TAPPED)
        triggerEvent(ProductListEvent.ShowBarcodeScanner)
    }

    fun reloadProductsFromDb(excludeProductId: Long? = null) {
        launch {
            val excludedProductIds: List<Long>? = excludeProductId?.let { id ->
                ArrayList<Long>().also { it.add(id) }
            }
            val products = productRepository.getProductList(productFilterOptions, excludedProductIds.orEmpty())

            resetOpenProductIfNotInList(products)

            updateProductList(products)

            viewState = viewState.copy(
                isEmptyViewVisible = products.isEmpty() && viewState.isSkeletonShown != true,
                /* if there are no products, hide Add Product button and use the empty view's button instead. */
                isAddProductButtonVisible = products.isNotEmpty(),
                displaySortAndFilterCard = products.isNotEmpty() || productFilterOptions.isNotEmpty()
            )
        }
    }

    private fun resetOpenProductIfNotInList(products: List<Product>) {
        val isOpenProductInTheList = products.any { selectedProductIdOnBigScreen.value == it.remoteId }
        if (!isOpenProductInTheList) selectedProductIdOnBigScreen.value = null
    }

    fun loadProducts(
        loadMore: Boolean = false,
        scrollToTop: Boolean = false,
        isRefreshing: Boolean = false,
        shouldTrackSearch: Boolean = false,
    ) {
        if (isLoading) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading products")
            return
        }

        if (loadMore && !productRepository.canLoadMoreProducts) {
            resetViewState()
            WooLog.d(WooLog.T.PRODUCTS, "can't load more products")
            return
        }

        if (isSearching()) {
            loadSearchProducts(loadMore, shouldTrackSearch)
        } else {
            loadBrowsingProducts(loadMore, scrollToTop, isRefreshing)
        }
    }

    private fun loadSearchProducts(loadMore: Boolean, shouldTrackSearch: Boolean) {
        val searchQuery = viewState.query
        val isSkuSearch = viewState.isSkuSearch
        // cancel any existing search, then start a new one after a brief delay so we don't actually perform
        // the fetch until the user stops typing
        searchJob?.cancel()
        searchJob = launch {
            delay(AppConstants.SEARCH_TYPING_DELAY_MS)
            if (checkConnection()) {
                viewState = viewState.copy(
                    isLoading = true,
                    isLoadingMore = loadMore,
                    isSkeletonShown = !loadMore,
                    isEmptyViewVisible = false,
                    displaySortAndFilterCard = false,
                    isAddProductButtonVisible = false,
                )
                if (shouldTrackSearch && !loadMore) {
                    trackSearch(searchQuery.orEmpty(), isSkuSearch)
                }
                fetchProductList(
                    searchQuery,
                    skuSearchOptions = if (isSkuSearch) {
                        WCProductStore.SkuSearchOptions.PartialMatch
                    } else {
                        WCProductStore.SkuSearchOptions.Disabled
                    },
                    loadMore = loadMore
                )
            } else {
                resetViewState()
            }
        }
    }

    private fun loadBrowsingProducts(loadMore: Boolean, scrollToTop: Boolean, isRefreshing: Boolean) {
        // if a fetch is already active, wait for it to finish before we start another one
        waitForExistingLoad()

        loadJob = launch {
            val showSkeleton: Boolean
            if (loadMore || isTrashing) {
                showSkeleton = false
            } else {
                // if this is the initial load, first get the products from the db and show them immediately
                val productsInDb = productRepository.getProductList(productFilterOptions)
                if (productsInDb.isEmpty()) {
                    showSkeleton = true
                } else {
                    updateProductList(productsInDb)
                    showSkeleton = false
                }
            }
            if (checkConnection()) {
                viewState = viewState.copy(
                    isLoading = true,
                    isLoadingMore = loadMore,
                    isSkeletonShown = showSkeleton,
                    isEmptyViewVisible = false,
                    isRefreshing = isRefreshing,
                    displaySortAndFilterCard = !showSkeleton,
                    isAddProductButtonVisible = shouldShowAddProductButton()
                )
                fetchProductList(loadMore = loadMore, scrollToTop = scrollToTop)
            } else {
                resetViewState()
            }
        }
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
        val shouldShowEmptyView = if (isSearching()) {
            viewState.query?.isNotEmpty() == true && _productList.value?.isEmpty() == true
        } else {
            _productList.value?.isEmpty() == true
        }

        viewState = viewState.copy(
            isSkeletonShown = false,
            isLoading = false,
            isLoadingMore = false,
            isRefreshing = false,
            canLoadMore = productRepository.canLoadMoreProducts,
            isEmptyViewVisible = shouldShowEmptyView,
            isAddProductButtonVisible = shouldShowAddProductButton(),
            displaySortAndFilterCard = !isSearching() &&
                (productFilterOptions.isNotEmpty() || _productList.value?.isNotEmpty() == true)
        )
    }

    private fun shouldShowAddProductButton(): Boolean =
        if (_productList.value.isNullOrEmpty()) {
            !viewState.query.isNullOrEmpty() || productFilterOptions.isNotEmpty()
        } else {
            !isSearching()
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

    fun onProductTapped(productId: Long) {
        if (isSelecting()) {
            toggleProductSelection(productId)
        } else {
            onOpenProduct(productId)
        }
    }

    fun onProductLongPressed(productId: Long) {
        if (productId !in selectedProductIds.value) {
            updateSelection(selectedProductIds.value + productId)
        }
    }

    fun toggleProductSelection(productId: Long) {
        val updatedIds = if (productId in selectedProductIds.value) {
            selectedProductIds.value - productId
        } else {
            selectedProductIds.value + productId
        }
        updateSelection(updatedIds)
    }

    private fun openFirstLoadedProductOnTablet(products: List<Product>) {
        if (isWindowClassLargeThanCompact()) {
            if (products.isNotEmpty()) {
                if (selectedProductIdOnBigScreen.value == null) {
                    val firstProductId = products.first().remoteId
                    selectedProductIdOnBigScreen.value = firstProductId
                    onOpenProduct(firstProductId)
                }
            } else {
                // Opening an empty product causes the search input to lose focus
                if (!isSearching()) {
                    triggerEvent(ProductListEvent.OpenEmptyProduct)
                }
            }
        }
    }

    fun onOpenProduct(productId: Long) {
        if (productHasChanges && isWindowClassLargeThanCompact()) {
            triggerEvent(
                ProductListEvent.ShowDiscardProductChangesConfirmationDialog(
                    productId,
                    getProduct(productId)?.name.orEmpty()
                )
            )
            return
        }

        analyticsTracker.track(
            AnalyticsEvent.PRODUCT_LIST_PRODUCT_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to IsScreenInTwoPaneLayout(
                    isWindowClassLargeThanCompact()
                ).deviceTypeToAnalyticsString
            )
        )

        if (isWindowClassLargeThanCompact()) {
            selectedProductIdOnBigScreen.value = productId
        }
        triggerEvent(ProductListEvent.OpenProduct(productId))
    }

    fun onSelectAllProductsClicked() {
        analyticsTracker.track(AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_SELECT_ALL_TAPPED)
        updateSelection(_productList.value.orEmpty().map { it.remoteId })
    }

    fun exitSelectionMode() {
        updateSelection(emptyList())
    }

    private fun refreshProducts(
        scrollToTop: Boolean = false,
        shouldTrackSearch: Boolean = false,
    ) {
        if (checkConnection()) {
            loadProducts(
                scrollToTop = scrollToTop,
                isRefreshing = true,
                shouldTrackSearch = shouldTrackSearch,
            )
        } else {
            resetViewState()
        }
    }

    @Suppress("NestedBlockDepth")
    private suspend fun fetchProductList(
        searchQuery: String? = null,
        skuSearchOptions: WCProductStore.SkuSearchOptions = WCProductStore.SkuSearchOptions.Disabled,
        loadMore: Boolean = false,
        scrollToTop: Boolean = false
    ) {
        if (!isSearching()) {
            val productList = productRepository.fetchProductList(loadMore, productFilterOptions).onFailure {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.product_list_fetch_error))
            }.getOrNull()
            // don't update the product list if a search was initiated while fetching
            if (isSearching()) {
                WooLog.i(WooLog.T.PRODUCTS, "Search initiated while fetching products")
            } else if (productList != null) {
                updateProductList(productList)
                if (!loadMore) {
                    reconcileSelectionAfterAuthoritativeLoad(productList)
                }
            }
        } else if (searchQuery?.isNotEmpty() == true) {
            productRepository.searchProductList(
                searchQuery = searchQuery,
                skuSearchOptions = skuSearchOptions,
                loadMore = loadMore,
                productFilterOptions = productFilterOptions
            )?.let { products ->
                // make sure the search query hasn't changed while the fetch was processing
                if (searchQuery == productRepository.lastSearchQuery &&
                    skuSearchOptions == productRepository.lastIsSkuSearch
                ) {
                    if (loadMore) {
                        updateProductList(
                            (_productList.value.orEmpty() + products).distinctBy(Product::remoteId)
                        )
                    } else {
                        updateProductList(products)
                    }
                } else {
                    WooLog.d(WooLog.T.PRODUCTS, "Search query changed")
                }
            }
        }

        if (scrollToTop) {
            triggerEvent(ScrollToTop)
        }

        resetViewState()
    }

    private fun getSortingTitle(): Int {
        return when (productRepository.productSortingChoice) {
            WCProductStore.ProductSorting.DATE_ASC -> R.string.product_list_sorting_oldest_to_newest_short
            WCProductStore.ProductSorting.DATE_DESC -> R.string.product_list_sorting_newest_to_oldest_short
            WCProductStore.ProductSorting.TITLE_DESC -> R.string.product_list_sorting_z_to_a_short
            WCProductStore.ProductSorting.TITLE_ASC -> R.string.product_list_sorting_a_to_z_short
            WCProductStore.ProductSorting.POPULARITY_ASC, WCProductStore.ProductSorting.POPULARITY_DESC ->
                error("Invalid sorting choice ${productRepository.productSortingChoice}")
        }
    }

    /**
     * Returns true if the network is connected, otherwise shows an offline snackbar and returns false
     */
    private fun checkConnection(): Boolean {
        return if (networkStatus.isConnected()) {
            true
        } else {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
            false
        }
    }

    fun getProduct(remoteProductId: Long) = productRepository.getProduct(remoteProductId)

    fun trashProduct(remoteProductId: Long) {
        if (checkConnection()) {
            loadJob = launch {
                viewState = viewState.copy(isTrashing = true)
                val successfullyTrashed = productRepository.trashProduct(remoteProductId)
                if (successfullyTrashed) {
                    fetchProductList(loadMore = false, scrollToTop = false)
                    viewState = viewState.copy(isTrashing = false)
                } else {
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.product_trash_error))
                }
            }
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshProducts(event: OnProductSortingChanged) {
        viewState = viewState.copy(sortingTitleResource = getSortingTitle())
        refreshProducts(scrollToTop = true)
    }

    fun onUpdateStatusConfirmed(
        selectedProductsRemoteIds: List<Long>,
        newStatus: ProductStatus,
    ) {
        val productIdsSnapshot = selectedProductsRemoteIds.toList()
        analyticsTracker.track(
            AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_CONFIRMED,
            mapOf(
                AnalyticsTracker.KEY_PROPERTY to AnalyticsTracker.VALUE_STATUS,
                AnalyticsTracker.KEY_SELECTED_PRODUCTS_COUNT to productIdsSnapshot.size
            )
        )
        bulkUpdateProducts(
            update = { productRepository.bulkUpdateProductsStatus(productIdsSnapshot, newStatus) },
            onSuccess = {
                analyticsTracker.track(
                    AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_SUCCESS,
                    mapOf(AnalyticsTracker.KEY_PROPERTY to AnalyticsTracker.VALUE_STATUS)
                )
            },
            onFailure = {
                analyticsTracker.track(
                    AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_FAILURE,
                    mapOf(AnalyticsTracker.KEY_PROPERTY to AnalyticsTracker.VALUE_STATUS)
                )
            },
            successMessage = R.string.product_bulk_update_status_updated
        )
    }

    fun onUpdatePriceConfirmed(
        selectedProductsRemoteIds: List<Long>,
        newPrice: String,
    ) {
        val productIdsSnapshot = selectedProductsRemoteIds.toList()
        analyticsTracker.track(
            AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_CONFIRMED,
            mapOf(
                AnalyticsTracker.KEY_PROPERTY to AnalyticsTracker.VALUE_PRICE,
                AnalyticsTracker.KEY_SELECTED_PRODUCTS_COUNT to productIdsSnapshot.size
            )
        )
        bulkUpdateProducts(
            update = { productRepository.bulkUpdateProductsPrice(productIdsSnapshot, newPrice) },
            onSuccess = {
                analyticsTracker.track(
                    AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_SUCCESS,
                    mapOf(AnalyticsTracker.KEY_PROPERTY to AnalyticsTracker.VALUE_PRICE)
                )
            },
            onFailure = {
                analyticsTracker.track(
                    AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_FAILURE,
                    mapOf(AnalyticsTracker.KEY_PROPERTY to AnalyticsTracker.VALUE_PRICE)
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
                        event = MultiLiveEvent.Event.ShowSnackbar(successMessage),
                        delay = EXPAND_COLLAPSE_ANIMATION_DURATION_MILLIS
                    )
                }

                else -> {
                    exitSelectionMode()
                    onFailure()
                    triggerEventWithDelay(
                        event = MultiLiveEvent.Event.ShowSnackbar(R.string.error_generic),
                        delay = EXPAND_COLLAPSE_ANIMATION_DURATION_MILLIS
                    )
                }
            }
            viewState = viewState.copy(isRefreshing = false)
        }
    }

    fun onBulkUpdatePriceClicked(selectedProductsRemoteIds: List<Long>) {
        val productIdsSnapshot = selectedProductsRemoteIds.toList()
        analyticsTracker.track(
            AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_REQUESTED,
            mapOf(
                AnalyticsTracker.KEY_PROPERTY to AnalyticsTracker.VALUE_PRICE,
                AnalyticsTracker.KEY_SELECTED_PRODUCTS_COUNT to productIdsSnapshot.size
            )
        )
        triggerEvent(ShowUpdateDialog.Price(productIdsSnapshot))
    }

    fun onBulkUpdateStatusClicked(selectedProductsRemoteIds: List<Long>) {
        val productIdsSnapshot = selectedProductsRemoteIds.toList()
        analyticsTracker.track(
            AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_REQUESTED,
            mapOf(
                AnalyticsTracker.KEY_PROPERTY to AnalyticsTracker.VALUE_STATUS,
                AnalyticsTracker.KEY_SELECTED_PRODUCTS_COUNT to productIdsSnapshot.size
            )
        )
        triggerEvent(ShowUpdateDialog.Status(productIdsSnapshot))
    }

    fun onBulkUpdateStockStatusClicked(selectedProductsRemoteIds: List<Long>) {
        val productIdsSnapshot = selectedProductsRemoteIds.toList()
        analyticsTracker.track(
            AnalyticsEvent.PRODUCT_LIST_BULK_UPDATE_REQUESTED,
            mapOf(
                AnalyticsTracker.KEY_PROPERTY to AnalyticsTracker.VALUE_STOCK_STATUS,
                AnalyticsTracker.KEY_SELECTED_PRODUCTS_COUNT to productIdsSnapshot.size
            )
        )
        triggerEvent(ProductListEvent.ShowProductUpdateStockStatusScreen(productIdsSnapshot))
    }

    fun isSquarePluginActive(): Boolean {
        val plugin = wooCommerceStore.getActiveSitePlugin(
            site = selectedSite.get(),
            plugin = WooCommerceStore.WooPlugin.WOO_SQUARE
        )
        return plugin != null
    }

    private fun updateSelection(productIds: Collection<Long>) {
        savedSelectedProductIds.value = LinkedHashSet(productIds)
    }

    private fun updateProductList(products: List<Product>) {
        _productList.value = products
        openFirstLoadedProductOnTablet(products)
    }

    private fun reconcileSelectionAfterAuthoritativeLoad(products: List<Product>) {
        if (hasReconciledSelectionAfterAuthoritativeLoad) return

        hasReconciledSelectionAfterAuthoritativeLoad = true
        val loadedProductIds = products.mapTo(mutableSetOf()) { it.remoteId }
        updateSelection(selectedProductIds.value.filterTo(linkedSetOf()) { it in loadedProductIds })
    }

    private fun trackSearch(query: String, isSkuSearch: Boolean) {
        val searchFilter = if (isSkuSearch) {
            AnalyticsTracker.VALUE_SEARCH_SKU
        } else {
            AnalyticsTracker.VALUE_SEARCH_ALL
        }
        analyticsTracker.track(
            AnalyticsEvent.PRODUCT_LIST_SEARCHED,
            mapOf(
                AnalyticsTracker.KEY_SEARCH to query,
                AnalyticsTracker.KEY_SEARCH_FILTER to searchFilter
            )
        )
    }

    object OnProductSortingChanged

    companion object {
        private const val KEY_PRODUCT_FILTER_OPTIONS = "key_product_filter_options"
        private const val KEY_PRODUCT_FILTER_SELECTED_CATEGORY_NAME = "key_product_filter_selected_category_name"
        private const val KEY_PRODUCT_SELECTED_ON_BIG_SCREEN = "key_product_selected_on_big_screen"
        private const val KEY_SELECTED_PRODUCT_IDS = "key_selected_product_ids"
    }
}
