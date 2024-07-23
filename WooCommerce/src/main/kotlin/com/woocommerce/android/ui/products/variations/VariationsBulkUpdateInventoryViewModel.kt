package com.woocommerce.android.ui.products.variations

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class VariationsBulkUpdateInventoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val variationRepository: VariationRepository,
    dispatchers: CoroutineDispatchers,
) : VariationsBulkUpdateBaseViewModel(savedStateHandle, dispatchers) {
    private val args: VariationsBulkUpdateInventoryFragmentArgs by savedStateHandle.navArgs()
    private val data: InventoryUpdateData = args.inventoryUpdateData
    private val variationsToUpdate: List<ProductVariation> = args.inventoryUpdateData.variationsToUpdate

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        viewState = viewState.copy(
            variationsToUpdateCount = data.variationsToUpdate.size,
            stockQuantity = data.stockQuantity
        )
    }

    override fun getDoneClickedAnalyticsEvent(): AnalyticsEvent =
        AnalyticsEvent.PRODUCT_VARIANTS_BULK_UPDATE_STOCK_QUANTITY_DONE_TAPPED

    override fun getSnackbarSuccessMessageTextRes(): Int = R.string.variations_bulk_update_stock_quantity_success

    override suspend fun performBulkUpdate(): Boolean {
        val productId = variationsToUpdate.first().remoteProductId
        val variationsIds = variationsToUpdate.map { it.remoteVariationId }
        return variationRepository.bulkUpdateVariations(
            productId,
            variationsIds,
            stockQuantity = viewState.stockQuantity ?: 0.0
        )
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
        val isDoneEnabled: Boolean = true
    ) : Parcelable

    @Parcelize
    data class InventoryUpdateData(
        val variationsToUpdate: List<ProductVariation>,
        val stockQuantity: Double? = null,
    ) : Parcelable
}
