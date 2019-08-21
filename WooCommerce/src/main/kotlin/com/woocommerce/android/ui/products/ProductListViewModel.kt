package com.woocommerce.android.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@OpenClassOnDebug
class ProductListViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val productRepository: ProductRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(mainDispatcher) {
    private var lastOffset = 0
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

    fun start() {
        loadProducts()
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
    }

    private fun loadProducts(offset: Int = 0) {
        launch {
            if (offset == 0) {
                val productsInDb = productRepository.getProductList()
                if (productsInDb.isEmpty()) {
                    _isSkeletonShown.value = true
                } else {
                    productList.value = productsInDb
                }
            }

            fetchProductList()
        }
    }

    fun loadMoreProducts() {
        if (canLoadMore) {
            val offset = lastOffset + 25
            _isLoadingMore.value = true
            loadProducts(offset)
        }
    }

    fun refreshProducts() {
        _isRefreshing.value = true
        loadProducts()
    }

    private suspend fun fetchProductList(offset: Int = 0) {
        if (networkStatus.isConnected()) {
            val fetchedProducts = productRepository.fetchProductList(offset)
            lastOffset = offset
            canLoadMore = productRepository.canLoadMore
            productList.value = fetchedProducts
        } else {
            _showSnackbarMessage.value = R.string.offline_error
        }

        _isSkeletonShown.value = false
        _isLoadingMore.value = false
        _isRefreshing.value = false
    }
}
