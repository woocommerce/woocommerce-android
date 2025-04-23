package com.woocommerce.android.ui.woopos.home.items.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemNavigationData.VariableProductData
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosProductsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ProductsPullToRefreshTriggered
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosProductsViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val priceFormat: WooPosFormatPrice,
    private val navigator: WooPosItemsNavigator,
    private val analyticsTracker: WooPosAnalyticsTracker,
) : ViewModel() {
    private var loadMoreProductsJob: Job? = null

    private val _viewState = MutableStateFlow<WooPosProductsViewState>(
        WooPosProductsViewState.Loading()
    )

    val viewState: StateFlow<WooPosProductsViewState> = _viewState
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    init {
        loadProducts(
            forceRefreshProducts = false,
            withPullToRefresh = false,
        )
    }

    fun onUIEvent(event: WooPosProductsUIEvent) {
        when (event) {
            is WooPosProductsUIEvent.EndOfItemsListReached -> {
                onEndOfProductsListReached()
            }

            is WooPosProductsUIEvent.ItemClicked -> {
                handleItemClick(event)
            }

            WooPosProductsUIEvent.PullToRefreshTriggered -> {
                loadProducts(
                    forceRefreshProducts = true,
                    withPullToRefresh = true,
                )
                viewModelScope.launch { analyticsTracker.track(ProductsPullToRefreshTriggered) }
            }

            WooPosProductsUIEvent.ProductsLoadingErrorRetryButtonClicked -> {
                loadProducts(
                    forceRefreshProducts = false,
                    withPullToRefresh = false,
                )
            }
        }
    }

    private fun handleItemClick(event: WooPosProductsUIEvent.ItemClicked) {
        when (event.item) {
            is WooPosItemSelectionViewState.Product.Simple -> {
                onItemClicked(
                    ItemClickedData.Product.Simple(
                        id = event.item.id
                    )
                )
            }

            is WooPosItemSelectionViewState.Product.Variable -> {
                viewModelScope.launch {
                    navigator.sendNavigationEvent(
                        NavigateToVariationsScreen(
                            VariableProductData(
                                id = event.item.id,
                                name = event.item.name,
                                numOfVariations = event.item.numOfVariations,
                            )
                        )
                    )
                }
            }

            is WooPosItemSelectionViewState.Product.Variation -> error("Variation item not supported in products list")
        }
    }

    private fun loadProducts(
        forceRefreshProducts: Boolean,
        withPullToRefresh: Boolean,
    ) {
        viewModelScope.launch {
            _viewState.value = if (withPullToRefresh) {
                buildProductsReloadingState()
            } else {
                WooPosProductsViewState.Loading()
            }

            productsDataSource.loadProducts(forceRefreshProducts = forceRefreshProducts).collect { result ->
                when (result) {
                    is WooPosProductsDataSource.ProductsResult.Cached -> {
                        if (result.products.isNotEmpty()) {
                            _viewState.value = result.products.toContentState()
                        }
                    }

                    is WooPosProductsDataSource.ProductsResult.Remote -> {
                        _viewState.value = when {
                            result.productsResult.isSuccess -> {
                                val products = result.productsResult.getOrThrow()
                                if (products.isNotEmpty()) {
                                    val currentState = _viewState.value
                                    val paginationState = if (loadMoreProductsJob?.isActive == true) {
                                        WooPosPaginationState.Loading
                                    } else {
                                        WooPosPaginationState.None
                                    }
                                    if (currentState is WooPosProductsViewState.Content) {
                                        currentState.copy(
                                            items = products.map { it.toItemSelectionViewState() },
                                            paginationState = paginationState,
                                            pullToRefreshState = WooPosPullToRefreshState.Enabled
                                        )
                                    } else {
                                        products.toContentState(
                                            paginationState = paginationState,
                                        )
                                    }
                                } else {
                                    WooPosProductsViewState.Empty()
                                }
                            }

                            else -> WooPosProductsViewState.Error()
                        }
                    }
                }
            }
        }
    }

    private fun buildProductsReloadingState() =
        when (val state = viewState.value) {
            is WooPosProductsViewState.Content -> state.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
            is WooPosProductsViewState.Loading -> state.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
            is WooPosProductsViewState.Error -> state.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
            is WooPosProductsViewState.Empty -> state.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
        }

    private suspend fun List<Product>.toContentState(
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) = WooPosProductsViewState.Content(
        items = map { it.toItemSelectionViewState() },
        paginationState = paginationState,
        pullToRefreshState = WooPosPullToRefreshState.Enabled,
    )

    private suspend fun Product.toItemSelectionViewState(): WooPosItemSelectionViewState {
        return if (this.isVariable()) {
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
    }

    private fun onEndOfProductsListReached() {
        val currentState = _viewState.value
        if (currentState !is WooPosProductsViewState.Content) {
            return
        }

        if (!productsDataSource.hasMorePages) {
            return
        }

        _viewState.value = currentState.copy(paginationState = WooPosPaginationState.Loading)

        loadMoreProductsJob?.cancel()
        loadMoreProductsJob = viewModelScope.launch {
            val result = productsDataSource.loadMore()
            _viewState.value = if (result.isSuccess) {
                result.getOrThrow().toContentState()
            } else {
                currentState.copy(paginationState = WooPosPaginationState.Error)
            }
        }
    }

    private fun onItemClicked(itemData: ItemClickedData) {
        sendEventToParent(ChildToParentEvent.ItemClickedInProductSelector(itemData))
    }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch { fromChildToParentEventSender.sendToParent(event) }
    }

    private fun Product.isVariable() =
        productType == ProductType.VARIABLE ||
            productType == ProductType.VARIATION
}
