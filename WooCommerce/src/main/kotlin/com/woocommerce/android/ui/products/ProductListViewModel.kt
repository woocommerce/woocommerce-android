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
    private val lastOffset = -1
    private val productList = MutableLiveData<List<Product>>()

    private val _isSkeletonShown = MutableLiveData<Boolean>()
    val isSkeletonShown: LiveData<Boolean> = _isSkeletonShown

    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    private val _exit = SingleLiveEvent<Unit>()
    val exit: LiveData<Unit> = _exit

    init {
        // TODO ?
    }

    fun start() {
        loadProductList()
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
    }

    private fun loadProductList(offset: Int = 0) {
        launch {
            val productsInDb = productRepository.getProductList()
            val shouldFetch = productsInDb.isEmpty() || offset != lastOffset
            if (shouldFetch) {
                _isSkeletonShown.value = true
                fetchProductList(offset)
            } else {
                _isSkeletonShown.value = false
            }
        }
    }

    private suspend fun fetchProductList(offset: Int = 0) {
        if (networkStatus.isConnected()) {
            val fetchedProducts = productRepository.fetchProductList(offset)
            productList.value = fetchedProducts
        } else {
            _showSnackbarMessage.value = R.string.offline_error
        }
        _isSkeletonShown.value = false
    }
}
