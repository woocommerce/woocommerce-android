package com.woocommerce.android.ui.woopos.home.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductSelectorViewModel @Inject constructor(
    private val productsDataSource: ProductsDataSource,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {

    private var loadProductsJob: Job? = null
    private var loadMoreProductsJob: Job? = null

    val viewState: StateFlow<ViewState> = productsDataSource.products.map { products ->
        ViewState(
            products = products.map { product ->
                ListItem(
                    productId = product.remoteId,
                    title = product.name,
                    imageUrl = product.firstImageUrl
                )
            }
        )
    }.toStateFlow(ViewState(products = emptyList()))

    init {
        loadProductsJob?.cancel()
        loadMoreProductsJob?.cancel()
        launch {
            productsDataSource.loadProducts()
        }
    }

    fun onEndOfProductsGridReached() {
        launch {
            productsDataSource.loadMore()
        }
    }
}
