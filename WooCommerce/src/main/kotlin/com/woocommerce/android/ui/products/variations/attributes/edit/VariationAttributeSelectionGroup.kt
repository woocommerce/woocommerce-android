package com.woocommerce.android.ui.products.variations.attributes.edit

import android.os.Parcelable
import com.woocommerce.android.model.ProductVariation.Option
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VariationAttributeSelectionGroup(
    val attributeName: String,
    val options: List<String>,
    var selectedOptionIndex: Int
): Parcelable {
    val selectedOption
        get() = options.getOrNull(selectedOptionIndex) ?: ""

    fun toVariantOption() = Option(
        attributeName = attributeName,
        optionChoice = selectedOption
    )
}
