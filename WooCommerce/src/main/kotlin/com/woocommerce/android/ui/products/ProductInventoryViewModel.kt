package com.woocommerce.android.ui.products

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch

class ProductInventoryViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val selectedSite: SelectedSite,
    private val productRepository: ProductDetailRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState, dispatchers) {
    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    fun start(remoteProductId: Long) {
        viewState.productInventoryParameters?.let {
            loadProductInventoryParameters(it)
        } ?: loadProduct(remoteProductId)
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
    }

    /**
     * Update all product inventory fields that are edited by the user
     */
    fun updateProductInventoryDraft(
        sku: String? = null,
        manageStock: Boolean? = null,
        stockStatus: ProductStockStatus? = null,
        soldIndividually: Boolean? = null,
        stockQuantity: String? = null,
        backorderStatus: ProductBackorderStatus? = null
    ) {
        sku?.let {
            if (it != viewState.productInventoryParameters?.sku) {
                viewState.productInventoryParameters?.sku = it
            }
        }
        manageStock?.let {
            if (it != viewState.productInventoryParameters?.manageStock) {
                viewState.productInventoryParameters?.manageStock = it
            }
        }
        stockStatus?.let {
            if (it != viewState.productInventoryParameters?.stockStatus) {
                viewState.productInventoryParameters?.stockStatus = it
            }
        }
        soldIndividually?.let {
            if (it != viewState.productInventoryParameters?.soldIndividually) {
                viewState.productInventoryParameters?.soldIndividually = it
            }
        }
        stockQuantity?.let {
            val quantity = it.toInt()
            if (quantity != viewState.productInventoryParameters?.stockQuantity) {
                viewState.productInventoryParameters?.stockQuantity = quantity
            }
        }
        backorderStatus?.let {
            if (it != viewState.productInventoryParameters?.backOrderStatus) {
                viewState.productInventoryParameters?.backOrderStatus = it
            }
        }
        viewState = viewState.copy(isProductUpdated = true)
    }

    private fun loadProduct(remoteProductId: Long) {
        launch {
            val productInDb = productRepository.getProduct(remoteProductId)
            if (productInDb != null) {
                loadProductInventoryParameters(ProductInventoryParameters(
                        productInDb.sku, productInDb.manageStock, productInDb.stockStatus,
                        productInDb.stockQuantity, productInDb.backorderStatus, productInDb.soldIndividually
                ))
            }
        }
    }

    private fun loadProductInventoryParameters(productInventoryParameters: ProductInventoryParameters?) {
        viewState = viewState.copy(productInventoryParameters = productInventoryParameters)
    }

    @Parcelize
    data class ViewState(
        val isProductUpdated: Boolean? = null,
        val shouldShowDiscardDialog: Boolean = true,
        val productInventoryParameters: ProductInventoryParameters? = null
    ) : Parcelable

    @Parcelize
    data class ProductInventoryParameters(
        var sku: String,
        var manageStock: Boolean,
        var stockStatus: ProductStockStatus,
        var stockQuantity: Int,
        var backOrderStatus: ProductBackorderStatus,
        var soldIndividually: Boolean
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductInventoryViewModel>
}
