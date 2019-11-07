package com.woocommerce.android.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class ProductVariantsViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val productVariantsRepository: ProductVariantsRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(mainDispatcher) {
    private var remoteProductId = 0L
    val productVariantList = MutableLiveData<List<ProductVariant>>()

    private val _isSkeletonShown = MutableLiveData<Boolean>()
    val isSkeletonShown: LiveData<Boolean> = _isSkeletonShown

    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _exit = SingleLiveEvent<Unit>()
    val exit: LiveData<Unit> = _exit

    fun start(remoteProductId: Long) {
        loadProductVariants(remoteProductId)
    }

    override fun onCleared() {
        super.onCleared()
        productVariantsRepository.onCleanup()
    }

    fun loadProductVariants(remoteProductId: Long) {
        val shouldFetch = remoteProductId != this.remoteProductId
        this.remoteProductId = remoteProductId

        launch {
            val variantsInDb = productVariantsRepository.getProductVariantList(remoteProductId)
            if (variantsInDb.isNullOrEmpty()) {
                _isSkeletonShown.value = true
                fetchProductVariants(remoteProductId)
            } else {
                productVariantList.value = variantsInDb
                if (shouldFetch) {
                    fetchProductVariants(remoteProductId)
                }
            }
            _isSkeletonShown.value = false
        }
    }

    private suspend fun fetchProductVariants(remoteProductId: Long) {
        if (networkStatus.isConnected()) {
            val fetchedVariants = productVariantsRepository.fetchProductVariants(remoteProductId)
            if (fetchedVariants.isNullOrEmpty()) {
                _showSnackbarMessage.value = R.string.product_variants_fetch_product_variants_error
                _exit.call()
            } else {
                productVariantList.value = fetchedVariants
            }
        } else {
            _showSnackbarMessage.value = R.string.offline_error
            _isSkeletonShown.value = false
        }
    }
}
