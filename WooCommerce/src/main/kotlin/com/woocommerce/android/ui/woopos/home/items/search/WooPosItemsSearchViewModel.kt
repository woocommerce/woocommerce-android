package com.woocommerce.android.ui.woopos.home.items.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemNavigationData.VariableProductData
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosItemsSearchViewModel @Inject constructor(
    private val emptyStateRepository: WooPosItemsSearchEmptyStateRepository,
    private val priceFormat: WooPosFormatPrice,
    private val dataSource: WooPosSearchProductsDataSource,
    private val childToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val navigator: WooPosItemsNavigator,
) : ViewModel() {
    private val _viewState =
        MutableStateFlow<WooPosItemsSearchViewState>(WooPosItemsSearchViewState.Empty)
    val viewState: StateFlow<WooPosItemsSearchViewState> = _viewState
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        viewModelScope.launch { setEmptySearchQueryState() }

        listenEventsFromParent()
    }

    fun onUIEvent(event: WooPosItemsSearchUiEvent) {
        when (event) {
            WooPosItemsSearchUiEvent.OnNextPageRequested -> onEndOfListReached()
            is WooPosItemsSearchUiEvent.ItemClicked -> handleItemClicked(event.item)
            WooPosItemsSearchUiEvent.LoadingErrorRetryButtonClicked -> {
                val currentState = _viewState.value as? WooPosItemsSearchViewState.Error ?: return
                viewModelScope.launch {
                    loadContent(currentState.searchQuery)
                }
            }

            is WooPosItemsSearchUiEvent.OnRecentSearchClicked -> {
                viewModelScope.launch {
                    childToParentEventSender.sendToParent(
                        ChildToParentEvent.SearchEvent.RecentSearchSelected(
                            event.recentSearch
                        )
                    )
                }
            }
        }
    }

    private fun listenEventsFromParent() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery -> handleChangedSearchQuery(event)

                    ParentToChildrenEvent.SearchEvent.Started -> Unit
                    ParentToChildrenEvent.SearchEvent.Finished -> Unit
                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> Unit
                    is ParentToChildrenEvent.ItemClickedInProductSelector -> Unit
                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> Unit
                    is ParentToChildrenEvent.CheckoutClicked -> Unit
                    is ParentToChildrenEvent.SearchEvent.RecentSearchSelected -> Unit
                }
            }
        }
    }

    private suspend fun CoroutineScope.handleChangedSearchQuery(
        event: ParentToChildrenEvent.SearchEvent.ChangedQuery
    ) {
        searchJob?.cancel()

        if (event.query.isEmpty()) {
            setEmptySearchQueryState()
        } else {
            searchJob = viewModelScope.launch {
                delay(SEARCH_DEBOUNCING_TIME)

                loadContent(event.query)
            }
        }
    }

    private suspend fun loadContent(searchQuery: String) {
        childToParentEventSender.sendToParent(ChildToParentEvent.SearchEvent.Started)

        dataSource.searchProducts(searchQuery).collect { result ->
            when (result) {
                is WooPosSearchProductsDataSource.ProductsResult.Cached -> {
                    if (result.products.isEmpty()) {
                        _viewState.value = WooPosItemsSearchViewState.Loading
                    } else {
                        _viewState.value = result.products.toContentState(
                            searchQuery = searchQuery,
                        )
                    }
                }

                is WooPosSearchProductsDataSource.ProductsResult.Remote -> {
                    if (result.productsResult.isSuccess) {
                        val products = result.productsResult.getOrThrow()
                        if (products.isEmpty()) {
                            _viewState.value = WooPosItemsSearchViewState.Empty
                        } else {
                            _viewState.value = products.toContentState(
                                searchQuery = searchQuery,
                            )
                        }
                    } else {
                        _viewState.value = WooPosItemsSearchViewState.Error(searchQuery = searchQuery)
                    }
                    childToParentEventSender.sendToParent(ChildToParentEvent.SearchEvent.Finished)
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
                result.getOrThrow().toContentState(
                    searchQuery = currentState.searchQuery,
                )
            } else {
                currentState.copy(paginationState = WooPosPaginationState.Error)
            }
        }
    }

    private fun handleItemClicked(item: WooPosItemSelectionViewState) {
        when (item) {
            is WooPosItemSelectionViewState.Product.Simple -> {
                viewModelScope.launch {
                    childToParentEventSender.sendToParent(
                        ChildToParentEvent.ItemClickedInProductSelector(
                            ItemClickedData.Product.Simple(id = item.id)
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
                            )
                        )
                    )
                }
            }

            is WooPosItemSelectionViewState.Variation -> {
                error("Variation item click is not supported")
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

    private suspend fun CoroutineScope.setEmptySearchQueryState() {
        val lastSearchesDeferred = async { emptyStateRepository.getLastSearches() }
        val popularItemsDeferred = async { emptyStateRepository.getPopularItems() }

        _viewState.value = WooPosItemsSearchViewState.EmptySearchQuery(
            popularItems = popularItemsDeferred.await().let { it.take(minOf(MAX_ITEMS_COUNT, it.size)) }
                .map { it.toViewModelProduct() },
            recentSearches = lastSearchesDeferred.await().let { it.take(minOf(MAX_ITEMS_COUNT, it.size)) },
        )
    }

    private companion object {
        const val MAX_ITEMS_COUNT = 3
        const val SEARCH_DEBOUNCING_TIME = 500L
    }
}
