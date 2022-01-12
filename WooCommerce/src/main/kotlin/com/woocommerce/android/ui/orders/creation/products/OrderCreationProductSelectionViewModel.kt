package com.woocommerce.android.ui.orders.creation.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.OrderCreationNavigationTarget.ShowProductVariations
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderCreationProductSelectionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val productListRepository: ProductListRepository
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val productList = MutableLiveData<List<Product>>()
    val productListData: LiveData<List<Product>> = productList

    init {
        loadProductList()
    }

    fun loadProductList(
        loadMore: Boolean = false
    ) {
        if (loadMore.not()) {
            viewState = viewState.copy(isSkeletonShown = true)
        }

        launch {
            val cachedProducts = productListRepository.getProductList()
                .takeIf { it.isNotEmpty() }
                ?.apply {
                    productList.value = this
                    viewState = viewState.copy(isSkeletonShown = false)
                }

            productListRepository.fetchProductList(loadMore)
                .takeIf { it != cachedProducts }
                ?.let { productList.value = it }

            viewState = viewState.copy(isSkeletonShown = false)
        }
    }

    fun onProductSelected(productId: Long) {
        val product = productList.value!!.first { it.remoteId == productId }
        if (product.numVariations == 0) {
            triggerEvent(AddProduct(productId))
        } else {
            triggerEvent(ShowProductVariations(productId))
        }
    }

    fun onSearchQuerySubmitted(query: String, loadMore: Boolean) {
        viewState = viewState.copy(query = query)
        launch {
            productListRepository.searchProductList(query)
                ?.takeIf { query == productListRepository.lastSearchQuery }
                ?.let { searchResult ->
                    productList.value = productList.value
                        ?.takeIf { loadMore && it.isNotEmpty() }
                        ?.let { searchResult + it }
                        ?: searchResult
                }
        }

    }

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val isSearchActive: Boolean? = null,
        val query: String? = null
    ) : Parcelable

    data class AddProduct(val productId: Long) : MultiLiveEvent.Event()
}
