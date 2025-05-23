package com.woocommerce.android.ui.woopos.home.items.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.SearchEvent.RecentSearchSelected
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsSearchHelper
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariableProductNavigationData
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@HiltViewModel
class WooPosItemsSearchViewModel @Inject constructor(
    private val emptyStateRepository: WooPosItemsSearchEmptyStateRepository,
    private val priceFormat: WooPosFormatPrice,
    private val dataSource: WooPosSearchProductsDataSource,
    private val childToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val navigator: WooPosItemsNavigator,
    private val searchHelper: WooPosItemsSearchHelper,
    private val analyticsTracker: WooPosItemsSearchAnalyticsTracker,
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
    private var localSearchJob: Job? = null
    private var remoteSearchJob: Job? = null

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
        }
    }

    private fun handleLoadingErrorRetryClick() {
        val currentState = _viewState.value as? WooPosItemsSearchViewState.Error ?: return

        _viewState.value = WooPosItemsSearchViewState.Loading
        performRemoteSearch(currentState.searchQuery)
    }

    private fun performSearch(query: String) {
        localSearchJob?.cancel()
        remoteSearchJob?.cancel()

        currentQuery.set(query)

        if (query.isEmpty()) {
            setEmptySearchQueryState()
        } else {
            performLocalSearch(query)
            performRemoteSearch(query)
        }
    }

    private fun performLocalSearch(query: String) {
        localSearchJob?.cancel()
        localSearchJob = viewModelScope.launch {
            val localProducts = dataSource.searchLocalProducts(query)

            if (query != currentQuery.get()) return@launch

            analyticsTracker.storedLocalSearchResultIds(localProducts.map { it.remoteId })

            if (localProducts.isEmpty()) {
                _viewState.value = WooPosItemsSearchViewState.Loading
            } else {
                _viewState.value = localProducts.toContentState(
                    searchQuery = query,
                )
            }
        }
    }

    private fun performRemoteSearch(query: String) {
        remoteSearchJob?.cancel()
        remoteSearchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCING_TIME)

            if (query != currentQuery.get()) {
                return@launch
            }

            childToParentEventSender.sendToParent(ChildToParentEvent.SearchEvent.Started)
            var result: Result<List<Product>>
            val searchTimeMillis = measureTimeMillis {
                result = dataSource.searchRemoteProducts(query)
            }

            if (query != currentQuery.get()) {
                childToParentEventSender.sendToParent(ChildToParentEvent.SearchEvent.Finished)
                return@launch
            }

            if (result.isSuccess) {
                val searchResult = result.getOrThrow()
                if (searchResult.isEmpty()) {
                    _viewState.value = WooPosItemsSearchViewState.Empty
                } else {
                    _viewState.value = searchResult.toContentState(searchQuery = query)
                }

                analyticsTracker.trackSearchPerformance(searchTimeMillis)
            } else {
                _viewState.value = WooPosItemsSearchViewState.Error(searchQuery = query)
            }

            childToParentEventSender.sendToParent(ChildToParentEvent.SearchEvent.Finished)
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
                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> Unit
                    is ParentToChildrenEvent.CheckoutClicked -> Unit
                    is ParentToChildrenEvent.SearchEvent.RecentSearchSelected -> Unit
                    is ParentToChildrenEvent.OrderCreated -> Unit
                    is ParentToChildrenEvent.RemoveCouponsClicked -> Unit
                    is ParentToChildrenEvent.CouponsRemoved -> Unit
                    is ParentToChildrenEvent.RefreshProductList -> Unit
                    is ParentToChildrenEvent.CouponsValidationFailed -> Unit
                    is ParentToChildrenEvent.ItemClickedInProductSelector -> {
                        if (event.itemData is ItemClickedData.Product.Variation && searchHelper.isSearchOpen()) {
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

        if (!dataSource.hasMorePages) {
            return
        }

        _viewState.value = currentState.copy(paginationState = WooPosPaginationState.Loading)

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            val result = dataSource.loadMore(query = currentState.searchQuery)
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
        when (item) {
            is WooPosItemSelectionViewState.Product.Simple -> {
                viewModelScope.launch {
                    val itemData = ItemClickedData.Product.Simple(id = item.id)
                    childToParentEventSender.sendToParent(
                        ChildToParentEvent.ItemClickedInProductSelector(
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
                    navigator.sendNavigationEvent(
                        NavigateToVariationsScreen(
                            WooPosVariableProductNavigationData(
                                id = item.id,
                                name = item.name,
                                numOfVariations = item.numOfVariations,
                                sourceType = sourceType,
                            )
                        )
                    )
                }
            }

            is WooPosItemSelectionViewState.Product.Variation -> {
                error("Variation item click is not supported")
            }

            is WooPosItemSelectionViewState.Coupon -> {
                error("Coupon item click is not supported")
            }
        }
    }

    private fun storeRecentSearch() {
        (_viewState.value as? WooPosItemsSearchViewState.Content)?.let {
            viewModelScope.launch {
                emptyStateRepository.addRecentSearch(it.searchQuery)
            }
        }
    }

    private suspend fun List<Product>.toContentState(
        searchQuery: String,
        paginationState: WooPosPaginationState = WooPosPaginationState.None,
    ) = WooPosItemsSearchViewState.Content(
        items = map { it.toViewModelProduct() },
        searchQuery = searchQuery,
        paginationState = paginationState,
    )

    private suspend fun Product.toViewModelProduct(): WooPosItemSelectionViewState.Product =
        if (productType == ProductType.VARIABLE) {
            WooPosItemSelectionViewState.Product.Variable(
                id = this.remoteId,
                name = this.name,
                price = priceFormat(this.price),
                imageUrl = this.firstImageUrl,
                numOfVariations = this.numVariations,
                variationIds = this.variationIds
            )
        } else {
            WooPosItemSelectionViewState.Product.Simple(
                id = this.remoteId,
                name = this.name,
                price = priceFormat(this.price),
                imageUrl = this.firstImageUrl,
            )
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
        const val SEARCH_DEBOUNCING_TIME = 500L
    }
}
