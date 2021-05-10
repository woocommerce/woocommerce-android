package com.woocommerce.android.ui.products.variations.attributes.edit

import android.os.Parcelable
import com.woocommerce.android.model.VariantOption
import kotlinx.parcelize.Parcelize

@Parcelize
data class VariationAttributeSelectionGroup(
    private val id: Long,
    private var options: List<String>,
    private var noOptionSelected: Boolean = false,
    val attributeName: String,
    var selectedOptionIndex: Int
) : Parcelable {
    companion object {
        const val anySelectionOption = "Any"
    }

    val selectedOption
        get() = options.getOrNull(selectedOptionIndex) ?: ""

    val attributeOptions
        get() = options

    val isAnyOptionSelected
        get() = options.getOrNull(selectedOptionIndex) == anySelectionOption

    init {
        options.toMutableList().apply { add(anySelectionOption) }
            .let { options = it }
            .takeIf { noOptionSelected }
            ?.let { selectedOptionIndex = options.indexOf(anySelectionOption) }
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
