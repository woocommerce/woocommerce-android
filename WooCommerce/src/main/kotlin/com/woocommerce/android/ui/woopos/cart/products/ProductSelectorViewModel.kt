package com.woocommerce.android.ui.woopos.cart.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductSelectorViewModel @Inject constructor(
    private val productsDataSource: ProductsDataSource,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {

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
        launch {
            productsDataSource.loadProducts()
        }
    }

    fun onLoadMore() {
        launch {
            productsDataSource.loadMore()
        }
    }

    data class ViewState(
        val products: List<ListItem>,
    )

    data class ListItem(
        val productId: Long,
        val title: String,
        val imageUrl: String? = null,
    )
}
