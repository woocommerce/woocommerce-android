package com.woocommerce.android.ui.woopos.home.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosProductsViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {

    private var loadMoreProductsJob: Job? = null

    private val selectedItems: MutableStateFlow<List<WooPosProductsListItem>> =
        savedState.getStateFlow(scope = viewModelScope, initialValue = emptyList(), key = "selectedItems")

    val viewState: StateFlow<WooPosProductsViewState> = productsDataSource.products.map { products ->
        WooPosProductsViewState(
            products = products.map { product ->
                WooPosProductsListItem(
                    productId = product.remoteId,
                    title = product.name,
                    imageUrl = product.firstImageUrl
                )
            }
        )
    }.toStateFlow(WooPosProductsViewState(products = emptyList()))

    init {
        launch {
            productsDataSource.loadProducts()
        }
    }

    fun onEndOfProductsGridReached() {
        loadMoreProductsJob?.cancel()
        loadMoreProductsJob = launch {
            productsDataSource.loadMore()
        }
    }

    fun onItemClicked(itemClicked: WooPosProductsListItem) {
        selectedItems.update { selectedItems ->
            if (selectedItems.contains(itemClicked)) {
                selectedItems - itemClicked
            } else {
                selectedItems + itemClicked
            }
        }
        viewModelScope.launch {
            fromChildToParentEventSender.sendToParent(
                ChildToParentEvent.ProductSelectionChangedInProductSelector(selectedItems.value)
            )
        }
    }
}
