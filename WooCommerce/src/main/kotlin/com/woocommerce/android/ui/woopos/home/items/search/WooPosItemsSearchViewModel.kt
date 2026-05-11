package com.woocommerce.android.ui.woopos.home.items.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.SearchEvent.RecentSearchSelected
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsSearchHelper
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.home.items.products.SearchProductsResult
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource.SyncStrategy
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogProductSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@HiltViewModel
class WooPosItemsSearchViewModel @Inject constructor(
    private val emptyStateRepository: WooPosItemsSearchEmptyStateRepository,
    private val priceFormat: WooPosFormatPrice,
    private val dataSource: WooPosProductsDataSource,
    private val childToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val searchHelper: WooPosItemsSearchHelper,
    private val analyticsTracker: WooPosItemsSearchAnalyticsTracker,
    private val resourceProvider: ResourceProvider,
    private val wooPosAnalyticsTracker: WooPosAnalyticsTracker,
) : ViewModel() {
    private val _viewState =
        MutableStateFlow<WooPosItemsSearchViewState>(
            WooPosItemsSearchViewState.EmptySearchQuery(
                popularItems = emptyList(),
                recentSearches = emptyList()
            )
        )
    val viewState: StateFlow<WooPosItemsSearchViewState> = _viewState
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    private var loadMoreJob: Job? = null
    private var searchJob: Job? = null

    private val currentQuery = AtomicReference("")

    init {
        setEmptySearchQueryState()
        listenEventsFromParent()
    }

    fun onUIEvent(event: WooPosItemsSearchUiEvent) {
        when (event) {
            WooPosItemsSearchUiEvent.OnNextPageRequested -> onEndOfListReached()
            is WooPosItemsSearchUiEvent.OnItemClicked -> {
                val sourceType = if (analyticsTracker.isProductInTheLocalSearchResult(event.item.id)) {
                    WooPosAnalyticsEventConstant.ItemsListSourceType.SEARCH_RESULT_LOCAL
                } else {
                    WooPosAnalyticsEventConstant.ItemsListSourceType.SEARCH_RESULT
                }
                handleItemClicked(event.item, sourceType)
            }

            WooPosItemsSearchUiEvent.LoadingErrorRetryButtonClicked -> handleLoadingErrorRetryClick()

            is WooPosItemsSearchUiEvent.OnRecentSearchClicked -> {
                viewModelScope.launch {
                    childToParentEventSender.sendToParent(
                        RecentSearchSelected(
                            event.recentSearch
                        )
                    )
                    analyticsTracker.trackRecentSearchSelected()
                }
            }

            is WooPosItemsSearchUiEvent.OnPopularItemClicked -> {
                viewModelScope.launch {
                    emptyStateRepository.addPopularItemsToCache()
                    handleItemClicked(event.item, WooPosAnalyticsEventConstant.ItemsListSourceType.POPULAR_PRODUCTS)
                }
            }

            WooPosItemsSearchUiEvent.OnPullToRefreshTriggered -> handlePullToRefresh()
        }
    }

    private fun handleLoadingErrorRetryClick() {
        val currentState = _viewState.value as? WooPosItemsSearchViewState.Error ?: return

        _viewState.value = WooPosItemsSearchViewState.Loading
        performSearch(currentState.searchQuery)
    }

    private fun performSearch(
        query: String,
        showLoadingState: Boolean = true,
        skipDebounce: Boolean = false,
    ) {
        searchJob?.cancel()
        currentQuery.set(query)

        if (query.isEmpty()) {
            setEmptySearchQueryState()
        } else {
            searchJob = viewModelScope.launch {
                val isRemoteSearch = dataSource.getCurrentSyncStrategy() == SyncStrategy.REMOTE
                if (!skipDebounce && isRemoteSearch) {
                    delay(SEARCH_DEBOUNCING_TIME)
                }
                if (query != currentQuery.get()) return@launch
                if (showLoadingState && isRemoteSearch &&
                    _viewState.value !is WooPosItemsSearchViewState.Content
                ) {
                    _viewState.value = WooPosItemsSearchViewState.Loading
                }

                executeSearch(query, showLoadingState)
            }
        }
    }

    private suspend fun executeSearch(
        query: String,
        showLoadingState: Boolean,
    ) {
        childToParentEventSender.sendToParent(ChildToParentEvent.SearchEvent.Started)

        dataSource.searchProducts(query).collectLatest { searchResult ->
            if (query != currentQuery.get()) {
                childToParentEventSender.sendToParent(ChildToParentEvent.SearchEvent.Finished)
                return@collectLatest
            }

            when (searchResult) {
                is SearchProductsResult.Local -> {
                    handleLocalSearchResult(searchResult, query, showLoadingState)
                }

                is SearchProductsResult.Remote -> {
                    handleRemoteSearchResult(searchResult, query)
                }
            }
        }

        if (query == currentQuery.get()) {
            childToParentEventSender.sendToParent(ChildToParentEvent.SearchEvent.Finished)
            if (_viewState.value is WooPosItemsSearchViewState.Loading) {
                _viewState.value = WooPosItemsSearchViewState.Empty(
                    pullToRefreshState = determinePullToRefreshState()
                )
            }
        }
    }

    private suspend fun handleLocalSearchResult(
        searchResult: SearchProductsResult.Local,
        query: String,
        showLoadingState: Boolean
    ) {
        analyticsTracker.storedLocalSearchResultIds(searchResult.products.map { it.remoteId })
        analyticsTracker.trackLocalSearchResults(
            resultsCount = searchResult.products.size,
            searchTimeMillis = searchResult.searchTimeMillis,
            searchMethod = searchResult.searchMethod,
        )

        if (searchResult.products.isEmpty()) {
            _viewState.value = if (showLoadingState) {
                WooPosItemsSearchViewState.Loading
            } else {
                WooPosItemsSearchViewState.Empty(pullToRefreshState = determinePullToRefreshState())
            }
        } else {
            _viewState.value = searchResult.products.toContentState(searchQuery = query)
        }
    }

    private suspend fun handleRemoteSearchResult(
        searchResult: SearchProductsResult.Remote,
        query: String
    ) {
        if (searchResult.productsResult.isSuccess) {
            val products = searchResult.productsResult.getOrThrow()
            _viewState.value = if (products.isEmpty()) {
                WooPosItemsSearchViewState.Empty(pullToRefreshState = determinePullToRefreshState())
            } else {
                products.toContentState(searchQuery = query)
            }
            analyticsTracker.trackSearchPerformance(searchResult.searchTimeMillis)
        } else {
            _viewState.value = WooPosItemsSearchViewState.Error(searchQuery = query)
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun listenEventsFromParent() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery -> performSearch(event.query)

                    ParentToChildrenEvent.SearchEvent.Started -> Unit
                    ParentToChildrenEvent.SearchEvent.Finished -> Unit
                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> Unit
                    is ParentToChildrenEvent.BarcodeEvent -> Unit
                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> Unit
                    is ParentToChildrenEvent.CheckoutClicked -> Unit
                    is ParentToChildrenEvent.SearchEvent.RecentSearchSelected -> Unit
                    is ParentToChildrenEvent.OrderCreated -> Unit
                    is ParentToChildrenEvent.RemoveCouponsClicked -> Unit
                    is ParentToChildrenEvent.CouponsRemoved -> Unit
                    is ParentToChildrenEvent.RefreshProductList -> Unit
                    is ParentToChildrenEvent.CouponsValidationFailed -> Unit
                    is ParentToChildrenEvent.RemoveProductsClicked -> Unit
                    is ParentToChildrenEvent.ProductsRemoved -> Unit
                    is ParentToChildrenEvent.MissingVariationEvent -> Unit
                    is ParentToChildrenEvent.SettingsEvent -> Unit
                    is ParentToChildrenEvent.CustomAmountSubmitted -> Unit
                    is ParentToChildrenEvent.ItemClickedInItemsList -> {
                        if (event.itemData is ItemClickedData.Product.Variation &&
                            searchHelper.isSearchOpen()
                        ) {
                            storeRecentSearch()
                        }
                    }
                }
            }
        }
    }

    private fun onEndOfListReached() {
        val currentState = _viewState.value
        if (currentState !is WooPosItemsSearchViewState.Content) {
            return
        }

        if (!dataSource.hasMoreSearchPages) {
            return
        }

        _viewState.value = currentState.copy(paginationState = WooPosPaginationState.Loading)

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            val result = dataSource.loadMoreSearchResults(currentState.searchQuery)
            _viewState.value = if (result.isSuccess) {
                analyticsTracker.trackItemsNextPageLoaded()
                result.getOrThrow().toContentState(
                    searchQuery = currentState.searchQuery,
                )
            } else {
                currentState.copy(paginationState = WooPosPaginationState.Error)
            }
        }
    }

    private fun handleItemClicked(
        item: WooPosItemSelectionViewState,
        sourceType: WooPosAnalyticsEventConstant.ItemsListSourceType
    ) {
        trackSearchResultTapped(item)

        when (item) {
            is WooPosItemSelectionViewState.Product.Simple -> {
                viewModelScope.launch {
                    val itemData = ItemClickedData.Product.Simple(id = item.id)
                    childToParentEventSender.sendToParent(
                        ChildToParentEvent.ItemClickedInItemsList(
                            itemData = itemData,
                            eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                                item = itemData,
                                source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                                sourceType = sourceType,
                            ),
                        )
                    )
                }

                storeRecentSearch()
            }

            is WooPosItemSelectionViewState.Product.Variable -> {
                viewModelScope.launch {
                    childToParentEventSender.sendToParent(
                        ChildToParentEvent.ItemClickedInItemsList(
                            itemData = ItemClickedData.VariableProduct(
                                id = item.id,
                                name = item.name,
                                numOfVariations = item.numOfVariations,
                                sourceType = sourceType,
                            ),
                            eventForTracking = null,
                        )
                    )
                }

                storeRecentSearch()
            }

            is WooPosItemSelectionViewState.Product.VariationSearchResult -> {
                handleVariationSearchResultClicked(item, sourceType)
            }

            is WooPosItemSelectionViewState.Product.Variation,
            is WooPosItemSelectionViewState.Coupon -> {
                error("${item::class.simpleName} item click is not supported in search")
            }
        }
    }

    private fun handleVariationSearchResultClicked(
        item: WooPosItemSelectionViewState.Product.VariationSearchResult,
        sourceType: WooPosAnalyticsEventConstant.ItemsListSourceType
    ) {
        viewModelScope.launch {
            val itemData = ItemClickedData.Product.Variation(
                productId = item.productId,
                id = item.id,
            )
            childToParentEventSender.sendToParent(
                ChildToParentEvent.ItemClickedInItemsList(
                    itemData = itemData,
                    eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                        item = itemData,
                        source = WooPosAnalyticsEventConstant.ItemsListSource.VARIATION,
                        sourceType = sourceType,
                    ),
                )
            )
        }
        storeRecentSearch()
    }

    private fun trackSearchResultTapped(item: WooPosItemSelectionViewState) {
        val contentState = _viewState.value as? WooPosItemsSearchViewState.Content ?: return
        val position = contentState.items.indexOfFirst { it.id == item.id }
        if (position < 0) return

        val resultType = when (item) {
            is WooPosItemSelectionViewState.Product.VariationSearchResult -> "variation"
            else -> "product"
        }

        viewModelScope.launch {
            analyticsTracker.trackSearchResultTapped(
                resultPosition = position,
                resultType = resultType,
            )
        }
    }

    private fun storeRecentSearch() {
        (_viewState.value as? WooPosItemsSearchViewState.Content)?.let {
            viewModelScope.launch {
                emptyStateRepository.addRecentSearch(it.searchQuery)
            }
        }
    }

    private suspend fun List<WooPosProductModel>.toContentState(
        searchQuery: String,
        paginationState: WooPosPaginationState = WooPosPaginationState.None,
        pullToRefreshState: WooPosPullToRefreshState = determinePullToRefreshState(),
    ) = WooPosItemsSearchViewState.Content(
        items = map { it.toViewModelProduct() },
        searchQuery = searchQuery,
        paginationState = paginationState,
        pullToRefreshState = pullToRefreshState,
    )

    private fun determinePullToRefreshState(): WooPosPullToRefreshState {
        return when (dataSource.getCurrentSyncStrategy()) {
            SyncStrategy.LOCAL_CATALOG_FILE -> WooPosPullToRefreshState.Enabled
            SyncStrategy.REMOTE -> WooPosPullToRefreshState.Disabled
        }
    }

    private fun handlePullToRefresh() {
        val currentState = _viewState.value
        _viewState.value = when (currentState) {
            is WooPosItemsSearchViewState.Content ->
                currentState.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
            is WooPosItemsSearchViewState.Empty ->
                currentState.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
            else -> return
        }

        viewModelScope.launch {
            wooPosAnalyticsTracker.track(
                WooPosAnalyticsEvent.Event.PullToRefreshTriggered(
                    WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                    WooPosAnalyticsEventConstant.ItemsListSourceType.SEARCH_RESULT
                )
            )

            dataSource.refreshProducts().onSuccess { syncResult ->
                check(syncResult is PosLocalCatalogProductSyncResult) {
                    "PTR should be enabled/available only for Local Catalog: $syncResult"
                }
                when (syncResult.value) {
                    is PosLocalCatalogSyncResult.Success -> {
                        performSearch(
                            query = currentQuery.get(),
                            showLoadingState = false,
                            skipDebounce = true,
                        )
                    }

                    is PosLocalCatalogSyncResult.Failure -> {
                        handlePTRError(syncResult.value)
                    }
                }
            }.onFailure {
                handlePTRError(failure = null)
            }
        }
    }

    private suspend fun handlePTRError(failure: PosLocalCatalogSyncResult.Failure?) {
        val messageResId = if (failure is PosLocalCatalogSyncResult.Failure.NetworkError) {
            R.string.woo_pos_ptr_offline_error
        } else {
            R.string.something_went_wrong_try_again
        }
        childToParentEventSender.sendToParent(
            ChildToParentEvent.ToastMessageDisplayed(
                message = resourceProvider.getString(messageResId)
            )
        )
        val currentState = _viewState.value
        _viewState.value = when (currentState) {
            is WooPosItemsSearchViewState.Content ->
                currentState.copy(pullToRefreshState = determinePullToRefreshState())

            is WooPosItemsSearchViewState.Empty ->
                currentState.copy(pullToRefreshState = determinePullToRefreshState())

            else -> currentState
        }
    }

    private suspend fun WooPosProductModel.toViewModelProduct(): WooPosItemSelectionViewState.Product =
        when (type) {
            is WooPosProductModel.WooPosProductType.Variable -> {
                WooPosItemSelectionViewState.Product.Variable(
                    id = this.remoteId,
                    name = this.name,
                    price = priceFormat(this.pricing.displayPrice),
                    imageUrl = this.firstImageUrl,
                    numOfVariations = this.variationIds.size,
                    variationIds = this.variationIds
                )
            }
            is WooPosProductModel.WooPosProductType.Variation -> {
                WooPosItemSelectionViewState.Product.VariationSearchResult(
                    id = this.remoteId,
                    name = this.name,
                    price = priceFormat(this.pricing.displayPrice),
                    imageUrl = this.firstImageUrl,
                    productId = requireNotNull(this.parentId),
                    parentProductName = requireNotNull(this.parentProductName),
                )
            }
            else -> {
                WooPosItemSelectionViewState.Product.Simple(
                    id = this.remoteId,
                    name = this.name,
                    price = priceFormat(this.pricing.displayPrice),
                    imageUrl = this.firstImageUrl,
                )
            }
        }

    private fun setEmptySearchQueryState() {
        viewModelScope.launch {
            val lastSearchesDeferred = async { emptyStateRepository.getLastSearches() }
            val popularItemsDeferred = async { emptyStateRepository.getPopularItems() }

            _viewState.value = WooPosItemsSearchViewState.EmptySearchQuery(
                popularItems = popularItemsDeferred.await().let { it.take(minOf(MAX_ITEMS_COUNT, it.size)) }
                    .map { it.toViewModelProduct() },
                recentSearches = lastSearchesDeferred.await().let { it.take(minOf(MAX_ITEMS_COUNT, it.size)) },
            )
        }
    }

    private companion object {
        const val MAX_ITEMS_COUNT = 10
        const val SEARCH_DEBOUNCING_TIME = 250L
    }
}
