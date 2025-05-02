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
import com.woocommerce.android.ui.woopos.home.items.WooPosItemNavigationData.VariableProductData
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsSearchHelper
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ItemAddedToCart.WooPosItemSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ItemsNextPageLoaded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.PreSearchRecentTermTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
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

@HiltViewModel
class WooPosItemsSearchViewModel @Inject constructor(
    private val emptyStateRepository: WooPosItemsSearchEmptyStateRepository,
    private val priceFormat: WooPosFormatPrice,
    private val dataSource: WooPosSearchProductsDataSource,
    private val childToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val navigator: WooPosItemsNavigator,
    private val searchHelper: WooPosItemsSearchHelper,
    private val analyticsTracker: WooPosAnalyticsTracker,
) : ViewModel() {
    private val _viewState =
        MutableStateFlow<WooPosItemsSearchViewState>(WooPosItemsSearchViewState.Empty)
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
            is WooPosItemsSearchUiEvent.OnItemClicked -> handleItemClicked(event.item, WooPosItemSource.SEARCH_RESULT)
            WooPosItemsSearchUiEvent.LoadingErrorRetryButtonClicked -> {
                val currentState = _viewState.value as? WooPosItemsSearchViewState.Error ?: return
                performSearch(currentState.searchQuery)
            }

            is WooPosItemsSearchUiEvent.OnRecentSearchClicked -> {
                viewModelScope.launch {
                    childToParentEventSender.sendToParent(
                        RecentSearchSelected(
                            event.recentSearch
                        )
                    )
                }
            }

            is WooPosItemsSearchUiEvent.OnPopularItemClicked -> {
                viewModelScope.launch {
                    emptyStateRepository.addPopularItemsToCache()
                    handleItemClicked(event.item, WooPosItemSource.POPULAR_PRODUCTS)
                    trackPopularItemClicked()
                }
            }
        }
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
            val result = dataSource.searchRemoteProducts(query)

            if (query != currentQuery.get()) {
                childToParentEventSender.sendToParent(ChildToParentEvent.SearchEvent.Finished)
                return@launch
            }

            if (result.isSuccess) {
                val products = result.getOrThrow()
                if (products.isEmpty()) {
                    _viewState.value = WooPosItemsSearchViewState.Empty
                } else {
                    _viewState.value = products.toContentState(searchQuery = query)
                }
            } else {
                _viewState.value = WooPosItemsSearchViewState.Error(searchQuery = query)
            }

            childToParentEventSender.sendToParent(ChildToParentEvent.SearchEvent.Finished)
        }
    }

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
                trackItemsNextPageLoaded()
                result.getOrThrow().toContentState(
                    searchQuery = currentState.searchQuery,
                )
            } else {
                currentState.copy(paginationState = WooPosPaginationState.Error)
            }
        }
    }

    private suspend fun trackItemsNextPageLoaded() {
        val event = ItemsNextPageLoaded.apply {
            addProperties(
                mapOf(
                    "item_list_type" to "products",
                    "search" to "true"
                )
            )
        }
        analyticsTracker.track(event)
    }

    private suspend fun trackPopularItemClicked() {
        val event = PreSearchRecentTermTapped.apply {
            addProperties(
                mapOf(
                    "item_list_type" to "products",
                )
            )
        }
        analyticsTracker.track(event)
    }

    private fun handleItemClicked(item: WooPosItemSelectionViewState, source: WooPosItemSource) {
        when (item) {
            is WooPosItemSelectionViewState.Product.Simple -> {
                viewModelScope.launch {
                    childToParentEventSender.sendToParent(
                        ChildToParentEvent.ItemClickedInProductSelector(
                            itemData = ItemClickedData.Product.Simple(id = item.id),
                            source = source,
                        )
                    )
                }

                storeRecentSearch()
            }

            is WooPosItemSelectionViewState.Product.Variable -> {
                viewModelScope.launch {
                    navigator.sendNavigationEvent(
                        NavigateToVariationsScreen(
                            VariableProductData(
                                id = item.id,
                                name = item.name,
                                numOfVariations = item.numOfVariations,
                                source = WooPosItemSource.SEARCH_RESULT
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
