package com.woocommerce.android.ui.products.variations.attributes.edit

import com.woocommerce.android.model.VariantOption

data class VariationAttributeSelectionGroup(
    val attributeName: String,
    val options: List<String>,
    var selectedOptionIndex: Int
) {
    val selectedOption
        get() = options.getOrNull(selectedOptionIndex) ?: ""

    fun toVariantOption() = VariantOption(
        id = null,
        name = attributeName,
        option = options[selectedOptionIndex]
    )
}
