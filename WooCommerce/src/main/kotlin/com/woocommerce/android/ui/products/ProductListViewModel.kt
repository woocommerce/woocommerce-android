package com.woocommerce.android.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@OpenClassOnDebug
class ProductListViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val productRepository: ProductListRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(mainDispatcher) {
    private var canLoadMore = true
    val productList = MutableLiveData<List<Product>>()

    private val _isSkeletonShown = MutableLiveData<Boolean>()
    val isSkeletonShown: LiveData<Boolean> = _isSkeletonShown

    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    final var searchQuery: String? = null
        private set(value) {
            field = value
        }

    fun start() {
        loadProducts()
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
    }

    fun loadProducts(loadMore: Boolean = false) {
        launch {
            _isLoadingMore.value = loadMore
            searchQuery = null
            // since this is the initial load, first get the products from the db and if there are any show them
            // immediately, otherwise make sure the skeleton shows
            if (!loadMore) {
                val productsInDb = productRepository.getProductList()
                if (productsInDb.isEmpty()) {
                    _isSkeletonShown.value = true
                } else {
                    productList.value = productsInDb
                }
            }
            fetchProductList(loadMore)
        }
    }

    fun loadMoreProducts() {
        if (!productRepository.canLoadMoreProducts) {
            WooLog.d(WooLog.T.PRODUCTS, "can't load more products")
            return
        }

        if (searchQuery == null) {
            loadProducts(true)
        } else {
            searchProducts(searchQuery!!, true)
        }
    }

    fun searchProducts(query: String, loadMore: Boolean = false) {
        launch {
            searchQuery = query
            _isLoadingMore.value = loadMore

            if (!loadMore) {
                _isSkeletonShown.value = true
            }

            searchProductList(query, loadMore)
        }
    }

    fun refreshProducts() {
        _isRefreshing.value = true
        if (searchQuery == null) {
            loadProducts()
        } else {
            searchProducts(searchQuery!!)
        }
    }

    private suspend fun fetchProductList(loadMore: Boolean = false) {
        if (networkStatus.isConnected()) {
            val fetchedProducts = productRepository.fetchProductList(loadMore)
            canLoadMore = productRepository.canLoadMoreProducts
            productList.value = fetchedProducts
        } else {
            _showSnackbarMessage.value = R.string.offline_error
        }

        _isSkeletonShown.value = false
        _isLoadingMore.value = false
        _isRefreshing.value = false
    }

    private suspend fun searchProductList(query: String, loadMore: Boolean = false) {
        if (networkStatus.isConnected()) {
            val fetchedProducts = productRepository.searchProductList(query, loadMore)
            canLoadMore = productRepository.canLoadMoreProducts
            // if we loaded more, add the fetched products to the existing list
            if (loadMore) {
                val allProducts = ArrayList<Product>()
                allProducts.addAll(productList.value!!)
                allProducts.addAll(fetchedProducts)
                productList.value = allProducts
            } else {
                productList.value = fetchedProducts
            }
        } else {
            _showSnackbarMessage.value = R.string.offline_error
        }

        _isSkeletonShown.value = false
        _isLoadingMore.value = false
        _isRefreshing.value = false
    }
}
