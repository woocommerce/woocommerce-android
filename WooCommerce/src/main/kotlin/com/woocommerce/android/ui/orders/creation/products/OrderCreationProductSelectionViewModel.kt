package com.woocommerce.android.ui.orders.creation.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import com.woocommerce.android.extensions.differsFrom
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.ShowProductVariations
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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
    val productListData: LiveData<List<Product>> = productList.map { products ->
        products.filter { it.isPurchasable && it.status == PUBLISH }
    }

    val isSearchActive
        get() = viewState.isSearchActive ?: false

    val currentQuery
        get() = viewState.query.orEmpty()

    private var searchJob: Job? = null
    private var loadingJob: Job? = null

    private val isLoading
        get() = loadingJob?.isActive == true || searchJob?.isActive == true

    init {
        loadProductList()
    }

    private fun loadProductList(loadMore: Boolean = false) {
        if (loadMore.not()) {
            viewState = viewState.copy(isSkeletonShown = true)
        }
        if (viewState.isSearchActive == true) {
            viewState.query?.let { searchProductList(it, loadMore) }
        } else loadFullProductList(loadMore)
    }

    private fun loadFullProductList(loadMore: Boolean) {
        loadingJob = launch {
            val cachedProducts = productListRepository.getProductList()
                .takeIf { it.isNotEmpty() }
                ?.apply {
                    productList.value = this
                    viewState = viewState.copy(isSkeletonShown = false)
                }

            productListRepository.fetchProductList(loadMore)
                .takeIf {
                    it != cachedProducts
                }
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

    fun searchProductList(query: String, loadMore: Boolean = false) {
        viewState = viewState.copy(query = query)
        searchJob?.cancel()
        searchJob = launch {
            productListRepository.searchProductList(query, loadMore)
                ?.takeIf { query == productListRepository.lastSearchQuery }
                ?.let { handleSearchResult(it, loadMore) }
        }
    }

    private fun handleSearchResult(
        searchResult: List<Product>,
        loadedMore: Boolean
    ) {
        productList.value = productList.value
            ?.takeIf { loadedMore && searchResult differsFrom it }
            ?.let { searchResult + it }
            ?: searchResult
    }

    fun onSearchOpened() {
        productList.value = emptyList()
        viewState = viewState.copy(
            isSearchActive = true
        )
    }

    fun onSearchClosed() {
        launch { searchJob?.cancelAndJoin() }
        viewState = viewState.copy(
            isSearchActive = false,
            query = null
        )
        loadProductList()
    }

    fun onSearchQueryCleared() {
        productList.value = emptyList()
        viewState = viewState.copy(
            query = null
        )
    }

    fun onLoadMoreRequest() {
        if (isLoading || !productListRepository.canLoadMoreProducts) return
        loadProductList(loadMore = true)
    }

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val isSearchActive: Boolean? = null,
        val query: String? = null
    ) : Parcelable

    data class AddProduct(val productId: Long) : MultiLiveEvent.Event()
}
