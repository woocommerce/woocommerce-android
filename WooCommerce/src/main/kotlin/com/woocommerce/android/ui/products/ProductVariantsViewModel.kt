package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ProductVariantsViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val productVariantsRepository: ProductVariantsRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedState, dispatchers) {
    private var remoteProductId = 0L

    private val _productVariantList = MutableLiveData<List<ProductVariant>>()
    val productVariantList: LiveData<List<ProductVariant>> = _productVariantList

    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    fun start(remoteProductId: Long) {
        loadProductVariants(remoteProductId)
    }

    fun refreshProductVariants(remoteProductId: Long) {
        viewState = viewState.copy(isRefreshing = true)
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
                viewState = viewState.copy(isSkeletonShown = true)
                fetchProductVariants(remoteProductId)
            } else {
                _productVariantList.value = combineData(variantsInDb)
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
                triggerEvent(ShowSnackbar(string.product_variants_fetch_product_variants_error))
                triggerEvent(Exit)
            } else {
                _productVariantList.value = combineData(fetchedVariants)
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
        viewState = viewState.copy(
                isSkeletonShown = false,
                isRefreshing = false
        )
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

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val isRefreshing: Boolean? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductVariantsViewModel>
}
