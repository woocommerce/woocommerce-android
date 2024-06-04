package com.woocommerce.android.ui.woopos.cartcheckout.products

import androidx.lifecycle.SavedStateHandle
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
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {

    private var loadMoreProductsJob: Job? = null

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
}
