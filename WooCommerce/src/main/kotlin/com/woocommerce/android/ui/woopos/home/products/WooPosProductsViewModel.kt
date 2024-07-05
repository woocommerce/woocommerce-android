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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosProductsViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val priceFormat: WooPosFormatPrice,
) : ViewModel() {
    private var loadMoreProductsJob: Job? = null

    private val _viewState = MutableStateFlow<WooPosProductsViewState>(WooPosProductsViewState.Loading)
    val viewState: StateFlow<WooPosProductsViewState> = _viewState

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            productsDataSource.products
                .map { products -> calculateViewState(products) }
                .collect { _viewState.value = it }
        }
        viewModelScope.launch {
            val result = productsDataSource.loadSimpleProducts()
            if (result.isFailure) {
                _viewState.value = WooPosProductsViewState.Error
            }
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
                reloadProducts()
            }
        }
    }

    private fun reloadProducts() {
        viewModelScope.launch {
            val currentState = viewState.value as? WooPosProductsViewState.Content ?: return@launch
            _viewState.value = currentState.copy(refreshingProducts = true)
            productsDataSource.loadSimpleProducts(forceRefreshProducts = true)
            _viewState.value = currentState.copy(refreshingProducts = false)
        }
    }

    private suspend fun calculateViewState(products: List<Product>): WooPosProductsViewState {
        return if (products.isEmpty() && !isRefreshingProducts()) {
            WooPosProductsViewState.Empty
        } else if (products.isEmpty() && isRefreshingProducts()) {
            WooPosProductsViewState.Loading
        } else {
            WooPosProductsViewState.Content(
                products = products.map { product ->
                    WooPosProductsListItem(
                        id = product.remoteId,
                        name = product.name,
                        price = priceFormat(product.price),
                        imageUrl = product.firstImageUrl,
                    )
                },
                loadingMore = false,
                refreshingProducts = false,
            )
        }
    }

    private fun isRefreshingProducts(): Boolean {
        return viewState.value is WooPosProductsViewState.Content &&
            (viewState.value as WooPosProductsViewState.Content).refreshingProducts
    }

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

    private fun onItemClicked(item: WooPosProductsListItem) {
        viewModelScope.launch {
            fromChildToParentEventSender.sendToParent(
                ChildToParentEvent.ItemClickedInProductSelector(item.id)
            )
        }
    }
}
