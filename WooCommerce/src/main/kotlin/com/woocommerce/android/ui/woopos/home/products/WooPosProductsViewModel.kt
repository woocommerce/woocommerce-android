package com.woocommerce.android.ui.woopos.home.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosProductsViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {

    private var loadMoreProductsJob: Job? = null

    val viewState: StateFlow<WooPosProductsViewState> =
        productsDataSource.products
            .map {
                it.filter { product -> product.price != null }
            }
            .map { products ->
                calculateViewState(products)
            }.toStateFlow(WooPosProductsViewState(products = emptyList()))

    init {
        launch {
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
        }
    }

    private fun calculateViewState(
        products: List<Product>
    ) = WooPosProductsViewState(
        products = products.map { product ->
            WooPosProductsListItem(
                productId = product.remoteId,
                title = product.name,
                imageUrl = product.firstImageUrl
            )
        }
    )

    private fun onEndOfProductsGridReached() {
        loadMoreProductsJob?.cancel()
        loadMoreProductsJob = launch {
            productsDataSource.loadMore()
        }
    }

    private fun onItemClicked(item: WooPosProductsListItem) {
        viewModelScope.launch {
            fromChildToParentEventSender.sendToParent(
                ChildToParentEvent.ItemClickedInProductSelector(item.productId)
            )
        }
    }
}
