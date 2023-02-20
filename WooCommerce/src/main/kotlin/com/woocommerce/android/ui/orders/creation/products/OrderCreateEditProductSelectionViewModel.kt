package com.woocommerce.android.ui.orders.creation.products

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.recyclerview.selection.SelectionTracker
import com.woocommerce.android.AppConstants
import com.woocommerce.android.extensions.differsFrom
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowProductVariations
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderCreateEditProductSelectionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val productListRepository: ProductListRepository
) : ScopedViewModel(savedState) {
    private var selectedProductsList: MutableList<Product>? = mutableListOf()
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    var tracker: SelectionTracker<Long>? = null

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
        // Add selected products from searching to the previously selected products from
        // the product listing screen before search.
        if (!selectedProductsList.isNullOrEmpty()) {
            selectedProductsList!!.addAll(
                productList.value?.filter { product ->
                    tracker?.isSelected(product.remoteId) == true
                } as MutableList<Product>
            )
        }
        loadingJob = launch {
            val cachedProducts = productListRepository.getProductList()
                .takeIf { it.isNotEmpty() }
                ?.apply {
                    if (!selectedProductsList.isNullOrEmpty()) {
                        val newProductList = this.map {
                            selectedProductsList?.forEach { selectedProduct ->
                                if (it.remoteId == selectedProduct.remoteId) {
                                    tracker?.select(it.remoteId)
                                    return@forEach
                                }
                            }
                            it
                        }
                        productList.value = newProductList
                    } else {
                        productList.value = this
                    }
                    viewState = viewState.copy(isSkeletonShown = false)
                }

            productListRepository.fetchProductList(loadMore)
                .takeIf {
                    it != cachedProducts
                }
                ?.let {
                if (!selectedProductsList.isNullOrEmpty()) {
                    val newProductList = productList.value?.map {
                        if (selectedProductsList?.contains(it) == true ) {
                            tracker?.select(it.remoteId)
                            it
                        } else {
                            it
                        }
                    }
                    productList.value = newProductList!!
                } else {
                    productList.value = it
                }
                }

            viewState = viewState.copy(isSkeletonShown = false)
        }
    }

    fun onProductSelected() {
            triggerEvent(
                AddProduct(
                    productList.value!!.filter {
                        tracker?.isSelected(it.remoteId)!!
                    }.map {
                        it.remoteId
                    }
                )
            )
    }

    fun searchProductList(query: String, loadMore: Boolean = false, delayed: Boolean = false) {
        viewState = viewState.copy(query = query, isEmptyViewShowing = false)
        searchJob?.cancel()
        searchJob = launch {
            if (delayed) {
                delay(AppConstants.SEARCH_TYPING_DELAY_MS)
            }
            if (query.isEmpty()) {
//                productList.value = emptyList()
                return@launch
            }
            productListRepository.searchProductList(query, loadMore)
                ?.takeIf { query == productListRepository.lastSearchQuery }
                ?.let { handleSearchResult(it, loadMore) }
        }
    }

    private fun handleSearchResult(
        searchResult: List<Product>,
        loadedMore: Boolean
    ) {
        // store all the selected products before searching
        selectedProductsList = productList.value?.filter { product ->
            tracker?.isSelected(product.remoteId) == true
        } as MutableList<Product>?
        productList.value = (productList.value
            ?.takeIf { loadedMore && searchResult differsFrom it }
            ?.let { searchResult + it }
            ?: searchResult)
        viewState = viewState.copy(isEmptyViewShowing = productListData.value?.isEmpty())
    }

    fun onSearchOpened() {
//        productList.value = emptyList()
        viewState = viewState.copy(
            isSearchActive = true
        )
    }

    fun onSearchClosed() {
        launch { searchJob?.cancelAndJoin() }
        viewState = viewState.copy(
            isSearchActive = false,
            query = null,
            isEmptyViewShowing = false
        )
        loadProductList()
    }

    fun onSearchQueryCleared() {
//        productList.value = emptyList()
        viewState = viewState.copy(
            query = null,
            isEmptyViewShowing = false
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
        val query: String? = null,
        val isEmptyViewShowing: Boolean? = null
    ) : Parcelable

    data class AddProduct(val productId: List<Long>) : MultiLiveEvent.Event()

    object ProductNotFound : MultiLiveEvent.Event()
}
