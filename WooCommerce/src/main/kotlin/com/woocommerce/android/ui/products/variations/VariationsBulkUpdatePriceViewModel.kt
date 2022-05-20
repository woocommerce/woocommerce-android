package com.woocommerce.android.ui.products.variations

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class VariationsBulkUpdatePriceViewModel(savedStateHandle: SavedStateHandle): ScopedViewModel(savedStateHandle) {
    fun onDoneClicked() {

    }

    fun onPriceEntered(price: BigDecimal?) {

    }

    @Parcelize
    data class PriceUpdateData(
        val variationsToUpdate: List<ProductVariation>,
        val priceType: PriceType,
    ): Parcelable

    enum class PriceType {
        Regular, Sale
    }
}
