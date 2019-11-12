package com.woocommerce.android.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Named

class ProductVariantsViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val productVariantsRepository: ProductVariantsRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter
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

    fun refreshProductVariants(remoteProductId: Long) {
        _isRefreshing.value = true
        loadProductVariants(remoteProductId, forceRefresh = true)
    }

    override fun onCleared() {
        super.onCleared()
        productVariantsRepository.onCleanup()
    }

    private fun loadProductVariants(remoteProductId: Long, forceRefresh: Boolean = false) {
        val shouldFetch = remoteProductId != this.remoteProductId
        this.remoteProductId = remoteProductId

        launch {
            val variantsInDb = productVariantsRepository.getProductVariantList(remoteProductId)
            if (variantsInDb.isNullOrEmpty()) {
                _isSkeletonShown.value = true
                fetchProductVariants(remoteProductId)
            } else {
                productVariantList.value = combineData(variantsInDb)
                if (shouldFetch || forceRefresh) {
                    fetchProductVariants(remoteProductId)
                }
            }
        }
    }

    private suspend fun fetchProductVariants(remoteProductId: Long) {
        if (networkStatus.isConnected()) {
            val fetchedVariants = productVariantsRepository.fetchProductVariants(remoteProductId)
            if (fetchedVariants.isNullOrEmpty()) {
                _showSnackbarMessage.value = R.string.product_variants_fetch_product_variants_error
                _exit.call()
            } else {
                productVariantList.value = combineData(fetchedVariants)
            }
        } else {
            _showSnackbarMessage.value = R.string.offline_error
        }
        _isRefreshing.value = false
        _isSkeletonShown.value = false
    }

    private fun combineData(productVariants: List<ProductVariant>): List<ProductVariant> {
        val currencyCode = productVariantsRepository.getCurrencyCode()
        productVariants.map { productVariant ->
            productVariant.priceWithCurrency = currencyCode?.let {
                currencyFormatter.formatCurrency(productVariant.price ?: BigDecimal.ZERO, it)
            } ?: productVariant.price.toString()
        }
        return productVariants
    }
}
