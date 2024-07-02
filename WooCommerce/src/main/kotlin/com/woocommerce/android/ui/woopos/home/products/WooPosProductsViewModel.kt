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
            productsDataSource.loadSimpleProducts()
        }
    }

    fun onUIEvent(event: WooPosProductsUIEvent) {
        when (event) {
            is WooPosProductsUIEvent.EndOfProductsGridReached -> {
                onEndOfProductsGridReached()
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
            if (viewState.value !is WooPosProductsViewState.Content) return@launch
            _viewState.value = (viewState.value as WooPosProductsViewState.Content).copy(refreshingProducts = true)
            productsDataSource.loadSimpleProducts()
            _viewState.value = (viewState.value as WooPosProductsViewState.Content).copy(refreshingProducts = false)
        }
    }

    private suspend fun calculateViewState(
        products: List<Product>
    ) = WooPosProductsViewState.Content(
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

    private fun onEndOfProductsGridReached() {
        val currentState = _viewState.value
        if (currentState !is WooPosProductsViewState.Content) {
            return
        }

        if (!productsDataSource.hasMorePages.get()) {
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
