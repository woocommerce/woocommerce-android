package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R.string
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isInteger
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ProductInventoryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val productRepository: ProductDetailRepository,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {
    private val navArgs: ProductInventoryFragmentArgs by savedState.navArgs()
    private val isProduct = navArgs.requestCode == RequestCodes.PRODUCT_DETAIL_INVENTORY

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(
        savedState,
        ViewState(
            inventoryData = navArgs.inventoryData,
            isIndividualSaleSwitchVisible = isProduct,
            isStockManagementVisible = !isProduct || navArgs.productType != EXTERNAL && navArgs.productType != GROUPED,
            isStockStatusVisible = !isProduct || navArgs.productType != VARIABLE,

            // Stock quantity field is only editable if the value is whole decimal (e.g: 10.0).
            // Otherwise it is set to read-only, because the API doesn't support updating amount with non-zero
            // fractional yet
            isStockQuantityEditable = navArgs.inventoryData.stockQuantity?.isInteger()
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
                    delay(AppConstants.SEARCH_TYPING_DELAY_MS)

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
        stockQuantity: Double? = inventoryData.stockQuantity,
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
        analyticsTracker.track(
            AnalyticsEvent.PRODUCT_INVENTORY_SETTINGS_DONE_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to hasChanges)
        )
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
        val isStockManagementVisible: Boolean? = null,
        val isStockQuantityEditable: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class InventoryData(
        val sku: String? = null,
        val isStockManaged: Boolean? = null,
        val isSoldIndividually: Boolean? = null,
        val stockStatus: ProductStockStatus? = null,
        val stockQuantity: Double? = null,
        val backorderStatus: ProductBackorderStatus? = null
    ) : Parcelable
}
