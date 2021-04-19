package com.woocommerce.android.ui.products.variations.attributes.edit

import android.os.Parcelable
import com.woocommerce.android.R.string
import com.woocommerce.android.model.VariantOption
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

typealias StringResourceCreator = (Int) -> String

@Parcelize
data class VariationAttributeSelectionGroup(
    private val id: Long,
    private var options: List<String>,
    private var noOptionSelected: Boolean = false,
    private val resourceCreator: @RawValue StringResourceCreator,
    val attributeName: String,
    var selectedOptionIndex: Int
) : Parcelable {
    val selectedOption
        get() = options.getOrNull(selectedOptionIndex) ?: ""

    val attributeOptions
        get() = options

    private val anySelectionOption
        get() = "${resourceCreator(string.product_any_attribute_hint)} $attributeName"

    private val isAnyOptionSelected
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
