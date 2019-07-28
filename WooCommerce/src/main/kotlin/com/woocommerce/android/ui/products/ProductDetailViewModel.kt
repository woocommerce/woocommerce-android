package com.woocommerce.android.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Named

class ProductDetailViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val productRepository: ProductRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(mainDispatcher) {
    private var remoteProductId = 0L
    private var currencyCode: String? = null
    private var weightUnit: String? = null
    private var dimensionUnit: String? = null

    private val _product = MutableLiveData<Product>()
    val product: LiveData<Product> = _product

    private val _isSkeletonShown = MutableLiveData<Boolean>()
    val isSkeletonShown: LiveData<Boolean> = _isSkeletonShown

    private val _shareProduct = SingleLiveEvent<Product>()
    val shareProduct: LiveData<Product> = _shareProduct

    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    private val _exit = SingleLiveEvent<Unit>()
    val exit: LiveData<Unit> = _exit

    fun start(remoteProductId: Long) {
        loadProduct(remoteProductId)
    }

    fun onShareButtonClicked() {
        _shareProduct.value = product.value
    }

    override fun onCleared() {
        super.onCleared()

        productRepository.onCleanup()
    }

    private fun loadProduct(remoteProductId: Long) {
        getSiteSettings()?.let { currencyCode = it.currencyCode }
        getProductSiteSettings()?.let { settings ->
            weightUnit = settings.weightUnit
            dimensionUnit = settings.dimensionUnit
        }

        val shouldFetch = remoteProductId != this.remoteProductId
        this.remoteProductId = remoteProductId

        launch {
            val productInDb = productRepository.getProduct(remoteProductId)
            if (productInDb != null) {
                _product.value = productInDb
                if (shouldFetch) {
                    fetchProduct(remoteProductId)
                }
            } else {
                _isSkeletonShown.value = true
                fetchProduct(remoteProductId)
            }
            _isSkeletonShown.value = false
        }
    }

    private suspend fun fetchProduct(remoteProductId: Long) {
        if (networkStatus.isConnected()) {
            val fetchedProduct = productRepository.fetchProduct(remoteProductId)
            if (fetchedProduct != null) {
                _product.value = fetchedProduct
            } else {
                _showSnackbarMessage.value = R.string.product_detail_fetch_product_error
                _exit.call()
            }
        } else {
            _showSnackbarMessage.value = R.string.offline_error
            _isSkeletonShown.value = false
        }
    }

    fun formatCurrency(amount: BigDecimal): String {
        return currencyCode?.let {
            currencyFormatter.formatCurrency(amount, it)
        } ?: amount.toString()
    }

    private fun getSiteSettings(): WCSettingsModel? =
            wooCommerceStore.getSiteSettings(selectedSite.get())

    private fun getProductSiteSettings(): WCProductSettingsModel? =
            wooCommerceStore.getProductSettings(selectedSite.get())

    fun getWeightUnit() = weightUnit ?: ""
    fun getDimensionUnit() = dimensionUnit ?: ""
}
