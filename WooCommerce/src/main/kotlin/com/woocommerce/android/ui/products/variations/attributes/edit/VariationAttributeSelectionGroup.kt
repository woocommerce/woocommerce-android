package com.woocommerce.android.ui.products.variations.attributes.edit

import android.os.Parcelable
import com.woocommerce.android.model.VariantOption
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VariationAttributeSelectionGroup(
    private val id: Long,
    val attributeName: String,
    private var options: List<String>,
    var selectedOptionIndex: Int,
    private var anySelectionEnabled: Boolean = false
) : Parcelable {
    val selectedOption
        get() = options.getOrNull(selectedOptionIndex) ?: ""

    val attributeOptions
        get() = options

    private val anySelectionOption = "Any $attributeName"

    private val isAnyOptionSelected
        get() = options.getOrNull(selectedOptionIndex) == anySelectionOption

    init {
        if (anySelectionEnabled) options.toMutableList()
            .apply {
                add(anySelectionOption)
                selectedOptionIndex = indexOf(anySelectionOption)
            }.also { options = it }
    }

    fun toVariantOption() =
        takeIf { isAnyOptionSelected.not() }?.let {
            VariantOption(
                id = id,
                name = attributeName,
                option = selectedOption
            )
        }
}
