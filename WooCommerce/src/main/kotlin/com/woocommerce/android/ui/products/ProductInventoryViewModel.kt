package com.woocommerce.android.ui.products

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProductInventoryViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val productRepository: ProductDetailRepository
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val SEARCH_TYPING_DELAY_MS = 500L
    }
    private val navArgs: ProductInventoryFragmentArgs by savedState.navArgs()
    private val isProduct = navArgs.requestCode == RequestCodes.PRODUCT_DETAIL_INVENTORY

    val viewStateData = LiveDataDelegate(
        savedState,
        ViewState(
            inventoryData = navArgs.inventoryData,
            isIndividualSaleSwitchVisible = isProduct,
            isStockManagementVisible = !isProduct || navArgs.productType != EXTERNAL && navArgs.productType != GROUPED,
            isStockStatusVisible = !isProduct || navArgs.productType != VARIABLE
        )
    )
    private var viewState by viewStateData

    private var skuVerificationJob: Job? = null
    private val originalSku = navArgs.sku
    private val originalInventoryData = navArgs.inventoryData

    val inventoryData
        get() = viewState.inventoryData

    private val hasChanges: Boolean
        get() = inventoryData != originalInventoryData

    /**
     * Called when user modifies the SKU field. Currently checks if the entered sku is available
     * in the local db. Only if it is not available, the API verification call is initiated.
     */
    fun onSkuChanged(sku: String) {
        // verify if the sku exists only if the text entered by the user does not match the sku stored locally
        if (sku.length > 2) {
            onDataChanged(sku = sku)

            if (sku == originalSku) {
                clearSkuError()
            } else {
                if (!productRepository.isSkuAvailableLocally(sku)) {
                    showSkuError()
                } else {
                    clearSkuError()
                }

                // cancel any existing verification search, then start a new one after a brief delay
                // so we don't actually perform the fetch until the user stops typing
                skuVerificationJob?.cancel()
                skuVerificationJob = launch {
                    delay(SEARCH_TYPING_DELAY_MS)

                    // only after the SKU is available remotely, reset the error if it's available locally, as well
                    // to avoid showing/hiding error message
                    productRepository.isSkuAvailableRemotely(sku)?.let { isRemotelyAvailable ->
                        if (isRemotelyAvailable) {
                            clearSkuError()
                        } else {
                            showSkuError()
                        }
                    }
                }
            }
        }
    }

    fun onDataChanged(
        sku: String? = inventoryData.sku,
        backorderStatus: ProductBackorderStatus? = inventoryData.backorderStatus,
        isSoldIndividually: Boolean? = inventoryData.isSoldIndividually,
        isStockManaged: Boolean? = inventoryData.isStockManaged,
        stockQuantity: Int? = inventoryData.stockQuantity,
        stockStatus: ProductStockStatus? = inventoryData.stockStatus
    ) {
        viewState = viewState.copy(
            inventoryData = InventoryData(
                sku = sku,
                backorderStatus = backorderStatus,
                isSoldIndividually = isSoldIndividually,
                isStockManaged = isStockManaged,
                stockQuantity = stockQuantity,
                stockStatus = stockStatus
            )
        )
    }

    fun onExit() {
        if (hasChanges && !hasSkuError()) {
            triggerEvent(ExitWithResult(inventoryData))
        } else {
            triggerEvent(Exit)
        }
    }

    private fun clearSkuError() {
        viewState = viewState.copy(skuErrorMessage = 0)
    }

    private fun showSkuError() {
        viewState = viewState.copy(skuErrorMessage = string.product_inventory_update_sku_error)
    }

    private fun hasSkuError() = viewState.skuErrorMessage != 0 && viewState.skuErrorMessage != null

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
    }

    @Parcelize
    data class ViewState(
        val inventoryData: InventoryData = InventoryData(),
        val skuErrorMessage: Int? = null,
        val isIndividualSaleSwitchVisible: Boolean? = null,
        val isStockStatusVisible: Boolean? = null,
        val isStockManagementVisible: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class InventoryData(
        val sku: String? = null,
        val isStockManaged: Boolean? = null,
        val isSoldIndividually: Boolean? = null,
        val stockStatus: ProductStockStatus? = null,
        val stockQuantity: Int? = null,
        val backorderStatus: ProductBackorderStatus? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductInventoryViewModel>
}
