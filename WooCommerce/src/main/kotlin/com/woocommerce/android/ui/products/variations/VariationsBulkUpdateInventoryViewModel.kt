package com.woocommerce.android.ui.products.variations

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.track
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class VariationsBulkUpdateInventoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val variationRepository: VariationRepository,
    private val dispatchers: CoroutineDispatchers,
) : ScopedViewModel(savedStateHandle) {
    private val args: VariationsBulkUpdateInventoryFragmentArgs by savedStateHandle.navArgs()
    private val data: InventoryUpdateData = args.inventoryUpdateData
    private val variationsToUpdate: List<ProductVariation> = args.inventoryUpdateData.variationsToUpdate

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        viewState = viewState.copy(
            variationsToUpdateCount = data.variationsToUpdate.size,
            stockQuantity = data.stockQuantity
        )
    }

    fun onDoneClicked() {
        track(AnalyticsEvent.PRODUCT_VARIANTS_BULK_UPDATE_STOCK_QUANTITY_DONE_TAPPED)
        viewState = viewState.copy(isProgressDialogShown = true)

        viewModelScope.launch(dispatchers.io) {
            val productId = variationsToUpdate.first().remoteProductId
            val variationsIds = variationsToUpdate.map { it.remoteVariationId }
            val result = variationRepository.bulkUpdateVariations(
                productId,
                variationsIds,
                stockQuantity = viewState.stockQuantity ?: 0.0
            )
            val snackText = if (result) {
                R.string.variations_bulk_update_stock_quantity_success
            } else {
                R.string.variations_bulk_update_error
            }

            withContext(dispatchers.main) {
                viewState = viewState.copy(isProgressDialogShown = false)
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(snackText))
                if (result) triggerEvent(MultiLiveEvent.Event.Exit)
            }
        }
    }

    fun onStockQuantityChanged(rawQuantity: String) {
        viewState = rawQuantity.toDoubleOrNull().let { quantity ->
            viewState.copy(stockQuantity = quantity, isDoneEnabled = quantity != null)
        }
    }

    @Parcelize
    data class ViewState(
        val variationsToUpdateCount: Int? = null,
        val stockQuantity: Double? = null,
        val stockQuantityGroupType: ValuesGroupType? = null,
        val isProgressDialogShown: Boolean = false,
        val isDoneEnabled: Boolean = true
    ) : Parcelable

    @Parcelize
    data class InventoryUpdateData(
        val variationsToUpdate: List<ProductVariation>,
        val stockQuantity: Double? = null,
    ) : Parcelable
}
