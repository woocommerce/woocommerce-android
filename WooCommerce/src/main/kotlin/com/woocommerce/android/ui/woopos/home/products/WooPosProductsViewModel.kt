package com.woocommerce.android.ui.woopos.home.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosProductsViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val priceFormat: WooPosFormatPrice,
) : ViewModel() {
    private var loadMoreProductsJob: Job? = null

    private val _viewState = MutableStateFlow<WooPosProductsViewState>(WooPosProductsViewState.Loading())
    val viewState: StateFlow<WooPosProductsViewState> = _viewState
        .onEach { notifyParentAboutStatusChange(it) }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    init {
        listenToProducts()
    }

    private fun listenToProducts() {
        viewModelScope.launch {
            productsDataSource.products
                .map { products -> calculateViewState(products) }
                .collect { _viewState.value = it }
        }
        viewModelScope.launch {
            loadProducts(withPullToRefresh = false)
        }
    }

    fun onUIEvent(event: WooPosProductsUIEvent) {
        when (event) {
            is WooPosProductsUIEvent.EndOfProductListReached -> {
                onEndOfProductsListReached()
            }

            is WooPosProductsUIEvent.ItemClicked -> {
                onItemClicked(event.item)
            }

            WooPosProductsUIEvent.PullToRefreshTriggered -> {
                loadProducts(withPullToRefresh = true)
            }

            WooPosProductsUIEvent.ProductsLoadingErrorRetryButtonClicked -> {
                loadProducts(withPullToRefresh = false)
            }
        }
    }

    private fun loadProducts(withPullToRefresh: Boolean) {
        viewModelScope.launch {
            _viewState.value = if (withPullToRefresh) {
                buildProductsReloadingState(isReloading = true)
            } else {
                WooPosProductsViewState.Loading()
            }

            val result = productsDataSource.loadSimpleProducts(forceRefreshProducts = true)

            _viewState.value = if (result.isFailure) {
                WooPosProductsViewState.Error()
            } else {
                if (withPullToRefresh) {
                    buildProductsReloadingState(isReloading = false)
                } else {
                    WooPosProductsViewState.Unknown
                }
            }
        }
    }

    private fun buildProductsReloadingState(isReloading: Boolean) =
        when (val state = viewState.value) {
            is WooPosProductsViewState.Content -> state.copy(reloadingProductsWithPullToRefresh = isReloading)
            is WooPosProductsViewState.Loading -> state.copy(reloadingProductsWithPullToRefresh = isReloading)
            is WooPosProductsViewState.Error -> state.copy(reloadingProductsWithPullToRefresh = isReloading)
            is WooPosProductsViewState.Empty -> state.copy(reloadingProductsWithPullToRefresh = isReloading)
            WooPosProductsViewState.Unknown -> WooPosProductsViewState.Unknown
        }

    private suspend fun calculateViewState(products: List<Product>): WooPosProductsViewState =
        when {
            products.isEmpty() -> when {
                viewState.value is WooPosProductsViewState.Loading -> viewState.value
                viewState.value.reloadingProductsWithPullToRefresh -> viewState.value
                else -> WooPosProductsViewState.Empty()
            }
            else -> products.toContentState()
        }

    private suspend fun List<Product>.toContentState() = WooPosProductsViewState.Content(
        products = map { product ->
            WooPosProductsListItem(
                id = product.remoteId,
                name = product.name,
                price = priceFormat(product.price),
                imageUrl = product.firstImageUrl,
            )
        },
        loadingMore = false,
        reloadingProductsWithPullToRefresh = false,
    )

    private fun onEndOfProductsListReached() {
        val currentState = _viewState.value
        if (currentState !is WooPosProductsViewState.Content) {
            return
        }

        if (!productsDataSource.hasMorePages) {
            return
        }

        _viewState.value = currentState.copy(loadingMore = true)

        loadMoreProductsJob?.cancel()
        loadMoreProductsJob = viewModelScope.launch {
            productsDataSource.loadMore()
        }
    }

    private fun notifyParentAboutStatusChange(newState: WooPosProductsViewState) {
        sendEventToParent(
            when (newState) {
                is WooPosProductsViewState.Content -> ChildToParentEvent.ProductsStatusChanged.WithCart

                WooPosProductsViewState.Unknown,
                is WooPosProductsViewState.Empty,
                is WooPosProductsViewState.Error,
                is WooPosProductsViewState.Loading -> ChildToParentEvent.ProductsStatusChanged.FullScreen
            }
        )
    }

    private fun onItemClicked(item: WooPosProductsListItem) {
        sendEventToParent(ChildToParentEvent.ItemClickedInProductSelector(item.id))
    }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch { fromChildToParentEventSender.sendToParent(event) }
    }
}
